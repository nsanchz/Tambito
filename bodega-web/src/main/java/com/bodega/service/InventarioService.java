package com.bodega.service;

import com.bodega.config.DatabaseConfig;
import com.bodega.dao.LoteProductoDAO;
import com.bodega.dao.MovimientoInventarioDAO;
import com.bodega.dao.ProductoDAO;
import com.bodega.model.LoteProducto;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.Producto;
import com.bodega.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio del Kárdex de inventario. Cada movimiento manual (entrada por compra,
 * ajuste, merma, devolución) se registra dentro de una transacción ACID propia:
 * se bloquea la fila del producto, se recalcula el stock, se inserta el movimiento
 * inmutable en el Kárdex y solo entonces se hace commit. Cualquier fallo revierte
 * ambos cambios (requisito de integridad transaccional).
 */
public class InventarioService {

    private final ProductoDAO productoDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final LoteProductoDAO loteProductoDAO;
    private final LoteProductoService loteProductoService;

    public InventarioService() {
        this.productoDAO = new ProductoDAO();
        this.movimientoDAO = new MovimientoInventarioDAO();
        this.loteProductoDAO = new LoteProductoDAO();
        this.loteProductoService = new LoteProductoService();
    }

    /** Resultado de registrar un movimiento manual de inventario. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;

        public ResultadoOperacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
    }

    /**
     * Lista movimientos del Kárdex aplicando filtros opcionales.
     *
     * @param filtroTipo    tipo de movimiento a filtrar, o {@code null} para no filtrar
     * @param productoId    id de producto a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return movimientos que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoInventario> listar(TipoMovimiento filtroTipo, Integer productoId, String textoBusqueda) throws SQLException {
        return movimientoDAO.listar(filtroTipo, productoId, textoBusqueda);
    }

    /**
     * Registra un movimiento manual de inventario (entrada por compra, ajuste, merma o
     * devolución) dentro de una transacción ACID propia: bloquea el producto, actualiza
     * su stock según el signo del tipo de movimiento e inserta el registro inmutable en
     * el Kárdex, con commit solo si todo tiene éxito.
     *
     * @param productoId       id del producto afectado
     * @param tipo             tipo de movimiento (determina si suma o resta stock)
     * @param cantidad         unidades a mover (debe ser mayor a cero)
     * @param motivo           motivo de la operación
     * @param observaciones    observaciones adicionales (ej. número de guía del proveedor)
     * @param fechaVencimiento fecha de vencimiento del lote a crear si {@code tipo} es de incremento
     *                         ({@code null} para productos no perecibles); ignorada si el tipo descuenta stock
     * @param usuarioId        id del usuario que registra el movimiento
     * @return resultado exitoso con el número del último movimiento generado, o un mensaje de error
     *         de negocio (cantidad inválida, producto inexistente, stock insuficiente) si no
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoOperacion registrarMovimiento(int productoId, TipoMovimiento tipo, int cantidad,
                                                   String motivo, String observaciones, LocalDate fechaVencimiento,
                                                   int usuarioId) throws SQLException {
        if (cantidad <= 0) {
            return new ResultadoOperacion(false, "La cantidad a mover debe ser mayor a cero.");
        }

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                Optional<Producto> productoOpt = productoDAO.buscarPorIdParaActualizar(con, productoId);
                if (productoOpt.isEmpty()) {
                    con.rollback();
                    return new ResultadoOperacion(false, "El producto seleccionado no existe.");
                }

                Producto producto = productoOpt.get();
                int stockAnterior = producto.getStockActual();
                int ultimoMovimientoId;

                if (tipo.isIncremento()) {
                    productoDAO.incrementarStock(con, productoId, cantidad);

                    LoteProducto lote = new LoteProducto();
                    lote.setProductoId(productoId);
                    lote.setFechaVencimiento(fechaVencimiento);
                    lote.setCantidadInicial(cantidad);
                    lote.setCantidadActual(cantidad);
                    lote.setUsuarioId(usuarioId);
                    int loteId = loteProductoDAO.crear(con, lote);

                    int stockResultante = stockAnterior + cantidad;
                    MovimientoInventario movimiento = new MovimientoInventario();
                    movimiento.setProductoId(productoId);
                    movimiento.setLoteId(loteId);
                    movimiento.setTipo(tipo);
                    movimiento.setCantidad(cantidad);
                    movimiento.setStockAnterior(stockAnterior);
                    movimiento.setStockResultante(stockResultante);
                    movimiento.setMotivo(motivo);
                    movimiento.setObservaciones(observaciones);
                    movimiento.setUsuarioId(usuarioId);
                    ultimoMovimientoId = movimientoDAO.registrar(con, movimiento);
                } else {
                    if (stockAnterior < cantidad) {
                        con.rollback();
                        return new ResultadoOperacion(false, "Stock insuficiente: disponible " + stockAnterior
                                + ", se intentó descontar " + cantidad + ".");
                    }

                    // Consumo FEFO: puede repartirse entre varios lotes, generando un movimiento por lote afectado.
                    List<LoteProductoService.ConsumoLote> consumos = loteProductoService.consumirFefo(con, productoId, cantidad);

                    ultimoMovimientoId = -1;
                    for (LoteProductoService.ConsumoLote consumo : consumos) {
                        productoDAO.descontarStock(con, productoId, consumo.cantidadConsumida());

                        int stockLuegoDelLote = obtenerStockActual(con, productoId);
                        MovimientoInventario movimiento = new MovimientoInventario();
                        movimiento.setProductoId(productoId);
                        movimiento.setLoteId(consumo.loteId());
                        movimiento.setTipo(tipo);
                        movimiento.setCantidad(consumo.cantidadConsumida());
                        movimiento.setStockAnterior(stockLuegoDelLote + consumo.cantidadConsumida());
                        movimiento.setStockResultante(stockLuegoDelLote);
                        movimiento.setMotivo(motivo);
                        movimiento.setObservaciones(observaciones);
                        movimiento.setUsuarioId(usuarioId);
                        ultimoMovimientoId = movimientoDAO.registrar(con, movimiento);
                    }
                }

                int stockFinal = obtenerStockActual(con, productoId);

                con.commit();
                return new ResultadoOperacion(true, "Movimiento MOV-" + String.format("%06d", ultimoMovimientoId)
                        + " registrado correctamente. Stock resultante: " + stockFinal + " unidades.");
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
