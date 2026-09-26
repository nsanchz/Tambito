package com.bodega.service;

import com.bodega.model.DetalleOrdenCompra;
import com.bodega.model.LoteProducto;
import com.bodega.model.OrdenCompra;
import com.bodega.model.Producto;
import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Envío de notificaciones por correo del módulo de Órdenes de Compra (creación, aprobación,
 * rechazo) y del comprobante de venta al cliente, usando Gmail SMTP con una Contraseña de
 * Aplicación (ver bloque "mail.smtp.*" en database.properties).
 * <p>
 * Ningún método de esta clase debe romper la operación de negocio que lo invoca: quien la
 * usa siempre envuelve la llamada en un try/catch que solo registra la advertencia, igual
 * que ya hace {@link AuditoriaService} con los fallos de auditoría.
 */
public class EmailService {

    private static final Properties PROPS;

    static {
        PROPS = new Properties();
        try (InputStream input = EmailService.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró el archivo database.properties en el classpath.");
            }
            PROPS.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar la configuración de correo: " + e.getMessage(), e);
        }
    }

    private final Session sesion;
    private final String remitente;

    public EmailService() {
        this.remitente = PROPS.getProperty("mail.from");

        Properties propsSmtp = new Properties();
        propsSmtp.put("mail.smtp.host", PROPS.getProperty("mail.smtp.host"));
        propsSmtp.put("mail.smtp.port", PROPS.getProperty("mail.smtp.port"));
        propsSmtp.put("mail.smtp.auth", "true");
        propsSmtp.put("mail.smtp.starttls.enable", "true");

        String usuario = PROPS.getProperty("mail.smtp.username");
        String clave = PROPS.getProperty("mail.smtp.password");

        this.sesion = Session.getInstance(propsSmtp, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, clave);
            }
        });
    }

    /**
     * Notifica a los administradores activos que un vendedor creó una nueva orden de
     * compra pendiente de aprobación.
     *
     * @param orden         orden recién creada
     * @param correosAdmins correos de los administradores activos a notificar (puede estar vacío)
     * @throws MessagingException si falla el envío
     */
    public void notificarCreacionOrden(OrdenCompra orden, List<String> correosAdmins) throws MessagingException {
        if (correosAdmins.isEmpty()) {
            return;
        }
        String asunto = "Nueva orden de compra pendiente de aprobación: " + orden.getNumero();
        String cuerpo = "El vendedor " + orden.getUsuarioNombre() + " ha creado la OC " + orden.getNumero()
                + " y está pendiente de aprobación.\n\n"
                + "Proveedor: " + orden.getProveedorNombre() + "\n"
                + "Ingrese al sistema para revisarla y aprobarla o rechazarla.";
        for (String correo : correosAdmins) {
            enviarTexto(correo, asunto, cuerpo);
        }
    }

    /**
     * Notifica al proveedor que su orden de compra fue aprobada, con el detalle del pedido.
     *
     * @param ordenConDetalle orden aprobada, con {@code detalles} ya cargado
     * @param correoProveedor correo del proveedor
     * @throws MessagingException si falla el envío
     */
    public void notificarAprobacionAProveedor(OrdenCompra ordenConDetalle, String correoProveedor) throws MessagingException {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Se ha aprobado el siguiente pedido, orden de compra ").append(ordenConDetalle.getNumero()).append(":\n\n");
        cuerpo.append(String.format("%-40s %10s %15s%n", "Producto", "Cantidad", "Precio unit."));
        for (DetalleOrdenCompra d : ordenConDetalle.getDetalles()) {
            cuerpo.append(String.format("%-40s %10d %15s%n",
                    d.getProductoNombre(), d.getCantidadPedida(), d.getPrecioUnitario()));
        }
        cuerpo.append("\nQuedamos atentos a la coordinación de la entrega.");

        enviarTexto(correoProveedor, "Confirmación de pedido — Orden de compra " + ordenConDetalle.getNumero(),
                cuerpo.toString());
    }

    /**
     * Notifica al vendedor que creó la orden que esta fue rechazada.
     *
     * @param orden        orden rechazada
     * @param correoVendedor correo del vendedor que la creó
     * @param motivo       motivo del rechazo indicado por el administrador, o {@code null}/vacío
     * @throws MessagingException si falla el envío
     */
    public void notificarRechazoAVendedor(OrdenCompra orden, String correoVendedor, String motivo) throws MessagingException {
        String cuerpo = "Su orden de compra " + orden.getNumero() + " (proveedor: " + orden.getProveedorNombre()
                + ") fue rechazada por un administrador."
                + (motivo != null && !motivo.isBlank() ? "\n\nMotivo: " + motivo : "");
        enviarTexto(correoVendedor, "Orden de compra rechazada: " + orden.getNumero(), cuerpo);
    }

    /**
     * Envía a los administradores activos el resumen diario de inventario (stock bajo,
     * lotes por vencer y productos de baja rotación). No envía nada si las tres listas
     * llegan vacías, para no mandar un correo vacío cuando no hay nada pendiente.
     *
     * @param correosAdmins correos de los administradores activos a notificar
     * @param stockBajo     productos con stock actual igual o menor a su stock mínimo
     * @param porVencer     lotes próximos a vencer
     * @param bajaRotacion  productos sin ventas recientes
     * @throws MessagingException si falla el envío
     */
    public void notificarResumenAlertasInventario(List<String> correosAdmins, List<Producto> stockBajo,
                                                   List<LoteProducto> porVencer, List<Producto> bajaRotacion)
            throws MessagingException {
        if (correosAdmins.isEmpty() || (stockBajo.isEmpty() && porVencer.isEmpty() && bajaRotacion.isEmpty())) {
            return;
        }

        StringBuilder cuerpo = new StringBuilder("Resumen diario de alertas de inventario:\n");

        if (!stockBajo.isEmpty()) {
            cuerpo.append("\nPRODUCTOS CON STOCK BAJO (").append(stockBajo.size()).append("):\n");
            for (Producto p : stockBajo) {
                cuerpo.append(String.format("  - %-40s stock actual: %-6d mínimo: %d%n",
                        p.getNombre(), p.getStockActual(), p.getStockMinimo()));
            }
        }

        if (!porVencer.isEmpty()) {
            cuerpo.append("\nLOTES PRÓXIMOS A VENCER (").append(porVencer.size()).append("):\n");
            for (LoteProducto l : porVencer) {
                cuerpo.append(String.format("  - %-40s lote %-12s vence: %-12s cantidad: %d%n",
                        l.getProductoNombre(), l.getNumeroLote(), l.getFechaVencimiento(), l.getCantidadActual()));
            }
        }

        if (!bajaRotacion.isEmpty()) {
            cuerpo.append("\nPRODUCTOS SIN VENTAS RECIENTES (").append(bajaRotacion.size()).append("):\n");
            for (Producto p : bajaRotacion) {
                cuerpo.append(String.format("  - %-40s stock actual: %d%n", p.getNombre(), p.getStockActual()));
            }
        }

        cuerpo.append("\nIngrese al sistema para más detalle.");

        for (String correo : correosAdmins) {
            enviarTexto(correo, "Resumen diario de alertas de inventario — Bodega Tambito", cuerpo.toString());
        }
    }

    /**
     * Envía el comprobante de una venta (PDF ya generado por {@link TicketVentaService}) como
     * adjunto al correo registrado del cliente.
     *
     * @param correoCliente correo del cliente
     * @param numeroVenta   número de la venta, usado en el asunto y el nombre del archivo adjunto
     * @param pdfTicket     bytes del PDF del comprobante
     * @throws MessagingException si falla el envío
     */
    public void enviarComprobanteVenta(String correoCliente, String numeroVenta, byte[] pdfTicket) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(correoCliente));
        mensaje.setSubject("Comprobante de su compra — " + numeroVenta);

        MimeBodyPart cuerpoTexto = new MimeBodyPart();
        cuerpoTexto.setText("Gracias por su compra. Adjuntamos el comprobante de la venta " + numeroVenta + ".");

        MimeBodyPart adjunto = new MimeBodyPart();
        DataSource fuente = new ByteArrayDataSource(pdfTicket, "application/pdf");
        adjunto.setDataHandler(new DataHandler(fuente));
        adjunto.setFileName("boleta-" + numeroVenta + ".pdf");

        MimeMultipart multiparte = new MimeMultipart();
        multiparte.addBodyPart(cuerpoTexto);
        multiparte.addBodyPart(adjunto);
        mensaje.setContent(multiparte);

        Transport.send(mensaje);
    }

    private void enviarTexto(String destinatario, String asunto, String cuerpo) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        Transport.send(mensaje);
    }
}
