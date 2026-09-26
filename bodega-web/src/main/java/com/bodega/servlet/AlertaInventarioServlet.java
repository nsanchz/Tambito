package com.bodega.servlet;

import com.bodega.model.LoteProducto;
import com.bodega.model.Producto;
import com.bodega.service.AlertaService;
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
 * Endpoint JSON consultado por el widget flotante de alertas (ver cabecera.jspf) para
 * las dos alertas de inventario configurables: baja rotación y lotes por vencer.
 * Análogo a {@link AlertaStockServlet}, pero con un contrato JSON propio.
 */
@WebServlet("/api/alertas-inventario")
public class AlertaInventarioServlet extends HttpServlet {

    private final AlertaService alertaService = new AlertaService();
    private final Gson gson = new Gson();

    /** DTO serializado a JSON por cada producto de baja rotación. */
    public static class AlertaRotacionDTO {
        public int productoId;
        public String productoNombre;
        public int stockActual;
    }

    /** DTO serializado a JSON por cada lote próximo a vencer. */
    public static class AlertaVencimientoDTO {
        public int loteId;
        public String numeroLote;
        public int productoId;
        public String productoNombre;
        public String fechaVencimiento;
        public int cantidadActual;
    }

    /**
     * @param req  petición HTTP (sin parámetros)
     * @param resp respuesta HTTP: cuerpo JSON {@code { "porVencer": [...], "bajaRotacion": [...] } }
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Producto> productosBajaRotacion = alertaService.productosBajaRotacion();
            List<AlertaRotacionDTO> bajaRotacion = new ArrayList<>();
            for (Producto p : productosBajaRotacion) {
                AlertaRotacionDTO dto = new AlertaRotacionDTO();
                dto.productoId = p.getId();
                dto.productoNombre = p.getNombre();
                dto.stockActual = p.getStockActual();
                bajaRotacion.add(dto);
            }

            List<LoteProducto> lotesPorVencer = alertaService.lotesPorVencer();
            List<AlertaVencimientoDTO> porVencer = new ArrayList<>();
            for (LoteProducto l : lotesPorVencer) {
                AlertaVencimientoDTO dto = new AlertaVencimientoDTO();
                dto.loteId = l.getId();
                dto.numeroLote = l.getNumeroLote();
                dto.productoId = l.getProductoId();
                dto.productoNombre = l.getProductoNombre();
                dto.fechaVencimiento = l.getFechaVencimiento() != null ? l.getFechaVencimiento().toString() : null;
                dto.cantidadActual = l.getCantidadActual();
                porVencer.add(dto);
            }

            RespuestaAlertas respuesta = new RespuestaAlertas();
            respuesta.porVencer = porVencer;
            respuesta.bajaRotacion = bajaRotacion;

            resp.getWriter().write(gson.toJson(respuesta));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar las alertas de inventario.", e);
        }
    }

    private static class RespuestaAlertas {
        List<AlertaVencimientoDTO> porVencer;
        List<AlertaRotacionDTO> bajaRotacion;
    }
}
