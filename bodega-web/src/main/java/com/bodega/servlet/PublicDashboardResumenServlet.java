package com.bodega.servlet;

import com.bodega.dao.DashboardDAO;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * API de solo lectura: métricas agregadas del negocio (mismos totales que alimentan el
 * panel de control interno, ver DashboardDAO). Solo números totales, ninguna venta ni
 * cliente individual. Protegido por ApiKeyFilter. Solo implementa doGet.
 */
@WebServlet("/api/public/v1/dashboard-resumen")
public class PublicDashboardResumenServlet extends HttpServlet {

    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final Gson gson = new Gson();

    public static class ResumenDTO {
        public BigDecimal ventasHoy;
        public BigDecimal ventasMes;
        public int cantidadVentasHoy;
        public int productosActivos;
        public int productosStockBajo;
        public int clientesActivos;
        public int ordenesCompraPendientes;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            ResumenDTO dto = new ResumenDTO();
            dto.ventasHoy = dashboardDAO.totalVentasHoy(null);
            dto.ventasMes = dashboardDAO.totalVentasMes(null);
            dto.cantidadVentasHoy = dashboardDAO.contarVentasHoy(null);
            dto.productosActivos = dashboardDAO.contarProductosActivos();
            dto.productosStockBajo = dashboardDAO.contarProductosStockBajo();
            dto.clientesActivos = dashboardDAO.contarClientesActivos();
            dto.ordenesCompraPendientes = dashboardDAO.contarOrdenesCompraPendientes();

            resp.getWriter().write(gson.toJson(dto));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el resumen del negocio.", e);
        }
    }
}
