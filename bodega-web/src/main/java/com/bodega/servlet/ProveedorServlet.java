package com.bodega.servlet;

import com.bodega.model.EstadoCuenta;
import com.bodega.model.Proveedor;
import com.bodega.service.ProveedorService;
import com.bodega.service.ProveedorService.ResultadoOperacion;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/** Gestiona el módulo de Proveedores (soporte de Órdenes de Compra). */
@WebServlet("/proveedores")
public class ProveedorServlet extends HttpServlet {

    private final ProveedorService proveedorService = new ProveedorService();

    /**
     * Lista proveedores aplicando los filtros de la query string.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String filtroEstadoParam = req.getParameter("estado");
        String busqueda = req.getParameter("busqueda");
        EstadoCuenta filtroEstado = (filtroEstadoParam != null && !filtroEstadoParam.equals("todos"))
                ? EstadoCuenta.valueOf(filtroEstadoParam) : null;

        try {
            List<Proveedor> proveedores = proveedorService.listar(filtroEstado, busqueda);
            req.setAttribute("proveedores", proveedores);
            req.setAttribute("tituloPagina", "Gestión de Proveedores");
            req.setAttribute("moduloActivo", "proveedores");
            req.getRequestDispatcher("/WEB-INF/views/proveedores/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar proveedores.", e);
        }
    }

    /**
     * Despacha las acciones sobre proveedores ({@code crear}, {@code desactivar}, {@code reactivar}).
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la operación contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String accion = req.getParameter("accion");

        try {
            ResultadoOperacion resultado = switch (accion) {
                case "crear" -> {
                    Proveedor p = new Proveedor();
                    p.setRuc(req.getParameter("ruc"));
                    p.setRazonSocial(req.getParameter("razonSocial"));
                    p.setContactoNombre(req.getParameter("contactoNombre"));
                    p.setTelefono(req.getParameter("telefono"));
                    p.setCorreo(req.getParameter("correo"));
                    p.setDireccion(req.getParameter("direccion"));
                    yield proveedorService.registrar(p);
                }
                case "desactivar" -> proveedorService.cambiarEstado(
                        Integer.parseInt(req.getParameter("proveedorId")), EstadoCuenta.INACTIVO);
                case "reactivar" -> proveedorService.cambiarEstado(
                        Integer.parseInt(req.getParameter("proveedorId")), EstadoCuenta.ACTIVO);
                default -> new ResultadoOperacion(false, "Acción no reconocida.");
            };

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre proveedores.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/proveedores");
    }
}
