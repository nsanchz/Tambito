package com.bodega.servlet;

import com.bodega.model.EstadoCuenta;
import com.bodega.model.Rol;
import com.bodega.model.Usuario;
import com.bodega.service.AuditoriaService;
import com.bodega.service.MfaService;
import com.bodega.service.UsuarioService;
import com.bodega.service.UsuarioService.ResultadoOperacion;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Gestiona el módulo "Gestión de Usuarios y Roles de Seguridad". Todo este Servlet está
 * bajo el prefijo /usuarios, exclusivo de ADMINISTRADOR (ver AuthorizationFilter).
 */
@WebServlet("/usuarios")
public class UsuarioServlet extends HttpServlet {

    private final UsuarioService usuarioService = new UsuarioService();
    private final MfaService mfaService = new MfaService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    /**
     * Lista usuarios aplicando los filtros de la query string ({@code rol}, {@code estado}, {@code busqueda}).
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String filtroRolParam = req.getParameter("rol");
        String filtroEstadoParam = req.getParameter("estado");
        String busqueda = req.getParameter("busqueda");

        Rol filtroRol = (filtroRolParam != null && !filtroRolParam.equals("todos")) ? Rol.valueOf(filtroRolParam) : null;
        EstadoCuenta filtroEstado = (filtroEstadoParam != null && !filtroEstadoParam.equals("todos"))
                ? EstadoCuenta.valueOf(filtroEstadoParam) : null;

        try {
            List<Usuario> usuarios = usuarioService.listar(filtroRol, filtroEstado, busqueda);
            req.setAttribute("usuarios", usuarios);
            req.setAttribute("tituloPagina", "Gestión de Usuarios y Roles de Seguridad");
            req.setAttribute("moduloActivo", "usuarios");
            req.getRequestDispatcher("/WEB-INF/views/usuarios/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar usuarios.", e);
        }
    }

    /**
     * Despacha las acciones sobre usuarios ({@code crear}, {@code cambiarRol},
     * {@code cambiarEstado}, {@code restablecerPassword}), registrando cada una en
     * auditoría si tuvo éxito.
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la operación contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String accion = req.getParameter("accion");
        int idAdminSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String ip = req.getRemoteAddr();

        // La creación se maneja aparte: si falla una validación (ej. usuario/correo duplicado),
        // se reabre el modal con los datos ya escritos en vez de perderlos en un redirect, porque
        // el banner de error de arriba pasaba fácilmente inadvertido (ver incidente reportado:
        // "al agregar usuarios no me salen, solo me sale el admin" — la creación sí se rechazaba,
        // solo que el aviso no era lo bastante visible como para notar que nada se había guardado).
        if ("crear".equals(accion)) {
            procesarCreacionYResponder(req, resp, idAdminSesion, ip);
            return;
        }

        try {
            Integer usuarioIdParam = req.getParameter("usuarioId") != null
                    ? Integer.parseInt(req.getParameter("usuarioId")) : null;

            ResultadoOperacion resultado = switch (accion) {
                case "cambiarRol" -> usuarioService.cambiarRol(usuarioIdParam, Rol.valueOf(req.getParameter("nuevoRol")), idAdminSesion);
                case "cambiarEstado" -> usuarioService.cambiarEstado(usuarioIdParam, EstadoCuenta.valueOf(req.getParameter("nuevoEstado")), idAdminSesion);
                case "desbloquear" -> usuarioService.desbloquear(usuarioIdParam);
                case "activarMfa" -> mfaService.iniciarActivacion(usuarioIdParam, idAdminSesion);
                case "desactivarMfa" -> mfaService.desactivar(usuarioIdParam);
                case "editar" -> usuarioService.editarDatos(usuarioIdParam, req.getParameter("nombres"),
                        req.getParameter("apellidos"), req.getParameter("correo"), req.getParameter("telefono"));
                case "restablecerPassword" -> {
                    String temporal = usuarioService.restablecerPassword(usuarioIdParam);
                    yield new ResultadoOperacion(true, "Contraseña temporal generada: " + temporal
                            + " (comuníquela de forma segura al usuario).");
                }
                default -> new ResultadoOperacion(false, "Acción no reconocida.");
            };

            if (resultado.exitoso) {
                auditoriaService.registrar(idAdminSesion, "USUARIO_" + accion.toUpperCase(), "USUARIO",
                        usuarioIdParam, resultado.mensaje, ip);
            }

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre usuarios.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/usuarios");
    }

    private void procesarCreacionYResponder(HttpServletRequest req, HttpServletResponse resp, int idAdminSesion, String ip)
            throws ServletException, IOException {
        try {
            ResultadoOperacion resultado = procesarCreacion(req, idAdminSesion);

            if (resultado.exitoso) {
                auditoriaService.registrar(idAdminSesion, "USUARIO_CREAR", "USUARIO", resultado.usuarioId, resultado.mensaje, ip);

                String mensajeFinal = resultado.mensaje;
                // Activar MFA desde la propia creación: el administrador nunca ve el QR (lo verá
                // el usuario en su primer login, igual que al activarlo después desde el menú de
                // acciones); esto solo evita el paso extra de volver a entrar a activarlo aparte.
                if ("true".equals(req.getParameter("activarMfaAlCrear"))) {
                    ResultadoOperacion resultadoMfa = mfaService.iniciarActivacion(resultado.usuarioId, idAdminSesion);
                    if (resultadoMfa.exitoso) {
                        auditoriaService.registrar(idAdminSesion, "MFA_ACTIVAR", "USUARIO", resultado.usuarioId,
                                "MFA activado durante la creación del usuario", ip);
                        mensajeFinal += " Se activó también la verificación en dos pasos: deberá completarla en su primer inicio de sesión.";
                    } else {
                        mensajeFinal += " (No se pudo activar el MFA: " + resultadoMfa.mensaje + ")";
                    }
                }

                req.getSession().setAttribute(Constantes.ATTR_MENSAJE, mensajeFinal);
                resp.sendRedirect(req.getContextPath() + "/usuarios");
                return;
            }

            req.setAttribute("errorCreacion", resultado.mensaje);
            req.setAttribute("abrirModalNuevoUsuario", true);
            req.setAttribute("usuarios", usuarioService.listar(null, null, null));
            req.setAttribute("tituloPagina", "Gestión de Usuarios y Roles de Seguridad");
            req.setAttribute("moduloActivo", "usuarios");
            req.getRequestDispatcher("/WEB-INF/views/usuarios/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre usuarios.", e);
        }
    }

    private ResultadoOperacion procesarCreacion(HttpServletRequest req, int idAdminSesion) throws SQLException {
        Usuario nuevo = new Usuario();
        nuevo.setNombres(req.getParameter("nombres"));
        nuevo.setApellidos(req.getParameter("apellidos"));
        nuevo.setNombreUsuario(req.getParameter("nombreUsuario"));
        nuevo.setCorreo(req.getParameter("correo"));
        nuevo.setTelefono(req.getParameter("telefono"));
        nuevo.setRol(Rol.valueOf(req.getParameter("rolSeleccionado")));
        nuevo.setEstado(EstadoCuenta.valueOf(req.getParameter("estadoInicial")));

        String password = req.getParameter("password");
        String confirmarPassword = req.getParameter("confirmarPassword");

        return usuarioService.registrar(nuevo, password, confirmarPassword, idAdminSesion);
    }
}
