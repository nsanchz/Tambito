package com.bodega.servlet;

import com.bodega.model.Rol;
import com.bodega.model.Usuario;
import com.bodega.service.AuditoriaService;
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
import java.util.Optional;

/** Mi Perfil de Usuario y Seguridad de Credenciales: cada usuario administra sus propios datos. */
@WebServlet("/perfil")
public class PerfilServlet extends HttpServlet {

    private final UsuarioService usuarioService = new UsuarioService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    /**
     * Muestra los datos del usuario en sesión. Si llega con {@code ?cambioObligatorio=true}
     * (redirigido tras un login con contraseña temporal), muestra el aviso correspondiente.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista o el error 404
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        try {
            Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(usuarioIdSesion);
            if (usuarioOpt.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado.");
                return;
            }

            req.setAttribute("usuario", usuarioOpt.get());
            req.setAttribute("cambioObligatorio", "true".equals(req.getParameter("cambioObligatorio")));
            req.setAttribute("tituloPagina", "Mi Perfil de Usuario y Seguridad de Credenciales");
            req.setAttribute("moduloActivo", "perfil");
            req.getRequestDispatcher("/WEB-INF/views/perfil/mi-perfil.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al cargar el perfil del usuario.", e);
        }
    }

    /**
     * Despacha las acciones sobre el propio perfil ({@code actualizarDatos}, {@code cambiarPassword}),
     * registrando el autocambio de contraseña en auditoría.
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la actualización contra la base de datos
     * @throws IOException      si falla el redirect final o el error 404
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);
        boolean esVendedor = Rol.VENDEDOR.name().equals(rolSesion);
        String accion = req.getParameter("accion");
        boolean redirigirAlPanel = false;

        try {
            ResultadoOperacion resultado;

            if ("actualizarDatos".equals(accion)) {
                // Un VENDEDOR no administra sus propios datos de contacto (solo un administrador
                // puede corregirlos, vía el módulo de Usuarios); nunca se confía en que el
                // formulario esté oculto en el HTML, se vuelve a validar aquí.
                if (esVendedor) {
                    resultado = new ResultadoOperacion(false,
                            "No tiene permiso para editar sus datos personales. Solicite el cambio a un administrador.");
                } else {
                    Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(usuarioIdSesion);
                    if (usuarioOpt.isEmpty()) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado.");
                        return;
                    }
                    Usuario usuario = usuarioOpt.get();
                    usuario.setNombres(req.getParameter("nombres"));
                    usuario.setApellidos(req.getParameter("apellidos"));
                    usuario.setCorreo(req.getParameter("correo"));
                    usuario.setTelefono(req.getParameter("telefono"));

                    resultado = usuarioService.actualizarDatosPropios(usuario);
                    if (resultado.exitoso) {
                        req.getSession().setAttribute(Constantes.SESSION_USUARIO_NOMBRE, usuario.getNombreCompleto());
                    }
                }
            } else if ("cambiarPassword".equals(accion)) {
                // Un VENDEDOR solo puede cambiar su contraseña mientras debe_cambiar_password
                // esté en TRUE: la primera vez que inicia sesión, o cuando un administrador se
                // la restableció (ver UsuarioService.restablecerPassword). Fuera de esos casos,
                // no tiene forma de autocambiarla.
                Optional<Usuario> usuarioOpt = usuarioService.buscarPorId(usuarioIdSesion);
                if (usuarioOpt.isEmpty()) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Usuario no encontrado.");
                    return;
                }
                if (esVendedor && !usuarioOpt.get().isDebeCambiarPassword()) {
                    resultado = new ResultadoOperacion(false,
                            "Solo puede cambiar su contraseña la primera vez que inicia sesión o cuando un administrador la restablezca.");
                } else {
                    resultado = usuarioService.cambiarPasswordPropia(usuarioIdSesion,
                            req.getParameter("passwordActual"), req.getParameter("passwordNueva"),
                            req.getParameter("confirmarPasswordNueva"));

                    if (resultado.exitoso) {
                        auditoriaService.registrar(usuarioIdSesion, "PASSWORD_AUTOCAMBIO", "USUARIO", usuarioIdSesion,
                                "El usuario actualizó su propia contraseña.", req.getRemoteAddr());
                        // Tras un cambio de contraseña válido (incluyendo el cambio obligatorio de la
                        // contraseña temporal) se envía directo al panel de control en vez de dejar al
                        // usuario en /perfil, evitando un paso extra que no aporta nada en ese flujo.
                        redirigirAlPanel = true;
                    }
                }
            } else {
                resultado = new ResultadoOperacion(false, "Acción no reconocida.");
            }

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al actualizar el perfil.", e);
        }

        resp.sendRedirect(req.getContextPath() + (redirigirAlPanel ? "/panel-de-control" : "/perfil"));
    }
}
