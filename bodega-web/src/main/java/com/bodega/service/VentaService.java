package com.bodega.service;

import com.bodega.config.DatabaseConfig;
import com.bodega.dao.ClienteDAO;
import com.bodega.dao.LoteProductoDAO;
import com.bodega.dao.MovimientoInventarioDAO;
import com.bodega.dao.ProductoDAO;
import com.bodega.dao.VentaDAO;
import com.bodega.model.DetalleVenta;
import com.bodega.model.EstadoVenta;
import com.bodega.model.MetodoPago;
import com.bodega.model.MonedaPago;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.Producto;
import com.bodega.model.TipoComprobante;
import com.bodega.model.TipoMovimiento;
import com.bodega.model.Venta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de ventas: el módulo transaccional más crítico del sistema.
 * <p>
 * {@link #registrarVenta} implementa el requisito ACID estricto: dentro de una única
 * transacción (autoCommit = false) se bloquea cada producto vendido, se valida stock,
 * se calculan los importes en el servidor (nunca se confía en el precio enviado por el
 * cliente), se inserta la cabecera y el detalle de la venta, se descuenta el stock y se
 * inserta un movimiento SALIDA_VENTA inmutable en el Kárdex por cada línea. Si cualquier
 * paso falla (por ejemplo, stock insuficiente de un producto a mitad del carrito), se hace
 * rollback de TODO — no queda ni la venta, ni el descuento de stock, ni el movimiento de
 * Kárdex a medias.
 * <p>
 * {@link #anularVenta} hace el proceso inverso: solo permitido para ADMINISTRADOR (validado
 * por el Servlet antes de llamar aquí), devuelve el stock de cada línea e inserta un
 * movimiento ANULACION_VENTA en el Kárdex, también con rollback total ante cualquier fallo.
 */
public class VentaService {

    /** Puntos de fidelización otorgados por cada S/ 10 del total de una venta completada. */
    private static final BigDecimal SOLES_POR_PUNTO = BigDecimal.TEN;

    private final VentaDAO ventaDAO;
    private final ProductoDAO productoDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final LoteProductoDAO loteProductoDAO;
    private final LoteProductoService loteProductoService;
    private final ClienteDAO clienteDAO;
    private final ConfiguracionService configuracionService;
    private final TipoCambioService tipoCambioService;

    public VentaService() {
        this.ventaDAO = new VentaDAO();
        this.productoDAO = new ProductoDAO();
        this.movimientoDAO = new MovimientoInventarioDAO();
        this.loteProductoDAO = new LoteProductoDAO();
        this.loteProductoService = new LoteProductoService();
        this.clienteDAO = new ClienteDAO();
        this.configuracionService = new ConfiguracionService();
        this.tipoCambioService = new TipoCambioService();
    }

    /** Línea del carrito del POS enviada por el cliente: producto y cantidad (nunca el precio). */
    public static class ItemCarrito {
        public final int productoId;
        public final int cantidad;

        public ItemCarrito(int productoId, int cantidad) {
            this.productoId = productoId;
            this.cantidad = cantidad;
        }
    }

    /** Resultado de registrar o anular una venta. */
    public static class ResultadoVenta {
        public final boolean exitoso;
        public final String mensaje;
        public final Integer ventaId;

        public ResultadoVenta(boolean exitoso, String mensaje, Integer ventaId) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.ventaId = ventaId;
        }
    }

    /**
     * Lista ventas aplicando filtros opcionales, delegando directamente en {@link VentaDAO#listar}.
     *
     * @param filtroEstado     estado a filtrar, o {@code null} para no filtrar
     * @param filtroMetodoPago método de pago a filtrar, o {@code null} para no filtrar
     * @param usuarioId        id de vendedor a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda    texto a buscar en comprobante o documento del cliente, o {@code null}/vacío para no filtrar
     * @return ventas que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Venta> listar(EstadoVenta filtroEstado, MetodoPago filtroMetodoPago, Integer usuarioId, String textoBusqueda) throws SQLException {
        return ventaDAO.listar(filtroEstado, filtroMetodoPago, usuarioId, textoBusqueda);
    }

    /**
     * @param id id de la venta
     * @return la venta con su detalle, o {@link Optional#empty()} si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<Venta> buscarPorId(int id) throws SQLException {
        return ventaDAO.buscarPorIdConDetalle(id);
    }

    /**
     * Registra una venta completa dentro de una única transacción ACID (ver Javadoc de clase).
     *
     * @param items          líneas del carrito (producto y cantidad; el precio se toma del catálogo, no del cliente)
     * @param tipoComprobante BOLETA o FACTURA
     * @param clienteId      id del cliente asociado, o {@code null} para "Cliente varios"
     * @param metodoPago     método de pago declarado por el cajero
     * @param descuento      monto de descuento a aplicar sobre el subtotal (no puede exceder el subtotal)
     * @param terminalId     terminal/caja desde la que se registra la venta
     * @param usuarioId      id del cajero que registra la venta
     * @param monedaPago     PEN (normal) o USD si el cliente pagó en efectivo en dólares; el total
     *                       de la venta SIEMPRE se calcula y se guarda en soles independientemente
     *                       de esto (ver comentario en {@code Venta.monedaPago})
     * @param montoRecibidoUsd monto en dólares que el cliente entregó, requerido solo cuando
     *                       {@code monedaPago == USD} (se usa únicamente para el ticket/conciliación,
     *                       no para calcular el total)
     * @return resultado con el id de la venta si fue exitosa, o un mensaje de error de negocio
     *         (carrito vacío, producto inexistente, stock insuficiente, descuento inválido, o tipo
     *         de cambio no disponible en este momento si se pidió pagar en USD) si no
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoVenta registrarVenta(List<ItemCarrito> items, TipoComprobante tipoComprobante, Integer clienteId,
                                          MetodoPago metodoPago, BigDecimal descuento, String terminalId,
                                          int usuarioId, MonedaPago monedaPago, BigDecimal montoRecibidoUsd) throws SQLException {
        if (items == null || items.isEmpty()) {
            return new ResultadoVenta(false, "El carrito de venta está vacío.", null);
        }
        if (tipoComprobante == TipoComprobante.FACTURA && clienteId == null) {
            // "Cliente varios" (sin documento identificado) solo puede recibir Boleta: una
            // Factura requiere un cliente con RUC asociado. Nunca se confía en el <select>
            // del navegador para esto, aunque ya venga deshabilitado ahí también.
            return new ResultadoVenta(false,
                    "No se puede emitir Factura para \"Cliente varios\". Seleccione un cliente registrado con RUC o cambie a Boleta.", null);
        }

        MonedaPago monedaEfectiva = monedaPago != null ? monedaPago : MonedaPago.PEN;
        BigDecimal tipoCambioAplicado = null;
        if (monedaEfectiva == MonedaPago.USD) {
            Optional<TipoCambioService.TipoCambio> tipoCambioOpt = tipoCambioService.obtenerTipoCambioDeHoy();
            if (tipoCambioOpt.isEmpty()) {
                return new ResultadoVenta(false,
                        "No se pudo obtener el tipo de cambio en este momento. Registre la venta en soles o intente nuevamente.", null);
            }
            tipoCambioAplicado = tipoCambioOpt.get().compra;
        }

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                BigDecimal subtotalImponible = BigDecimal.ZERO;
                DetalleVenta[] detalles = new DetalleVenta[items.size()];

                // Fase 1: bloquear cada producto, validar stock y calcular importes con el precio real de servidor.
                for (int i = 0; i < items.size(); i++) {
                    ItemCarrito item = items.get(i);
                    Optional<Producto> productoOpt = productoDAO.buscarPorIdParaActualizar(con, item.productoId);
                    if (productoOpt.isEmpty()) {
                        con.rollback();
                        return new ResultadoVenta(false, "Un producto del carrito ya no existe en el catálogo.", null);
                    }

                    Producto producto = productoOpt.get();
                    if (producto.getStockActual() < item.cantidad) {
                        con.rollback();
                        return new ResultadoVenta(false, "Stock insuficiente para \"" + producto.getNombre()
                                + "\": disponible " + producto.getStockActual() + ", solicitado " + item.cantidad + ".", null);
                    }

                    DetalleVenta detalle = new DetalleVenta();
                    detalle.setProductoId(producto.getId());
                    detalle.setProductoNombre(producto.getNombre());
                    detalle.setCantidad(item.cantidad);
                    detalle.setPrecioUnitario(producto.getPrecioVenta());
                    detalles[i] = detalle;

                    subtotalImponible = subtotalImponible.add(detalle.getSubtotal());
                }

                BigDecimal descuentoAplicado = descuento != null ? descuento : BigDecimal.ZERO;
                if (descuentoAplicado.compareTo(subtotalImponible) > 0) {
                    con.rollback();
                    return new ResultadoVenta(false, "El descuento no puede ser mayor al subtotal de la venta.", null);
                }

                BigDecimal baseImponible = subtotalImponible.subtract(descuentoAplicado);
                BigDecimal igv = baseImponible.multiply(configuracionService.obtenerTasaIgvComoFraccion())
                        .setScale(2, RoundingMode.HALF_UP);
                BigDecimal total = baseImponible.add(igv);

                Venta venta = new Venta();
                venta.setTipoComprobante(tipoComprobante);
                venta.setClienteId(clienteId);
                venta.setUsuarioId(usuarioId);
                venta.setTerminalId(terminalId);
                venta.setSubtotalImponible(subtotalImponible.setScale(2, RoundingMode.HALF_UP));
                venta.setDescuento(descuentoAplicado.setScale(2, RoundingMode.HALF_UP));
                venta.setIgv(igv);
                venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
                venta.setMetodoPago(metodoPago);
                venta.setMonedaPago(monedaEfectiva);
                venta.setTipoCambioAplicado(tipoCambioAplicado);
                venta.setMontoPagadoUsd(monedaEfectiva == MonedaPago.USD ? montoRecibidoUsd : null);
                venta.setEstado(EstadoVenta.COMPLETADA);

                int ventaId = ventaDAO.crearCabecera(con, venta);

                // Fase 2: insertar detalle y, por cada línea, consumir sus lotes en orden FEFO
                // (fecha de vencimiento más próxima primero), descontando stock e insertando un
                // movimiento de Kárdex por cada lote afectado (una línea puede tocar varios lotes).
                for (DetalleVenta detalle : detalles) {
                    ventaDAO.agregarDetalle(con, ventaId, detalle);

                    List<LoteProductoService.ConsumoLote> consumos =
                            loteProductoService.consumirFefo(con, detalle.getProductoId(), detalle.getCantidad());

                    for (LoteProductoService.ConsumoLote consumo : consumos) {
                        productoDAO.descontarStock(con, detalle.getProductoId(), consumo.cantidadConsumida());

                        MovimientoInventario movimiento = new MovimientoInventario();
                        movimiento.setProductoId(detalle.getProductoId());
                        movimiento.setLoteId(consumo.loteId());
                        movimiento.setTipo(TipoMovimiento.SALIDA_VENTA);
                        movimiento.setCantidad(consumo.cantidadConsumida());
                        // El stock anterior/resultante del Kárdex se referencia contra el momento de esta venta;
                        // se recalcula aquí porque productoDAO.descontarStock ya aplicó el descuento.
                        int stockLuegoDeVenta = obtenerStockActual(con, detalle.getProductoId());
                        movimiento.setStockAnterior(stockLuegoDeVenta + consumo.cantidadConsumida());
                        movimiento.setStockResultante(stockLuegoDeVenta);
                        movimiento.setMotivo("Venta " + tipoComprobante);
                        movimiento.setVentaId(ventaId);
                        movimiento.setUsuarioId(usuarioId);
                        movimientoDAO.registrar(con, movimiento);
                    }
                }

                // Fidelización: se acreditan puntos dentro de la misma transacción, de modo que
                // si algo falla y hay rollback, los puntos otorgados también se revierten.
                if (clienteId != null) {
                    int puntosGanados = total.divide(SOLES_POR_PUNTO, 0, RoundingMode.DOWN).intValue();
                    if (puntosGanados > 0) {
                        clienteDAO.sumarPuntos(con, clienteId, puntosGanados);
                    }
                }

                con.commit();
                return new ResultadoVenta(true, "Venta registrada correctamente.", ventaId);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Anula una venta completa: solo debe invocarse tras verificar que el usuario es
     * ADMINISTRADOR (esa verificación ocurre en el Servlet, no aquí). Devuelve el stock
     * de cada línea, revierte los puntos de fidelización otorgados y marca la venta como
     * ANULADA, todo dentro de una única transacción ACID.
     *
     * @param ventaId       id de la venta a anular
     * @param motivo        motivo de la anulación (obligatorio)
     * @param usuarioAdminId id del administrador que ejecuta la anulación
     * @return resultado exitoso, o un mensaje de error de negocio (venta inexistente, ya anulada,
     *         motivo vacío, producto ya no existe) si no se pudo anular
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoVenta anularVenta(int ventaId, String motivo, int usuarioAdminId) throws SQLException {
        if (motivo == null || motivo.isBlank()) {
            return new ResultadoVenta(false, "Debe indicar el motivo de la anulación.", null);
        }

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Optional<EstadoVenta> estadoActual = ventaDAO.obtenerEstadoParaActualizar(con, ventaId);
                if (estadoActual.isEmpty()) {
                    con.rollback();
                    return new ResultadoVenta(false, "La venta no existe.", null);
                }
                if (estadoActual.get() == EstadoVenta.ANULADA) {
                    con.rollback();
                    return new ResultadoVenta(false, "Esta venta ya se encuentra anulada.", null);
                }

                // Se revierte a partir de los movimientos SALIDA_VENTA originales (no del detalle de
                // venta) porque cada uno ya sabe exactamente qué lote afectó y cuánto le descontó;
                // así la reversión es lote por lote, no un ajuste agregado a un solo lote del producto.
                List<MovimientoInventario> movimientosOriginales = movimientoDAO.listarPorVentaParaAnular(con, ventaId);

                for (MovimientoInventario original : movimientosOriginales) {
                    Optional<Producto> productoOpt = productoDAO.buscarPorIdParaActualizar(con, original.getProductoId());
                    if (productoOpt.isEmpty()) {
                        con.rollback();
                        return new ResultadoVenta(false, "Un producto de la venta ya no existe en el catálogo.", null);
                    }
                    int stockAnterior = productoOpt.get().getStockActual();

                    productoDAO.incrementarStock(con, original.getProductoId(), original.getCantidad());
                    if (original.getLoteId() != null) {
                        loteProductoDAO.incrementarCantidad(con, original.getLoteId(), original.getCantidad());
                    }

                    MovimientoInventario movimiento = new MovimientoInventario();
                    movimiento.setProductoId(original.getProductoId());
                    movimiento.setLoteId(original.getLoteId());
                    movimiento.setTipo(TipoMovimiento.ANULACION_VENTA);
                    movimiento.setCantidad(original.getCantidad());
                    movimiento.setStockAnterior(stockAnterior);
                    movimiento.setStockResultante(stockAnterior + original.getCantidad());
                    movimiento.setMotivo("Anulación de venta: " + motivo);
                    movimiento.setVentaId(ventaId);
                    movimiento.setUsuarioId(usuarioAdminId);
                    movimientoDAO.registrar(con, movimiento);
                }

                ventaDAO.anular(con, ventaId, motivo, usuarioAdminId);

                // Revierte los puntos de fidelización otorgados originalmente por esta venta.
                Optional<VentaDAO.DatosClienteVenta> datosCliente = ventaDAO.obtenerDatosClienteParaAnular(con, ventaId);
                if (datosCliente.isPresent() && datosCliente.get().clienteId() != null) {
                    int puntosARevertir = datosCliente.get().total().divide(SOLES_POR_PUNTO, 0, RoundingMode.DOWN).intValue();
                    if (puntosARevertir > 0) {
                        clienteDAO.sumarPuntos(con, datosCliente.get().clienteId(), -puntosARevertir);
                    }
                }

                con.commit();
                return new ResultadoVenta(true, "Venta anulada correctamente. El stock ha sido restituido.", ventaId);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private int obtenerStockActual(Connection con, int productoId) throws SQLException {
        return productoDAO.buscarPorIdParaActualizar(con, productoId)
                .map(Producto::getStockActual)
                .orElseThrow(() -> new SQLException("Producto no encontrado durante el cálculo de Kárdex."));
    }
}
