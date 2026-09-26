package com.bodega.servlet;

import com.bodega.model.Usuario;
import com.bodega.service.AuditoriaService;
import com.bodega.service.ConfiguracionService;
import com.bodega.util.Constantes;
import com.bodega.util.SesionActivaRegistry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Apertura de la sesión "completa" (autenticado) tras un login exitoso — compartida entre
 * {@link LoginServlet} (login directo, sin MFA) y {@link LoginMfaServlet} (tras completar el
 * segundo factor), para que ambos caminos construyan la sesión de forma idéntica.
 */
final class LoginSessionHelper {

    private static final AuditoriaService auditoriaService = new AuditoriaService();
    private static final ConfiguracionService configuracionService = new ConfiguracionService();

    private LoginSessionHelper() {
    }

    /**
     * @param req        petición HTTP (se usa para crear la sesión)
     * @param usuario    usuario ya completamente autenticado (contraseña, y MFA si aplica)
     * @param terminalId terminal seleccionado en el formulario de login
     * @param nombreEmpresa nombre de la empresa a guardar en sesión (para el layout de las vistas)
     * @param ip         IP de origen, para el registro de auditoría
     */
    static void abrirSesionCompleta(HttpServletRequest req, Usuario usuario, String terminalId,
                                     String nombreEmpresa, String ip) {
        HttpSession session = req.getSession(true);
        session.removeAttribute(Constantes.SESSION_MFA_PENDIENTE_USUARIO_ID);
        session.removeAttribute(Constantes.SESSION_MFA_PENDIENTE_EXPIRA);
        session.removeAttribute(Constantes.SESSION_MFA_ENROLANDO);
        session.setAttribute(Constantes.SESSION_USUARIO_ID, usuario.getId());
        session.setAttribute(Constantes.SESSION_USUARIO_NOMBRE, usuario.getNombreCompleto());
        session.setAttribute(Constantes.SESSION_USUARIO_ROL, usuario.getRol().name());
        session.setAttribute(Constantes.SESSION_TERMINAL_ID, terminalId);
        session.setAttribute(Constantes.SESSION_NOMBRE_EMPRESA, nombreEmpresa);
        SesionActivaRegistry.registrar(usuario.getId(), session);

        // Timeout de sesión diferenciado por rol (ver Configuración del Sistema): el valor
        // fijo de web.xml es solo el máximo por defecto; aquí se ajusta por sesión según el
        // rol real del usuario que acaba de autenticarse.
        try {
            int minutos = configuracionService.obtenerTimeoutSesionMinutos(usuario.getRol());
            session.setMaxInactiveInterval(minutos * 60);
        } catch (SQLException e) {
            // Si falla la consulta del parámetro, se conserva el timeout por defecto de web.xml.
        }

        auditoriaService.registrar(usuario.getId(), "LOGIN_EXITOSO", "USUARIO",
                usuario.getId(), "Inicio de sesión desde terminal " + terminalId, ip);
    }

    /** Redirige a Mi Perfil (si debe cambiar contraseña) o al Panel de Control, tras abrir sesión. */
    static void redirigirTrasLogin(HttpServletRequest req, HttpServletResponse resp, Usuario usuario)
            throws IOException {
        if (usuario.isDebeCambiarPassword()) {
            resp.sendRedirect(req.getContextPath() + "/perfil?cambioObligatorio=true");
        } else {
            resp.sendRedirect(req.getContextPath() + "/panel-de-control");
        }
    }
}
