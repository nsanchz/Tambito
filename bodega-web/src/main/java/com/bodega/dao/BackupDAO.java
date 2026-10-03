package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.EstadoRespaldo;
import com.bodega.model.RegistroRespaldo;
import com.bodega.model.TipoRespaldo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de la bitácora de respaldos (bitacora_respaldos). Tabla de solo inserción. */
public class BackupDAO {

    /**
     * Inserta un registro de bitácora para una ejecución de respaldo (exitosa o fallida).
     *
     * @param r registro a insertar (sin id)
     * @return el id autogenerado
     * @throws SQLException si falla la inserción
     */
    public int registrar(RegistroRespaldo r) throws SQLException {
        String sql = "INSERT INTO bitacora_respaldos " +
                "(tipo, estado, fecha_inicio, fecha_fin, tamano_bytes, ruta_destino, mensaje_error, usuario_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getTipo().name());
            ps.setString(2, r.getEstado().name());
            ps.setTimestamp(3, Timestamp.valueOf(r.getFechaInicio()));
            if (r.getFechaFin() != null) {
                ps.setTimestamp(4, Timestamp.valueOf(r.getFechaFin()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            if (r.getTamanoBytes() != null) {
                ps.setLong(5, r.getTamanoBytes());
            } else {
                ps.setNull(5, java.sql.Types.BIGINT);
            }
            ps.setString(6, r.getRutaDestino());
            ps.setString(7, r.getMensajeError());
            if (r.getUsuarioId() != null) {
                ps.setInt(8, r.getUsuarioId());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al registrar el respaldo en la bitácora.");
    }

    /**
     * @return los últimos 100 respaldos registrados, más reciente primero, con el nombre
     *         del usuario que lo disparó manualmente (o {@code null} si fue automático)
     * @throws SQLException si falla la consulta
     */
    public List<RegistroRespaldo> listarRecientes() throws SQLException {
        String sql = "SELECT b.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos " +
                "FROM bitacora_respaldos b " +
                "LEFT JOIN usuarios u ON u.id = b.usuario_id " +
                "ORDER BY b.fecha_inicio DESC LIMIT 100";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<RegistroRespaldo> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
            return resultado;
        }
    }

    /**
     * @param id id del registro de bitácora
     * @return el registro si existe, usado para resolver la ruta del archivo a descargar
     * @throws SQLException si falla la consulta
     */
    public Optional<RegistroRespaldo> buscarPorId(int id) throws SQLException {
        String sql = "SELECT b.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos " +
                "FROM bitacora_respaldos b " +
                "LEFT JOIN usuarios u ON u.id = b.usuario_id " +
                "WHERE b.id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private RegistroRespaldo mapear(ResultSet rs) throws SQLException {
        RegistroRespaldo r = new RegistroRespaldo();
        r.setId(rs.getInt("id"));
        r.setTipo(TipoRespaldo.valueOf(rs.getString("tipo")));
        r.setEstado(EstadoRespaldo.valueOf(rs.getString("estado")));

        Timestamp fechaInicio = rs.getTimestamp("fecha_inicio");
        r.setFechaInicio(fechaInicio != null ? fechaInicio.toLocalDateTime() : null);

        Timestamp fechaFin = rs.getTimestamp("fecha_fin");
        r.setFechaFin(fechaFin != null ? fechaFin.toLocalDateTime() : null);

        long tamanoBytes = rs.getLong("tamano_bytes");
        r.setTamanoBytes(rs.wasNull() ? null : tamanoBytes);

        r.setRutaDestino(rs.getString("ruta_destino"));
        r.setMensajeError(rs.getString("mensaje_error"));

        int usuarioId = rs.getInt("usuario_id");
        if (!rs.wasNull()) {
            r.setUsuarioId(usuarioId);
            r.setUsuarioNombre(rs.getString("usuario_nombres") + " " + rs.getString("usuario_apellidos"));
        }

        return r;
    }
}
