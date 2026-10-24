package com.bodega.servlet;

import com.bodega.model.Producto;
import com.bodega.service.ProductoService;
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
 * API de solo lectura: productos con stock por debajo del mínimo. Protegido por
 * ApiKeyFilter. Solo implementa doGet.
 */
@WebServlet("/api/public/v1/inventario-stock-bajo")
public class PublicStockBajoServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final Gson gson = new Gson();

    public static class StockBajoDTO {
        public int productoId;
        public String nombre;
        public int stockActual;
        public int stockMinimo;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Producto> productos = productoService.listarConStockBajoMinimo();
            List<StockBajoDTO> salida = new ArrayList<>();

            for (Producto p : productos) {
                StockBajoDTO dto = new StockBajoDTO();
                dto.productoId = p.getId();
                dto.nombre = p.getNombre();
                dto.stockActual = p.getStockActual();
                dto.stockMinimo = p.getStockMinimo();
                salida.add(dto);
            }

            resp.getWriter().write(gson.toJson(salida));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el stock bajo.", e);
        }
    }
}
