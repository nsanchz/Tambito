package com.bodega.servlet;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.LoteProducto;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.Producto;
import com.bodega.model.TipoMovimiento;
import com.bodega.service.ExportExcelService;
import com.bodega.service.InventarioService;
import com.bodega.service.InventarioService.ResultadoOperacion;
import com.bodega.service.ProductoService;
import com.bodega.util.Constantes;
import com.bodega.util.DescargaHttpUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** Gestiona el módulo "Inventario y Movimientos de Almacén (Kárdex)". */
@WebServlet("/inventario")
public class InventarioServlet extends HttpServlet {

    private final InventarioService inventarioService = new InventarioService();
    private final ProductoService productoService = new ProductoService();
    private final LoteProductoDAO loteProductoDAO = new LoteProductoDAO();
    private final ExportExcelService exportExcelService = new ExportExcelService();

    /**
     * Lista movimientos del Kárdex aplicando filtros, junto con el catálogo de productos
     * activos (para el selector del modal) y las alertas de stock bajo.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String tipoParam = req.getParameter("tipo");
        String productoIdParam = req.getParameter("productoId");
        String busqueda = req.getParameter("busqueda");

        TipoMovimiento filtroTipo = (tipoParam != null && !tipoParam.isBlank()) ? TipoMovimiento.valueOf(tipoParam) : null;
        Integer productoId = (productoIdParam != null && !productoIdParam.isBlank()) ? Integer.parseInt(productoIdParam) : null;

        try {
            if ("excel".equals(req.getParameter("exportar"))) {
                byte[] contenido = (productoId != null && filtroTipo == null && (busqueda == null || busqueda.isBlank()))
                        ? exportExcelService.exportarKardexProducto(productoId)
                        : exportExcelService.exportarKardexGeneral(filtroTipo, productoId, busqueda);
                DescargaHttpUtil.enviarExcel(resp, contenido, "kardex.xlsx");
                return;
            }

            List<MovimientoInventario> movimientos = inventarioService.listar(filtroTipo, productoId, busqueda);
            List<Producto> productosActivos = productoService.listar(null, EstadoCuenta.ACTIVO, null);
            List<Producto> productosStockBajo = productoService.listarConStockBajoMinimo();

            req.setAttribute("movimientos", movimientos);
            req.setAttribute("productos", productosActivos);
            req.setAttribute("productosStockBajo", productosStockBajo);
            req.setAttribute("tiposMovimiento", TipoMovimiento.values());

            String verLotesProductoId = req.getParameter("verLotes");
            if (verLotesProductoId != null && !verLotesProductoId.isBlank()) {
                List<LoteProducto> lotesProducto = loteProductoDAO.listarPorProducto(Integer.parseInt(verLotesProductoId));
                req.setAttribute("lotesProducto", lotesProducto);
                req.setAttribute("productoLotesId", verLotesProductoId);
            }

            req.setAttribute("tituloPagina", "Inventario y Movimientos de Almacén (Kárdex)");
            req.setAttribute("moduloActivo", "inventario");
            req.getRequestDispatcher("/WEB-INF/views/inventario/movimientos.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar movimientos de inventario.", e);
        }
    }

    /**
     * Registra un movimiento manual de inventario (entrada por compra, ajuste, merma o devolución).
     *
     * @param req  petición HTTP con {@code productoId}, {@code tipoMovimiento}, {@code cantidad},
     *             {@code motivo} y {@code observaciones}
     * @param resp respuesta HTTP
     * @throws ServletException si falla la transacción contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        try {
            int productoId = Integer.parseInt(req.getParameter("productoId"));
            TipoMovimiento tipo = TipoMovimiento.valueOf(req.getParameter("tipoMovimiento"));
            int cantidad = Integer.parseInt(req.getParameter("cantidad"));
            String motivo = req.getParameter("motivo");
            String observaciones = req.getParameter("observaciones");
            String fechaVencimientoParam = req.getParameter("fechaVencimiento");
            LocalDate fechaVencimiento = (fechaVencimientoParam != null && !fechaVencimientoParam.isBlank())
                    ? LocalDate.parse(fechaVencimientoParam) : null;

            ResultadoOperacion resultado = inventarioService.registrarMovimiento(
                    productoId, tipo, cantidad, motivo, observaciones, fechaVencimiento, usuarioIdSesion);

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al registrar el movimiento de inventario.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/inventario");
    }
}
