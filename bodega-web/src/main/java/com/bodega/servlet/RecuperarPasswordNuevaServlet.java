package com.bodega.servlet;

import com.bodega.service.AuditoriaService;
import com.bodega.service.PasswordResetService;
import com.bodega.service.UsuarioService.ResultadoOperacion;
import com.bodega.util.Constantes;
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
 * Tercer y último paso: elegir la nueva contraseña. Solo alcanzable si el paso anterior
 * ({@link RecuperarPasswordCodigoServlet}) ya marcó la sesión como verificada — no se puede
 * saltar directamente aquí con la URL.
 */
@WebServlet("/recuperar-password-nueva")
public class RecuperarPasswordNuevaServlet extends HttpServlet {

    private final PasswordResetService passwordResetService = new PasswordResetService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (obtenerUsuarioVerificado(req) == null) {
            resp.sendRedirect(req.getContextPath() + "/recuperar-password");
            return;
        }
        req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password-nueva.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer usuarioId = obtenerUsuarioVerificado(req);
        if (usuarioId == null) {
            resp.sendRedirect(req.getContextPath() + "/recuperar-password");
            return;
        }

        try {
            ResultadoOperacion resultado = passwordResetService.restablecer(
                    usuarioId, req.getParameter("passwordNueva"), req.getParameter("confirmarPassword"));

            if (resultado.exitoso) {
                auditoriaService.registrar(usuarioId, "PASSWORD_RESTABLECIDA_AUTOSERVICIO", "USUARIO", usuarioId,
                        "El usuario restableció su propia contraseña mediante un código de verificación enviado por correo.",
                        req.getRemoteAddr());

                HttpSession session = req.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                resp.sendRedirect(req.getContextPath() + "/login?reestablecida=1");
                return;
            }

            req.setAttribute(Constantes.ATTR_ERROR, resultado.mensaje);
            req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password-nueva.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al restablecer la contraseña.", e);
        }
    }

    /**
     * @return el id del usuario con el código ya verificado en este flujo, o {@code null} si
     *         no hay sesión pendiente vigente o si todavía no pasó por el paso de verificación
     */
    private Integer obtenerUsuarioVerificado(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object usuarioId = session.getAttribute(Constantes.SESSION_RESET_USUARIO_ID);
        Object expira = session.getAttribute(Constantes.SESSION_RESET_EXPIRA);
        Object verificado = session.getAttribute(Constantes.SESSION_RESET_VERIFICADO);
        if (usuarioId == null || !(expira instanceof LocalDateTime) || !Boolean.TRUE.equals(verificado)) {
            return null;
        }
        if (LocalDateTime.now().isAfter((LocalDateTime) expira)) {
            session.removeAttribute(Constantes.SESSION_RESET_USUARIO_ID);
            session.removeAttribute(Constantes.SESSION_RESET_EXPIRA);
            session.removeAttribute(Constantes.SESSION_RESET_VERIFICADO);
            return null;
        }
        return (Integer) usuarioId;
    }
}
