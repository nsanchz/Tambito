package com.bodega.service;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.dao.MovimientoInventarioDAO;
import com.bodega.dao.OrdenCompraDAO;
import com.bodega.dao.VentaDAO;
import com.bodega.model.EstadoOrdenCompra;
import com.bodega.model.EstadoVenta;
import com.bodega.model.LoteProducto;
import com.bodega.model.MetodoPago;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.OrdenCompra;
import com.bodega.model.TipoMovimiento;
import com.bodega.model.Venta;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera exportaciones a Excel (.xlsx) del Kárdex, historial de ventas y órdenes de compra,
 * usando Apache POI. Es la contraparte de {@link ReporteService} (que genera PDF con OpenPDF):
 * no se fusionan en una sola clase para no mezclar ambas dependencias en una única responsabilidad,
 * pero ambas reutilizan los mismos DAOs para no duplicar el acceso a datos.
 */
public class ExportExcelService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FORMATO_SOLO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovimientoInventarioDAO movimientoDAO;
    private final LoteProductoDAO loteProductoDAO;
    private final VentaDAO ventaDAO;
    private final OrdenCompraDAO ordenCompraDAO;

    public ExportExcelService() {
        this.movimientoDAO = new MovimientoInventarioDAO();
        this.loteProductoDAO = new LoteProductoDAO();
        this.ventaDAO = new VentaDAO();
        this.ordenCompraDAO = new OrdenCompraDAO();
    }

    /**
     * Exporta el Kárdex general aplicando los mismos filtros disponibles en la pantalla de Inventario.
     *
     * @param filtroTipo    tipo de movimiento a filtrar, o {@code null} para no filtrar
     * @param productoId    id de producto a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto de búsqueda, o {@code null}/vacío para no filtrar
     * @return el archivo .xlsx generado como arreglo de bytes
     * @throws SQLException si falla la consulta de movimientos
     */
    public byte[] exportarKardexGeneral(TipoMovimiento filtroTipo, Integer productoId, String textoBusqueda) throws SQLException {
        List<MovimientoInventario> movimientos = movimientoDAO.listar(filtroTipo, productoId, textoBusqueda);
        return generarExcelKardex("Kardex", movimientos);
    }

    /**
     * Exporta el Kárdex completo de un producto específico: todas las entradas, salidas,
     * ajustes, mermas, devoluciones y anulaciones que lo afectaron, en orden cronológico.
     *
     * @param productoId id del producto a exportar
     * @return el archivo .xlsx generado como arreglo de bytes
     * @throws SQLException si falla la consulta de movimientos
     */
    public byte[] exportarKardexProducto(int productoId) throws SQLException {
        List<MovimientoInventario> movimientos = movimientoDAO.listarPorProducto(productoId);
        return generarExcelKardex("Kardex del producto", movimientos);
    }

    private byte[] generarExcelKardex(String nombreHoja, List<MovimientoInventario> movimientos) throws SQLException {
        Map<Integer, LoteProducto> lotesConsultados = new HashMap<>();

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet(nombreHoja);
            CellStyle estiloEncabezado = crearEstiloEncabezado(libro);

            crearFilaEncabezado(hoja, estiloEncabezado, "N° movimiento", "Fecha", "Tipo", "Producto",
                    "Lote", "Vencimiento del lote", "Cantidad", "Stock anterior", "Stock resultante",
                    "Motivo", "Observaciones", "Usuario");

            int filaIndex = 1;
            for (MovimientoInventario m : movimientos) {
                Row fila = hoja.createRow(filaIndex++);
                fila.createCell(0).setCellValue(m.getNumeroMovimiento());
                fila.createCell(1).setCellValue(m.getFecha() != null ? m.getFecha().format(FORMATO_FECHA) : "-");
                fila.createCell(2).setCellValue(m.getTipo().name());
                fila.createCell(3).setCellValue(m.getProductoNombre());
                fila.createCell(4).setCellValue(m.getNumeroLote() != null ? m.getNumeroLote() : "-");

                String vencimientoLote = "-";
                if (m.getLoteId() != null) {
                    LoteProducto lote = lotesConsultados.computeIfAbsent(m.getLoteId(), id -> {
                        try {
                            return loteProductoDAO.buscarPorId(id).orElse(null);
                        } catch (SQLException e) {
                            return null;
                        }
                    });
                    if (lote != null && lote.getFechaVencimiento() != null) {
                        vencimientoLote = lote.getFechaVencimiento().format(FORMATO_SOLO_FECHA);
                    }
                }
                fila.createCell(5).setCellValue(vencimientoLote);

                fila.createCell(6).setCellValue(m.getCantidad());
                fila.createCell(7).setCellValue(m.getStockAnterior());
                fila.createCell(8).setCellValue(m.getStockResultante());
                fila.createCell(9).setCellValue(m.getMotivo());
                fila.createCell(10).setCellValue(m.getObservaciones() != null ? m.getObservaciones() : "-");
                fila.createCell(11).setCellValue(m.getUsuarioNombre());
            }

            autoajustarColumnas(hoja, 12);
            return escribirLibro(libro);
        } catch (IOException e) {
            throw new SQLException("Error al generar el archivo Excel del Kárdex.", e);
        }
    }

    /**
     * Exporta el historial de ventas aplicando los mismos filtros disponibles en la pantalla de Ventas.
     *
     * @param filtroEstado     estado a filtrar, o {@code null} para no filtrar
     * @param filtroMetodoPago método de pago a filtrar, o {@code null} para no filtrar
     * @param usuarioId        id de vendedor a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda    texto de búsqueda, o {@code null}/vacío para no filtrar
     * @return el archivo .xlsx generado como arreglo de bytes
     * @throws SQLException si falla la consulta de ventas
     */
    public byte[] exportarHistorialVentas(EstadoVenta filtroEstado, MetodoPago filtroMetodoPago,
                                           Integer usuarioId, String textoBusqueda) throws SQLException {
        List<Venta> ventas = ventaDAO.listar(filtroEstado, filtroMetodoPago, usuarioId, textoBusqueda);

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Ventas");
            CellStyle estiloEncabezado = crearEstiloEncabezado(libro);

            crearFilaEncabezado(hoja, estiloEncabezado, "Comprobante", "Fecha", "Cliente", "Vendedor",
                    "Método de pago", "Subtotal", "Descuento", "IGV", "Total", "Estado", "Motivo de anulación");

            int filaIndex = 1;
            for (Venta v : ventas) {
                Row fila = hoja.createRow(filaIndex++);
                fila.createCell(0).setCellValue(v.getNumeroComprobante());
                fila.createCell(1).setCellValue(v.getFechaCreacion() != null ? v.getFechaCreacion().format(FORMATO_FECHA) : "-");
                fila.createCell(2).setCellValue(v.getClienteNombre());
                fila.createCell(3).setCellValue(v.getUsuarioNombre());
                fila.createCell(4).setCellValue(v.getMetodoPago().name());
                fila.createCell(5).setCellValue(v.getSubtotalImponible().doubleValue());
                fila.createCell(6).setCellValue(v.getDescuento().doubleValue());
                fila.createCell(7).setCellValue(v.getIgv().doubleValue());
                fila.createCell(8).setCellValue(v.getTotal().doubleValue());
                fila.createCell(9).setCellValue(v.getEstado().name());
                fila.createCell(10).setCellValue(v.getMotivoAnulacion() != null ? v.getMotivoAnulacion() : "-");
            }

            autoajustarColumnas(hoja, 11);
            return escribirLibro(libro);
        } catch (IOException e) {
            throw new SQLException("Error al generar el archivo Excel de ventas.", e);
        }
    }

    /**
     * Exporta las órdenes de compra a proveedores ("pedidos"/OCs son la misma entidad).
     *
     * @param filtroEstado estado a filtrar, o {@code null} para listar todas
     * @return el archivo .xlsx generado como arreglo de bytes
     * @throws SQLException si falla la consulta de órdenes de compra
     */
    public byte[] exportarOrdenesCompra(EstadoOrdenCompra filtroEstado) throws SQLException {
        List<OrdenCompra> ordenes = ordenCompraDAO.listar(filtroEstado);

        try (XSSFWorkbook libro = new XSSFWorkbook()) {
            Sheet hoja = libro.createSheet("Ordenes de compra");
            CellStyle estiloEncabezado = crearEstiloEncabezado(libro);

            crearFilaEncabezado(hoja, estiloEncabezado, "N° orden", "Proveedor", "Usuario", "Estado",
                    "Observaciones", "Fecha de creación", "Última recepción");

            int filaIndex = 1;
            for (OrdenCompra o : ordenes) {
                Row fila = hoja.createRow(filaIndex++);
                fila.createCell(0).setCellValue(o.getNumero());
                fila.createCell(1).setCellValue(o.getProveedorNombre());
                fila.createCell(2).setCellValue(o.getUsuarioNombre());
                fila.createCell(3).setCellValue(o.getEstado().name());
                fila.createCell(4).setCellValue(o.getObservaciones() != null ? o.getObservaciones() : "-");
                fila.createCell(5).setCellValue(o.getFechaCreacion() != null ? o.getFechaCreacion().format(FORMATO_FECHA) : "-");
                fila.createCell(6).setCellValue(o.getFechaUltimaRecepcion() != null ? o.getFechaUltimaRecepcion().format(FORMATO_FECHA) : "-");
            }

            autoajustarColumnas(hoja, 7);
            return escribirLibro(libro);
        } catch (IOException e) {
            throw new SQLException("Error al generar el archivo Excel de órdenes de compra.", e);
        }
    }

    private CellStyle crearEstiloEncabezado(XSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());

        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        estilo.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private void crearFilaEncabezado(Sheet hoja, CellStyle estilo, String... columnas) {
        Row fila = hoja.createRow(0);
        for (int i = 0; i < columnas.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilo);
        }
    }

    private void autoajustarColumnas(Sheet hoja, int cantidadColumnas) {
        for (int i = 0; i < cantidadColumnas; i++) {
            hoja.autoSizeColumn(i);
        }
    }

    private byte[] escribirLibro(XSSFWorkbook libro) throws IOException {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            libro.write(salida);
            return salida.toByteArray();
        }
    }
}
