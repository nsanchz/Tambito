package com.bodega.service;

import com.bodega.dao.UsuarioDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Usuario;
import com.bodega.service.UsuarioService.ResultadoOperacion;
import com.bodega.util.Constantes;
import com.bodega.util.PasswordUtil;
import com.bodega.util.SesionActivaRegistry;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Restablecimiento autoservicio de contraseña ("olvidé mi contraseña") mediante un código de
 * 6 dígitos enviado por correo — complementa a {@code UsuarioService.restablecerPassword},
 * que sigue siendo la vía con la que un ADMINISTRADOR restablece la clave de otro usuario
 * sin pasar por correo.
 * <p>
 * Flujo en 3 pasos (ver {@code RecuperarPasswordServlet} / {@code RecuperarPasswordCodigoServlet}
 * / {@code RecuperarPasswordNuevaServlet}), cada uno validado también en el servidor para que
 * no pueda saltarse ningún paso escribiendo la URL directamente:
 * 1) {@link #solicitarCodigo}: genera y envía el código, SIEMPRE con la misma respuesta
 *    genérica, exista o no la cuenta (para no revelar qué usuarios/correos están registrados).
 * 2) {@link #verificarCodigo}: valida el código contra su hash, con límite de intentos.
 * 3) {@link #restablecer}: aplica la nueva contraseña, solo si el paso 2 ya se completó.
 */
public class PasswordResetService {

    /** Centinela usado cuando el usuario/correo ingresado no existe, para no filtrar esa información. */
    public static final int USUARIO_INEXISTENTE = -1;

    private final UsuarioDAO usuarioDAO;
    private final EmailService emailService;

    public PasswordResetService() {
        this.usuarioDAO = new UsuarioDAO();
        this.emailService = new EmailService();
    }

    /**
     * Inicia el flujo: valida que {@code login} corresponda a una cuenta activa con correo
     * registrado, genera un código de 6 dígitos, lo guarda hasheado con vencimiento, y lo
     * envía por correo. A diferencia de un flujo típico "olvidé mi contraseña", esta variante
     * SÍ confirma explícitamente si la cuenta existe (devuelve vacío si no) y muestra a qué
     * correo se envió el código — decisión explícita del negocio, priorizando que el usuario
     * confirme de inmediato si escribió bien su usuario, por sobre ocultar qué cuentas existen.
     * Cualquier fallo de envío de correo se registra en el log pero nunca se propaga — el
     * flujo de recuperación nunca debe reventar porque el SMTP esté caído en ese momento.
     *
     * @param login nombre de usuario o correo ingresado en el formulario
     * @return el usuario (con el código ya enviado) si la cuenta existe, está activa y tiene
     *         correo registrado; {@link Optional#empty()} en cualquier otro caso
     * @throws SQLException si falla la base de datos
     */
    public Optional<Usuario> solicitarCodigo(String login) throws SQLException {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorUsuarioOCorreo(login.trim());
        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }
        Usuario usuario = usuarioOpt.get();
        if (usuario.getEstado() != EstadoCuenta.ACTIVO
                || usuario.getCorreo() == null || usuario.getCorreo().isBlank()) {
            return Optional.empty();
        }

        String codigo = generarCodigoNumerico();
        LocalDateTime expira = LocalDateTime.now().plusMinutes(Constantes.CODIGO_RESET_VALIDO_MINUTOS);
        usuarioDAO.guardarCodigoRecuperacion(usuario.getId(), PasswordUtil.hash(codigo), expira);

        try {
            emailService.enviarCodigoRecuperacionPassword(usuario.getCorreo(), usuario.getNombreUsuario(),
                    codigo, Constantes.CODIGO_RESET_VALIDO_MINUTOS);
        } catch (Exception correoFallido) {
            System.err.println("No se pudo enviar el código de recuperación de contraseña: " + correoFallido.getMessage());
        }
        return Optional.of(usuario);
    }

    /**
     * Enmascara un correo para mostrarlo en pantalla sin exponerlo por completo (ej.
     * {@code "neyder.sanchez@gmail.com"} → {@code "n************z@gmail.com"}), usado por la
     * pantalla de verificación para confirmar a dónde se envió el código.
     *
     * @param correo correo completo
     * @return el correo con la parte local enmascarada (primer y último carácter visibles) y
     *         el dominio sin modificar; el propio {@code correo} tal cual si no tiene el
     *         formato esperado
     */
    public static String enmascararCorreo(String correo) {
        if (correo == null) {
            return "";
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 1) {
            return correo;
        }
        String local = correo.substring(0, arroba);
        String dominio = correo.substring(arroba);
        if (local.length() <= 2) {
            return local.charAt(0) + "*".repeat(local.length() - 1) + dominio;
        }
        return local.charAt(0) + "*".repeat(local.length() - 2) + local.charAt(local.length() - 1) + dominio;
    }

    /**
     * Verifica el código de 6 dígitos ingresado en el segundo paso.
     *
     * @param usuarioId id pendiente guardado en sesión ({@link #USUARIO_INEXISTENTE} para una
     *                  solicitud que nunca correspondió a una cuenta real: siempre falla,
     *                  manteniendo la misma respuesta genérica que vería un usuario real)
     * @param codigo    código de 6 dígitos ingresado
     * @return resultado exitoso si el código es válido y no venció; de lo contrario, un
     *         mensaje de error genérico (nunca distingue "no existe" de "código incorrecto")
     * @throws SQLException si falla la base de datos
     */
    public ResultadoOperacion verificarCodigo(int usuarioId, String codigo) throws SQLException {
        String mensajeGenerico = "El código ingresado no es válido o ya venció. Solicite uno nuevo.";
        if (usuarioId == USUARIO_INEXISTENTE || codigo == null || codigo.isBlank()) {
            return new ResultadoOperacion(false, mensajeGenerico);
        }

        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty()) {
            return new ResultadoOperacion(false, mensajeGenerico);
        }
        Usuario usuario = usuarioOpt.get();

        if (!usuario.tieneCodigoRecuperacionVigente()) {
            return new ResultadoOperacion(false, mensajeGenerico);
        }
        if (usuario.getResetPasswordIntentos() >= Constantes.MAX_INTENTOS_CODIGO_RESET) {
            usuarioDAO.limpiarRecuperacion(usuarioId);
            return new ResultadoOperacion(false,
                    "Demasiados intentos incorrectos. Por seguridad, solicite un código nuevo.");
        }

        if (!PasswordUtil.verificar(codigo.trim(), usuario.getResetPasswordCodigoHash())) {
            usuarioDAO.incrementarIntentosRecuperacion(usuarioId);
            return new ResultadoOperacion(false, mensajeGenerico);
        }

        usuarioDAO.marcarRecuperacionVerificada(usuarioId);
        return new ResultadoOperacion(true, "Código verificado correctamente.");
    }

    /**
     * Aplica la nueva contraseña — solo si {@link #verificarCodigo} ya marcó el código como
     * verificado para este usuario (si no, se rechaza, para que no se pueda saltar el paso 2
     * enviando directamente un POST a este paso). Cierra cualquier sesión activa que el
     * usuario tuviera abierta en otro dispositivo, igual que hace un administrador al
     * restablecer la clave de alguien manualmente.
     *
     * @param usuarioId      id del usuario (debe tener {@code reset_password_verificado = true})
     * @param passwordNueva  nueva contraseña en texto plano
     * @param confirmar      confirmación de la nueva contraseña
     * @return resultado exitoso, o un mensaje de error de negocio si no
     * @throws SQLException si falla la base de datos
     */
    public ResultadoOperacion restablecer(int usuarioId, String passwordNueva, String confirmar) throws SQLException {
        if (usuarioId == USUARIO_INEXISTENTE) {
            return new ResultadoOperacion(false, "La sesión de recuperación expiró. Vuelva a solicitar el código.");
        }
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorId(usuarioId);
        if (usuarioOpt.isEmpty() || !usuarioOpt.get().isResetPasswordVerificado()) {
            return new ResultadoOperacion(false, "La sesión de recuperación expiró. Vuelva a solicitar el código.");
        }
        if (passwordNueva == null || !passwordNueva.equals(confirmar)) {
            return new ResultadoOperacion(false, "Las contraseñas ingresadas no coinciden.");
        }
        if (!PasswordUtil.cumplePoliticaMinima(passwordNueva)) {
            return new ResultadoOperacion(false,
                    "La contraseña debe tener al menos 8 caracteres, con mayúscula, minúscula y número.");
        }

        usuarioDAO.actualizarPasswordPropia(usuarioId, PasswordUtil.hash(passwordNueva));
        usuarioDAO.limpiarRecuperacion(usuarioId);
        SesionActivaRegistry.invalidarSiExiste(usuarioId);
        return new ResultadoOperacion(true, "Contraseña restablecida correctamente. Ya puede iniciar sesión.", usuarioId);
    }

    /** @return un código numérico de 6 dígitos (000000-999999), con ceros a la izquierda si corresponde */
    private String generarCodigoNumerico() {
        int numero = new SecureRandom().nextInt(1_000_000);
        return String.format("%06d", numero);
    }
}
