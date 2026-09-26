package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.DetalleOrdenCompra;
import com.bodega.model.EstadoOrdenCompra;
import com.bodega.model.OrdenCompra;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Acceso a datos de órdenes de compra y su detalle. Los métodos que reciben una
 * {@link Connection} externa participan en la transacción ACID de creación o
 * recepción de mercadería controlada por {@link com.bodega.service.OrdenCompraService}.
 */
public class OrdenCompraDAO {

    // Base común de SELECT de cabecera reutilizada por listar()/buscarPorIdConDetalle():
    // resuelve el nombre y correo del proveedor, el nombre y correo del vendedor que creó
    // la orden, y (con LEFT JOIN, porque puede ser NULL si aún no se aprobó) el nombre del
    // administrador que la aprobó.
    private static final String CABECERA_SELECT_BASE =
            "SELECT o.*, p.razon_social AS proveedor_nombre, p.correo AS proveedor_correo, " +
                    "u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, u.correo AS usuario_correo, " +
                    "ap.nombres AS aprobado_por_nombres, ap.apellidos AS aprobado_por_apellidos " +
                    "FROM ordenes_compra o " +
                    "JOIN proveedores p ON p.id = o.proveedor_id " +
                    "JOIN usuarios u ON u.id = o.usuario_id " +
                    "LEFT JOIN usuarios ap ON ap.id = o.aprobado_por_id";

    /**
     * Inserta la cabecera de una orden de compra y le asigna su número correlativo.
     *
     * @param con conexión de la transacción externa controlada por {@link com.bodega.service.OrdenCompraService}
     * @param o   orden a crear (sin id ni número), con estado PENDIENTE
     * @return el id autogenerado de la orden
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crearCabecera(Connection con, OrdenCompra o) throws SQLException {
        String sql = "INSERT INTO ordenes_compra (proveedor_id, usuario_id, estado, observaciones, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.getProveedorId());
            ps.setInt(2, o.getUsuarioId());
            ps.setString(3, o.getEstado().name());
            ps.setString(4, o.getObservaciones());
            ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID generado al crear la orden de compra.");
                }
                int id = rs.getInt(1);
                asignarNumero(con, id);
                return id;
            }
        }
    }

    private void asignarNumero(Connection con, int id) throws SQLException {
        String numero = String.format("OC-%06d", id);
        try (PreparedStatement ps = con.prepareStatement("UPDATE ordenes_compra SET numero = ? WHERE id = ?")) {
            ps.setString(1, numero);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Inserta una línea de detalle de una orden de compra, con {@code cantidad_recibida}
     * inicial en 0.
     *
     * @param con           conexión de la transacción externa
     * @param ordenCompraId id de la orden de compra ya creada
     * @param d             línea a insertar (producto, cantidad pedida, precio unitario)
     * @throws SQLException si falla la inserción
     */
    public void agregarDetalle(Connection con, int ordenCompraId, DetalleOrdenCompra d) throws SQLException {
        String sql = "INSERT INTO detalle_orden_compra (orden_compra_id, producto_id, cantidad_pedida, cantidad_recibida, precio_unitario) " +
                "VALUES (?, ?, ?, 0, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ordenCompraId);
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidadPedida());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.executeUpdate();
        }
    }

    /**
     * Actualiza la cantidad acumulada recibida de una línea de detalle (permite
     * recepciones parciales en varias entregas del proveedor).
     *
     * @param con                   conexión de la transacción externa
     * @param detalleId             id de la línea de detalle
     * @param nuevaCantidadRecibida cantidad total recibida acumulada (no el incremento)
     * @throws SQLException si falla la actualización
     */
    public void actualizarCantidadRecibida(Connection con, int detalleId, int nuevaCantidadRecibida) throws SQLException {
        String sql = "UPDATE detalle_orden_compra SET cantidad_recibida = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, nuevaCantidadRecibida);
            ps.setInt(2, detalleId);
            ps.executeUpdate();
        }
    }

    /**
     * @param con            conexión de la transacción externa
     * @param ordenId        id de la orden de compra
     * @param estado         nuevo estado (PENDIENTE, RECIBIDA_PARCIAL, RECIBIDA_COMPLETA o CANCELADA)
     * @param fechaRecepcion instante de la recepción que motiva el cambio, o {@code null} si no aplica (ej. cancelación)
     * @throws SQLException si falla la actualización
     */
    public void actualizarEstado(Connection con, int ordenId, EstadoOrdenCompra estado, LocalDateTime fechaRecepcion) throws SQLException {
        String sql = "UPDATE ordenes_compra SET estado = ?, fecha_ultima_recepcion = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setTimestamp(2, fechaRecepcion != null ? Timestamp.valueOf(fechaRecepcion) : null);
            ps.setInt(3, ordenId);
            ps.executeUpdate();
        }
    }

    /**
     * Lee el estado actual de una orden bloqueando la fila, para usar dentro de una
     * transacción externa antes de una transición de estado (aprobar/rechazar/recibir)
     * y así evitar una carrera de doble-clic entre dos peticiones concurrentes.
     *
     * @param con conexión de la transacción externa
     * @param id  id de la orden de compra
     * @return el estado actual, o {@code null} si la orden no existe
     * @throws SQLException si falla la consulta
     */
    public EstadoOrdenCompra buscarEstadoParaActualizar(Connection con, int id) throws SQLException {
        String sql = "SELECT estado FROM ordenes_compra WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? EstadoOrdenCompra.valueOf(rs.getString("estado")) : null;
            }
        }
    }

    /**
     * Aprueba una orden de compra (transición PENDIENTE → APROBADA validada previamente
     * por {@link com.bodega.service.OrdenCompraService#aprobar}).
     *
     * @param con     conexión de la transacción externa
     * @param id      id de la orden de compra
     * @param adminId id del administrador que aprueba
     * @throws SQLException si falla la actualización
     */
    public void aprobar(Connection con, int id, int adminId) throws SQLException {
        String sql = "UPDATE ordenes_compra SET estado = 'APROBADA', aprobado_por_id = ?, fecha_aprobacion = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    /**
     * Rechaza una orden de compra (transición PENDIENTE → RECHAZADA validada previamente
     * por {@link com.bodega.service.OrdenCompraService#rechazar}).
     *
     * @param con    conexión de la transacción externa
     * @param id     id de la orden de compra
     * @param motivo motivo del rechazo, o {@code null}/vacío si no se indicó ninguno
     * @throws SQLException si falla la actualización
     */
    public void rechazar(Connection con, int id, String motivo) throws SQLException {
        String sql = "UPDATE ordenes_compra SET estado = 'RECHAZADA', motivo_rechazo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, motivo);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Lista órdenes de compra (sin su detalle) con el nombre de proveedor y usuario resueltos.
     *
     * @param filtroEstado estado a filtrar, o {@code null} para listar todas
     * @return órdenes que cumplen el filtro, ordenadas por fecha de creación descendente
     * @throws SQLException si falla la consulta
     */
    public List<OrdenCompra> listar(EstadoOrdenCompra filtroEstado) throws SQLException {
        StringBuilder sql = new StringBuilder(CABECERA_SELECT_BASE + " WHERE 1=1");
        if (filtroEstado != null) {
            sql.append(" AND o.estado = ?");
        }
        sql.append(" ORDER BY o.fecha_creacion DESC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (filtroEstado != null) {
                ps.setString(1, filtroEstado.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<OrdenCompra> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapearCabecera(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Busca una orden de compra junto con todas sus líneas de detalle (para la pantalla
     * de recepción de mercadería).
     *
     * @param id id de la orden de compra
     * @return la orden con su lista de detalles poblada, o {@link Optional#empty()} si no existe
     * @throws SQLException si falla alguna de las dos consultas (cabecera y detalle)
     */
    public Optional<OrdenCompra> buscarPorIdConDetalle(int id) throws SQLException {
        String sqlCabecera = CABECERA_SELECT_BASE + " WHERE o.id = ?";

        try (Connection con = DatabaseConfig.getConnection()) {
            OrdenCompra orden;
            try (PreparedStatement ps = con.prepareStatement(sqlCabecera)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    orden = mapearCabecera(rs);
                }
            }

            String sqlDetalle = "SELECT d.*, pr.nombre AS producto_nombre FROM detalle_orden_compra d " +
                    "JOIN productos pr ON pr.id = d.producto_id WHERE d.orden_compra_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DetalleOrdenCompra> detalles = new ArrayList<>();
                    while (rs.next()) {
                        detalles.add(mapearDetalle(rs));
                    }
                    orden.setDetalles(detalles);
                }
            }
            return Optional.of(orden);
        }
    }

    /**
     * Variante para uso dentro de una transacción de recepción ya abierta: bloquea las
     * filas de detalle con {@code FOR UPDATE} para evitar recepciones concurrentes
     * inconsistentes sobre la misma orden.
     *
     * @param con           conexión de la transacción externa
     * @param ordenCompraId id de la orden de compra
     * @return mapa de {@code detalleId -> línea de detalle} (sin el nombre de producto, no es necesario aquí)
     * @throws SQLException si falla la consulta
     */
    public Map<Integer, DetalleOrdenCompra> obtenerDetallesParaActualizar(Connection con, int ordenCompraId) throws SQLException {
        String sql = "SELECT * FROM detalle_orden_compra WHERE orden_compra_id = ? FOR UPDATE";
        Map<Integer, DetalleOrdenCompra> detalles = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ordenCompraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DetalleOrdenCompra d = mapearDetalle(rs);
                    detalles.put(d.getId(), d);
                }
            }
        }
        return detalles;
    }

    private OrdenCompra mapearCabecera(ResultSet rs) throws SQLException {
        OrdenCompra o = new OrdenCompra();
        o.setId(rs.getInt("id"));
        o.setNumero(rs.getString("numero"));
        o.setProveedorId(rs.getInt("proveedor_id"));
        o.setProveedorNombre(rs.getString("proveedor_nombre"));
        o.setUsuarioId(rs.getInt("usuario_id"));
        o.setUsuarioNombre(rs.getString("usuario_nombres") + " " + rs.getString("usuario_apellidos"));
        o.setUsuarioCorreo(rs.getString("usuario_correo"));
        o.setProveedorCorreo(rs.getString("proveedor_correo"));
        o.setEstado(EstadoOrdenCompra.valueOf(rs.getString("estado")));
        o.setObservaciones(rs.getString("observaciones"));

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        o.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        Timestamp fechaRecepcion = rs.getTimestamp("fecha_ultima_recepcion");
        o.setFechaUltimaRecepcion(fechaRecepcion != null ? fechaRecepcion.toLocalDateTime() : null);

        int aprobadoPorId = rs.getInt("aprobado_por_id");
        o.setAprobadoPorId(rs.wasNull() ? null : aprobadoPorId);

        String aprobadoPorNombres = rs.getString("aprobado_por_nombres");
        o.setAprobadoPorNombre(aprobadoPorNombres != null ? aprobadoPorNombres + " " + rs.getString("aprobado_por_apellidos") : null);

        Timestamp fechaAprobacion = rs.getTimestamp("fecha_aprobacion");
        o.setFechaAprobacion(fechaAprobacion != null ? fechaAprobacion.toLocalDateTime() : null);

        o.setMotivoRechazo(rs.getString("motivo_rechazo"));

        return o;
    }

    private DetalleOrdenCompra mapearDetalle(ResultSet rs) throws SQLException {
        DetalleOrdenCompra d = new DetalleOrdenCompra();
        d.setId(rs.getInt("id"));
        d.setOrdenCompraId(rs.getInt("orden_compra_id"));
        d.setProductoId(rs.getInt("producto_id"));
        try {
            d.setProductoNombre(rs.getString("producto_nombre"));
        } catch (SQLException ignorado) {
            // Columna no proyectada en la variante "para actualizar" (FOR UPDATE); no es necesaria allí.
        }
        d.setCantidadPedida(rs.getInt("cantidad_pedida"));
        d.setCantidadRecibida(rs.getInt("cantidad_recibida"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        return d;
    }
}
