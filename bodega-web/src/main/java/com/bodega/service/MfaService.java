package com.bodega.service;

import com.bodega.dao.UsuarioDAO;
import com.bodega.model.Usuario;
import com.bodega.service.UsuarioService.ResultadoOperacion;
import com.bodega.util.CifradoUtil;
import com.bodega.util.SesionActivaRegistry;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;

/**
 * Autenticación multifactor (TOTP / RFC 6238, compatible con Google Authenticator).
 * <p>
 * Activación y baja son EXCLUSIVAS del rol ADMINISTRADOR (nunca autoservicio) — ver
 * {@code UsuarioServlet}, que ya está bajo el prefijo {@code /usuarios} restringido por
 * {@link com.bodega.filter.AuthorizationFilter}. El flujo es:
 * 1) el administrador activa el MFA de un usuario → se genera un secreto y queda "pendiente"
 *    (guardado cifrado, {@code mfa_habilitado = FALSE} todavía);
 * 2) se muestra el QR en el momento, para que el empleado lo escanee ahí mismo con su celular;
 * 3) el empleado dicta el primer código de 6 dígitos y el administrador lo confirma → recién
 *    ahí {@code mfa_habilitado = TRUE}.
 * <p>
 * El secreto se guarda siempre cifrado con {@link CifradoUtil} (AES-256-GCM), igual que
 * {@code clientes.direccion} — nunca en texto plano en la base de datos.
 */
public class MfaService {

    private static final String EMISOR = "BodegaTAMBITO";

    private final UsuarioDAO usuarioDAO;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());

    public MfaService() {
        this.usuarioDAO = new UsuarioDAO();
    }

    /** Datos necesarios para que el administrador muestre el QR de activación pendiente. */
    public static class DatosActivacionPendiente {
        public final String qrDataUri;
        public final String otpAuthUriManual;

        public DatosActivacionPendiente(String qrDataUri, String otpAuthUriManual) {
            this.qrDataUri = qrDataUri;
            this.otpAuthUriManual = otpAuthUriManual;
        }
    }

    /**
     * Inicia la activación de MFA para un usuario: genera un secreto TOTP nuevo, lo guarda
     * cifrado en estado pendiente (no habilitado todavía) y fuerza el cierre de la sesión
     * activa de ese usuario si la tiene abierta en este momento. El propio usuario verá el
     * QR y lo confirmará recién cuando vuelva a iniciar sesión (ver {@code AutenticacionService}
     * / {@code LoginMfaServlet}) — el administrador nunca ve el código QR de otra persona.
     * Si el usuario ya tenía MFA activo, se rechaza — primero hay que desactivarlo
     * explícitamente para generar uno nuevo.
     *
     * @param usuarioId id del usuario al que se le activa el MFA
     * @param adminId   id del administrador que ejecuta la activación
     * @throws SQLException si falla la base de datos
     */
    public ResultadoOperacion iniciarActivacion(int usuarioId, int adminId) throws SQLException {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return new ResultadoOperacion(false, "Usuario no encontrado.");
        }
        if (usuarioOpt.get().isMfaHabilitado()) {
            return new ResultadoOperacion(false,
                    "Este usuario ya tiene el MFA activado. Desactívelo primero si desea generar un nuevo código QR.");
        }
        String secreto = secretGenerator.generate();
        usuarioDAO.guardarSecretoMfaPendiente(usuarioId, CifradoUtil.cifrar(secreto), adminId);
        SesionActivaRegistry.invalidarSiExiste(usuarioId);
        return new ResultadoOperacion(true,
                "MFA activado. La próxima vez que este usuario inicie sesión, deberá escanear el código QR "
                        + "y confirmarlo con Google Authenticator antes de poder continuar.");
    }

    /**
     * @param usuarioId id del usuario
     * @return los datos para mostrar el QR (imagen + URI manual), si hay una activación
     *         pendiente de confirmar para ese usuario; {@link Optional#empty()} si no hay
     *         ninguna pendiente o si el MFA ya está confirmado
     * @throws SQLException si falla la base de datos
     */
    public Optional<DatosActivacionPendiente> obtenerActivacionPendiente(int usuarioId) throws SQLException {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = usuarioOpt.get();
        if (usuario.isMfaHabilitado() || usuario.getMfaSecret() == null) {
            return Optional.empty();
        }
        String secretoPlano = CifradoUtil.descifrar(usuario.getMfaSecret());
        String otpAuthUri = construirOtpAuthUri(usuario.getCorreo(), secretoPlano);
        try {
            return Optional.of(new DatosActivacionPendiente(generarQrDataUri(otpAuthUri), otpAuthUri));
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Error al generar el código QR de MFA.", e);
        }
    }

    /**
     * Confirma la activación validando el primer código de 6 dígitos, ingresado por el propio
     * usuario en su siguiente login (ver {@code LoginMfaServlet}). Si el código no calza, el
     * secreto pendiente se conserva (no se invalida por un simple error de tipeo) para poder
     * reintentar sin volver a escanear el QR.
     *
     * @param usuarioId id del usuario
     * @param codigo    código de 6 dígitos mostrado en Google Authenticator
     * @throws SQLException si falla la base de datos
     */
    public ResultadoOperacion confirmarActivacion(int usuarioId, String codigo) throws SQLException {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty() || usuarioOpt.get().getMfaSecret() == null) {
            return new ResultadoOperacion(false, "No hay una activación de MFA pendiente para este usuario.");
        }
        Usuario usuario = usuarioOpt.get();
        String secretoPlano = CifradoUtil.descifrar(usuario.getMfaSecret());

        if (codigo == null || codigo.isBlank() || !codeVerifier.isValidCode(secretoPlano, codigo.trim())) {
            return new ResultadoOperacion(false,
                    "El código ingresado no es válido. Verifique la hora del celular e intente nuevamente.");
        }
        usuarioDAO.confirmarMfa(usuarioId);
        return new ResultadoOperacion(true, "MFA activado correctamente.");
    }

    /**
     * Desactiva el MFA de un usuario, borrando su secreto por completo. Solo el administrador
     * puede invocar esta operación (verificado por estar bajo {@code /usuarios}).
     *
     * @param usuarioId id del usuario
     * @throws SQLException si falla la base de datos
     */
    public ResultadoOperacion desactivar(int usuarioId) throws SQLException {
        usuarioDAO.desactivarMfa(usuarioId);
        return new ResultadoOperacion(true, "MFA desactivado correctamente para este usuario.");
    }

    /**
     * Verifica el código de 6 dígitos ingresado en el segundo paso del login (una vez que la
     * contraseña ya fue validada). Usado por {@code LoginServlet}/{@code AutenticacionService}.
     *
     * @param usuario usuario con MFA habilitado (su {@code mfaSecret} ya viene cifrado)
     * @param codigo  código ingresado por el usuario
     * @return {@code true} si el código es válido para el secreto de este usuario
     */
    public boolean verificarCodigoLogin(Usuario usuario, String codigo) {
        if (usuario.getMfaSecret() == null || codigo == null || codigo.isBlank()) {
            return false;
        }
        String secretoPlano = CifradoUtil.descifrar(usuario.getMfaSecret());
        return codeVerifier.isValidCode(secretoPlano, codigo.trim());
    }

    private String construirOtpAuthUri(String correo, String secretoBase32) {
        QrData data = new QrData.Builder()
                .label(correo)
                .secret(secretoBase32)
                .issuer(EMISOR)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    /**
     * Genera el QR como PNG codificado en Base64 (data URI), usando directamente
     * {@code com.google.zxing:core} (ya es dependencia del proyecto para otros usos) en vez
     * de sumar una librería nueva solo para esto. No usa {@code zxing:javase} (no está en el
     * proyecto): la conversión de {@link BitMatrix} a imagen se hace a mano.
     */
    private String generarQrDataUri(String otpAuthUri) throws WriterException, IOException {
        BitMatrix matrix = new QRCodeWriter().encode(otpAuthUri, BarcodeFormat.QR_CODE, 260, 260);
        int ancho = matrix.getWidth();
        int alto = matrix.getHeight();
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                imagen.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "PNG", salida);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(salida.toByteArray());
    }
}
