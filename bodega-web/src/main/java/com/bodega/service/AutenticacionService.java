package com.bodega.service;

import com.bodega.dao.UsuarioDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Usuario;
import com.bodega.util.PasswordUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Lógica de negocio de autenticación: validación de credenciales, control de intentos
 * fallidos y bloqueo temporal de cuentas.
 */
public class AutenticacionService {

    private final UsuarioDAO usuarioDAO;
    private final ConfiguracionService configuracionService;

    public AutenticacionService() {
        this.usuarioDAO = new UsuarioDAO();
        this.configuracionService = new ConfiguracionService();
    }

    /** Resultado de un intento de autenticación. */
    public enum ResultadoLogin {
        EXITO,
        CREDENCIALES_INVALIDAS,
        CUENTA_INACTIVA,
        CUENTA_BLOQUEADA
    }

    /** Resultado completo de un intento de login: el veredicto y, si se encontró, el usuario. */
    public static class RespuestaLogin {
        public final ResultadoLogin resultado;
        public final Usuario usuario;

        public RespuestaLogin(ResultadoLogin resultado, Usuario usuario) {
            this.resultado = resultado;
            this.usuario = usuario;
        }
    }

    /**
     * Valida credenciales de login contra la base de datos, gestionando el conteo de
     * intentos fallidos y el bloqueo temporal de la cuenta según los parámetros configurables
     * {@link ConfiguracionService#CLAVE_MAX_INTENTOS_LOGIN} y
     * {@link ConfiguracionService#CLAVE_MINUTOS_BLOQUEO_LOGIN} (editables desde Configuración
     * del Sistema, con 3 intentos / 15 minutos como valores por defecto).
     *
     * @param login         nombre de usuario o correo ingresado
     * @param passwordPlano contraseña en texto plano ingresada (se compara con BCrypt, nunca en texto plano)
     * @return el resultado del intento: EXITO, CREDENCIALES_INVALIDAS, CUENTA_INACTIVA o CUENTA_BLOQUEADA,
     *         junto con el usuario encontrado (si lo hay); en caso de bloqueo, {@code usuario.getBloqueadoHasta()}
     *         trae el instante exacto de desbloqueo para que el llamador pueda mostrarlo al usuario
     * @throws SQLException si falla alguna operación de base de datos
     */
    public RespuestaLogin autenticar(String login, String passwordPlano) throws SQLException {
        Optional<Usuario> usuarioOpt = usuarioDAO.buscarPorUsuarioOCorreo(login);

        if (usuarioOpt.isEmpty()) {
            return new RespuestaLogin(ResultadoLogin.CREDENCIALES_INVALIDAS, null);
        }

        Usuario usuario = usuarioOpt.get();

        if (usuario.isBloqueado()) {
            return new RespuestaLogin(ResultadoLogin.CUENTA_BLOQUEADA, usuario);
        }

        if (usuario.getEstado() == EstadoCuenta.INACTIVO) {
            return new RespuestaLogin(ResultadoLogin.CUENTA_INACTIVA, usuario);
        }

        boolean claveValida = PasswordUtil.verificar(passwordPlano, usuario.getPasswordHash());

        if (!claveValida) {
            int intentos = usuario.getIntentosFallidos() + 1;
            LocalDateTime bloqueadoHasta = null;
            ResultadoLogin resultado = ResultadoLogin.CREDENCIALES_INVALIDAS;

            if (intentos >= configuracionService.obtenerMaxIntentosLogin()) {
                bloqueadoHasta = LocalDateTime.now().plusMinutes(configuracionService.obtenerMinutosBloqueoLogin());
                resultado = ResultadoLogin.CUENTA_BLOQUEADA;
            }
            usuarioDAO.registrarIntentoFallido(usuario.getId(), intentos, bloqueadoHasta);
            usuario.setBloqueadoHasta(bloqueadoHasta);
            return new RespuestaLogin(resultado, usuario);
        }

        usuarioDAO.registrarAccesoExitoso(usuario.getId());
        return new RespuestaLogin(ResultadoLogin.EXITO, usuario);
    }
}
