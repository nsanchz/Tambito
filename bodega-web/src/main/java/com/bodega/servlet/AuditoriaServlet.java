package com.bodega.servlet;

import com.bodega.model.RegistroAuditoria;
import com.bodega.service.AuditoriaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Auditoría del Sistema y Control de Seguridad. Módulo de solo lectura sobre la
 * bitácora inmutable registrada por {@link AuditoriaService}. Ruta exclusiva de
 * ADMINISTRADOR (ver AuthorizationFilter: prefijo "/auditoria").
 */
@WebServlet("/auditoria")
public class AuditoriaServlet extends HttpServlet {

    private final AuditoriaService auditoriaService = new AuditoriaService();

    private static final String[] ACCIONES_FILTRABLES = {
            "LOGIN_EXITOSO", "LOGIN_FALLIDO", "LOGIN_CUENTA_INACTIVA", "LOGIN_CUENTA_BLOQUEADA",
            "USUARIO_CREAR", "USUARIO_CAMBIARROL", "USUARIO_CAMBIARESTADO", "USUARIO_RESTABLECERPASSWORD",
            "VENTA_ANULADA", "CONFIGURACION_ACTUALIZADA"
    };

    /**
     * Lista los registros de auditoría aplicando los filtros de la query string.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String filtroAccion = req.getParameter("accion");
            String busqueda = req.getParameter("busqueda");

            List<RegistroAuditoria> registros = auditoriaService.listar(filtroAccion, null, busqueda);

            req.setAttribute("registros", registros);
            req.setAttribute("accionesFiltrables", ACCIONES_FILTRABLES);
            req.setAttribute("tituloPagina", "Auditoría del Sistema y Control de Seguridad");
            req.setAttribute("moduloActivo", "auditoria");
            req.getRequestDispatcher("/WEB-INF/views/auditoria/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al consultar la bitácora de auditoría.", e);
        }
    }
}
