package com.bodega.servlet;

import com.bodega.model.DetalleOrdenCompra;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.EstadoOrdenCompra;
import com.bodega.model.OrdenCompra;
import com.bodega.model.Producto;
import com.bodega.model.Proveedor;
import com.bodega.model.Rol;
import com.bodega.service.AuditoriaService;
import com.bodega.service.ExportExcelService;
import com.bodega.service.OrdenCompraService;
import com.bodega.service.OrdenCompraService.ResultadoOperacion;
import com.bodega.service.ProductoService;
import com.bodega.service.ProveedorService;
import com.bodega.util.Constantes;
import com.bodega.util.DescargaHttpUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Gestiona el módulo de Órdenes de Compra a Proveedores (incluye la recepción de mercadería). */
@WebServlet("/ordenes-compra")
public class OrdenCompraServlet extends HttpServlet {

    private final OrdenCompraService ordenCompraService = new OrdenCompraService();
    private final ProveedorService proveedorService = new ProveedorService();
    private final ProductoService productoService = new ProductoService();
    private final ExportExcelService exportExcelService = new ExportExcelService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    /**
     * Lista órdenes de compra, y opcionalmente precarga el modal de nueva orden con
     * un producto y cantidad sugerida (llegando desde la alerta de stock bajo vía
     * {@code ?productoId=}), o carga el detalle de una orden para recibir mercadería
     * (vía {@code ?ver=}).
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String estadoParam = req.getParameter("estado");
            EstadoOrdenCompra filtroEstado = (estadoParam != null && !estadoParam.isBlank())
                    ? EstadoOrdenCompra.valueOf(estadoParam) : null;

            if ("excel".equals(req.getParameter("exportar"))) {
                DescargaHttpUtil.enviarExcel(resp, exportExcelService.exportarOrdenesCompra(filtroEstado), "ordenes-compra.xlsx");
                return;
            }

            List<OrdenCompra> ordenes = ordenCompraService.listar(filtroEstado);
            List<Proveedor> proveedores = proveedorService.listar(EstadoCuenta.ACTIVO, null);
            List<Producto> productos = productoService.listar(null, EstadoCuenta.ACTIVO, null);

            req.setAttribute("ordenes", ordenes);
            req.setAttribute("proveedores", proveedores);
            req.setAttribute("productos", productos);
            req.setAttribute("estadosOrden", EstadoOrdenCompra.values());

            // Preselección desde la alerta flotante de stock bajo ("Pedir producto"),
            // con la cantidad sugerida calculada a partir de la velocidad de venta real (Kárdex).
            String productoSugeridoId = req.getParameter("productoId");
            if (productoSugeridoId != null && !productoSugeridoId.isBlank()) {
                int cantidadSugerida = Math.max(
                        ordenCompraService.sugerirCantidadReposicion(Integer.parseInt(productoSugeridoId)), 1);
                req.setAttribute("productoSugeridoId", productoSugeridoId);
                req.setAttribute("cantidadSugeridaInicial", cantidadSugerida);
                req.setAttribute("abrirModalNuevaOrden", true);
            }

            String verOrdenId = req.getParameter("ver");
            if (verOrdenId != null && !verOrdenId.isBlank()) {
                Optional<OrdenCompra> ordenSeleccionada = ordenCompraService.buscarPorId(Integer.parseInt(verOrdenId));
                ordenSeleccionada.ifPresent(o -> req.setAttribute("ordenSeleccionada", o));
            }

            req.setAttribute("tituloPagina", "Órdenes de Compra a Proveedores");
            req.setAttribute("moduloActivo", "ordenes-compra");
            req.getRequestDispatcher("/WEB-INF/views/ordenes-compra/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar órdenes de compra.", e);
        }
    }

    /**
     * Despacha las acciones sobre órdenes de compra ({@code crear}, {@code recibir}, {@code cancelar}).
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la operación contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);
        boolean esAdministrador = Rol.ADMINISTRADOR.name().equals(rolSesion);
        String accion = req.getParameter("accion");
        Integer ordenIdParam = req.getParameter("ordenId") != null
                ? Integer.parseInt(req.getParameter("ordenId")) : null;

        try {
            // "aprobar"/"rechazar" exigen rol ADMINISTRADOR verificado aquí mismo: a diferencia
            // de /usuarios, /auditoria, etc., la ruta /ordenes-compra NO está en
            // AuthorizationFilter.RUTAS_SOLO_ADMINISTRADOR (la usan también los VENDEDOR para
            // crear/recibir), así que sin esta verificación cualquier VENDEDOR podría aprobar
            // su propia orden.
            ResultadoOperacion resultado = switch (accion) {
                case "crear" -> procesarCreacion(req, usuarioIdSesion);
                case "recibir" -> procesarRecepcion(req, usuarioIdSesion);
                case "cancelar" -> ordenCompraService.cancelar(ordenIdParam);
                case "aprobar" -> esAdministrador
                        ? ordenCompraService.aprobar(ordenIdParam, usuarioIdSesion)
                        : new ResultadoOperacion(false, "No tiene permiso para aprobar órdenes de compra.");
                case "rechazar" -> esAdministrador
                        ? ordenCompraService.rechazar(ordenIdParam, req.getParameter("motivo"))
                        : new ResultadoOperacion(false, "No tiene permiso para rechazar órdenes de compra.");
                default -> new ResultadoOperacion(false, "Acción no reconocida.");
            };

            if (resultado.exitoso && ("aprobar".equals(accion) || "rechazar".equals(accion))) {
                auditoriaService.registrar(usuarioIdSesion, "ORDEN_COMPRA_" + accion.toUpperCase(), "ORDEN_COMPRA",
                        ordenIdParam, resultado.mensaje, req.getRemoteAddr());
            }

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre la orden de compra.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/ordenes-compra");
    }

    /**
     * Arma las líneas de detalle desde los arreglos paralelos del formulario y registra la orden.
     *
     * @param req       petición HTTP con {@code proveedorId}, {@code observaciones} y los arreglos
     *                  {@code lineaProductoId}/{@code lineaCantidad}/{@code lineaPrecioUnitario}
     * @param usuarioId id del administrador que registra la orden
     * @return resultado de {@link OrdenCompraService#registrar}
     * @throws SQLException si falla la transacción
     */
    private ResultadoOperacion procesarCreacion(HttpServletRequest req, int usuarioId) throws SQLException {
        int proveedorId = Integer.parseInt(req.getParameter("proveedorId"));
        String observaciones = req.getParameter("observaciones");

        String[] productoIds = req.getParameterValues("lineaProductoId");
        String[] cantidades = req.getParameterValues("lineaCantidad");
        String[] precios = req.getParameterValues("lineaPrecioUnitario");

        List<DetalleOrdenCompra> lineas = new ArrayList<>();
        if (productoIds != null) {
            for (int i = 0; i < productoIds.length; i++) {
                if (productoIds[i] == null || productoIds[i].isBlank()) {
                    continue;
                }
                DetalleOrdenCompra d = new DetalleOrdenCompra();
                d.setProductoId(Integer.parseInt(productoIds[i]));
                d.setCantidadPedida(Integer.parseInt(cantidades[i]));
                d.setPrecioUnitario(new BigDecimal(precios[i]));
                lineas.add(d);
            }
        }

        return ordenCompraService.registrar(proveedorId, usuarioId, observaciones, lineas);
    }

    /**
     * Arma el mapa {@code detalleId -> cantidad recibida} desde los arreglos paralelos
     * del formulario y registra la recepción de mercadería.
     *
     * @param req       petición HTTP con {@code ordenId} y los arreglos {@code detalleId}/{@code cantidadRecibida}
     * @param usuarioId id del usuario que registra la recepción
     * @return resultado de {@link OrdenCompraService#recibirMercaderia}
     * @throws SQLException si falla la transacción
     */
    private ResultadoOperacion procesarRecepcion(HttpServletRequest req, int usuarioId) throws SQLException {
        int ordenId = Integer.parseInt(req.getParameter("ordenId"));

        String[] detalleIds = req.getParameterValues("detalleId");
        String[] cantidadesRecibidas = req.getParameterValues("cantidadRecibida");
        String[] fechasVencimiento = req.getParameterValues("fechaVencimiento");

        Map<Integer, Integer> cantidades = new HashMap<>();
        Map<Integer, LocalDate> fechasVencimientoPorDetalle = new HashMap<>();
        if (detalleIds != null) {
            for (int i = 0; i < detalleIds.length; i++) {
                int cantidad = (cantidadesRecibidas[i] == null || cantidadesRecibidas[i].isBlank())
                        ? 0 : Integer.parseInt(cantidadesRecibidas[i]);
                int detalleId = Integer.parseInt(detalleIds[i]);
                cantidades.put(detalleId, cantidad);

                if (fechasVencimiento != null && i < fechasVencimiento.length
                        && fechasVencimiento[i] != null && !fechasVencimiento[i].isBlank()) {
                    fechasVencimientoPorDetalle.put(detalleId, LocalDate.parse(fechasVencimiento[i]));
                }
            }
        }

        return ordenCompraService.recibirMercaderia(ordenId, cantidades, fechasVencimientoPorDetalle, usuarioId);
    }
}
