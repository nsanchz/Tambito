package com.bodega.servlet;

import com.bodega.model.Categoria;
import com.bodega.model.Cliente;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.MetodoPago;
import com.bodega.model.Producto;
import com.bodega.model.TipoComprobante;
import com.bodega.model.TipoDocumentoCliente;
import com.bodega.model.Venta;
import com.bodega.service.CategoriaService;
import com.bodega.service.ClienteService;
import com.bodega.service.EmailService;
import com.bodega.service.ProductoService;
import com.bodega.service.TicketVentaService;
import com.bodega.service.TurnoCajaService;
import com.bodega.service.VentaService;
import com.bodega.service.VentaService.ItemCarrito;
import com.bodega.service.VentaService.ResultadoVenta;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Punto de Venta (POS): pantalla de registro de ventas usada por cajeros y administradores. */
@WebServlet("/pos")
public class PosServlet extends HttpServlet {

    private final ProductoService productoService = new ProductoService();
    private final CategoriaService categoriaService = new CategoriaService();
    private final ClienteService clienteService = new ClienteService();
    private final VentaService ventaService = new VentaService();
    private final TurnoCajaService turnoCajaService = new TurnoCajaService();
    private final TicketVentaService ticketVentaService = new TicketVentaService();
    private final EmailService emailService = new EmailService();

    /**
     * Muestra la terminal del POS con el catálogo de productos activos, exigiendo que
     * el usuario tenga un turno de caja abierto (redirige a {@code /turno-caja} si no).
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el redirect o el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        try {
            // El POS exige un turno de caja abierto, igual que una caja registradora física real.
            if (turnoCajaService.obtenerTurnoAbierto(usuarioIdSesion).isEmpty()) {
                resp.sendRedirect(req.getContextPath() + "/turno-caja?requerido=true");
                return;
            }

            List<Producto> productos = productoService.listar(null, EstadoCuenta.ACTIVO, req.getParameter("busqueda"));
            List<Categoria> categorias = categoriaService.listar(EstadoCuenta.ACTIVO, null);

            req.setAttribute("productos", productos);
            req.setAttribute("categorias", categorias);
            req.setAttribute("tituloPagina", "Punto de Venta (POS)");
            req.setAttribute("moduloActivo", "pos");
            req.getRequestDispatcher("/WEB-INF/views/pos/terminal.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al cargar el catálogo del Punto de Venta.", e);
        }
    }

    /**
     * Procesa el cobro de una venta armada en el carrito del POS. Vuelve a validar el
     * turno de caja (defensa en profundidad) antes de delegar en {@link VentaService#registrarVenta}.
     *
     * @param req  petición HTTP con las líneas del carrito, tipo de comprobante, método de pago,
     *             descuento y datos del cliente
     * @param resp respuesta HTTP
     * @throws ServletException si falla la transacción contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String terminalId = (String) req.getSession().getAttribute(Constantes.SESSION_TERMINAL_ID);

        try {
            // Defensa en profundidad: nunca confiar solo en que el GET ya validó el turno abierto.
            if (turnoCajaService.obtenerTurnoAbierto(usuarioIdSesion).isEmpty()) {
                req.getSession().setAttribute(Constantes.ATTR_ERROR, "Debe abrir un turno de caja antes de registrar ventas.");
                resp.sendRedirect(req.getContextPath() + "/turno-caja?requerido=true");
                return;
            }

            Integer clienteId = resolverCliente(req);

            String[] productoIds = req.getParameterValues("carritoProductoId");
            String[] cantidades = req.getParameterValues("carritoCantidad");

            List<ItemCarrito> items = new ArrayList<>();
            if (productoIds != null) {
                for (int i = 0; i < productoIds.length; i++) {
                    items.add(new ItemCarrito(Integer.parseInt(productoIds[i]), Integer.parseInt(cantidades[i])));
                }
            }

            TipoComprobante tipoComprobante = TipoComprobante.valueOf(req.getParameter("tipoComprobante"));
            MetodoPago metodoPago = MetodoPago.valueOf(req.getParameter("metodoPago"));
            String descuentoParam = req.getParameter("descuento");
            BigDecimal descuento = (descuentoParam != null && !descuentoParam.isBlank())
                    ? new BigDecimal(descuentoParam) : BigDecimal.ZERO;

            ResultadoVenta resultado = ventaService.registrarVenta(
                    items, tipoComprobante, clienteId, metodoPago, descuento, terminalId, usuarioIdSesion);

            if (resultado.exitoso) {
                enviarComprobantePorCorreoEnSegundoPlano(resultado.ventaId, clienteId);
                req.getSession().setAttribute(Constantes.ATTR_MENSAJE, resultado.mensaje);
                resp.sendRedirect(req.getContextPath() + "/ventas?ver=" + resultado.ventaId);
                return;
            } else {
                req.getSession().setAttribute(Constantes.ATTR_ERROR, resultado.mensaje);
            }
        } catch (SQLException e) {
            throw new ServletException("Error al registrar la venta.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/pos");
    }

    /**
     * Resuelve el cliente de la venta: uno ya registrado (por documento), uno nuevo
     * capturado directamente en el POS, o "Cliente varios" ({@code null}).
     *
     * @param req petición HTTP con {@code clienteTipo} y, si es "registrado", los datos del cliente
     * @return id del cliente a asociar a la venta, o {@code null} para "Cliente varios"
     * @throws SQLException si falla la consulta o el registro del cliente nuevo
     */
    private Integer resolverCliente(HttpServletRequest req) throws SQLException {
        String tipoCliente = req.getParameter("clienteTipo");
        if (!"registrado".equals(tipoCliente)) {
            return null;
        }

        String numeroDocumento = req.getParameter("clienteNumeroDocumento");
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            return null;
        }

        Optional<Cliente> clienteOpt = clienteService.buscarPorNumeroDocumento(numeroDocumento);
        if (clienteOpt.isPresent()) {
            return clienteOpt.get().getId();
        }

        // Cliente nuevo capturado directamente desde el POS.
        Cliente nuevo = new Cliente();
        nuevo.setTipoDocumento(TipoDocumentoCliente.valueOf(req.getParameter("clienteTipoDocumento")));
        nuevo.setNumeroDocumento(numeroDocumento);
        nuevo.setNombreCompleto(req.getParameter("clienteNombre"));
        nuevo.setTelefono(req.getParameter("clienteTelefono"));

        var resultado = clienteService.registrar(nuevo);
        return resultado.exitoso ? resultado.clienteId : null;
    }

    /**
     * Si la venta tiene un cliente registrado con correo, le envía el comprobante en PDF
     * como adjunto, en un hilo aparte para no demorar la respuesta al cajero con el
     * round-trip de SMTP. Cualquier error (cliente sin correo, fallo de generación del PDF
     * o de envío) solo se registra en el log: nunca afecta la venta ya confirmada.
     *
     * @param ventaId   id de la venta recién registrada
     * @param clienteId id del cliente asociado, o {@code null} para "Cliente varios"
     */
    private void enviarComprobantePorCorreoEnSegundoPlano(Integer ventaId, Integer clienteId) {
        if (clienteId == null) {
            return;
        }
        Thread hilo = new Thread(() -> {
            try {
                Optional<Cliente> clienteOpt = clienteService.buscarPorId(clienteId);
                if (clienteOpt.isEmpty() || clienteOpt.get().getCorreo() == null || clienteOpt.get().getCorreo().isBlank()) {
                    return;
                }
                Optional<Venta> ventaOpt = ventaService.buscarPorId(ventaId);
                if (ventaOpt.isEmpty()) {
                    return;
                }
                byte[] pdfTicket = ticketVentaService.generarTicket(ventaId);
                emailService.enviarComprobanteVenta(clienteOpt.get().getCorreo(), ventaOpt.get().getNumeroComprobante(), pdfTicket);
            } catch (Exception e) {
                System.err.println("No se pudo enviar el comprobante por correo de la venta #" + ventaId + ": " + e.getMessage());
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }
}
