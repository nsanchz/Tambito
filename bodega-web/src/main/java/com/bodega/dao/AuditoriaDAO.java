package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.RegistroAuditoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la bitácora de auditoría. Es, al igual que el Kárdex, una tabla
 * de solo inserción: ningún Servlet ni DAO expone UPDATE/DELETE sobre ella, porque
 * cada fila es evidencia histórica de una acción realizada en el sistema.
 */
public class AuditoriaDAO {

    /**
     * Inserta una entrada inmutable en la bitácora de auditoría.
     *
     * @param usuarioId   id del usuario que ejecutó la acción, o {@code null} si fue un intento anónimo
     *                    (ej. login fallido con un usuario que no existe)
     * @param accion      código de la acción realizada (ej. "LOGIN_EXITOSO", "USUARIO_CREAR", "VENTA_ANULADA")
     * @param entidad     tipo de entidad afectada (ej. "USUARIO", "VENTA", "CONFIGURACION")
     * @param entidadId   id de la entidad afectada, o {@code null} si no aplica
     * @param detalle     descripción legible del evento
     * @param direccionIp dirección IP de origen de la petición
     * @throws SQLException si falla la inserción
     */
    public void registrar(Integer usuarioId, String accion, String entidad, Integer entidadId,
                           String detalle, String direccionIp) throws SQLException {
        String sql = "INSERT INTO auditoria (usuario_id, accion, entidad, entidad_id, detalle, direccion_ip, fecha) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (usuarioId != null) {
                ps.setInt(1, usuarioId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, accion);
            ps.setString(3, entidad);
            if (entidadId != null) {
                ps.setInt(4, entidadId);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, detalle);
            ps.setString(6, direccionIp);
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * Lista los registros de auditoría más recientes aplicando filtros opcionales.
     *
     * @param filtroAccion  código de acción a filtrar, o {@code null}/vacío para no filtrar
     * @param filtroEntidad tipo de entidad a filtrar, o {@code null}/vacío para no filtrar
     * @param textoBusqueda texto a buscar en el detalle o en el nombre del usuario, o {@code null}/vacío para no filtrar
     * @return hasta 500 registros que cumplen los filtros, ordenados por fecha descendente
     * @throws SQLException si falla la consulta
     */
    public List<RegistroAuditoria> listar(String filtroAccion, String filtroEntidad, String textoBusqueda) throws SQLException {
        // El segundo LEFT JOIN resuelve el nombre real del usuario AFECTADO por la acción
        // (ej. a quién se le creó/desactivó/restableció la contraseña) cuando entidad = 'USUARIO',
        // para no mostrar en la vista un opaco "USUARIO #3" sin poder identificar de quién se trata.
        // Solo aplica cuando la entidad es 'USUARIO'; para el resto de entidades (VENTA,
        // CONFIGURACION, etc.) entidad_id no referencia la tabla usuarios y el join no calza.
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, " +
                        "afectado.nombres AS afectado_nombres, afectado.apellidos AS afectado_apellidos, " +
                        "afectado.nombre_usuario AS afectado_nombre_usuario " +
                        "FROM auditoria a " +
                        "LEFT JOIN usuarios u ON u.id = a.usuario_id " +
                        "LEFT JOIN usuarios afectado ON a.entidad = 'USUARIO' AND afectado.id = a.entidad_id " +
                        "WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroAccion != null && !filtroAccion.isBlank()) {
            sql.append(" AND a.accion = ?");
            parametros.add(filtroAccion);
        }
        if (filtroEntidad != null && !filtroEntidad.isBlank()) {
            sql.append(" AND a.entidad = ?");
            parametros.add(filtroEntidad);
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (a.detalle LIKE ? OR u.nombres LIKE ? OR u.apellidos LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY a.fecha DESC LIMIT 500");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RegistroAuditoria> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    private RegistroAuditoria mapear(ResultSet rs) throws SQLException {
        RegistroAuditoria r = new RegistroAuditoria();
        r.setId(rs.getInt("id"));

        int usuarioId = rs.getInt("usuario_id");
        r.setUsuarioId(rs.wasNull() ? null : usuarioId);

        String nombres = rs.getString("usuario_nombres");
        r.setUsuarioNombre(nombres != null ? nombres + " " + rs.getString("usuario_apellidos") : "Sistema / Anónimo");

        r.setAccion(rs.getString("accion"));
        r.setEntidad(rs.getString("entidad"));

        int entidadId = rs.getInt("entidad_id");
        r.setEntidadId(rs.wasNull() ? null : entidadId);

        String afectadoNombres = rs.getString("afectado_nombres");
        if (afectadoNombres != null) {
            r.setEntidadNombre(afectadoNombres + " " + rs.getString("afectado_apellidos")
                    + " (" + rs.getString("afectado_nombre_usuario") + ")");
        }

        r.setDetalle(rs.getString("detalle"));
        r.setDireccionIp(rs.getString("direccion_ip"));

        Timestamp fecha = rs.getTimestamp("fecha");
        r.setFecha(fecha != null ? fecha.toLocalDateTime() : null);

        return r;
    }
}
