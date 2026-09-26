package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.DetalleVenta;
import com.bodega.model.EstadoVenta;
import com.bodega.model.MetodoPago;
import com.bodega.model.TipoComprobante;
import com.bodega.model.Venta;

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
 * Acceso a datos de ventas y su detalle. Los métodos de escritura reciben una
 * {@link Connection} externa porque siempre participan en la transacción ACID
 * controlada por {@link com.bodega.service.VentaService} (registro o anulación).
 */
public class VentaDAO {

    /**
     * Inserta la cabecera de una venta y le asigna su número de comprobante (serie B001
     * para Boleta, F001 para Factura).
     *
     * @param con conexión de la transacción externa controlada por {@link com.bodega.service.VentaService}
     * @param v   venta a crear (sin id ni número de comprobante), con los importes ya calculados
     * @return el id autogenerado de la venta
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crearCabecera(Connection con, Venta v) throws SQLException {
        String sql = "INSERT INTO ventas (tipo_comprobante, cliente_id, usuario_id, terminal_id, " +
                "subtotal_imponible, descuento, igv, total, metodo_pago, estado, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, v.getTipoComprobante().name());
            if (v.getClienteId() != null) {
                ps.setInt(2, v.getClienteId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setInt(3, v.getUsuarioId());
            ps.setString(4, v.getTerminalId());
            ps.setBigDecimal(5, v.getSubtotalImponible());
            ps.setBigDecimal(6, v.getDescuento());
            ps.setBigDecimal(7, v.getIgv());
            ps.setBigDecimal(8, v.getTotal());
            ps.setString(9, v.getMetodoPago().name());
            ps.setString(10, v.getEstado().name());
            ps.setTimestamp(11, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("No se pudo obtener el ID generado al crear la venta.");
                }
                int id = rs.getInt(1);
                asignarNumeroComprobante(con, id, v.getTipoComprobante());
                return id;
            }
        }
    }

    private void asignarNumeroComprobante(Connection con, int id, TipoComprobante tipo) throws SQLException {
        String serie = tipo == TipoComprobante.FACTURA ? "F001" : "B001";
        String numero = serie + "-" + String.format("%06d", id);
        try (PreparedStatement ps = con.prepareStatement("UPDATE ventas SET numero_comprobante = ? WHERE id = ?")) {
            ps.setString(1, numero);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * @param con    conexión de la transacción externa
     * @param ventaId id de la venta ya creada
     * @param d      línea de detalle a insertar (producto, cantidad, precio unitario al momento de la venta)
     * @throws SQLException si falla la inserción
     */
    public void agregarDetalle(Connection con, int ventaId, DetalleVenta d) throws SQLException {
        String sql = "INSERT INTO detalle_venta (venta_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            ps.setInt(2, d.getProductoId());
            ps.setInt(3, d.getCantidad());
            ps.setBigDecimal(4, d.getPrecioUnitario());
            ps.executeUpdate();
        }
    }

    /**
     * Marca una venta como ANULADA con su motivo y el administrador responsable.
     *
     * @param con                conexión de la transacción externa
     * @param ventaId            id de la venta a anular
     * @param motivo             motivo de la anulación ingresado por el administrador
     * @param usuarioAnulacionId id del administrador que ejecuta la anulación
     * @throws SQLException si falla la actualización
     */
    public void anular(Connection con, int ventaId, String motivo, int usuarioAnulacionId) throws SQLException {
        String sql = "UPDATE ventas SET estado = ?, motivo_anulacion = ?, usuario_anulacion_id = ?, fecha_anulacion = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, EstadoVenta.ANULADA.name());
            ps.setString(2, motivo);
            ps.setInt(3, usuarioAnulacionId);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, ventaId);
            ps.executeUpdate();
        }
    }

    /**
     * Busca una venta junto con todas sus líneas de detalle, para el historial de ventas
     * y la generación del ticket PDF.
     *
     * @param id id de la venta
     * @return la venta con su lista de detalles poblada, o {@link Optional#empty()} si no existe
     * @throws SQLException si falla alguna de las dos consultas (cabecera y detalle)
     */
    public Optional<Venta> buscarPorIdConDetalle(int id) throws SQLException {
        String sqlCabecera = "SELECT v.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, " +
                "c.nombre_completo AS cliente_nombre FROM ventas v " +
                "JOIN usuarios u ON u.id = v.usuario_id " +
                "LEFT JOIN clientes c ON c.id = v.cliente_id WHERE v.id = ?";

        try (Connection con = DatabaseConfig.getConnection()) {
            Venta venta;
            try (PreparedStatement ps = con.prepareStatement(sqlCabecera)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Optional.empty();
                    }
                    venta = mapearCabecera(rs);
                }
            }

            String sqlDetalle = "SELECT d.*, p.nombre AS producto_nombre FROM detalle_venta d " +
                    "JOIN productos p ON p.id = d.producto_id WHERE d.venta_id = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDetalle)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    List<DetalleVenta> detalles = new ArrayList<>();
                    while (rs.next()) {
                        detalles.add(mapearDetalle(rs));
                    }
                    venta.setDetalles(detalles);
                }
            }
            return Optional.of(venta);
        }
    }

    /**
     * Lee el estado actual de la venta dentro de la transacción, bloqueando la fila con
     * {@code FOR UPDATE} para evitar anulaciones concurrentes de la misma venta.
     *
     * @param con     conexión de la transacción externa
     * @param ventaId id de la venta
     * @return el estado actual, o {@link Optional#empty()} si la venta no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<EstadoVenta> obtenerEstadoParaActualizar(Connection con, int ventaId) throws SQLException {
        String sql = "SELECT estado FROM ventas WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(EstadoVenta.valueOf(rs.getString("estado"))) : Optional.empty();
            }
        }
    }

    /** Datos mínimos de cabecera necesarios para revertir puntos de fidelización al anular. */
    public record DatosClienteVenta(Integer clienteId, java.math.BigDecimal total) {
    }

    /**
     * @param con     conexión de la transacción externa
     * @param ventaId id de la venta
     * @return el cliente asociado (si lo hay) y el total de la venta, o {@link Optional#empty()} si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<DatosClienteVenta> obtenerDatosClienteParaAnular(Connection con, int ventaId) throws SQLException {
        String sql = "SELECT cliente_id, total FROM ventas WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, ventaId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                int clienteId = rs.getInt("cliente_id");
                return Optional.of(new DatosClienteVenta(rs.wasNull() ? null : clienteId, rs.getBigDecimal("total")));
            }
        }
    }

    /**
     * Lista ventas aplicando filtros opcionales, con el nombre de vendedor y cliente
     * ya resueltos (join) para el Historial de Ventas.
     *
     * @param filtroEstado      estado a filtrar (COMPLETADA/ANULADA), o {@code null} para no filtrar
     * @param filtroMetodoPago  método de pago a filtrar, o {@code null} para no filtrar
     * @param usuarioId         id de vendedor a filtrar (un VENDEDOR solo ve las suyas), o {@code null} para no filtrar
     * @param textoBusqueda     texto a buscar en número de comprobante o documento del cliente, o {@code null}/vacío para no filtrar
     * @return ventas que cumplen los filtros, ordenadas por fecha descendente
     * @throws SQLException si falla la consulta
     */
    public List<Venta> listar(EstadoVenta filtroEstado, MetodoPago filtroMetodoPago, Integer usuarioId, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT v.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, " +
                        "c.nombre_completo AS cliente_nombre FROM ventas v " +
                        "JOIN usuarios u ON u.id = v.usuario_id " +
                        "LEFT JOIN clientes c ON c.id = v.cliente_id WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (filtroEstado != null) {
            sql.append(" AND v.estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (filtroMetodoPago != null) {
            sql.append(" AND v.metodo_pago = ?");
            parametros.add(filtroMetodoPago.name());
        }
        if (usuarioId != null) {
            sql.append(" AND v.usuario_id = ?");
            parametros.add(usuarioId);
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (v.numero_comprobante LIKE ? OR c.numero_documento LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            parametros.add(comodin);
            parametros.add(comodin);
        }
        sql.append(" ORDER BY v.fecha_creacion DESC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Venta> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapearCabecera(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Ventas de un usuario dentro de un rango de fechas (base del detalle de un turno de caja:
     * el rango es la apertura hasta el cierre, o hasta ahora si el turno sigue abierto).
     *
     * @param usuarioId id del cajero
     * @param desde     inicio del rango, inclusive
     * @param hasta     fin del rango, inclusive
     * @return ventas del usuario en el rango, ordenadas por fecha ascendente
     * @throws SQLException si falla la consulta
     */
    public List<Venta> listarPorUsuarioEntreFechas(int usuarioId, LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        String sql = "SELECT v.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos, " +
                "c.nombre_completo AS cliente_nombre FROM ventas v " +
                "JOIN usuarios u ON u.id = v.usuario_id " +
                "LEFT JOIN clientes c ON c.id = v.cliente_id " +
                "WHERE v.usuario_id = ? AND v.fecha_creacion BETWEEN ? AND ? " +
                "ORDER BY v.fecha_creacion ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setTimestamp(2, Timestamp.valueOf(desde));
            ps.setTimestamp(3, Timestamp.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                List<Venta> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapearCabecera(rs));
                }
                return resultado;
            }
        }
    }

    private Venta mapearCabecera(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setNumeroComprobante(rs.getString("numero_comprobante"));
        v.setTipoComprobante(TipoComprobante.valueOf(rs.getString("tipo_comprobante")));

        int clienteId = rs.getInt("cliente_id");
        v.setClienteId(rs.wasNull() ? null : clienteId);
        v.setClienteNombre(rs.getString("cliente_nombre") != null ? rs.getString("cliente_nombre") : "Cliente varios");

        v.setUsuarioId(rs.getInt("usuario_id"));
        v.setUsuarioNombre(rs.getString("usuario_nombres") + " " + rs.getString("usuario_apellidos"));
        v.setTerminalId(rs.getString("terminal_id"));
        v.setSubtotalImponible(rs.getBigDecimal("subtotal_imponible"));
        v.setDescuento(rs.getBigDecimal("descuento"));
        v.setIgv(rs.getBigDecimal("igv"));
        v.setTotal(rs.getBigDecimal("total"));
        v.setMetodoPago(MetodoPago.valueOf(rs.getString("metodo_pago")));
        v.setEstado(EstadoVenta.valueOf(rs.getString("estado")));
        v.setMotivoAnulacion(rs.getString("motivo_anulacion"));

        int usuarioAnulacionId = rs.getInt("usuario_anulacion_id");
        v.setUsuarioAnulacionId(rs.wasNull() ? null : usuarioAnulacionId);

        Timestamp fechaAnulacion = rs.getTimestamp("fecha_anulacion");
        v.setFechaAnulacion(fechaAnulacion != null ? fechaAnulacion.toLocalDateTime() : null);

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        v.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        return v;
    }

    private DetalleVenta mapearDetalle(ResultSet rs) throws SQLException {
        DetalleVenta d = new DetalleVenta();
        d.setId(rs.getInt("id"));
        d.setVentaId(rs.getInt("venta_id"));
        d.setProductoId(rs.getInt("producto_id"));
        try {
            d.setProductoNombre(rs.getString("producto_nombre"));
        } catch (SQLException ignorado) {
            // No proyectada en la variante usada durante la anulación; no es necesaria allí.
        }
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        return d;
    }
}
