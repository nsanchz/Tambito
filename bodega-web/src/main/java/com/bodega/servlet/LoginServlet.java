package com.bodega.servlet;

import com.bodega.service.AuditoriaService;
import com.bodega.service.AutenticacionService;
import com.bodega.service.AutenticacionService.RespuestaLogin;
import com.bodega.service.ConfiguracionService;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Gestiona el formulario de inicio de sesión y la validación de credenciales. */
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private final AutenticacionService autenticacionService = new AutenticacionService();
    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final ConfiguracionService configuracionService = new ConfiguracionService();

    /**
     * Muestra el formulario de login. Si llega con {@code ?expirada=true} (redirigido por
     * {@link com.bodega.filter.AuthenticationFilter}), muestra un aviso de sesión expirada.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla el reenvío a la vista
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        if ("true".equals(req.getParameter("expirada"))) {
            req.setAttribute(Constantes.ATTR_ERROR, "Su sesión ha expirado o requiere iniciar sesión para continuar.");
        }
        req.setAttribute("nombreEmpresa", obtenerNombreEmpresa());
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }

    /** @return el nombre de la empresa configurado, o un valor por defecto si la consulta falla. */
    private String obtenerNombreEmpresa() {
        try {
            return configuracionService.obtenerTodos().get(ConfiguracionService.CLAVE_NOMBRE_EMPRESA);
        } catch (SQLException e) {
            return "BodegaControl";
        }
    }

    /**
     * Procesa el envío del formulario de login: valida credenciales, registra el intento
     * en auditoría (exitoso o no) y, si es exitoso, abre la sesión y redirige al panel de
     * control (o a Mi Perfil si debe cambiar su contraseña temporal).
     *
     * @param req  petición HTTP con los parámetros {@code j_username}, {@code j_password} y {@code terminalId}
     * @param resp respuesta HTTP
     * @throws ServletException si falla la validación contra la base de datos
     * @throws IOException      si falla el redirect o el reenvío a la vista
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String usuarioLogin = req.getParameter("j_username");
        String password = req.getParameter("j_password");
        String terminalId = req.getParameter("terminalId");
        String ip = req.getRemoteAddr();

        // Throttling por IP: independiente del bloqueo por cuenta (que protege una cuenta
        // puntual), esto evita que una sola IP martille el endpoint de login a alto volumen
        // probando usuarios distintos o inexistentes.
        if (!LoginRateLimiter.permitirIntento(ip)) {
            auditoriaService.registrar(null, "LOGIN_RATE_LIMIT", "USUARIO", null,
                    "Se bloqueó temporalmente el login desde " + ip + " por exceso de intentos.", ip);
            mostrarError(req, resp, usuarioLogin,
                    "Demasiados intentos de inicio de sesión desde esta red. Intente nuevamente en unos minutos.");
            return;
        }

        try {
            RespuestaLogin respuesta = autenticacionService.autenticar(usuarioLogin, password);

            switch (respuesta.resultado) {
                case EXITO -> {
                    LoginSessionHelper.abrirSesionCompleta(req, respuesta.usuario, terminalId, obtenerNombreEmpresa(), ip);
                    LoginSessionHelper.redirigirTrasLogin(req, resp, respuesta.usuario);
                }
                case REQUIERE_MFA, REQUIERE_ENROLAMIENTO_MFA -> {
                    // Contraseña correcta, pero falta el segundo factor: se abre una sesión
                    // "pendiente" de corta duración, SIN atributos de sesión real, para que
                    // AuthenticationFilter siga bloqueando cualquier ruta protegida hasta que
                    // se complete /login-mfa. Si un administrador activó el MFA pero todavía no
                    // se confirmó (REQUIERE_ENROLAMIENTO_MFA), este es el momento en que el
                    // propio usuario ve el QR y lo confirma, en vez de haberlo visto el admin.
                    HttpSession session = req.getSession(true);
                    session.setAttribute(Constantes.SESSION_MFA_PENDIENTE_USUARIO_ID, respuesta.usuario.getId());
                    session.setAttribute(Constantes.SESSION_MFA_PENDIENTE_EXPIRA,
                            LocalDateTime.now().plusMinutes(Constantes.MFA_PENDIENTE_MINUTOS));
                    session.setAttribute(Constantes.SESSION_TERMINAL_ID, terminalId);
                    session.setAttribute(Constantes.SESSION_MFA_ENROLANDO,
                            respuesta.resultado == AutenticacionService.ResultadoLogin.REQUIERE_ENROLAMIENTO_MFA);
                    resp.sendRedirect(req.getContextPath() + "/login-mfa");
                }
                case CREDENCIALES_INVALIDAS -> {
                    auditoriaService.registrar(null, "LOGIN_FALLIDO", "USUARIO", null,
                            "Intento fallido de inicio de sesión para \"" + usuarioLogin + "\"", ip);
                    mostrarError(req, resp, usuarioLogin,
                            "Usuario o contraseña incorrectos. Verifique sus credenciales e intente nuevamente.");
                }
                case CUENTA_INACTIVA -> {
                    auditoriaService.registrar(null, "LOGIN_CUENTA_INACTIVA", "USUARIO", null,
                            "Intento de inicio de sesión de la cuenta inactiva \"" + usuarioLogin + "\"", ip);
                    mostrarError(req, resp, usuarioLogin,
                            "Su cuenta se encuentra inactiva. Comuníquese con un administrador del sistema.");
                }
                case CUENTA_BLOQUEADA -> {
                    auditoriaService.registrar(null, "LOGIN_CUENTA_BLOQUEADA", "USUARIO", null,
                            "Intento de inicio de sesión de la cuenta bloqueada \"" + usuarioLogin + "\"", ip);
                    mostrarError(req, resp, usuarioLogin,
                            "Cuenta bloqueada temporalmente por múltiples intentos fallidos. "
                                    + describirTiempoRestante(respuesta.usuario.getBloqueadoHasta())
                                    + " o contacte a un administrador.");
                }
            }
        } catch (SQLException e) {
            throw new ServletException("Error al validar credenciales contra la base de datos.", e);
        }
    }

    /**
     * Describe, en texto legible, el tiempo exacto que falta para que expire un bloqueo de
     * cuenta, a partir del {@code bloqueado_hasta} real calculado con el parámetro configurable
     * de minutos de bloqueo (nunca un valor fijo hardcodeado, para que un cambio de ese
     * parámetro en Configuración del Sistema se refleje de inmediato en este mensaje).
     *
     * @param bloqueadoHasta instante exacto de desbloqueo devuelto por {@link AutenticacionService}
     * @return frase lista para insertar en el mensaje de error (ej. "Podrá volver a intentarlo
     *         en 47 segundos (23:45:12)")
     */
    private String describirTiempoRestante(LocalDateTime bloqueadoHasta) {
        if (bloqueadoHasta == null) {
            return "Intente nuevamente más tarde";
        }
        long segundosRestantes = Math.max(0, Duration.between(LocalDateTime.now(), bloqueadoHasta).getSeconds());
        String horaExacta = bloqueadoHasta.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        String tiempo;
        if (segundosRestantes < 60) {
            tiempo = segundosRestantes + " segundo" + (segundosRestantes == 1 ? "" : "s");
        } else {
            long minutos = segundosRestantes / 60;
            long segundos = segundosRestantes % 60;
            tiempo = minutos + " minuto" + (minutos == 1 ? "" : "s")
                    + (segundos > 0 ? " y " + segundos + " segundo" + (segundos == 1 ? "" : "s") : "");
        }
        return "Podrá volver a intentarlo en " + tiempo + " (a las " + horaExacta + ")";
    }

    private void mostrarError(HttpServletRequest req, HttpServletResponse resp, String usuarioLogin, String mensaje)
            throws ServletException, IOException {
        req.setAttribute(Constantes.ATTR_ERROR, mensaje);
        req.setAttribute("usuarioIngresado", usuarioLogin);
        req.setAttribute("nombreEmpresa", obtenerNombreEmpresa());
        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp").forward(req, resp);
    }
}
