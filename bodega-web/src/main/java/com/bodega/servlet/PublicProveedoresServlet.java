package com.bodega.servlet;

import com.bodega.dao.ProveedorDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Proveedor;
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
 * API de solo lectura: proveedores activos. Protegido por ApiKeyFilter. Solo implementa
 * doGet. No incluye el correo del proveedor (dato de contacto interno, no de catálogo).
 */
@WebServlet("/api/public/v1/proveedores")
public class PublicProveedoresServlet extends HttpServlet {

    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final Gson gson = new Gson();

    public static class ProveedorPublicoDTO {
        public int id;
        public String razonSocial;
        public String ruc;
        public String telefono;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        try {
            List<Proveedor> proveedores = proveedorDAO.listar(EstadoCuenta.ACTIVO, null);
            List<ProveedorPublicoDTO> salida = new ArrayList<>();

            for (Proveedor p : proveedores) {
                ProveedorPublicoDTO dto = new ProveedorPublicoDTO();
                dto.id = p.getId();
                dto.razonSocial = p.getRazonSocial();
                dto.ruc = p.getRuc();
                dto.telefono = p.getTelefono();
                salida.add(dto);
            }

            resp.getWriter().write(gson.toJson(salida));
        } catch (SQLException e) {
            throw new ServletException("Error al consultar los proveedores.", e);
        }
    }
}
