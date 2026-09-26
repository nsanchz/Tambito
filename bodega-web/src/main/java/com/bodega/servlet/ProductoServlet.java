package com.bodega.servlet;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.model.Categoria;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.LoteProducto;
import com.bodega.model.Producto;
import com.bodega.model.Proveedor;
import com.bodega.model.Rol;
import com.bodega.model.UnidadMedida;
import com.bodega.service.CategoriaService;
import com.bodega.service.ProductoService;
import com.bodega.service.ProductoService.ResultadoOperacion;
import com.bodega.service.ProveedorService;
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
import java.util.Optional;

/** Gestiona el módulo "Gestión de Productos, Catálogo e Inventario". */
@WebServlet("/productos")
public class ProductoServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ProveedorService proveedorService = new ProveedorService();
    private final LoteProductoDAO loteProductoDAO = new LoteProductoDAO();

    /**
     * Lista productos aplicando filtros, junto con las categorías y proveedores activos
     * (para los selectores del modal de creación).
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String categoriaIdParam = req.getParameter("categoriaId");
        String filtroEstadoParam = req.getParameter("estado");
        String busqueda = req.getParameter("busqueda");

        Integer categoriaId = (categoriaIdParam != null && !categoriaIdParam.isBlank())
                ? Integer.parseInt(categoriaIdParam) : null;
        EstadoCuenta filtroEstado = (filtroEstadoParam != null && !filtroEstadoParam.equals("todos"))
                ? EstadoCuenta.valueOf(filtroEstadoParam) : null;

        try {
            List<Producto> productos = productoService.listar(categoriaId, filtroEstado, busqueda);
            List<Categoria> categorias = categoriaService.listar(EstadoCuenta.ACTIVO, null);
            List<Proveedor> proveedores = proveedorService.listar(EstadoCuenta.ACTIVO, null);

            req.setAttribute("productos", productos);
            req.setAttribute("categorias", categorias);
            req.setAttribute("proveedores", proveedores);
            req.setAttribute("unidadesMedida", UnidadMedida.values());
            req.setAttribute("proximosVencimientos", loteProductoDAO.obtenerProximoVencimientoPorProducto());

            String verProductoId = req.getParameter("ver");
            if (verProductoId != null && !verProductoId.isBlank()) {
                int productoId2 = Integer.parseInt(verProductoId);
                Optional<Producto> productoSeleccionado = productoService.buscarPorId(productoId2);
                if (productoSeleccionado.isPresent()) {
                    Producto p = productoSeleccionado.get();
                    req.setAttribute("productoSeleccionado", p);
                    categoriaService.buscarPorId(p.getCategoriaId())
                            .ifPresent(c -> req.setAttribute("categoriaSeleccionada", c));
                    if (p.getProveedorId() != null) {
                        proveedorService.buscarPorId(p.getProveedorId())
                                .ifPresent(prov -> req.setAttribute("proveedorSeleccionado", prov));
                    }
                    List<LoteProducto> lotesDelProducto = loteProductoDAO.listarPorProducto(productoId2);
                    req.setAttribute("lotesDelProducto", lotesDelProducto);
                }
            }

            req.setAttribute("tituloPagina", "Gestión de Productos y Catálogo");
            req.setAttribute("moduloActivo", "productos");
            req.getRequestDispatcher("/WEB-INF/views/productos/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar productos.", e);
        }
    }

    /**
     * Despacha las acciones sobre productos ({@code crear}, {@code desactivar}, {@code reactivar}).
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
        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);

        try {
            ResultadoOperacion resultado = switch (accion) {
                case "crear" -> productoService.registrar(construirProductoDesdeFormulario(req));
                case "desactivar" -> productoService.cambiarEstado(
                        Integer.parseInt(req.getParameter("productoId")), EstadoCuenta.INACTIVO);
                case "reactivar" -> productoService.cambiarEstado(
                        Integer.parseInt(req.getParameter("productoId")), EstadoCuenta.ACTIVO);
                case "editar" -> {
                    // El detalle es visible para ambos roles, pero la edición nunca se confía al
                    // HTML (que ya oculta el formulario para VENDEDOR): se vuelve a validar aquí.
                    if (!Rol.ADMINISTRADOR.name().equals(rolSesion)) {
                        yield new ResultadoOperacion(false, "Solo un administrador puede editar productos.");
                    }
                    yield productoService.actualizar(construirProductoParaActualizar(req));
                }
                default -> new ResultadoOperacion(false, "Acción no reconocida.");
            };

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre productos.", e);
        }

        String productoIdParam = req.getParameter("productoId");
        String destino = req.getContextPath() + "/productos"
                + ("editar".equals(accion) && productoIdParam != null ? "?ver=" + productoIdParam : "");
        resp.sendRedirect(destino);
    }

    /**
     * @param req petición HTTP con los campos editables del formulario de detalle de producto
     * @return un {@link Producto} con el id de la fila a actualizar y los nuevos valores
     *         (SKU, código de barras salvo lo indicado, y estado no se editan aquí)
     */
    private Producto construirProductoParaActualizar(HttpServletRequest req) {
        Producto p = new Producto();
        p.setId(Integer.parseInt(req.getParameter("productoId")));
        p.setNombre(req.getParameter("nombre"));
        p.setDescripcion(req.getParameter("descripcion"));
        p.setCategoriaId(Integer.parseInt(req.getParameter("categoriaId")));
        String proveedorIdParam = req.getParameter("proveedorId");
        p.setProveedorId((proveedorIdParam != null && !proveedorIdParam.isBlank()) ? Integer.parseInt(proveedorIdParam) : null);
        p.setMarca(req.getParameter("marca"));
        p.setPrecioCompra(new BigDecimal(req.getParameter("precioCompra")));
        p.setPrecioVenta(new BigDecimal(req.getParameter("precioVenta")));
        p.setStockMinimo(Integer.parseInt(req.getParameter("stockMinimo")));
        p.setUnidadMedida(UnidadMedida.valueOf(req.getParameter("unidadMedida")));
        p.setCodigoBarras(req.getParameter("codigoBarras"));
        return p;
    }

    /**
     * @param req petición HTTP con todos los campos del modal de nuevo producto
     * @return un {@link Producto} construido a partir de los parámetros del formulario
     */
    private Producto construirProductoDesdeFormulario(HttpServletRequest req) {
        Producto p = new Producto();
        p.setSku(req.getParameter("sku"));
        p.setCodigoBarras(req.getParameter("codigoBarras"));
        p.setNombre(req.getParameter("nombre"));
        p.setDescripcion(req.getParameter("descripcion"));
        p.setCategoriaId(Integer.parseInt(req.getParameter("categoriaId")));
        String proveedorIdParam = req.getParameter("proveedorId");
        p.setProveedorId((proveedorIdParam != null && !proveedorIdParam.isBlank()) ? Integer.parseInt(proveedorIdParam) : null);
        p.setMarca(req.getParameter("marca"));
        p.setPrecioCompra(new BigDecimal(req.getParameter("precioCompra")));
        p.setPrecioVenta(new BigDecimal(req.getParameter("precioVenta")));
        p.setStockActual(Integer.parseInt(req.getParameter("stockInicial")));
        p.setStockMinimo(Integer.parseInt(req.getParameter("stockMinimo")));
        p.setUnidadMedida(UnidadMedida.valueOf(req.getParameter("unidadMedida")));
        p.setEstado(EstadoCuenta.valueOf(req.getParameter("estadoInicial")));
        return p;
    }
}
