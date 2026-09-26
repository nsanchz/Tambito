package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.LoteProducto;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acceso a datos de los lotes de producto (trazabilidad de vencimiento). Los métodos
 * de escritura y la consulta FEFO reciben una {@link Connection} externa porque siempre
 * participan en la transacción ACID de una venta, anulación, recepción de orden de compra
 * o movimiento manual de inventario.
 */
public class LoteProductoDAO {

    /**
     * Inserta un lote nuevo dentro de una transacción externa y le asigna su número correlativo.
     *
     * @param con   conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param lote  lote a crear (sin id ni número de lote)
     * @return el id autogenerado del lote
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Connection con, LoteProducto lote) throws SQLException {
        String sqlInsert = "INSERT INTO lotes_producto " +
                "(producto_id, fecha_vencimiento, cantidad_inicial, cantidad_actual, precio_compra, " +
                "orden_compra_id, usuario_id, fecha_ingreso) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        int id;
        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lote.getProductoId());
            if (lote.getFechaVencimiento() != null) {
                ps.setDate(2, Date.valueOf(lote.getFechaVencimiento()));
            } else {
                ps.setNull(2, java.sql.Types.DATE);
            }
            ps.setInt(3, lote.getCantidadInicial());
            ps.setInt(4, lote.getCantidadActual());
            if (lote.getPrecioCompra() != null) {
                ps.setBigDecimal(5, lote.getPrecioCompra());
            } else {
                ps.setNull(5, java.sql.Types.DECIMAL);
            }
            if (lote.getOrdenCompraId() != null) {
                ps.setInt(6, lote.getOrdenCompraId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setInt(7, lote.getUsuarioId());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID generado al crear el lote de producto.");
                }
                id = rs.getInt(1);
            }
        }

        String numeroLote = String.format("LOT-%06d", id);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE lotes_producto SET numero_lote = ? WHERE id = ?")) {
            ps.setString(1, numeroLote);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        return id;
    }

    /**
     * Lotes con stock disponible de un producto, bloqueados con {@code FOR UPDATE} y
     * ordenados en orden FEFO (First-Expired-First-Out): primero los que tienen fecha de
     * vencimiento más próxima, y al final los que no tienen fecha de vencimiento (no perecibles).
     *
     * @param con        conexión de la transacción externa (venta, anulación o ajuste de inventario)
     * @param productoId id del producto a consultar
     * @return lotes con {@code cantidad_actual > 0}, en orden de consumo FEFO
     * @throws SQLException si falla la consulta
     */
    public List<LoteProducto> obtenerDisponiblesParaActualizar(Connection con, int productoId) throws SQLException {
        String sql = "SELECT * FROM lotes_producto WHERE producto_id = ? AND cantidad_actual > 0 " +
                "ORDER BY (fecha_vencimiento IS NULL) ASC, fecha_vencimiento ASC, id ASC FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoteProducto> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Descuenta cantidad de un lote dentro de una transacción externa. La condición
     * {@code cantidad_actual >= ?} en el WHERE hace que la actualización afecte 0 filas
     * si no hay cantidad suficiente en el lote, lo que se traduce en la excepción de abajo.
     *
     * @param con      conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param loteId   id del lote a descontar
     * @param cantidad unidades a descontar
     * @throws SQLException si el lote no existe o no tiene cantidad suficiente
     */
    public void descontarCantidad(Connection con, int loteId, int cantidad) throws SQLException {
        String sql = "UPDATE lotes_producto SET cantidad_actual = cantidad_actual - ? WHERE id = ? AND cantidad_actual >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, loteId);
            ps.setInt(3, cantidad);
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Cantidad insuficiente en el lote ID " + loteId);
            }
        }
    }

    /**
     * Devuelve cantidad a un lote dentro de una transacción externa (anulación de venta).
     *
     * @param con      conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param loteId   id del lote a incrementar
     * @param cantidad unidades a devolver
     * @throws SQLException si falla la actualización
     */
    public void incrementarCantidad(Connection con, int loteId, int cantidad) throws SQLException {
        String sql = "UPDATE lotes_producto SET cantidad_actual = cantidad_actual + ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, loteId);
            ps.executeUpdate();
        }
    }

    /**
     * @param id id del lote
     * @return el lote si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<LoteProducto> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM lotes_producto WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * @param productoId id del producto
     * @return todos los lotes del producto (con o sin stock), ordenados por fecha de vencimiento
     * @throws SQLException si falla la consulta
     */
    public List<LoteProducto> listarPorProducto(int productoId) throws SQLException {
        String sql = "SELECT * FROM lotes_producto WHERE producto_id = ? " +
                "ORDER BY (fecha_vencimiento IS NULL) ASC, fecha_vencimiento ASC, id ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoteProducto> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Lotes con stock disponible próximos a vencer dentro de la ventana indicada, de
     * productos activos, con el nombre del producto ya resuelto (join).
     *
     * @param diasVentana días hacia adelante desde hoy a considerar "próximo a vencer"
     * @return lotes por vencer, ordenados por fecha de vencimiento ascendente
     * @throws SQLException si falla la consulta
     */
    public List<LoteProducto> listarPorVencer(int diasVentana) throws SQLException {
        String sql = "SELECT l.*, p.nombre AS producto_nombre FROM lotes_producto l " +
                "JOIN productos p ON p.id = l.producto_id " +
                "WHERE l.fecha_vencimiento IS NOT NULL AND l.cantidad_actual > 0 AND p.estado = 'ACTIVO' " +
                "AND l.fecha_vencimiento <= CURDATE() + INTERVAL ? DAY " +
                "ORDER BY l.fecha_vencimiento ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, diasVentana);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoteProducto> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Fecha de vencimiento más próxima entre los lotes con stock disponible de cada producto,
     * usada para la columna "Vencimiento" del catálogo de productos.
     *
     * @return mapa {@code productoId -> fecha de vencimiento más próxima} (solo incluye productos
     *         que tienen al menos un lote con stock y fecha de vencimiento no nula)
     * @throws SQLException si falla la consulta
     */
    public Map<Integer, java.time.LocalDate> obtenerProximoVencimientoPorProducto() throws SQLException {
        String sql = "SELECT producto_id, MIN(fecha_vencimiento) AS proximo_vencimiento FROM lotes_producto " +
                "WHERE cantidad_actual > 0 AND fecha_vencimiento IS NOT NULL GROUP BY producto_id";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<Integer, java.time.LocalDate> resultado = new HashMap<>();
            while (rs.next()) {
                resultado.put(rs.getInt("producto_id"), rs.getDate("proximo_vencimiento").toLocalDate());
            }
            return resultado;
        }
    }

    private LoteProducto mapear(ResultSet rs) throws SQLException {
        LoteProducto l = new LoteProducto();
        l.setId(rs.getInt("id"));
        l.setNumeroLote(rs.getString("numero_lote"));
        l.setProductoId(rs.getInt("producto_id"));
        try {
            l.setProductoNombre(rs.getString("producto_nombre"));
        } catch (SQLException ignorado) {
            // Columna no proyectada en las variantes sin join; no es necesaria allí.
        }

        Date fechaVencimiento = rs.getDate("fecha_vencimiento");
        l.setFechaVencimiento(fechaVencimiento != null ? fechaVencimiento.toLocalDate() : null);

        l.setCantidadInicial(rs.getInt("cantidad_inicial"));
        l.setCantidadActual(rs.getInt("cantidad_actual"));
        l.setPrecioCompra(rs.getBigDecimal("precio_compra"));

        int ordenCompraId = rs.getInt("orden_compra_id");
        l.setOrdenCompraId(rs.wasNull() ? null : ordenCompraId);

        l.setUsuarioId(rs.getInt("usuario_id"));

        Timestamp fechaIngreso = rs.getTimestamp("fecha_ingreso");
        l.setFechaIngreso(fechaIngreso != null ? fechaIngreso.toLocalDateTime() : null);

        return l;
    }
}
