package com.bodega.service;

import com.bodega.config.DatabaseConfig;
import com.bodega.dao.LoteProductoDAO;
import com.bodega.dao.MovimientoInventarioDAO;
import com.bodega.dao.OrdenCompraDAO;
import com.bodega.dao.ProductoDAO;
import com.bodega.dao.UsuarioDAO;
import com.bodega.model.Producto;
import com.bodega.model.DetalleOrdenCompra;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.EstadoOrdenCompra;
import com.bodega.model.LoteProducto;
import com.bodega.model.MovimientoInventario;
import com.bodega.model.OrdenCompra;
import com.bodega.model.Rol;
import com.bodega.model.TipoMovimiento;
import com.bodega.model.Usuario;
import jakarta.mail.MessagingException;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lógica de negocio de órdenes de compra a proveedores. La recepción de mercadería
 * es la operación crítica: dentro de una única transacción ACID se bloquean las líneas
 * de detalle, se incrementa el stock de cada producto recibido, se inserta el movimiento
 * ENTRADA_COMPRA inmutable en el Kárdex (referenciando la orden) y se recalcula el estado
 * de la orden (RECIBIDA_PARCIAL o RECIBIDA_COMPLETA). Cualquier error revierte todo.
 */
public class OrdenCompraService {

    private final OrdenCompraDAO ordenCompraDAO;
    private final ProductoDAO productoDAO;
    private final MovimientoInventarioDAO movimientoDAO;
    private final LoteProductoDAO loteProductoDAO;
    private final UsuarioDAO usuarioDAO;
    private final EmailService emailService;

    public OrdenCompraService() {
        this.ordenCompraDAO = new OrdenCompraDAO();
        this.productoDAO = new ProductoDAO();
        this.movimientoDAO = new MovimientoInventarioDAO();
        this.loteProductoDAO = new LoteProductoDAO();
        this.usuarioDAO = new UsuarioDAO();
        this.emailService = new EmailService();
    }

    /** Resultado de crear, recibir o cancelar una orden de compra. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;

        public ResultadoOperacion(boolean exitoso, String mensaje) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
        }
    }

    /** Días de cobertura de stock que se buscan cubrir con la reposición sugerida. */
    private static final int DIAS_COBERTURA_OBJETIVO = 14;
    private static final int DIAS_HISTORICO_ANALIZADO = 28;

    /**
     * Sugiere la cantidad a pedir para un producto a partir de su velocidad de venta
     * real (promedio diario del Kárdex en las últimas 4 semanas), en vez de dejar que
     * el administrador la adivine. La fórmula cubre {@value DIAS_COBERTURA_OBJETIVO}
     * días de venta proyectada, descontando el stock que ya queda disponible.
     *
     * @param productoId id del producto a analizar
     * @return cantidad sugerida a pedir (nunca menor al stock mínimo del producto); 0 si el producto no existe
     * @throws SQLException si falla la consulta
     */
    public int sugerirCantidadReposicion(int productoId) throws SQLException {
        Optional<Producto> productoOpt = productoDAO.buscarPorId(productoId);
        if (productoOpt.isEmpty()) {
            return 0;
        }
        Producto producto = productoOpt.get();

        double promedioDiario = movimientoDAO.promedioVentaDiaria(productoId, DIAS_HISTORICO_ANALIZADO);
        int proyeccionConsumo = (int) Math.ceil(promedioDiario * DIAS_COBERTURA_OBJETIVO);
        int sugerido = proyeccionConsumo - producto.getStockActual();

        // Sin historial de ventas suficiente, se sugiere al menos reponer hasta el stock mínimo.
        return Math.max(sugerido, producto.getStockMinimo());
    }

    /**
     * @param filtroEstado estado a filtrar, o {@code null} para listar todas
     * @return órdenes de compra que cumplen el filtro
     * @throws SQLException si falla la consulta
     */
    public List<OrdenCompra> listar(EstadoOrdenCompra filtroEstado) throws SQLException {
        return ordenCompraDAO.listar(filtroEstado);
    }

    /**
     * @param id id de la orden de compra
     * @return la orden con su detalle, o {@link Optional#empty()} si no existe
     * @throws SQLException si falla la consulta
     */
    public Optional<OrdenCompra> buscarPorId(int id) throws SQLException {
        return ordenCompraDAO.buscarPorIdConDetalle(id);
    }

    /**
     * Registra una nueva orden de compra (cabecera + detalle) dentro de una transacción ACID.
     * No afecta stock ni Kárdex: eso ocurre recién al recibir la mercadería con {@link #recibirMercaderia}.
     *
     * @param proveedorId   id del proveedor al que se le hace el pedido
     * @param usuarioId     id del administrador que registra la orden
     * @param observaciones observaciones generales de la orden
     * @param lineas        líneas de detalle (producto, cantidad pedida, precio unitario); no puede estar vacía
     * @return resultado exitoso con el número de orden generado, o un mensaje de error si la lista de líneas está vacía
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoOperacion registrar(int proveedorId, int usuarioId, String observaciones,
                                         List<DetalleOrdenCompra> lineas) throws SQLException {
        if (lineas == null || lineas.isEmpty()) {
            return new ResultadoOperacion(false, "La orden de compra debe tener al menos un producto.");
        }

        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                OrdenCompra orden = new OrdenCompra();
                orden.setProveedorId(proveedorId);
                orden.setUsuarioId(usuarioId);
                orden.setEstado(EstadoOrdenCompra.PENDIENTE);
                orden.setObservaciones(observaciones);

                int ordenId = ordenCompraDAO.crearCabecera(con, orden);
                for (DetalleOrdenCompra linea : lineas) {
                    ordenCompraDAO.agregarDetalle(con, ordenId, linea);
                }

                con.commit();
                notificarCreacionAAdmins(ordenId);
                return new ResultadoOperacion(true, "Orden de compra OC-" + String.format("%06d", ordenId)
                        + " registrada correctamente y enviada al proveedor.");
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * Notifica por correo a todos los administradores activos que se creó una nueva orden
     * pendiente de aprobación. Se ejecuta después del commit (lectura fresca de la orden ya
     * confirmada); un fallo de envío nunca deshace la creación ya guardada.
     *
     * @param ordenId id de la orden recién creada
     */
    private void notificarCreacionAAdmins(int ordenId) {
        try {
            Optional<OrdenCompra> ordenOpt = ordenCompraDAO.buscarPorIdConDetalle(ordenId);
            if (ordenOpt.isEmpty()) {
                return;
            }
            List<String> correosAdmins = usuarioDAO.listar(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, null).stream()
                    .map(Usuario::getCorreo)
                    .filter(correo -> correo != null && !correo.isBlank())
                    .toList();
            emailService.notificarCreacionOrden(ordenOpt.get(), correosAdmins);
        } catch (SQLException | MessagingException e) {
            System.err.println("No se pudo notificar por correo la creación de la OC-"
                    + String.format("%06d", ordenId) + ": " + e.getMessage());
        }
    }

    /**
     * Aprueba una orden de compra pendiente y notifica por correo al proveedor con el
     * detalle del pedido.
     *
     * @param ordenId id de la orden a aprobar
     * @param adminId id del administrador que aprueba
     * @return resultado exitoso, o un mensaje de error si la orden no existe o ya fue procesada
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoOperacion aprobar(int ordenId, int adminId) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                EstadoOrdenCompra actual = ordenCompraDAO.buscarEstadoParaActualizar(con, ordenId);
                if (actual == null) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden de compra no existe.");
                }
                if (actual != EstadoOrdenCompra.PENDIENTE) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden ya fue procesada (estado actual: " + actual + ").");
                }
                ordenCompraDAO.aprobar(con, ordenId, adminId);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }

        String mensaje = "Orden de compra aprobada correctamente.";
        try {
            Optional<OrdenCompra> ordenOpt = ordenCompraDAO.buscarPorIdConDetalle(ordenId);
            if (ordenOpt.isPresent()) {
                OrdenCompra orden = ordenOpt.get();
                if (orden.getProveedorCorreo() != null && !orden.getProveedorCorreo().isBlank()) {
                    emailService.notificarAprobacionAProveedor(orden, orden.getProveedorCorreo());
                } else {
                    mensaje += " El proveedor no tiene correo registrado, no se le pudo notificar.";
                }
            }
        } catch (MessagingException e) {
            mensaje += " No se pudo notificar al proveedor por correo (" + e.getMessage() + ").";
        }
        return new ResultadoOperacion(true, mensaje);
    }

    /**
     * Rechaza una orden de compra pendiente y notifica por correo al vendedor que la creó.
     *
     * @param ordenId id de la orden a rechazar
     * @param motivo  motivo del rechazo, o {@code null}/vacío si no se indicó ninguno
     * @return resultado exitoso, o un mensaje de error si la orden no existe o ya fue procesada
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoOperacion rechazar(int ordenId, String motivo) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                EstadoOrdenCompra actual = ordenCompraDAO.buscarEstadoParaActualizar(con, ordenId);
                if (actual == null) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden de compra no existe.");
                }
                if (actual != EstadoOrdenCompra.PENDIENTE) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden ya fue procesada (estado actual: " + actual + ").");
                }
                ordenCompraDAO.rechazar(con, ordenId, motivo);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }

        String mensaje = "Orden de compra rechazada correctamente.";
        try {
            Optional<OrdenCompra> ordenOpt = ordenCompraDAO.buscarPorIdConDetalle(ordenId);
            if (ordenOpt.isPresent()) {
                OrdenCompra orden = ordenOpt.get();
                if (orden.getUsuarioCorreo() != null && !orden.getUsuarioCorreo().isBlank()) {
                    emailService.notificarRechazoAVendedor(orden, orden.getUsuarioCorreo(), motivo);
                }
            }
        } catch (MessagingException e) {
            mensaje += " No se pudo notificar al vendedor por correo (" + e.getMessage() + ").";
        }
        return new ResultadoOperacion(true, mensaje);
    }

    /**
     * Recibe mercadería de una orden de compra dentro de una única transacción ACID:
     * bloquea las líneas de detalle, incrementa el stock de cada producto recibido,
     * inserta el movimiento ENTRADA_COMPRA en el Kárdex (referenciando la orden) y
     * recalcula el estado de la orden.
     *
     * @param ordenCompraId               id de la orden de compra a recibir
     * @param cantidadesRecibidas         mapa {@code detalleId -> cantidad recibida EN ESTA RECEPCIÓN}
     *                                    (permite recepciones parciales en varias entregas del proveedor)
     * @param fechasVencimientoPorDetalle mapa {@code detalleId -> fecha de vencimiento del lote recibido}
     *                                    (puede tener valores {@code null} para productos no perecibles,
     *                                    o no traer entrada para un detalle sin fecha indicada)
     * @param usuarioId                   id del usuario que registra la recepción
     * @return resultado exitoso con el nuevo estado de la orden (RECIBIDA_PARCIAL o RECIBIDA_COMPLETA),
     *         o un mensaje de error de negocio (orden inexistente, cantidad excede lo pendiente,
     *         producto ya no existe, ninguna cantidad recibida) si no
     * @throws SQLException si falla alguna operación de base de datos (la transacción se revierte)
     */
    public ResultadoOperacion recibirMercaderia(int ordenCompraId, Map<Integer, Integer> cantidadesRecibidas,
                                                 Map<Integer, LocalDate> fechasVencimientoPorDetalle,
                                                 int usuarioId) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection()) {
            con.setAutoCommit(false);
            try {
                EstadoOrdenCompra estadoActual = ordenCompraDAO.buscarEstadoParaActualizar(con, ordenCompraId);
                if (estadoActual == null) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden de compra no existe.");
                }
                if (estadoActual != EstadoOrdenCompra.APROBADA
                        && estadoActual != EstadoOrdenCompra.RECIBIDA_PARCIAL) {
                    con.rollback();
                    return new ResultadoOperacion(false,
                            "Solo se puede recibir mercadería de una orden aprobada (estado actual: " + estadoActual + ").");
                }

                Map<Integer, DetalleOrdenCompra> detalles = ordenCompraDAO.obtenerDetallesParaActualizar(con, ordenCompraId);
                if (detalles.isEmpty()) {
                    con.rollback();
                    return new ResultadoOperacion(false, "La orden de compra no existe o no tiene líneas de detalle.");
                }

                boolean huboRecepcion = false;
                boolean quedaPendiente = false;

                for (DetalleOrdenCompra detalle : detalles.values()) {
                    Integer cantidadRecibidaAhora = cantidadesRecibidas.get(detalle.getId());
                    if (cantidadRecibidaAhora == null || cantidadRecibidaAhora <= 0) {
                        if (detalle.getCantidadPendiente() > 0) {
                            quedaPendiente = true;
                        }
                        continue;
                    }
                    if (cantidadRecibidaAhora > detalle.getCantidadPendiente()) {
                        con.rollback();
                        return new ResultadoOperacion(false, "La cantidad recibida para \"" + detalle.getProductoNombre()
                                + "\" excede lo pendiente por recibir (" + detalle.getCantidadPendiente() + ").");
                    }

                    var productoOpt = productoDAO.buscarPorIdParaActualizar(con, detalle.getProductoId());
                    if (productoOpt.isEmpty()) {
                        con.rollback();
                        return new ResultadoOperacion(false, "El producto asociado al detalle ya no existe en el catálogo.");
                    }
                    int stockAnterior = productoOpt.get().getStockActual();

                    productoDAO.incrementarStock(con, detalle.getProductoId(), cantidadRecibidaAhora);

                    LoteProducto lote = new LoteProducto();
                    lote.setProductoId(detalle.getProductoId());
                    lote.setFechaVencimiento(fechasVencimientoPorDetalle != null
                            ? fechasVencimientoPorDetalle.get(detalle.getId()) : null);
                    lote.setCantidadInicial(cantidadRecibidaAhora);
                    lote.setCantidadActual(cantidadRecibidaAhora);
                    lote.setPrecioCompra(detalle.getPrecioUnitario());
                    lote.setOrdenCompraId(ordenCompraId);
                    lote.setUsuarioId(usuarioId);
                    int loteId = loteProductoDAO.crear(con, lote);

                    MovimientoInventario movimiento = new MovimientoInventario();
                    movimiento.setProductoId(detalle.getProductoId());
                    movimiento.setLoteId(loteId);
                    movimiento.setTipo(TipoMovimiento.ENTRADA_COMPRA);
                    movimiento.setCantidad(cantidadRecibidaAhora);
                    movimiento.setStockAnterior(stockAnterior);
                    movimiento.setStockResultante(stockAnterior + cantidadRecibidaAhora);
                    movimiento.setMotivo("Recepción de mercadería - Orden de compra");
                    movimiento.setObservaciones("Orden OC-" + String.format("%06d", ordenCompraId));
                    movimiento.setCompraId(ordenCompraId);
                    movimiento.setUsuarioId(usuarioId);
                    movimientoDAO.registrar(con, movimiento);

                    int nuevaCantidadRecibida = detalle.getCantidadRecibida() + cantidadRecibidaAhora;
                    ordenCompraDAO.actualizarCantidadRecibida(con, detalle.getId(), nuevaCantidadRecibida);

                    huboRecepcion = true;
                    if (nuevaCantidadRecibida < detalle.getCantidadPedida()) {
                        quedaPendiente = true;
                    }
                }

                if (!huboRecepcion) {
                    con.rollback();
                    return new ResultadoOperacion(false, "Debe indicar al menos una cantidad recibida mayor a cero.");
                }

                EstadoOrdenCompra nuevoEstado = quedaPendiente ? EstadoOrdenCompra.RECIBIDA_PARCIAL : EstadoOrdenCompra.RECIBIDA_COMPLETA;
                ordenCompraDAO.actualizarEstado(con, ordenCompraId, nuevoEstado, LocalDateTime.now());

                con.commit();
                return new ResultadoOperacion(true, "Recepción registrada correctamente. Estado de la orden: " + nuevoEstado + ".");
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * @param ordenCompraId id de la orden de compra a cancelar
     * @return resultado exitoso
     * @throws SQLException si falla la actualización
     */
    public ResultadoOperacion cancelar(int ordenCompraId) throws SQLException {
        try (Connection con = DatabaseConfig.getConnection()) {
            ordenCompraDAO.actualizarEstado(con, ordenCompraId, EstadoOrdenCompra.CANCELADA, null);
            return new ResultadoOperacion(true, "Orden de compra cancelada correctamente.");
        }
    }
}
