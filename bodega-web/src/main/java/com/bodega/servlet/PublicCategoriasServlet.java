package com.bodega.servlet;

import com.bodega.dao.CategoriaDAO;
import com.bodega.model.Categoria;
import com.bodega.model.EstadoCuenta;
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
 * API de solo lectura: categorías activas de productos. Protegido por ApiKeyFilter.
 * Solo implementa doGet.
 */
@WebServlet("/api/public/v1/categorias")
public class PublicCategoriasServlet extends HttpServlet {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final Gson gson = new Gson();

    public static class CategoriaPublicaDTO {
        public int id;
        public String codigo;
        public String nombre;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Categoria> categorias = categoriaDAO.listar(EstadoCuenta.ACTIVO, null);
            List<CategoriaPublicaDTO> salida = new ArrayList<>();

            for (Categoria c : categorias) {
                CategoriaPublicaDTO dto = new CategoriaPublicaDTO();
                dto.id = c.getId();
                dto.codigo = c.getCodigo();
                dto.nombre = c.getNombre();
                salida.add(dto);
            }

            resp.getWriter().write(gson.toJson(salida));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar las categorías.", e);
        }
    }
}
