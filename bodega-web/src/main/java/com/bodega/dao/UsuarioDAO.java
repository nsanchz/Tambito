package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Rol;
import com.bodega.model.Usuario;

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

/**
 * Acceso a datos de la tabla usuarios mediante JDBC puro con PreparedStatement.
 * Todas las conexiones se obtienen del pool HikariCP centralizado en {@link DatabaseConfig}.
 */
public class UsuarioDAO {

    /**
     * Inserta un nuevo usuario.
     *
     * @param u usuario a crear, con {@code passwordHash} ya calculado (nunca texto plano)
     * @return el id autogenerado por la base de datos
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Usuario u) throws SQLException {
        String sql = "INSERT INTO usuarios " +
                "(nombres, apellidos, nombre_usuario, correo, telefono, password_hash, rol, estado, " +
                "debe_cambiar_password, fecha_creacion, creado_por_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getNombres());
            ps.setString(2, u.getApellidos());
            ps.setString(3, u.getNombreUsuario());
            ps.setString(4, u.getCorreo());
            ps.setString(5, u.getTelefono());
            ps.setString(6, u.getPasswordHash());
            ps.setString(7, u.getRol().name());
            ps.setString(8, u.getEstado().name());
            ps.setBoolean(9, u.isDebeCambiarPassword());
            ps.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            if (u.getCreadoPorId() != null) {
                ps.setInt(11, u.getCreadoPorId());
            } else {
                ps.setNull(11, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al crear el usuario.");
    }

    /**
     * @param id id del usuario
     * @return el usuario si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Usuario> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Busca por nombre de usuario o correo, usado por el proceso de autenticación (login),
     * ya que el formulario acepta indistintamente uno u otro.
     *
     * @param login nombre de usuario o correo ingresado en el formulario de login
     * @return el usuario si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Usuario> buscarPorUsuarioOCorreo(String login) throws SQLException {
        String sql = "SELECT * FROM usuarios WHERE nombre_usuario = ? OR correo = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, login);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lista usuarios aplicando filtros opcionales, usados por la pantalla de Gestión de Usuarios.
     *
     * @param filtroRol     rol a filtrar, o {@code null} para no filtrar por rol
     * @param filtroEstado  estado a filtrar (ACTIVO/INACTIVO), o {@code null} para no filtrar por estado
     * @param textoBusqueda texto a buscar en nombres, apellidos, usuario o correo, o {@code null}/vacío para no filtrar
     * @return usuarios que cumplen los filtros, ordenados por fecha de creación descendente
     * @throws SQLException si falla la consulta
     */
    public List<Usuario> listar(Rol filtroRol, EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM usuarios WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroRol != null) {
            sql.append(" AND rol = ?");
            parametros.add(filtroRol.name());
        }
        if (filtroEstado != null) {
            sql.append(" AND estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (nombres LIKE ? OR apellidos LIKE ? OR nombre_usuario LIKE ? OR correo LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY fecha_creacion DESC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Usuario> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Actualiza los datos de contacto de un usuario (nombres, apellidos, correo, teléfono).
     * No modifica rol, estado ni contraseña: esos cambios usan sus propios métodos.
     *
     * @param u usuario con el id de la fila a actualizar y los nuevos valores
     * @throws SQLException si falla la actualización
     */
    public void actualizarDatos(Usuario u) throws SQLException {
        String sql = "UPDATE usuarios SET nombres = ?, apellidos = ?, correo = ?, telefono = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getNombres());
            ps.setString(2, u.getApellidos());
            ps.setString(3, u.getCorreo());
            ps.setString(4, u.getTelefono());
            ps.setInt(5, u.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @param id      id del usuario
     * @param nuevoRol ADMINISTRADOR o VENDEDOR
     * @throws SQLException si falla la actualización
     */
    public void cambiarRol(int id, Rol nuevoRol) throws SQLException {
        String sql = "UPDATE usuarios SET rol = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoRol.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * @param id          id del usuario
     * @param nuevoEstado ACTIVO o INACTIVO
     * @throws SQLException si falla la actualización
     */
    public void cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        String sql = "UPDATE usuarios SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Limpia un bloqueo temporal por intentos fallidos sin tocar la contraseña (a diferencia
     * de {@link #restablecerPassword}, que también fuerza un cambio de clave). Pensado para
     * cuando el administrador solo quiere levantar el bloqueo porque ya verificó con el usuario
     * que la contraseña que tiene es la correcta.
     *
     * @param id id del usuario a desbloquear
     * @throws SQLException si falla la actualización
     */
    public void desbloquear(int id) throws SQLException {
        String sql = "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Restablece la contraseña (usado por un administrador) y marca la cuenta para forzar
     * el cambio en el próximo login, además de limpiar cualquier bloqueo previo por intentos fallidos.
     *
     * @param id       id del usuario
     * @param nuevoHash hash BCrypt de la nueva contraseña temporal
     * @throws SQLException si falla la actualización
     */
    public void restablecerPassword(int id, String nuevoHash) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = TRUE, " +
                "intentos_fallidos = 0, bloqueado_hasta = NULL WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Cambio de contraseña autoiniciado por el propio usuario desde Mi Perfil (no requiere
     * forzar un nuevo cambio en el próximo login, a diferencia de {@link #restablecerPassword}).
     *
     * @param id       id del usuario
     * @param nuevoHash hash BCrypt de la nueva contraseña
     * @throws SQLException si falla la actualización
     */
    public void actualizarPasswordPropia(int id, String nuevoHash) throws SQLException {
        String sql = "UPDATE usuarios SET password_hash = ?, debe_cambiar_password = FALSE WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoHash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Registra un intento de login fallido, actualizando el contador y, si corresponde,
     * el bloqueo temporal de la cuenta (ver {@link com.bodega.service.AutenticacionService}).
     *
     * @param id               id del usuario
     * @param intentosFallidos nuevo valor del contador de intentos fallidos consecutivos
     * @param bloqueadoHasta   instante hasta el cual la cuenta queda bloqueada, o {@code null} si no se bloquea aún
     * @throws SQLException si falla la actualización
     */
    public void registrarIntentoFallido(int id, int intentosFallidos, LocalDateTime bloqueadoHasta) throws SQLException {
        String sql = "UPDATE usuarios SET intentos_fallidos = ?, bloqueado_hasta = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, intentosFallidos);
            ps.setTimestamp(2, bloqueadoHasta != null ? Timestamp.valueOf(bloqueadoHasta) : null);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Registra un login exitoso: limpia el contador de intentos fallidos y cualquier
     * bloqueo, y actualiza la fecha de último acceso.
     *
     * @param id id del usuario
     * @throws SQLException si falla la actualización
     */
    public void registrarAccesoExitoso(int id) throws SQLException {
        String sql = "UPDATE usuarios SET intentos_fallidos = 0, bloqueado_hasta = NULL, ultimo_acceso = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * @param nombreUsuario nombre de usuario a verificar
     * @param correo        correo a verificar
     * @return {@code true} si ya existe un usuario con ese nombre de usuario o ese correo
     * @throws SQLException si falla la consulta
     */
    public boolean existeNombreUsuarioOCorreo(String nombreUsuario, String correo) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE nombre_usuario = ? OR correo = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setString(2, correo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Igual que {@link #existeNombreUsuarioOCorreo}, pero excluyendo al propio usuario que
     * se está editando (de lo contrario, guardar sin cambiar el correo se rechazaría por
     * chocar consigo mismo).
     *
     * @param correo    correo a verificar
     * @param idExcluir id del usuario que se está editando (se excluye de la búsqueda)
     * @return {@code true} si otro usuario distinto ya tiene ese correo
     * @throws SQLException si falla la consulta
     */
    public boolean existeCorreoEnOtroUsuario(String correo, int idExcluir) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE correo = ? AND id != ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, correo);
            ps.setInt(2, idExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * @param rol rol a contar
     * @return cantidad de usuarios con ese rol
     * @throws SQLException si falla la consulta
     */
    public int contarPorRol(Rol rol) throws SQLException {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE rol = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rol.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Mapea la fila actual de un {@link ResultSet} de la tabla usuarios a un {@link Usuario}. */
    private Usuario mapear(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setCorreo(rs.getString("correo"));
        u.setTelefono(rs.getString("telefono"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(Rol.valueOf(rs.getString("rol")));
        u.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));
        u.setDebeCambiarPassword(rs.getBoolean("debe_cambiar_password"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));

        Timestamp bloqueadoHasta = rs.getTimestamp("bloqueado_hasta");
        u.setBloqueadoHasta(bloqueadoHasta != null ? bloqueadoHasta.toLocalDateTime() : null);

        Timestamp ultimoAcceso = rs.getTimestamp("ultimo_acceso");
        u.setUltimoAcceso(ultimoAcceso != null ? ultimoAcceso.toLocalDateTime() : null);

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        u.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        int creadoPorId = rs.getInt("creado_por_id");
        u.setCreadoPorId(rs.wasNull() ? null : creadoPorId);

        return u;
    }
}
