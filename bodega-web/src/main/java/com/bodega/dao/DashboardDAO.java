package com.bodega.dao;

import com.bodega.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Consultas agregadas para los paneles de control (administrador y vendedor).
 * Se agrupan en un DAO propio porque son lecturas de solo resumen (COUNT/SUM)
 * que no pertenecen naturalmente a ningún DAO de entidad concreta.
 */
public class DashboardDAO {

    /**
     * @param usuarioId id de vendedor a filtrar, o {@code null} para sumar de todos (vista administrador)
     * @return total facturado en ventas completadas de hoy
     * @throws SQLException si falla la consulta
     */
    public BigDecimal totalVentasHoy(Integer usuarioId) throws SQLException {
        // Consulta la vista vista_ventas_hoy (ver schema.sql) en vez de repetir aquí el
        // rango de fecha CURDATE()/CURDATE() + INTERVAL 1 DAY.
        String sql = "SELECT COALESCE(SUM(total), 0) FROM vista_ventas_hoy" +
                (usuarioId != null ? " WHERE usuario_id = ?" : "");
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (usuarioId != null) {
                ps.setInt(1, usuarioId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * @param usuarioId id de vendedor a filtrar, o {@code null} para sumar de todos (vista administrador)
     * @return total facturado en ventas completadas del mes en curso
     * @throws SQLException si falla la consulta
     */
    public BigDecimal totalVentasMes(Integer usuarioId) throws SQLException {
        // Consulta autocontenida (sin fragmentos de SQL pasados como parámetro entre
        // métodos): el rango de fechas del mes en curso va siempre fijo en la propia
        // sentencia, nunca ensamblado a partir de un valor externo.
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE estado = 'COMPLETADA' " +
                "AND fecha_creacion >= DATE_FORMAT(NOW(), '%Y-%m-01') " +
                "AND fecha_creacion < DATE_FORMAT(NOW(), '%Y-%m-01') + INTERVAL 1 MONTH" +
                (usuarioId != null ? " AND usuario_id = ?" : "");
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (usuarioId != null) {
                ps.setInt(1, usuarioId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * @param usuarioId id de vendedor a filtrar, o {@code null} para contar de todos
     * @return cantidad de ventas completadas hoy
     * @throws SQLException si falla la consulta
     */
    public int contarVentasHoy(Integer usuarioId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM vista_ventas_hoy" + (usuarioId != null ? " WHERE usuario_id = ?" : "");
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (usuarioId != null) {
                ps.setInt(1, usuarioId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * @return cantidad de ventas anuladas en el mes en curso
     * @throws SQLException si falla la consulta
     */
    public int contarVentasAnuladasMes() throws SQLException {
        String sql = "SELECT COUNT(*) FROM ventas WHERE estado = 'ANULADA' " +
                "AND fecha_creacion >= DATE_FORMAT(NOW(), '%Y-%m-01')";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * @return cantidad de productos en estado ACTIVO
     * @throws SQLException si falla la consulta
     */
    public int contarProductosActivos() throws SQLException {
        return contarSimple("SELECT COUNT(*) FROM productos WHERE estado = 'ACTIVO'");
    }

    /**
     * @return cantidad de productos activos con stock actual igual o menor a su stock mínimo
     * @throws SQLException si falla la consulta
     */
    public int contarProductosStockBajo() throws SQLException {
        return contarSimple("SELECT COUNT(*) FROM productos WHERE stock_actual <= stock_minimo AND estado = 'ACTIVO'");
    }

    /**
     * @return cantidad de clientes en estado ACTIVO
     * @throws SQLException si falla la consulta
     */
    public int contarClientesActivos() throws SQLException {
        return contarSimple("SELECT COUNT(*) FROM clientes WHERE estado = 'ACTIVO'");
    }

    /**
     * @return cantidad de órdenes de compra en estado PENDIENTE o RECIBIDA_PARCIAL
     * @throws SQLException si falla la consulta
     */
    public int contarOrdenesCompraPendientes() throws SQLException {
        return contarSimple("SELECT COUNT(*) FROM ordenes_compra WHERE estado IN ('PENDIENTE', 'RECIBIDA_PARCIAL')");
    }

    /**
     * @param sql consulta {@code SELECT COUNT(*) ...} sin parámetros
     * @return el conteo devuelto por la consulta
     * @throws SQLException si falla la consulta
     */
    private int contarSimple(String sql) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
