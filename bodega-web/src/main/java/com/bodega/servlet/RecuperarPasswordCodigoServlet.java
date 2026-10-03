package com.bodega.servlet;

import com.bodega.service.PasswordResetService;
import com.bodega.service.UsuarioService.ResultadoOperacion;
import com.bodega.util.Constantes;
import com.bodega.util.LoginRateLimiter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Segundo paso: verifica el código de 6 dígitos enviado por correo en
 * {@link RecuperarPasswordServlet}. Solo alcanzable con una sesión "pendiente de recuperación"
 * vigente (dejada por el paso anterior) — sin eso, redirige de vuelta al paso 1, igual que
 * {@code LoginMfaServlet} hace con la sesión pendiente de MFA.
 */
@WebServlet("/recuperar-password-codigo")
public class RecuperarPasswordCodigoServlet extends HttpServlet {

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (obtenerUsuarioPendiente(req) == null) {
            resp.sendRedirect(req.getContextPath() + "/recuperar-password");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password-codigo.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        Integer usuarioId = obtenerUsuarioPendiente(req);
        if (usuarioId == null) {
            resp.sendRedirect(req.getContextPath() + "/recuperar-password");
            return;
        }

        if (!LoginRateLimiter.permitirIntento(ip)) {
            mostrarError(req, resp, "Demasiados intentos desde esta red. Intente nuevamente en unos minutos.");
            return;
        }

        try {
            ResultadoOperacion resultado = passwordResetService.verificarCodigo(usuarioId, req.getParameter("codigo"));
            if (resultado.exitoso) {
                req.getSession().setAttribute(Constantes.SESSION_RESET_VERIFICADO, true);
                resp.sendRedirect(req.getContextPath() + "/recuperar-password-nueva");
                return;
            }
            mostrarError(req, resp, resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al verificar el código de recuperación de contraseña.", e);
        }
    }

    /**
     * @return el id pendiente de verificación guardado en sesión (puede ser
     *         {@link PasswordResetService#USUARIO_INEXISTENTE}, una solicitud que nunca
     *         correspondió a una cuenta real), o {@code null} si no hay ninguna sesión
     *         pendiente vigente
     */
    private Integer obtenerUsuarioPendiente(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object usuarioId = session.getAttribute(Constantes.SESSION_RESET_USUARIO_ID);
        Object expira = session.getAttribute(Constantes.SESSION_RESET_EXPIRA);
        if (usuarioId == null || !(expira instanceof LocalDateTime)) {
            return null;
        }
        if (LocalDateTime.now().isAfter((LocalDateTime) expira)) {
            session.removeAttribute(Constantes.SESSION_RESET_USUARIO_ID);
            session.removeAttribute(Constantes.SESSION_RESET_EXPIRA);
            session.removeAttribute(Constantes.SESSION_RESET_VERIFICADO);
            session.removeAttribute(Constantes.SESSION_RESET_CORREO_MOSTRADO);
            return null;
        }
        return (Integer) usuarioId;
    }

    private void mostrarError(HttpServletRequest req, HttpServletResponse resp, String mensaje)
            throws ServletException, IOException {
        req.setAttribute(Constantes.ATTR_ERROR, mensaje);
        req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password-codigo.jsp").forward(req, resp);
    }
}
