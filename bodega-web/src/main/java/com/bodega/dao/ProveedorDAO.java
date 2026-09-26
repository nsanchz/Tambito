package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de la tabla proveedores mediante JDBC puro con PreparedStatement. */
public class ProveedorDAO {

    /**
     * Inserta un nuevo proveedor.
     *
     * @param p proveedor a crear (sin id)
     * @return el id autogenerado por la base de datos
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Proveedor p) throws SQLException {
        String sql = "INSERT INTO proveedores (ruc, razon_social, contacto_nombre, telefono, correo, direccion, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getRuc());
            ps.setString(2, p.getRazonSocial());
            ps.setString(3, p.getContactoNombre());
            ps.setString(4, p.getTelefono());
            ps.setString(5, p.getCorreo());
            ps.setString(6, p.getDireccion());
            ps.setString(7, p.getEstado().name());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al crear el proveedor.");
    }

    /**
     * @param id id del proveedor
     * @return el proveedor si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Proveedor> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM proveedores WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lista proveedores aplicando filtros opcionales.
     *
     * @param filtroEstado  estado a filtrar (ACTIVO/INACTIVO), o {@code null} para no filtrar por estado
     * @param textoBusqueda texto a buscar en razón social, RUC o nombre de contacto, o {@code null}/vacío para no filtrar
     * @return proveedores que cumplen los filtros, ordenados alfabéticamente por razón social
     * @throws SQLException si falla la consulta
     */
    public List<Proveedor> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM proveedores WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroEstado != null) {
            sql.append(" AND estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (razon_social LIKE ? OR ruc LIKE ? OR contacto_nombre LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY razon_social ASC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Proveedor> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Actualiza los datos editables de un proveedor (el RUC no se modifica aquí).
     *
     * @param p proveedor con el id de la fila a actualizar y los nuevos valores
     * @throws SQLException si falla la actualización
     */
    public void actualizar(Proveedor p) throws SQLException {
        String sql = "UPDATE proveedores SET razon_social = ?, contacto_nombre = ?, telefono = ?, correo = ?, direccion = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getRazonSocial());
            ps.setString(2, p.getContactoNombre());
            ps.setString(3, p.getTelefono());
            ps.setString(4, p.getCorreo());
            ps.setString(5, p.getDireccion());
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @param id          id del proveedor
     * @param nuevoEstado ACTIVO o INACTIVO
     * @throws SQLException si falla la actualización
     */
    public void cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        String sql = "UPDATE proveedores SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Mapea la fila actual de un {@link ResultSet} de la tabla proveedores a un {@link Proveedor}. */
    private Proveedor mapear(ResultSet rs) throws SQLException {
        Proveedor p = new Proveedor();
        p.setId(rs.getInt("id"));
        p.setRuc(rs.getString("ruc"));
        p.setRazonSocial(rs.getString("razon_social"));
        p.setContactoNombre(rs.getString("contacto_nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setCorreo(rs.getString("correo"));
        p.setDireccion(rs.getString("direccion"));
        p.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        p.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        return p;
    }
}
