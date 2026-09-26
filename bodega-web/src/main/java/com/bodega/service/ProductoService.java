package com.bodega.service;

import com.bodega.dao.ProductoDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Producto;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Lógica de negocio del catálogo maestro de productos. */
public class ProductoService {

    private final ProductoDAO productoDAO;

    public ProductoService() {
        this.productoDAO = new ProductoDAO();
    }

    /** Resultado de una operación de gestión de productos. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;

        public ResultadoOperacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
    }

    /**
     * @param categoriaId   id de categoría a filtrar, o {@code null} para no filtrar
     * @param filtroEstado  estado a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return productos que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listar(Integer categoriaId, EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        return productoDAO.listar(categoriaId, filtroEstado, textoBusqueda);
    }

    /**
     * @param id id del producto
     * @return el producto si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Producto> buscarPorId(int id) throws SQLException {
        return productoDAO.buscarPorId(id);
    }

    /**
     * @return productos activos cuyo stock actual es menor o igual a su stock mínimo
     * @throws SQLException si falla la consulta
     */
    public List<Producto> listarConStockBajoMinimo() throws SQLException {
        return productoDAO.listarConStockBajoMinimo();
    }

    /**
     * Registra un nuevo producto, validando que el precio de venta no sea menor al de
     * compra y que el SKU no esté duplicado.
     *
     * @param p producto a crear
     * @return resultado exitoso con el id creado en el mensaje, o un mensaje de error de negocio
     *         (precio inválido, SKU duplicado) si no
     * @throws SQLException si falla la inserción
     */
    public ResultadoOperacion registrar(Producto p) throws SQLException {
        if (p.getPrecioVenta().compareTo(p.getPrecioCompra()) < 0) {
            return new ResultadoOperacion(false, "El precio de venta no puede ser menor al precio de compra.");
        }
        if (productoDAO.existeSku(p.getSku())) {
            return new ResultadoOperacion(false, "Ya existe un producto registrado con ese código interno (SKU).");
        }

        p.setEstado(p.getEstado() != null ? p.getEstado() : EstadoCuenta.ACTIVO);
        int id = productoDAO.crear(p);
        return new ResultadoOperacion(true, "Producto \"" + p.getNombre() + "\" registrado correctamente con ID " + id + ".");
    }

    /**
     * @param p producto con el id de la fila a actualizar y los nuevos valores
     * @return resultado exitoso, o un mensaje de error si el precio de venta es menor al de compra
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion actualizar(Producto p) throws SQLException {
        if (p.getPrecioVenta().compareTo(p.getPrecioCompra()) < 0) {
            return new ResultadoOperacion(false, "El precio de venta no puede ser menor al precio de compra.");
        }
        productoDAO.actualizar(p);
        return new ResultadoOperacion(true, "Producto actualizado correctamente.");
    }

    /**
     * @param productoId  id del producto
     * @param nuevoEstado ACTIVO o INACTIVO (un producto inactivo deja de aparecer en el POS)
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarEstado(int productoId, EstadoCuenta nuevoEstado) throws SQLException {
        productoDAO.cambiarEstado(productoId, nuevoEstado);
        return new ResultadoOperacion(true, nuevoEstado == EstadoCuenta.INACTIVO
                ? "Producto desactivado; ya no aparecerá disponible en el Punto de Venta."
                : "Producto reactivado correctamente.");
    }
}
