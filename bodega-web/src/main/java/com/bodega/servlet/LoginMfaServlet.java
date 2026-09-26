package com.bodega.servlet;

import com.bodega.model.Usuario;
import com.bodega.service.AuditoriaService;
import com.bodega.service.MfaService;
import com.bodega.service.MfaService.DatosActivacionPendiente;
import com.bodega.service.UsuarioService;
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
 * Segundo paso del login cuando el usuario tiene MFA activo: pide el código de 6 dígitos de
 * Google Authenticator. Solo es alcanzable tras {@link LoginServlet} haber dejado una sesión
 * "pendiente de MFA" ({@link Constantes#SESSION_MFA_PENDIENTE_USUARIO_ID}); sin eso, redirige
 * de vuelta a {@code /login} — esta ruta está en {@code AuthenticationFilter.RUTAS_PUBLICAS}
 * porque en este punto todavía NO existe una sesión autenticada completa.
 */
@WebServlet("/login-mfa")
public class LoginMfaServlet extends HttpServlet {

    /** Tras esta cantidad de códigos incorrectos en la misma sesión pendiente, se obliga a reiniciar el login. */
    private static final int MAX_INTENTOS_EN_SESION = Constantes.MAX_INTENTOS_MFA_FALLIDOS;
    private static final String ATTR_INTENTOS_MFA = "intentosMfaFallidos";

    private final MfaService mfaService = new MfaService();
    private final UsuarioService usuarioService = new UsuarioService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer usuarioId = obtenerUsuarioPendiente(req);
        if (usuarioId == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        if (esEnrolamiento(req)) {
            try {
                Optional<DatosActivacionPendiente> datos = mfaService.obtenerActivacionPendiente(usuarioId);
                if (datos.isEmpty()) {
                    // El administrador desactivó/canceló la activación mientras tanto, o ya se
                    // confirmó desde otra pestaña: no hay nada pendiente que enrolar.
                    req.getSession().invalidate();
                    resp.sendRedirect(req.getContextPath() + "/login");
                    return;
                }
                req.setAttribute("mfaQrDataUri", datos.get().qrDataUri);
                req.setAttribute("mfaOtpAuthUri", datos.get().otpAuthUriManual);
            } catch (SQLException e) {
                throw new ServletException("Error al preparar el enrolamiento de MFA.", e);
            }
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/login-mfa.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String ip = req.getRemoteAddr();
        Integer usuarioId = obtenerUsuarioPendiente(req);

        if (usuarioId == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }

        // Mismo limitador de intentos por IP que ya protege /login, para que el paso de MFA
        // no se vuelva un segundo endpoint fuerza-bruteable sin control.
        if (!LoginRateLimiter.permitirIntento(ip)) {
            mostrarError(req, resp, "Demasiados intentos desde esta red. Intente nuevamente en unos minutos.");
            return;
        }

        HttpSession session = req.getSession(true);
        String codigo = req.getParameter("codigo");
        boolean enrolando = esEnrolamiento(req);

        try {
            Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(usuarioId);
            if (usuarioOpt.isEmpty() || (enrolando ? usuarioOpt.get().getMfaSecret() == null : !usuarioOpt.get().isMfaHabilitado())) {
                session.invalidate();
                resp.sendRedirect(req.getContextPath() + "/login");
                return;
            }
            Usuario usuario = usuarioOpt.get();

            boolean codigoValido;
            if (enrolando) {
                // Confirma la activación: mismo secreto pendiente que generó el administrador,
                // pero validado y confirmado por el propio usuario en este momento.
                var resultado = mfaService.confirmarActivacion(usuarioId, codigo);
                codigoValido = resultado.exitoso;
            } else {
                codigoValido = mfaService.verificarCodigoLogin(usuario, codigo);
            }

            if (codigoValido) {
                if (enrolando) {
                    auditoriaService.registrar(usuario.getId(), "MFA_CONFIRMAR", "USUARIO",
                            usuario.getId(), "El usuario confirmó su propia activación de MFA en el login", ip);
                }
                String terminalId = (String) session.getAttribute(Constantes.SESSION_TERMINAL_ID);
                String nombreEmpresa = obtenerNombreEmpresa(req);
                LoginSessionHelper.abrirSesionCompleta(req, usuario, terminalId, nombreEmpresa, ip);
                LoginSessionHelper.redirigirTrasLogin(req, resp, usuario);
                return;
            }

            auditoriaService.registrar(usuario.getId(), "LOGIN_MFA_FALLIDO", "USUARIO",
                    usuario.getId(), "Código MFA incorrecto en el intento de inicio de sesión", ip);

            int intentos = incrementarIntentos(session);
            if (intentos >= MAX_INTENTOS_EN_SESION) {
                session.invalidate();
                req.setAttribute(Constantes.ATTR_ERROR,
                        "Demasiados códigos incorrectos. Por seguridad, debe iniciar sesión nuevamente.");
                req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
                return;
            }

            if (enrolando) {
                // Reintento: el secreto pendiente se conserva, se vuelve a mostrar el mismo QR.
                req.setAttribute(Constantes.ATTR_ERROR, "Código incorrecto. Verifique la hora de su celular e intente nuevamente.");
                doGet(req, resp);
                return;
            }
            mostrarError(req, resp, "Código incorrecto. Verifique la hora de su celular e intente nuevamente.");
        } catch (SQLException e) {
            throw new ServletException("Error al verificar el código MFA.", e);
        }
    }

    /** @return {@code true} si esta sesión pendiente corresponde a un enrolamiento (QR aún sin confirmar). */
    private boolean esEnrolamiento(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(Constantes.SESSION_MFA_ENROLANDO));
    }

    /**
     * @return el id del usuario con login pendiente de MFA, o {@code null} si no hay sesión
     *         pendiente vigente (no existe, o ya expiró el margen de {@link Constantes#MFA_PENDIENTE_MINUTOS})
     */
    private Integer obtenerUsuarioPendiente(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session == null) {
            return null;
        }
        Object usuarioId = session.getAttribute(Constantes.SESSION_MFA_PENDIENTE_USUARIO_ID);
        Object expira = session.getAttribute(Constantes.SESSION_MFA_PENDIENTE_EXPIRA);
        if (usuarioId == null || !(expira instanceof LocalDateTime)) {
            return null;
        }
        if (LocalDateTime.now().isAfter((LocalDateTime) expira)) {
            session.invalidate();
            return null;
        }
        return (Integer) usuarioId;
    }

    private int incrementarIntentos(HttpSession session) {
        Integer actual = (Integer) session.getAttribute(ATTR_INTENTOS_MFA);
        int nuevo = (actual == null ? 0 : actual) + 1;
        session.setAttribute(ATTR_INTENTOS_MFA, nuevo);
        return nuevo;
    }

    private String obtenerNombreEmpresa(HttpServletRequest req) {
        Object nombre = req.getSession().getAttribute(Constantes.SESSION_NOMBRE_EMPRESA);
        return nombre != null ? (String) nombre : "BodegaControl";
    }

    private void mostrarError(HttpServletRequest req, HttpServletResponse resp, String mensaje)
            throws ServletException, IOException {
        req.setAttribute(Constantes.ATTR_ERROR, mensaje);
        req.getRequestDispatcher("/WEB-INF/views/auth/login-mfa.jsp").forward(req, resp);
    }
}
