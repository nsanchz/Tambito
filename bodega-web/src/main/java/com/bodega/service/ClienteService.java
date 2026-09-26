package com.bodega.service;

import com.bodega.dao.ClienteDAO;
import com.bodega.model.Cliente;
import com.bodega.model.EstadoCuenta;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Lógica de negocio de clientes. */
public class ClienteService {

    private final ClienteDAO clienteDAO;

    public ClienteService() {
        this.clienteDAO = new ClienteDAO();
    }

    /** Resultado de una operación de gestión de clientes. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;
        public final Integer clienteId;

        public ResultadoOperacion(boolean exitoso, String mensaje, Integer clienteId) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.clienteId = clienteId;
        }
    }

    /**
     * @param filtroEstado  estado a filtrar, o {@code null} para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return clientes que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<Cliente> listar(EstadoCuenta filtroEstado, String textoBusqueda) throws SQLException {
        return clienteDAO.listar(filtroEstado, textoBusqueda);
    }

    /**
     * @param numeroDocumento DNI o RUC exacto a buscar
     * @return el cliente si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorNumeroDocumento(String numeroDocumento) throws SQLException {
        return clienteDAO.buscarPorNumeroDocumento(numeroDocumento);
    }

    /**
     * @param id id del cliente
     * @return el cliente si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<Cliente> buscarPorId(int id) throws SQLException {
        return clienteDAO.buscarPorId(id);
    }

    /**
     * Registra un nuevo cliente (desde la pantalla de Clientes o directamente desde el POS).
     *
     * @param c cliente a crear, con tipo y número de documento obligatorios
     * @return resultado exitoso con el id creado, o un mensaje de error de negocio
     *         (documento vacío, documento duplicado) si no
     * @throws SQLException si falla la inserción
     */
    public ResultadoOperacion registrar(Cliente c) throws SQLException {
        if (c.getNumeroDocumento() == null || c.getNumeroDocumento().isBlank()) {
            return new ResultadoOperacion(false, "El número de documento es obligatorio.", null);
        }
        if (clienteDAO.existeNumeroDocumento(c.getNumeroDocumento())) {
            return new ResultadoOperacion(false, "Ya existe un cliente registrado con ese número de documento.", null);
        }
        c.setEstado(EstadoCuenta.ACTIVO);
        int id = clienteDAO.crear(c);
        return new ResultadoOperacion(true, "Cliente registrado correctamente.", id);
    }

    /**
     * @param c cliente con el id de la fila a actualizar y los nuevos datos de contacto
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion actualizar(Cliente c) throws SQLException {
        clienteDAO.actualizar(c);
        return new ResultadoOperacion(true, "Cliente actualizado correctamente.", c.getId());
    }

    /**
     * @param id          id del cliente
     * @param nuevoEstado ACTIVO o INACTIVO
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cambiarEstado(int id, EstadoCuenta nuevoEstado) throws SQLException {
        clienteDAO.cambiarEstado(id, nuevoEstado);
        return new ResultadoOperacion(true, "Estado del cliente actualizado correctamente.", id);
    }
}
