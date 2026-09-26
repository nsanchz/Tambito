package com.bodega.servlet;

import com.bodega.model.Cliente;
import com.bodega.model.EstadoCuenta;
import com.bodega.model.TipoDocumentoCliente;
import com.bodega.model.Venta;
import com.bodega.service.ClienteService;
import com.bodega.service.ClienteService.ResultadoOperacion;
import com.bodega.service.VentaService;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Gestión de Clientes y Fidelización, incluida la ficha de detalle con historial de compras. */
@WebServlet("/clientes")
public class ClienteServlet extends HttpServlet {

    private final ClienteService clienteService = new ClienteService();
    private final VentaService ventaService = new VentaService();

    /**
     * Lista clientes aplicando filtros, y si llega {@code ?ver=<id>} carga además la
     * ficha de detalle con su historial de compras.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            String filtroEstadoParam = req.getParameter("estado");
            String busqueda = req.getParameter("busqueda");
            EstadoCuenta filtroEstado = (filtroEstadoParam != null && !filtroEstadoParam.equals("todos"))
                    ? EstadoCuenta.valueOf(filtroEstadoParam) : null;

            List<Cliente> clientes = clienteService.listar(filtroEstado, busqueda);
            req.setAttribute("clientes", clientes);

            String verClienteId = req.getParameter("ver");
            if (verClienteId != null && !verClienteId.isBlank()) {
                cargarFichaCliente(req, Integer.parseInt(verClienteId));
            }

            req.setAttribute("tituloPagina", "Gestión de Clientes y Fidelización");
            req.setAttribute("moduloActivo", "clientes");
            req.getRequestDispatcher("/WEB-INF/views/clientes/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al listar clientes.", e);
        }
    }

    /**
     * Carga en el request los datos de la ficha de un cliente (datos, historial de
     * compras y total histórico) si el cliente existe.
     *
     * @param req       petición HTTP donde se colocan los atributos {@code clienteSeleccionado},
     *                  {@code historialCompras} y {@code totalHistoricoCompras}
     * @param clienteId id del cliente a mostrar
     * @throws SQLException si falla la consulta a la base de datos
     */
    private void cargarFichaCliente(HttpServletRequest req, int clienteId) throws SQLException {
        Optional<Cliente> clienteOpt = clienteService.buscarPorId(clienteId);
        if (clienteOpt.isEmpty()) {
            return;
        }
        req.setAttribute("clienteSeleccionado", clienteOpt.get());

        List<Venta> historialCompras = ventaService.listar(null, null, null, clienteOpt.get().getNumeroDocumento());
        req.setAttribute("historialCompras", historialCompras);

        java.math.BigDecimal totalHistorico = historialCompras.stream()
                .filter(v -> v.getEstado() == com.bodega.model.EstadoVenta.COMPLETADA)
                .map(Venta::getTotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        req.setAttribute("totalHistoricoCompras", totalHistorico);
    }

    /**
     * Despacha las acciones sobre clientes ({@code crear}, {@code actualizar},
     * {@code desactivar}, {@code reactivar}).
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la operación contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String accion = req.getParameter("accion");

        try {
            ResultadoOperacion resultado = switch (accion) {
                case "crear" -> {
                    Cliente c = new Cliente();
                    c.setTipoDocumento(TipoDocumentoCliente.valueOf(req.getParameter("tipoDocumento")));
                    c.setNumeroDocumento(req.getParameter("numeroDocumento"));
                    c.setNombreCompleto(req.getParameter("nombreCompleto"));
                    c.setTelefono(req.getParameter("telefono"));
                    c.setCorreo(req.getParameter("correo"));
                    c.setDireccion(req.getParameter("direccion"));
                    yield clienteService.registrar(c);
                }
                case "actualizar" -> {
                    Cliente c = new Cliente();
                    c.setId(Integer.parseInt(req.getParameter("clienteId")));
                    c.setNombreCompleto(req.getParameter("nombreCompleto"));
                    c.setTelefono(req.getParameter("telefono"));
                    c.setCorreo(req.getParameter("correo"));
                    c.setDireccion(req.getParameter("direccion"));
                    yield clienteService.actualizar(c);
                }
                case "desactivar" -> clienteService.cambiarEstado(Integer.parseInt(req.getParameter("clienteId")), EstadoCuenta.INACTIVO);
                case "reactivar" -> clienteService.cambiarEstado(Integer.parseInt(req.getParameter("clienteId")), EstadoCuenta.ACTIVO);
                default -> new ResultadoOperacion(false, "Acción no reconocida.", null);
            };

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación sobre clientes.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/clientes");
    }
}
