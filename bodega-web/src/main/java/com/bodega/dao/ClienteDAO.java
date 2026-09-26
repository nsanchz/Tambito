package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.Cliente;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.TipoDocumentoCliente;
import com.bodega.util.CifradoUtil;

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

/** Acceso a datos de la tabla clientes mediante JDBC puro con PreparedStatement. */
public class ClienteDAO {

    /**
     * Inserta un nuevo cliente.
     *
     * @param c cliente a crear (sin id)
     * @return el id autogenerado por la base de datos
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Cliente c) throws SQLException {
        String sql = "INSERT INTO clientes (tipo_documento, numero_documento, nombre_completo, telefono, correo, direccion, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getTipoDocumento().name());
            ps.setString(2, c.getNumeroDocumento());
            ps.setString(3, c.getNombreCompleto());
            ps.setString(4, c.getTelefono());
            ps.setString(5, c.getCorreo());
            // direccion se cifra con AES-256-GCM antes de persistir (ver CifradoUtil y el
            // comentario de decisión de cifrado en el módulo "Gestión de Clientes" de schema.sql).
            ps.setString(6, CifradoUtil.cifrar(c.getDireccion()));
            ps.setString(7, c.getEstado().name());
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al crear el cliente.");
    }

    /**
     * @param id id del cliente
     * @return el cliente si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Busca un cliente por su número de documento exacto, usado por el POS al asociar
     * una venta a un "cliente registrado" y por la ficha de detalle del cliente.
     *
     * @param numeroDocumento DNI o RUC exacto a buscar
     * @return el cliente si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorNumeroDocumento(String numeroDocumento) throws SQLException {
        String sql = "SELECT * FROM clientes WHERE numero_documento = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lista clientes aplicando filtros opcionales.
     *
     * @param filtroEstado  estado a filtrar (ACTIVO/INACTIVO), o {@code null} para no filtrar por estado
     * @param textoBusqueda texto a buscar en nombre completo o número de documento, o {@code null}/vacío para no filtrar
     * @return clientes que cumplen los filtros, ordenados alfabéticamente
     * @throws SQLException si falla la consulta
     */
    public List<Cliente> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM clientes WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroEstado != null) {
            sql.append(" AND estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (nombre_completo LIKE ? OR numero_documento LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY nombre_completo ASC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Cliente> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Actualiza los datos de contacto de un cliente. El tipo y número de documento
     * no se modifican aquí (son la identidad fiscal del cliente).
     *
     * @param c cliente con el id de la fila a actualizar y los nuevos valores
     * @throws SQLException si falla la actualización
     */
    public void actualizar(Cliente c) throws SQLException {
        String sql = "UPDATE clientes SET nombre_completo = ?, telefono = ?, correo = ?, direccion = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombreCompleto());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getCorreo());
            ps.setString(4, CifradoUtil.cifrar(c.getDireccion()));
            ps.setInt(5, c.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @param id          id del cliente
     * @param nuevoEstado ACTIVO o INACTIVO
     * @throws SQLException si falla la actualización
     */
    public void cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        String sql = "UPDATE clientes SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Acredita (o resta, con {@code puntos} negativo) puntos de fidelización dentro de la
     * transacción ACID de una venta o de su anulación (ver VentaService). No es una
     * escritura "best effort" aparte: si la venta hace rollback, los puntos otorgados
     * también se revierten porque comparten conexión.
     *
     * @param con       conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param clienteId id del cliente a acreditar
     * @param puntos    puntos a sumar; un valor negativo los resta (usado al anular una venta)
     * @throws SQLException si falla la actualización
     */
    public void sumarPuntos(Connection con, int clienteId, int puntos) throws SQLException {
        String sql = "UPDATE clientes SET puntos_fidelizacion = puntos_fidelizacion + ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, puntos);
            ps.setInt(2, clienteId);
            ps.executeUpdate();
        }
    }

    /**
     * @param numeroDocumento número de documento a verificar
     * @return {@code true} si ya existe un cliente con ese número de documento
     * @throws SQLException si falla la consulta
     */
    public boolean existeNumeroDocumento(String numeroDocumento) throws SQLException {
        String sql = "SELECT COUNT(*) FROM clientes WHERE numero_documento = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Mapea la fila actual de un {@link ResultSet} de la tabla clientes a un {@link Cliente}. */
    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setTipoDocumento(TipoDocumentoCliente.valueOf(rs.getString("tipo_documento")));
        c.setNumeroDocumento(rs.getString("numero_documento"));
        c.setNombreCompleto(rs.getString("nombre_completo"));
        c.setTelefono(rs.getString("telefono"));
        c.setCorreo(rs.getString("correo"));
        c.setDireccion(CifradoUtil.descifrar(rs.getString("direccion")));
        c.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));
        c.setPuntosFidelizacion(rs.getInt("puntos_fidelizacion"));

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        c.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        return c;
    }
}
