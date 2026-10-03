package com.bodega.servlet;

import com.bodega.model.Usuario;
import com.bodega.service.PasswordResetService;
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
import java.util.Optional;

/**
 * Primer paso del restablecimiento autoservicio de contraseña: pide el usuario o correo de la
 * cuenta. Siempre responde igual (redirige al paso de código) exista o no esa cuenta, para no
 * revelar qué usuarios/correos están registrados — ver {@link PasswordResetService#solicitarCodigo}.
 */
@WebServlet("/recuperar-password")
public class RecuperarPasswordServlet extends HttpServlet {

    private final PasswordResetService passwordResetService = new PasswordResetService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        if (!LoginRateLimiter.permitirIntento(ip)) {
            req.setAttribute(Constantes.ATTR_ERROR, "Demasiados intentos desde esta red. Intente nuevamente en unos minutos.");
            req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password.jsp").forward(req, resp);
            return;
        }

        try {
            Optional<Usuario> usuarioOpt = passwordResetService.solicitarCodigo(req.getParameter("login"));
            if (usuarioOpt.isEmpty()) {
                req.setAttribute(Constantes.ATTR_ERROR,
                        "No encontramos una cuenta activa con correo registrado para ese usuario o correo.");
                req.getRequestDispatcher("/WEB-INF/views/auth/recuperar-password.jsp").forward(req, resp);
                return;
            }
            Usuario usuario = usuarioOpt.get();

            HttpSession session = req.getSession(true);
            session.setAttribute(Constantes.SESSION_RESET_USUARIO_ID, usuario.getId());
            session.setAttribute(Constantes.SESSION_RESET_EXPIRA,
                    LocalDateTime.now().plusMinutes(Constantes.RESET_PENDIENTE_MINUTOS));
            session.setAttribute(Constantes.SESSION_RESET_CORREO_MOSTRADO,
                    PasswordResetService.enmascararCorreo(usuario.getCorreo()));
            session.removeAttribute(Constantes.SESSION_RESET_VERIFICADO);

            resp.sendRedirect(req.getContextPath() + "/recuperar-password-codigo");
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la solicitud de recuperación de contraseña.", e);
        }
    }
}
