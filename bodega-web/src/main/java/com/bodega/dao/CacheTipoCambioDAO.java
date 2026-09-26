package com.bodega.dao;

import com.bodega.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caché del tipo de cambio USD del día (apisperu.com, fuente SUNAT), para no consultar la
 * API en cada venta pagada en dólares — un tipo de cambio ya consultado hoy sigue vigente
 * el resto del día calendario.
 */
public class CacheTipoCambioDAO {

    public static class Entrada {
        public final BigDecimal compra;
        public final BigDecimal venta;

        public Entrada(BigDecimal compra, BigDecimal venta) {
            this.compra = compra;
            this.venta = venta;
        }
    }

    /**
     * @return el tipo de cambio cacheado de HOY, si ya se consultó, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Entrada> buscarDeHoy() throws SQLException {
        String sql = "SELECT compra, venta FROM cache_tipo_cambio WHERE fecha = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Entrada(rs.getBigDecimal("compra"), rs.getBigDecimal("venta")));
            }
        }
    }

    /**
     * Guarda (o refresca) el tipo de cambio consultado hoy.
     *
     * @throws SQLException si falla la escritura
     */
    public void guardarDeHoy(BigDecimal compra, BigDecimal venta) throws SQLException {
        String sql = "INSERT INTO cache_tipo_cambio (fecha, compra, venta, fecha_consulta) VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE compra = VALUES(compra), venta = VALUES(venta), fecha_consulta = VALUES(fecha_consulta)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setBigDecimal(2, compra);
            ps.setBigDecimal(3, venta);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }
}
