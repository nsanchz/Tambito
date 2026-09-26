package com.bodega.servlet;

import com.bodega.service.OrdenCompraService;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Endpoint JSON consultado desde el modal de Nueva Orden de Compra: al elegir un
 * producto, sugiere la cantidad a pedir según su velocidad de venta real (Kárdex),
 * en vez de que el administrador la adivine.
 */
@WebServlet("/api/sugerencia-reposicion")
public class SugerenciaReposicionServlet extends HttpServlet {

    private final OrdenCompraService ordenCompraService = new OrdenCompraService();
    private final Gson gson = new Gson();

    /** DTO serializado a JSON con la cantidad sugerida a pedir. */
    public static class SugerenciaDTO {
        public int cantidadSugerida;
    }

    /**
     * @param req  petición HTTP con el parámetro {@code productoId}
     * @param resp respuesta HTTP: cuerpo JSON {@code {"cantidadSugerida": N}}
     * @throws ServletException si falla el cálculo de la sugerencia
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            int productoId = Integer.parseInt(req.getParameter("productoId"));
            SugerenciaDTO dto = new SugerenciaDTO();
            dto.cantidadSugerida = Math.max(ordenCompraService.sugerirCantidadReposicion(productoId), 1);
            resp.getWriter().write(gson.toJson(dto));
        } catch (SQLException e) {
            throw new ServletException("Error al calcular la sugerencia de reposición.", e);
        }
    }
}
