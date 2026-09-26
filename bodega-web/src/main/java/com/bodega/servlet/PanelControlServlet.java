package com.bodega.servlet;

import com.bodega.model.Rol;
import com.bodega.service.DashboardService;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Panel de control: una sola URL (/panel-de-control) que renderiza una vista distinta
 * según el rol en sesión — dashboard gerencial completo para ADMINISTRADOR, y un
 * resumen acotado a "mi turno" para VENDEDOR.
 */
@WebServlet("/panel-de-control")
public class PanelControlServlet extends HttpServlet {

    private final DashboardService dashboardService = new DashboardService();

    /**
     * Calcula el resumen correspondiente al rol en sesión y reenvía a la vista adecuada.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla el cálculo de los KPIs
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);
        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        try {
            req.setAttribute("tituloPagina", "Panel de Control");
            req.setAttribute("moduloActivo", "panel-de-control");

            if (Rol.ADMINISTRADOR.name().equals(rolSesion)) {
                req.setAttribute("resumen", dashboardService.obtenerResumenAdministrador());
                req.getRequestDispatcher("/WEB-INF/views/panel/administrador.jsp").forward(req, resp);
            } else {
                req.setAttribute("resumen", dashboardService.obtenerResumenVendedor(usuarioIdSesion));
                req.getRequestDispatcher("/WEB-INF/views/panel/vendedor.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException("Error al calcular el resumen del panel de control.", e);
        }
    }
}
