package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.TipoDocumentoCliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Caché de resultados de la consulta externa de RUC/DNI (apisperu.com), para no repetir
 * llamadas al mismo número y cuidar la cuota mensual gratuita del servicio de terceros.
 * Solo guarda datos ya públicos (nombre/razón social, dirección), nunca el token de la API.
 */
public class CacheConsultaDocumentoDAO {

    /** Un resultado de consulta se considera vigente por este período antes de refrescarse. */
    private static final int DIAS_VIGENCIA = 30;

    public static class Entrada {
        public final String nombreORazonSocial;
        public final String direccion;

        public Entrada(String nombreORazonSocial, String direccion) {
            this.nombreORazonSocial = nombreORazonSocial;
            this.direccion = direccion;
        }
    }

    /**
     * @param numeroDocumento número a buscar en caché
     * @return la entrada cacheada si existe y todavía está vigente (dentro de {@link #DIAS_VIGENCIA}
     *         días), o {@link Optional#empty()} si no hay caché o ya expiró
     * @throws SQLException si falla la consulta
     */
    public Optional<Entrada> buscarVigente(String numeroDocumento) throws SQLException {
        String sql = "SELECT nombre_o_razon_social, direccion FROM cache_consulta_documento " +
                "WHERE numero_documento = ? AND fecha_consulta >= ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroDocumento);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now().minusDays(DIAS_VIGENCIA)));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Entrada(rs.getString("nombre_o_razon_social"), rs.getString("direccion")));
            }
        }
    }

    /**
     * Guarda (o refresca) el resultado de una consulta exitosa a la API externa.
     *
     * @throws SQLException si falla la escritura
     */
    public void guardar(String numeroDocumento, TipoDocumentoCliente tipo, String nombreORazonSocial, String direccion)
            throws SQLException {
        String sql = "INSERT INTO cache_consulta_documento " +
                "(numero_documento, tipo_documento, nombre_o_razon_social, direccion, fecha_consulta) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE nombre_o_razon_social = VALUES(nombre_o_razon_social), " +
                "direccion = VALUES(direccion), fecha_consulta = VALUES(fecha_consulta)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroDocumento);
            ps.setString(2, tipo.name());
            ps.setString(3, nombreORazonSocial);
            ps.setString(4, direccion);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }
}
