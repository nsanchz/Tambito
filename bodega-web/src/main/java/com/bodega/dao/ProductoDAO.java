package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Producto;
import com.bodega.model.UnidadMedida;

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
 * Acceso a datos de la tabla productos mediante JDBC puro con PreparedStatement.
 * Los métodos de ajuste de stock aceptan una {@link Connection} externa para poder
 * participar en la transacción ACID de una venta o de una anulación (ver VentaService),
 * evitando así que HikariCP entregue una conexión distinta a mitad de la operación.
 */
public class ProductoDAO {

    /**
     * Inserta un nuevo producto.
     *
     * @param p producto a crear (sin id)
     * @return el id autogenerado por la base de datos
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int crear(Producto p) throws SQLException {
        String sql = "INSERT INTO productos " +
                "(sku, codigo_barras, nombre, descripcion, categoria_id, proveedor_id, marca, precio_compra, precio_venta, " +
                "stock_actual, stock_minimo, unidad_medida, estado, imagen_url, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getSku());
            ps.setString(2, p.getCodigoBarras());
            ps.setString(3, p.getNombre());
            ps.setString(4, p.getDescripcion());
            ps.setInt(5, p.getCategoriaId());
            if (p.getProveedorId() != null) {
                ps.setInt(6, p.getProveedorId());
            } else {
                ps.setNull(6, java.sql.Types.INTEGER);
            }
            ps.setString(7, p.getMarca());
            ps.setBigDecimal(8, p.getPrecioCompra());
            ps.setBigDecimal(9, p.getPrecioVenta());
            ps.setInt(10, p.getStockActual());
            ps.setInt(11, p.getStockMinimo());
            ps.setString(12, p.getUnidadMedida().name());
            ps.setString(13, p.getEstado().name());
            ps.setString(14, p.getImagenUrl());
            ps.setTimestamp(15, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al crear el producto.");
    }

    /**
     * @param id id del producto
     * @return el producto si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Producto> buscarPorId(int id) throws SQLException {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Variante para usar dentro de una transacción de venta ya abierta: bloquea la fila
     * con {@code FOR UPDATE} para evitar que dos ventas concurrentes vendan el mismo
     * stock antes de que la primera confirme su descuento.
     *
     * @param con conexión de la transacción externa (venta, anulación o movimiento de inventario)
     * @param id  id del producto a bloquear
     * @return el producto si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Producto> buscarPorIdParaActualizar(Connection con, int id) throws SQLException {
        String sql = "SELECT * FROM productos WHERE id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Busca un producto por SKU o código de barras, usado por el buscador del POS.
     *
     * @param texto SKU o código de barras exacto a buscar
     * @return el producto si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Producto> buscarPorSkuOCodigoBarras(String texto) throws SQLException {
        String sql = "SELECT * FROM productos WHERE sku = ? OR codigo_barras = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, texto);
            ps.setString(2, texto);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Lista productos aplicando filtros opcionales.
     *
     * @param categoriaId   id de categoría a filtrar, o {@code null} para no filtrar por categoría
     * @param filtroEstado  estado a filtrar (ACTIVO/INACTIVO), o {@code null} para no filtrar por estado
     * @param textoBusqueda texto a buscar en SKU, código de barras, nombre o marca, o {@code null}/vacío para no filtrar
     * @return productos que cumplen los filtros, ordenados por nombre
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listar(Integer categoriaId, EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM productos WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (categoriaId != null) {
            sql.append(" AND categoria_id = ?");
            parametros.add(categoriaId);
        }
        if (filtroEstado != null) {
            sql.append(" AND estado = ?");
            parametros.add(filtroEstado.name());
        }
        if (textoBusqueda != null && !textoBusqueda.isBlank()) {
            sql.append(" AND (sku LIKE ? OR codigo_barras LIKE ? OR nombre LIKE ? OR marca LIKE ?)");
            String comodin = "%" + textoBusqueda.trim() + "%";
            for (int i = 0; i < 4; i++) {
                parametros.add(comodin);
            }
        }
        sql.append(" ORDER BY nombre ASC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * @return productos activos cuyo stock actual es menor o igual a su stock mínimo,
     *         ordenados de menor a mayor stock (los más urgentes primero)
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listarConStockBajoMinimo() throws SQLException {
        // Consulta la vista vista_productos_stock_bajo (ver schema.sql) en vez de repetir
        // aquí el WHERE stock_actual <= stock_minimo AND estado = 'ACTIVO' ORDER BY stock_actual ASC.
        String sql = "SELECT * FROM vista_productos_stock_bajo";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Producto> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
            return resultado;
        }
    }

    /**
     * Productos activos que no registran ninguna venta COMPLETADA en los últimos
     * {@code dias} días, usados para la alerta de baja rotación.
     *
     * @param dias tamaño de la ventana a analizar, en días
     * @return productos activos sin venta reciente, ordenados por nombre
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listarSinVentaReciente(int dias) throws SQLException {
        String sql = "SELECT p.* FROM productos p WHERE p.estado = 'ACTIVO' AND NOT EXISTS ( " +
                "SELECT 1 FROM detalle_venta dv JOIN ventas v ON v.id = dv.venta_id " +
                "WHERE dv.producto_id = p.id AND v.estado = 'COMPLETADA' " +
                "AND v.fecha_creacion >= NOW() - INTERVAL ? DAY) ORDER BY p.nombre ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, dias);
            try (ResultSet rs = ps.executeQuery()) {
                List<Producto> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    /**
     * Actualiza los datos editables de un producto (SKU y stock actual no se tocan aquí:
     * el stock solo cambia vía {@link #descontarStock} / {@link #incrementarStock}).
     *
     * @param p producto con el id de la fila a actualizar y los nuevos valores
     * @throws SQLException si falla la actualización
     */
    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE productos SET nombre = ?, descripcion = ?, categoria_id = ?, proveedor_id = ?, marca = ?, " +
                "precio_compra = ?, precio_venta = ?, stock_minimo = ?, unidad_medida = ?, codigo_barras = ? " +
                "WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setInt(3, p.getCategoriaId());
            if (p.getProveedorId() != null) {
                ps.setInt(4, p.getProveedorId());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, p.getMarca());
            ps.setBigDecimal(6, p.getPrecioCompra());
            ps.setBigDecimal(7, p.getPrecioVenta());
            ps.setInt(8, p.getStockMinimo());
            ps.setString(9, p.getUnidadMedida().name());
            ps.setString(10, p.getCodigoBarras());
            ps.setInt(11, p.getId());
            ps.executeUpdate();
        }
    }

    /**
     * @param id          id del producto
     * @param nuevoEstado ACTIVO o INACTIVO
     * @throws SQLException si falla la actualización
     */
    public void cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        String sql = "UPDATE productos SET estado = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevoEstado.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Descuenta stock dentro de una transacción externa (venta). La condición
     * {@code stock_actual >= ?} en el WHERE hace que la actualización afecte 0 filas
     * si no hay stock suficiente, lo que se traduce en la excepción de abajo para que
     * el llamador (VentaService) pueda hacer rollback de toda la operación.
     *
     * @param con        conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param productoId id del producto a descontar
     * @param cantidad   unidades a descontar
     * @throws SQLException si el producto no existe o no tiene stock suficiente
     */
    public void descontarStock(Connection con, int productoId, int cantidad) throws SQLException {
        String sql = "UPDATE productos SET stock_actual = stock_actual - ? WHERE id = ? AND stock_actual >= ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.setInt(3, cantidad);
            int filasAfectadas = ps.executeUpdate();
            if (filasAfectadas == 0) {
                throw new SQLException("Stock insuficiente para el producto ID " + productoId);
            }
        }
    }

    /**
     * Devuelve stock al inventario dentro de una transacción externa (anulación de venta
     * o recepción de una orden de compra).
     *
     * @param con        conexión de la transacción externa (nunca se abre ni cierra aquí)
     * @param productoId id del producto a incrementar
     * @param cantidad   unidades a devolver al stock
     * @throws SQLException si falla la actualización
     */
    public void incrementarStock(Connection con, int productoId, int cantidad) throws SQLException {
        String sql = "UPDATE productos SET stock_actual = stock_actual + ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        }
    }

    /**
     * @param sku código interno a verificar
     * @return {@code true} si ya existe un producto con ese SKU
     * @throws SQLException si falla la consulta
     */
    public boolean existeSku(String sku) throws SQLException {
        String sql = "SELECT COUNT(*) FROM productos WHERE sku = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Mapea la fila actual de un {@link ResultSet} de la tabla productos a un {@link Producto}. */
    private Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setId(rs.getInt("id"));
        p.setSku(rs.getString("sku"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        int proveedorId = rs.getInt("proveedor_id");
        p.setProveedorId(rs.wasNull() ? null : proveedorId);
        p.setMarca(rs.getString("marca"));
        p.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        p.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        p.setStockActual(rs.getInt("stock_actual"));
        p.setStockMinimo(rs.getInt("stock_minimo"));
        p.setUnidadMedida(UnidadMedida.valueOf(rs.getString("unidad_medida")));
        p.setEstado(EstadoCuenta.valueOf(rs.getString("estado")));
        p.setImagenUrl(rs.getString("imagen_url"));

        Timestamp fechaCreacion = rs.getTimestamp("fecha_creacion");
        p.setFechaCreacion(fechaCreacion != null ? fechaCreacion.toLocalDateTime() : null);

        return p;
    }
}
