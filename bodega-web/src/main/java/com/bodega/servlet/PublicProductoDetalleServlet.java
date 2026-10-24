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
import java.util.Optional;

/**
 * API de solo lectura: detalle de un producto por id. Protegido por ApiKeyFilter.
 * Solo implementa doGet. Devuelve 404 si no existe o está inactivo (nunca expone
 * productos dados de baja a través de la API pública).
 */
@WebServlet("/api/public/v1/producto")
public class PublicProductoDetalleServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final Gson gson = new Gson();

    /** Mismo DTO público que el listado de catálogo (ver PublicProductosServlet). */
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

        String idParam = req.getParameter("id");
        int id;
        try {
            id = Integer.parseInt(idParam);
        } catch (NumberFormatException | NullPointerException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\":\"Parámetro 'id' requerido y debe ser numérico.\"}");
            return;
        }

        try {
            Optional<Producto> productoOpt = productoService.buscarPorId(id);

            if (productoOpt.isEmpty() || productoOpt.get().getEstado() != EstadoCuenta.ACTIVO) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.getWriter().write("{\"error\":\"Producto no encontrado.\"}");
                return;
            }

            Producto p = productoOpt.get();
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

            resp.getWriter().write(gson.toJson(dto));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el producto.", e);
        }
    }
}
