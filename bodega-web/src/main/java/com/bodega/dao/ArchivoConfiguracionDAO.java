package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.ArchivoBinario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Acceso a archivos binarios de configuración (ej. logo de la tienda), almacenados como
 * BLOB en la tabla archivos_configuracion, hermana de configuracion pero para valores
 * binarios (configuracion.valor es VARCHAR(255) y no puede alojarlos).
 */
public class ArchivoConfiguracionDAO {

    /**
     * Guarda o reemplaza el archivo asociado a una clave (ej. "logo").
     *
     * @param clave           clave fija del archivo (ej. "logo")
     * @param contenido       contenido binario del archivo
     * @param contentType     tipo MIME del archivo (ej. "image/png")
     * @param nombreOriginal  nombre original del archivo subido, o {@code null} si no se conoce
     * @throws SQLException si falla la inserción/actualización
     */
    public void guardar(String clave, byte[] contenido, String contentType, String nombreOriginal) throws SQLException {
        String sql = "INSERT INTO archivos_configuracion (clave, contenido, content_type, nombre_original, fecha_actualizacion) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE contenido = VALUES(contenido), content_type = VALUES(content_type), " +
                "nombre_original = VALUES(nombre_original), fecha_actualizacion = VALUES(fecha_actualizacion)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            ps.setBytes(2, contenido);
            ps.setString(3, contentType);
            ps.setString(4, nombreOriginal);
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    /**
     * @param clave clave del archivo a buscar (ej. "logo")
     * @return el archivo si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<ArchivoBinario> obtener(String clave) throws SQLException {
        String sql = "SELECT contenido, content_type, nombre_original FROM archivos_configuracion WHERE clave = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new ArchivoBinario(
                        rs.getBytes("contenido"),
                        rs.getString("content_type"),
                        rs.getString("nombre_original")));
            }
        }
    }
}
