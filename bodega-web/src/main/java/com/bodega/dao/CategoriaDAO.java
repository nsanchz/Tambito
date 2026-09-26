package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.Categoria;
import com.bodega.model.EstadoCuenta;

import java.math.BigDecimal;
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

/** Acceso a datos de la tabla categorias mediante JDBC puro con PreparedStatement. */
public class CategoriaDAO {

    /**
     * Inserta una nueva categoría.
     *
     * @param c categoría a crear (sin id; el código correlativo debe asignarse antes con {@link #siguienteCodigo()})
     * @return el id autogenerado por la base de datos
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Categoria c) throws SQLException {
        String sql = "INSERT INTO categorias (codigo, nombre, descripcion, icono, margen_sugerido, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCodigo());
            ps.setString(2, c.getNombre());
            ps.setString(3, c.getDescripcion());
            ps.setString(4, c.getIcono());
            ps.setBigDecimal(5, c.getMargenSugerido());
            ps.setString(6, c.getEstado().name());
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al crear la categoría.");
    }

    /**
     * @param id id de la categoría
     * @return la categoría si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Categoria> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM categorias WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lista categorías aplicando filtros opcionales, usadas por el catálogo y el POS.
     *
     * @param filtroEstado   estado a filtrar (ACTIVO/INACTIVO), o {@code null} para no filtrar por estado
     * @param textoBusqueda  texto a buscar en código, nombre o descripción (coincidencia parcial), o {@code null}/vacío para no filtrar
     * @return categorías que cumplen los filtros, ordenadas por código
     * @throws SQLException si falla la consulta
     */
    public List<Categoria> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM categorias WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroEstado != null) {
            sql.append(" AND estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (codigo LIKE ? OR nombre LIKE ? OR descripcion LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY codigo ASC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Categoria> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Cuenta cuántos productos están asignados a una categoría, usado por el modal de
     * desactivación para advertir cuántos productos quedarán sin agrupación visible.
     *
     * @param categoriaId id de la categoría
     * @return cantidad de productos con {@code categoria_id} igual al indicado
     * @throws SQLException si falla la consulta
     */
    public int contarProductosAsignados(int categoriaId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos WHERE categoria_id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, categoriaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Actualiza los datos editables de una categoría (el código no se puede modificar).
     *
     * @param c categoría con el id de la fila a actualizar y los nuevos valores
     * @throws SQLException si falla la actualización
     */
    public void actualizar(Categoria c) throws SQLException {
        String sql = "UPDATE categorias SET nombre = ?, descripcion = ?, icono = ?, margen_sugerido = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDescripcion());
            ps.setString(3, c.getIcono());
            ps.setBigDecimal(4, c.getMargenSugerido());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @param id          id de la categoría
     * @param nuevoEstado ACTIVO o INACTIVO
     * @throws SQLException si falla la actualización
     */
    public void cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        String sql = "UPDATE categorias SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * @param codigo código a verificar (ej. "CAT-001")
     * @return {@code true} si ya existe una categoría con ese código
     * @throws SQLException si falla la consulta
     */
    public boolean existeCodigo(String codigo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorias WHERE codigo = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Genera el siguiente código correlativo a partir del último registrado.
     *
     * @return el siguiente código con formato {@code CAT-NNN} (ej. "CAT-001" si la tabla está vacía)
     * @throws SQLException si falla la consulta
     */
    public String siguienteCodigo() throws SQLException {
        String sql = "SELECT codigo FROM categorias ORDER BY id DESC LIMIT 1";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int ultimoNumero = Integer.parseInt(rs.getString("codigo").split("-")[1]);
                return String.format("CAT-%03d", ultimoNumero + 1);
            }
            return "CAT-001";
        }
    }

    private Categoria mapear(ResultSet rs) throws SQLException {
        Categoria c = new Categoria();
        c.setId(rs.getInt("id"));
        c.setCodigo(rs.getString("codigo"));
        c.setNombre(rs.getString("nombre"));
        c.setDescripcion(rs.getString("descripcion"));
        c.setIcono(rs.getString("icono"));
        c.setMargenSugerido(rs.getBigDecimal("margen_sugerido"));
        c.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        c.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        return c;
    }
}
