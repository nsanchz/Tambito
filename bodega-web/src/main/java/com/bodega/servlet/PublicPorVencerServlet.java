package com.bodega.servlet;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.model.LoteProducto;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * API de solo lectura: lotes próximos a vencer. Protegido por ApiKeyFilter. Solo
 * implementa doGet. Parámetro opcional de query "dias" (ventana de días, por defecto 7).
 */
@WebServlet("/api/public/v1/inventario-por-vencer")
public class PublicPorVencerServlet extends HttpServlet {

    private final LoteProductoDAO loteProductoDAO = new LoteProductoDAO();
    private final Gson gson = new Gson();

    public static class LotePorVencerDTO {
        public String productoNombre;
        public String numeroLote;
        public String fechaVencimiento;
        public int cantidadActual;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        int dias = 7;
        String diasParam = req.getParameter("dias");
        if (diasParam != null && !diasParam.isBlank()) {
            try {
                dias = Integer.parseInt(diasParam.trim());
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"'dias' debe ser numérico.\"}");
                return;
            }
        }

        try {
            List<LoteProducto> lotes = loteProductoDAO.listarPorVencer(dias);
            List<LotePorVencerDTO> salida = new ArrayList<>();

            for (LoteProducto l : lotes) {
                LotePorVencerDTO dto = new LotePorVencerDTO();
                dto.productoNombre = l.getProductoNombre();
                dto.numeroLote = l.getNumeroLote();
                dto.fechaVencimiento = l.getFechaVencimiento() != null ? l.getFechaVencimiento().toString() : null;
                dto.cantidadActual = l.getCantidadActual();
                salida.add(dto);
            }

            resp.getWriter().write(gson.toJson(salida));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar los lotes por vencer.", e);
        }
    }
}
