package com.bodega.servlet;

import com.bodega.dao.ProveedorDAO;
import com.bodega.model.Producto;
import com.bodega.model.Proveedor;
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
import java.util.Optional;

/**
 * Endpoint JSON consultado por el widget flotante de alertas de stock bajo
 * (ver cabecera.jspf). Se consulta de forma periódica vía fetch() desde el
 * navegador en todas las páginas del sistema, sin necesidad de que cada
 * Servlet individual precargue esta información.
 */
@WebServlet("/api/alertas-stock")
public class AlertaStockServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final Gson gson = new Gson();

    /** DTO serializado a JSON por cada producto con stock bajo, con su proveedor resuelto. */
    public static class AlertaStockDTO {
        public int productoId;
        public String productoNombre;
        public int stockActual;
        public int stockMinimo;
        public Integer proveedorId;
        public String proveedorNombre;
    }

    /**
     * @param req  petición HTTP (sin parámetros)
     * @param resp respuesta HTTP: cuerpo JSON con la lista de {@link AlertaStockDTO}
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Producto> productosStockBajo = productoService.listarConStockBajoMinimo();
            List<AlertaStockDTO> alertas = new ArrayList<>();

            for (Producto p : productosStockBajo) {
                AlertaStockDTO dto = new AlertaStockDTO();
                dto.productoId = p.getId();
                dto.productoNombre = p.getNombre();
                dto.stockActual = p.getStockActual();
                dto.stockMinimo = p.getStockMinimo();

                if (p.getProveedorId() != null) {
                    Optional<Proveedor> proveedorOpt = proveedorDAO.buscarPorId(p.getProveedorId());
                    dto.proveedorId = p.getProveedorId();
                    dto.proveedorNombre = proveedorOpt.map(Proveedor::getRazonSocial).orElse("Sin proveedor asignado");
                } else {
                    dto.proveedorNombre = "Sin proveedor asignado";
                }

                alertas.add(dto);
            }

            resp.getWriter().write(gson.toJson(alertas));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar productos con stock bajo.", e);
        }
    }
}
