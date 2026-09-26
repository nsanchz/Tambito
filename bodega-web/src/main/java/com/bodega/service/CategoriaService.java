package com.bodega.service;

import com.bodega.dao.CategoriaDAO;
import com.bodega.model.Categoria;
import com.bodega.model.EstadoCuenta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Lógica de negocio de categorías de productos. */
public class CategoriaService {

    private final CategoriaDAO categoriaDAO;

    public CategoriaService() {
        this.categoriaDAO = new CategoriaDAO();
    }

    /** Resultado de una operación de gestión de categorías. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;

        public ResultadoOperacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
    }

    /**
     * @param filtroEstado  estado a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return categorías que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Categoria> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        return categoriaDAO.listar(filtroEstado, textoBusqueda);
    }

    /**
     * @param id id de la categoría
     * @return la categoría si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Categoria> buscarPorId(int id) throws SQLException {
        return categoriaDAO.buscarPorId(id);
    }

    /**
     * Registra una nueva categoría, generando su código correlativo automáticamente.
     *
     * @param nombre         nombre de la categoría (obligatorio)
     * @param descripcion    descripción comercial
     * @param icono          identificador del ícono para el POS, o {@code null}/vacío para usar "category" por defecto
     * @param margenSugerido margen de ganancia sugerido, o {@code null} para usar cero
     * @param estadoInicial  estado inicial, o {@code null} para usar ACTIVO
     * @return resultado exitoso con el código generado en el mensaje, o un mensaje de error si falta el nombre
     * @throws SQLException si falla la inserción
     */
    public ResultadoOperacion registrar(String nombre, String descripcion, String icono,
                                         BigDecimal margenSugerido, EstadoCuenta estadoInicial) throws SQLException {
        if (nombre == null || nombre.isBlank()) {
            return new ResultadoOperacion(false, "El nombre de la categoría es obligatorio.");
        }

        Categoria c = new Categoria();
        c.setCodigo(categoriaDAO.siguienteCodigo());
        c.setNombre(nombre.trim());
        c.setDescripcion(descripcion);
        c.setIcono(icono != null && !icono.isBlank() ? icono : "category");
        c.setMargenSugerido(margenSugerido != null ? margenSugerido : BigDecimal.ZERO);
        c.setEstado(estadoInicial != null ? estadoInicial : EstadoCuenta.ACTIVO);

        int id = categoriaDAO.crear(c);
        return new ResultadoOperacion(true, "Categoría " + c.getCodigo() + " creada correctamente con ID " + id + ".");
    }

    /**
     * Desactiva una categoría, informando cuántos productos quedan sin agrupación visible.
     *
     * @param categoriaId id de la categoría a desactivar
     * @return resultado exitoso con la cantidad de productos afectados en el mensaje
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion desactivar(int categoriaId) throws SQLException {
        int productosAsignados = categoriaDAO.contarProductosAsignados(categoriaId);
        categoriaDAO.cambiarEstado(categoriaId, EstadoCuenta.INACTIVO);
        return new ResultadoOperacion(true, "Categoría desactivada. " + productosAsignados
                + " producto(s) asociado(s) permanecen en el catálogo pero sin agrupación visible en el POS.");
    }

    /**
     * @param categoriaId id de la categoría a reactivar
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion reactivar(int categoriaId) throws SQLException {
        categoriaDAO.cambiarEstado(categoriaId, EstadoCuenta.ACTIVO);
        return new ResultadoOperacion(true, "Categoría reactivada correctamente.");
    }
}
