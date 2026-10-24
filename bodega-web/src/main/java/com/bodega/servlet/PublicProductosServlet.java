package com.bodega.servlet;

import com.bodega.model.EstadoCuenta;
import com.bodega.model.Producto;
import com.bodega.service.ProductoService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * API de solo lectura: catálogo de productos activos. Protegido por ApiKeyFilter
 * (header X-API-Key), no por sesión. Solo implementa doGet: no expone precioCompra
 * ni el stock exacto (datos operativos internos), solo lo necesario para un catálogo.
 * Parámetros opcionales de query: categoriaId, texto (mismo filtro que ProductoDAO.listar).
 */
@WebServlet("/api/public/v1/productos")
public class PublicProductosServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final Gson gson = new Gson();

    /** DTO público de catálogo: sin precioCompra ni stockActual exacto. */
    public static class ProductoPublicoDTO {
        public int id;
        public String sku;
        public String nombre;
        public String descripcion;
        public String marca;
        public BigDecimal precioVenta;
        public String unidadMedida;
        public boolean disponible;
        public String imagenUrl;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        Integer categoriaId = null;
        String categoriaIdParam = req.getParameter("categoriaId");
        if (categoriaIdParam != null && !categoriaIdParam.isBlank()) {
            try {
                categoriaId = Integer.parseInt(categoriaIdParam.trim());
            } catch (NumberFormatException e) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"categoriaId debe ser numérico.\"}");
                return;
            }
        }
        String texto = req.getParameter("texto");

        try {
            List<Producto> productos = productoService.listar(categoriaId, EstadoCuenta.ACTIVO, texto);
            List<ProductoPublicoDTO> salida = new ArrayList<>();

            for (Producto p : productos) {
                ProductoPublicoDTO dto = new ProductoPublicoDTO();
                dto.id = p.getId();
                dto.sku = p.getSku();
                dto.nombre = p.getNombre();
                dto.descripcion = p.getDescripcion();
                dto.marca = p.getMarca();
                dto.precioVenta = p.getPrecioVenta();
                dto.unidadMedida = p.getUnidadMedida() != null ? p.getUnidadMedida().name() : null;
                dto.disponible = p.getStockActual() > 0;
                dto.imagenUrl = p.getImagenUrl();
                salida.add(dto);
            }

            resp.getWriter().write(gson.toJson(salida));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el catálogo de productos.", e);
        }
    }
}
