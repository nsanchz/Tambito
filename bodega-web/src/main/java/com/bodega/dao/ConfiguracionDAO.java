package com.bodega.dao;

import com.bodega.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Acceso a los parámetros generales del sistema, almacenados como pares clave-valor
 * en la tabla configuracion. Permite añadir nuevos parámetros sin migrar el esquema.
 */
public class ConfiguracionDAO {

    /**
     * @return todos los pares clave-valor almacenados, ordenados alfabéticamente por clave
     * @throws SQLException si falla la consulta
     */
    public Map<String, String> listarTodos() throws SQLException {
        String sql = "SELECT clave, valor FROM configuracion ORDER BY clave ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Map<String, String> resultado = new LinkedHashMap<>();
            while (rs.next()) {
                resultado.put(rs.getString("clave"), rs.getString("valor"));
            }
            return resultado;
        }
    }

    /**
     * @param clave           clave del parámetro (ver constantes en {@link com.bodega.service.ConfiguracionService})
     * @param valorPorDefecto valor a devolver si la clave no existe en la tabla
     * @return el valor almacenado, o {@code valorPorDefecto} si la clave no existe
     * @throws SQLException si falla la consulta
     */
    public String obtener(String clave, String valorPorDefecto) throws SQLException {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("valor") : valorPorDefecto;
            }
        }
    }

    /**
     * Actualiza el valor de un parámetro existente. No inserta filas nuevas: la clave
     * debe existir previamente (sembrada en schema.sql).
     *
     * @param clave clave del parámetro a actualizar
     * @param valor nuevo valor
     * @throws SQLException si falla la actualización
     */
    public void actualizar(String clave, String valor) throws SQLException {
        String sql = "UPDATE configuracion SET valor = ? WHERE clave = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, clave);
            ps.executeUpdate();
        }
    }
}
