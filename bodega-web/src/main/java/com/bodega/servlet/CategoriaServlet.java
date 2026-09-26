package com.bodega.servlet;

import com.bodega.model.Categoria;
import com.bodega.model.EstadoCuenta;
import com.bodega.service.CategoriaService;
import com.bodega.service.CategoriaService.ResultadoOperacion;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Gestiona el módulo "Gestión de Categorías de Productos y Clasificación POS". */
@WebServlet("/categorias")
public class CategoriaServlet extends HttpServlet {

    private final CategoriaService categoriaService = new CategoriaService();

    /**
     * Lista categorías aplicando los filtros de la query string ({@code estado}, {@code busqueda}).
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
            List<Categoria> categorias = categoriaService.listar(filtroEstado, busqueda);
            req.setAttribute("categorias", categorias);
            req.setAttribute("tituloPagina", "Gestión de Categorías de Productos");
            req.setAttribute("moduloActivo", "categorias");
            req.getRequestDispatcher("/WEB-INF/views/categorias/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar categorías.", e);
        }
    }

    /**
     * Despacha las acciones sobre categorías ({@code crear}, {@code desactivar}, {@code reactivar}).
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
                case "crear" -> categoriaService.registrar(
                        req.getParameter("nombre"),
                        req.getParameter("descripcion"),
                        req.getParameter("icono"),
                        parseDecimalOpcional(req.getParameter("margenSugerido")),
                        EstadoCuenta.valueOf(req.getParameter("estadoInicial")));
                case "desactivar" -> categoriaService.desactivar(Integer.parseInt(req.getParameter("categoriaId")));
                case "reactivar" -> categoriaService.reactivar(Integer.parseInt(req.getParameter("categoriaId")));
                default -> new ResultadoOperacion(false, "Acción no reconocida.");
            };

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre categorías.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/categorias");
    }

    private BigDecimal parseDecimalOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return new BigDecimal(valor);
    }
}
