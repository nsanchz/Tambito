package com.bodega.service;

import com.bodega.dao.ClienteDAO;
import com.bodega.model.Cliente;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.TipoDocumentoCliente;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Lógica de negocio de clientes. */
public class ClienteService {

    // Formato de correo simple pero suficiente (no se intenta validar RFC 5322 completo,
    // solo descartar entradas obviamente inválidas antes de depender de él para enviar boletas).
    private static final Pattern PATRON_CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

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
        ResultadoOperacion errorValidacion = validar(c);
        if (errorValidacion != null) {
            return errorValidacion;
        }
        if (clienteDAO.existeNumeroDocumento(c.getNumeroDocumento())) {
            return new ResultadoOperacion(false, "Ya existe un cliente registrado con ese número de documento.", null);
        }
        c.setEstado(EstadoCuenta.ACTIVO);
        int id = clienteDAO.crear(c);
        return new ResultadoOperacion(true, "Cliente registrado correctamente.", id);
    }

    /**
     * Valida documento (obligatorio, formato correcto según DNI=8 dígitos / RUC=11 dígitos)
     * y correo (obligatorio y con formato válido, para poder enviarle la boleta por correo
     * tras cada venta — ver {@code EmailService.enviarComprobanteVenta}).
     *
     * @return {@code null} si todo es válido, o el {@link ResultadoOperacion} de error a devolver
     */
    private ResultadoOperacion validar(Cliente c) {
        if (c.getNumeroDocumento() == null || c.getNumeroDocumento().isBlank()) {
            return new ResultadoOperacion(false, "El número de documento es obligatorio.", null);
        }
        if (c.getTipoDocumento() == null) {
            return new ResultadoOperacion(false, "Debe indicar el tipo de documento (DNI o RUC).", null);
        }
        String numero = c.getNumeroDocumento().trim();
        if (c.getTipoDocumento() == TipoDocumentoCliente.DNI && !numero.matches("\\d{8}")) {
            return new ResultadoOperacion(false, "El DNI debe tener exactamente 8 dígitos numéricos.", null);
        }
        if (c.getTipoDocumento() == TipoDocumentoCliente.RUC && !numero.matches("\\d{11}")) {
            return new ResultadoOperacion(false, "El RUC debe tener exactamente 11 dígitos numéricos.", null);
        }
        if (c.getCorreo() == null || c.getCorreo().isBlank()) {
            return new ResultadoOperacion(false,
                    "El correo es obligatorio: se usa para enviarle la boleta electrónica de cada compra.", null);
        }
        if (!PATRON_CORREO.matcher(c.getCorreo().trim()).matches()) {
            return new ResultadoOperacion(false, "El correo ingresado no tiene un formato válido.", null);
        }
        return null;
    }

    /**
     * @param c cliente con el id de la fila a actualizar y los nuevos datos de contacto
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion actualizar(Cliente c) throws SQLException {
        ResultadoOperacion errorValidacion = validar(c);
        if (errorValidacion != null) {
            return errorValidacion;
        }
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
