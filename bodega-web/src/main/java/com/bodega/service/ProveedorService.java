package com.bodega.service;

import com.bodega.dao.ProveedorDAO;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.Proveedor;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Lógica de negocio de proveedores. */
public class ProveedorService {

    private final ProveedorDAO proveedorDAO;

    public ProveedorService() {
        this.proveedorDAO = new ProveedorDAO();
    }

    /** Resultado de una operación de gestión de proveedores. */
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
     * @return proveedores que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Proveedor> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        return proveedorDAO.listar(filtroEstado, textoBusqueda);
    }

    /**
     * @param id id del proveedor
     * @return el proveedor si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Proveedor> buscarPorId(int id) throws SQLException {
        return proveedorDAO.buscarPorId(id);
    }

    /**
     * @param p proveedor a crear, con razón social obligatoria
     * @return resultado exitoso con el id creado en el mensaje, o un mensaje de error si falta la razón social
     * @throws SQLException si falla la inserción
     */
    public ResultadoOperacion registrar(Proveedor p) throws SQLException {
        if (p.getRazonSocial() == null || p.getRazonSocial().isBlank()) {
            return new ResultadoOperacion(false, "La razón social del proveedor es obligatoria.");
        }
        p.setEstado(EstadoCuenta.ACTIVO);
        int id = proveedorDAO.crear(p);
        return new ResultadoOperacion(true, "Proveedor \"" + p.getRazonSocial() + "\" registrado correctamente con ID " + id + ".");
    }

    /**
     * @param id          id del proveedor
     * @param nuevoEstado ACTIVO o INACTIVO
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        proveedorDAO.cambiarEstado(id, nuevoEstado);
        return new ResultadoOperacion(true, "Estado del proveedor actualizado correctamente.");
    }
}
