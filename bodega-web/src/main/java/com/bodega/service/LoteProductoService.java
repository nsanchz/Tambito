package com.bodega.service;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.model.LoteProducto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lógica de consumo FEFO (First-Expired-First-Out) de lotes de producto, compartida por
 * {@link VentaService} (venta y anulación) e {@link InventarioService} (ajustes/mermas
 * manuales), evitando duplicar el algoritmo de reparto entre lotes en ambos lugares.
 * <p>
 * No abre transacción propia: recibe la {@link Connection} externa de la transacción ACID
 * controlada por el servicio llamador, igual que un DAO transaccional.
 */
public class LoteProductoService {

    private final LoteProductoDAO loteProductoDAO;

    public LoteProductoService() {
        this.loteProductoDAO = new LoteProductoDAO();
    }

    /** Porción consumida de un lote específico durante un reparto FEFO. */
    public record ConsumoLote(int loteId, String numeroLote, int cantidadConsumida) {
    }

    /**
     * Descuenta {@code cantidadRequerida} unidades del producto consumiendo sus lotes
     * disponibles en orden FEFO (fecha de vencimiento más próxima primero), pudiendo
     * repartir el consumo entre varios lotes si el primero no alcanza.
     *
     * @param con               conexión de la transacción externa (venta, anulación o ajuste manual)
     * @param productoId        id del producto a descontar
     * @param cantidadRequerida unidades a descontar en total
     * @return el detalle de cuánto se consumió de cada lote afectado, en el orden en que se consumieron
     * @throws SQLException si la suma de los lotes disponibles no alcanza la cantidad requerida
     *                       (inconsistencia entre el stock agregado del producto y sus lotes) o si
     *                       falla alguna operación de base de datos
     */
    public List<ConsumoLote> consumirFefo(Connection con, int productoId, int cantidadRequerida) throws SQLException {
        List<LoteProducto> lotesDisponibles = loteProductoDAO.obtenerDisponiblesParaActualizar(con, productoId);

        List<ConsumoLote> consumos = new ArrayList<>();
        int restante = cantidadRequerida;
        for (LoteProducto lote : lotesDisponibles) {
            if (restante <= 0) {
                break;
            }
            int aConsumir = Math.min(restante, lote.getCantidadActual());
            loteProductoDAO.descontarCantidad(con, lote.getId(), aConsumir);
            consumos.add(new ConsumoLote(lote.getId(), lote.getNumeroLote(), aConsumir));
            restante -= aConsumir;
        }

        if (restante > 0) {
            throw new SQLException("Inconsistencia de stock: el producto ID " + productoId
                    + " no tiene suficientes unidades en sus lotes para cubrir la cantidad solicitada"
                    + " (faltan " + restante + " unidades).");
        }

        return consumos;
    }
}
