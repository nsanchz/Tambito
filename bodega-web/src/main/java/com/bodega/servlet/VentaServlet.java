package com.bodega.servlet;

import com.bodega.model.EstadoVenta;
import com.bodega.model.MetodoPago;
import com.bodega.model.Rol;
import com.bodega.model.Venta;
import com.bodega.service.AuditoriaService;
import com.bodega.service.ExportExcelService;
import com.bodega.service.TicketVentaService;
import com.bodega.service.VentaService;
import com.bodega.service.VentaService.ResultadoVenta;
import com.bodega.util.Constantes;
import com.bodega.util.DescargaHttpUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Historial de Ventas y Comprobantes. La anulación de una venta está restringida a
 * ADMINISTRADOR: aunque este Servlet es visible para ambos roles (para que el vendedor
 * consulte su historial), el botón de anular solo se renderiza para administradores en
 * la vista JSP y aquí se vuelve a validar el rol en el servidor antes de ejecutar nada
 * (nunca confiar solo en que el botón esté oculto en el HTML).
 */
@WebServlet("/ventas")
public class VentaServlet extends HttpServlet {

    private final VentaService ventaService = new VentaService();
    private final AuditoriaService auditoriaService = new AuditoriaService();
    private final TicketVentaService ticketVentaService = new TicketVentaService();
    private final ExportExcelService exportExcelService = new ExportExcelService();

    /**
     * Si llega {@code ?ticketPdf=<id>}, descarga el ticket de esa venta. En caso contrario,
     * lista el historial de ventas (acotado a las propias si el rol es VENDEDOR) y,
     * si llega {@code ?ver=<id>}, carga además el detalle de una venta seleccionada.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos o la generación del PDF
     * @throws IOException      si falla el envío del PDF o el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String ticketVentaId = req.getParameter("ticketPdf");
        if (ticketVentaId != null && !ticketVentaId.isBlank()) {
            enviarTicketPdf(resp, Integer.parseInt(ticketVentaId));
            return;
        }

        try {
            String estadoParam = req.getParameter("estado");
            String metodoPagoParam = req.getParameter("metodoPago");
            String busqueda = req.getParameter("busqueda");

            EstadoVenta filtroEstado = (estadoParam != null && !estadoParam.isBlank()) ? EstadoVenta.valueOf(estadoParam) : null;
            MetodoPago filtroMetodoPago = (metodoPagoParam != null && !metodoPagoParam.isBlank()) ? MetodoPago.valueOf(metodoPagoParam) : null;

            // Un VENDEDOR solo ve sus propias ventas; el ADMINISTRADOR ve todas.
            String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);
            Integer usuarioIdFiltro = Rol.ADMINISTRADOR.name().equals(rolSesion)
                    ? null : (Integer) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

            if ("excel".equals(req.getParameter("exportar"))) {
                byte[] contenido = exportExcelService.exportarHistorialVentas(filtroEstado, filtroMetodoPago, usuarioIdFiltro, busqueda);
                DescargaHttpUtil.enviarExcel(resp, contenido, "ventas.xlsx");
                return;
            }

            List<Venta> ventas = ventaService.listar(filtroEstado, filtroMetodoPago, usuarioIdFiltro, busqueda);
            req.setAttribute("ventas", ventas);
            req.setAttribute("estadosVenta", EstadoVenta.values());
            req.setAttribute("metodosPago", MetodoPago.values());

            String verVentaId = req.getParameter("ver");
            if (verVentaId != null && !verVentaId.isBlank()) {
                Optional<Venta> ventaSeleccionada = ventaService.buscarPorId(Integer.parseInt(verVentaId));
                ventaSeleccionada.ifPresent(v -> req.setAttribute("ventaSeleccionada", v));
            }

            req.setAttribute("tituloPagina", "Historial de Ventas y Comprobantes");
            req.setAttribute("moduloActivo", "ventas");
            req.getRequestDispatcher("/WEB-INF/views/ventas/historial.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar el historial de ventas.", e);
        }
    }

    /**
     * Anula una venta. Verifica en el servidor que el rol sea ADMINISTRADOR antes de
     * ejecutar nada (nunca confía en que el botón de anular esté oculto para un VENDEDOR
     * en el HTML), y registra la anulación en auditoría.
     *
     * @param req  petición HTTP con {@code ventaId} y {@code motivoAnulacion}
     * @param resp respuesta HTTP
     * @throws ServletException si falla la transacción de anulación
     * @throws IOException      si falla el error 403 o el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);
        if (!Rol.ADMINISTRADOR.name().equals(rolSesion)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Solo un administrador puede anular ventas.");
            return;
        }

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        int ventaId = Integer.parseInt(req.getParameter("ventaId"));
        String motivo = req.getParameter("motivoAnulacion");

        try {
            ResultadoVenta resultado = ventaService.anularVenta(ventaId, motivo, usuarioIdSesion);

            if (resultado.exitoso) {
                auditoriaService.registrar(usuarioIdSesion, "VENTA_ANULADA", "VENTA", ventaId,
                        "Venta anulada. Motivo: " + motivo, req.getRemoteAddr());
            }

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al anular la venta.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/ventas");
    }

    /**
     * @param resp    respuesta HTTP donde se escribe el PDF
     * @param ventaId id de la venta a imprimir
     * @throws ServletException si falla la generación del ticket
     * @throws IOException      si falla la escritura en el stream de salida
     */
    private void enviarTicketPdf(HttpServletResponse resp, int ventaId) throws ServletException, IOException {
        try {
            byte[] pdf = ticketVentaService.generarTicket(ventaId);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "inline; filename=\"ticket-venta-" + ventaId + ".pdf\"");
            resp.setContentLength(pdf.length);
            resp.getOutputStream().write(pdf);
            resp.getOutputStream().flush();
        } catch (SQLException e) {
            throw new ServletException("Error al generar el ticket de la venta.", e);
        }
    }
}
