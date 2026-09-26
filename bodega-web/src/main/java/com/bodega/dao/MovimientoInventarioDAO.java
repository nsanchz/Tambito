package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.TipoMovimiento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos del Kárdex de inventario. La tabla es de solo inserción (insert-only):
 * no existen métodos de actualización ni borrado porque cada movimiento es un hecho
 * histórico inmutable. Los métodos que reciben una {@link Connection} externa participan
 * en la transacción ACID de una venta, anulación o movimiento manual (ver InventarioService
 * y VentaService), que son quienes deciden el commit/rollback final.
 */
public class MovimientoInventarioDAO {

    /**
     * Inserta el movimiento dentro de una transacción externa y le asigna su número correlativo.
     *
     * @param con conexión de la transacción externa (venta, anulación u orden de compra;
     *            nunca se abre ni cierra aquí)
     * @param m   movimiento a registrar (sin id ni número de movimiento)
     * @return el id autogenerado del movimiento
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int registrar(Connection con, MovimientoInventario m) throws SQLException {
        String sqlInsert = "INSERT INTO movimientos_inventario " +
                "(producto_id, lote_id, tipo, cantidad, stock_anterior, stock_resultante, motivo, observaciones, " +
                "venta_id, compra_id, usuario_id, fecha) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int id;
        try (PreparedStatement ps = con.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, m.getProductoId());
            if (m.getLoteId() != null) {
                ps.setInt(2, m.getLoteId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, m.getTipo().name());
            ps.setInt(4, m.getCantidad());
            ps.setInt(5, m.getStockAnterior());
            ps.setInt(6, m.getStockResultante());
            ps.setString(7, m.getMotivo());
            ps.setString(8, m.getObservaciones());
            if (m.getVentaId() != null) {
                ps.setInt(9, m.getVentaId());
            } else {
                ps.setNull(9, java.sql.Types.INTEGER);
            }
            if (m.getCompraId() != null) {
                ps.setInt(10, m.getCompraId());
            } else {
                ps.setNull(10, java.sql.Types.INTEGER);
            }
            ps.setInt(11, m.getUsuarioId());
            ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID generado al registrar el movimiento de Kárdex.");
                }
                id = rs.getInt(1);
            }
        }

        String numeroMovimiento = String.format("MOV-%06d", id);
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE movimientos_inventario SET numero_movimiento = ? WHERE id = ?")) {
            ps.setString(1, numeroMovimiento);
            ps.setInt(2, id);
            ps.executeUpdate();
        }

        return id;
    }

    /**
     * Promedio de unidades vendidas por día en los últimos {@code dias} días, calculado
     * a partir de los movimientos SALIDA_VENTA del Kárdex. Base de la sugerencia
     * automática de reposición en Órdenes de Compra.
     *
     * @param productoId id del producto a analizar
     * @param dias       tamaño de la ventana histórica a analizar, en días
     * @return unidades vendidas por día en promedio en la ventana indicada (0 si no hubo ventas)
     * @throws SQLException si falla la consulta
     */
    public double promedioVentaDiaria(int productoId, int dias) throws SQLException {
        String sql = "SELECT COALESCE(SUM(cantidad), 0) FROM movimientos_inventario " +
                "WHERE producto_id = ? AND tipo = 'SALIDA_VENTA' AND fecha >= NOW() - INTERVAL ? DAY";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ps.setInt(2, dias);
            try (ResultSet rs = ps.executeQuery()) {
                int totalVendido = rs.next() ? rs.getInt(1) : 0;
                return totalVendido / (double) dias;
            }
        }
    }

    /**
     * Lista movimientos del Kárdex aplicando filtros opcionales, con el nombre del
     * producto y del usuario ya resueltos (join) para la vista de Inventario.
     *
     * @param filtroTipo    tipo de movimiento a filtrar, o {@code null} para no filtrar por tipo
     * @param productoId    id de producto a filtrar, o {@code null} para no filtrar por producto
     * @param textoBusqueda texto a buscar en SKU, nombre de producto, motivo o número de movimiento,
     *                      o {@code null}/vacío para no filtrar
     * @return movimientos que cumplen los filtros, ordenados por fecha descendente
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoInventario> listar(TipoMovimiento filtroTipo, Integer productoId, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT m.*, p.nombre AS producto_nombre, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos " +
                        "FROM movimientos_inventario m " +
                        "JOIN productos p ON p.id = m.producto_id " +
                        "JOIN usuarios u ON u.id = m.usuario_id " +
                        "WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroTipo != null) {
            sql.append(" AND m.tipo = ?");
            parametros.add(filtroTipo.name());
        }
        if (productoId != null) {
            sql.append(" AND m.producto_id = ?");
            parametros.add(productoId);
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (p.sku LIKE ? OR p.nombre LIKE ? OR m.motivo LIKE ? OR m.numero_movimiento LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            for (int i = 0; i < 4; i++) {
                parametros.add(comodin);
            }
        }
        sql.append(" ORDER BY m.fecha DESC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<MovimientoInventario> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Historial cronológico completo de un producto (todos los tipos de movimiento),
     * con el nombre de usuario resuelto (join), usado para el export de Kárdex por producto.
     *
     * @param productoId id del producto
     * @return movimientos del producto, ordenados por fecha ascendente
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoInventario> listarPorProducto(int productoId) throws SQLException {
        String sql = "SELECT m.*, p.nombre AS producto_nombre, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, " +
                "lp.numero_lote AS numero_lote " +
                "FROM movimientos_inventario m " +
                "JOIN productos p ON p.id = m.producto_id " +
                "JOIN usuarios u ON u.id = m.usuario_id " +
                "LEFT JOIN lotes_producto lp ON lp.id = m.lote_id " +
                "WHERE m.producto_id = ? ORDER BY m.fecha ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MovimientoInventario> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Movimientos SALIDA_VENTA originales de una venta, dentro de la transacción de
     * anulación ya abierta, para revertir la cantidad exacta a cada lote afectado.
     *
     * @param con     conexión de la transacción externa
     * @param ventaId id de la venta a anular
     * @return movimientos SALIDA_VENTA de esa venta (con su lote_id ya poblado)
     * @throws SQLException si falla la consulta
     */
    public List<MovimientoInventario> listarPorVentaParaAnular(Connection con, int ventaId) throws SQLException {
        String sql = "SELECT * FROM movimientos_inventario WHERE venta_id = ? AND tipo = 'SALIDA_VENTA'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                List<MovimientoInventario> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        MovimientoInventario m = new MovimientoInventario();
        m.setId(rs.getInt("id"));
        m.setNumeroMovimiento(rs.getString("numero_movimiento"));
        m.setProductoId(rs.getInt("producto_id"));
        try {
            m.setProductoNombre(rs.getString("producto_nombre"));
        } catch (SQLException ignorado) {
            // Columna no proyectada en la variante sin join (listarPorVentaParaAnular); no es necesaria allí.
        }

        int loteId = rs.getInt("lote_id");
        m.setLoteId(rs.wasNull() ? null : loteId);
        try {
            m.setNumeroLote(rs.getString("numero_lote"));
        } catch (SQLException ignorado) {
            // Columna no proyectada en las variantes sin join a lotes_producto; no es necesaria allí.
        }

        m.setTipo(TipoMovimiento.valueOf(rs.getString("tipo")));
        m.setCantidad(rs.getInt("cantidad"));
        m.setStockAnterior(rs.getInt("stock_anterior"));
        m.setStockResultante(rs.getInt("stock_resultante"));
        m.setMotivo(rs.getString("motivo"));
        m.setObservaciones(rs.getString("observaciones"));

        int ventaId = rs.getInt("venta_id");
        m.setVentaId(rs.wasNull() ? null : ventaId);

        int compraId = rs.getInt("compra_id");
        m.setCompraId(rs.wasNull() ? null : compraId);

        m.setUsuarioId(rs.getInt("usuario_id"));
        try {
            m.setUsuarioNombre(rs.getString("usuario_nombres") + " " + rs.getString("usuario_apellidos"));
        } catch (SQLException ignorado) {
            // Columnas no proyectadas en la variante sin join (listarPorVentaParaAnular); no son necesarias allí.
        }

        Timestamp fecha = rs.getTimestamp("fecha");
        m.setFecha(fecha != null ? fecha.toLocalDateTime() : null);

        return m;
    }
}
