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

        StringBuilder contenido = new StringBuilder(
                "<p>Este es el resumen diario de alertas de inventario que requieren su atención:</p>");

        if (!stockBajo.isEmpty()) {
            contenido.append("<p style=\"font-weight:bold;color:").append(COLOR_ACENTO)
                    .append(";margin-bottom:0;\">⚠ Productos con stock bajo (").append(stockBajo.size()).append(")</p>")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">")
                    .append(encabezadoTabla("Producto", "Stock actual", "Mínimo"));
            for (Producto p : stockBajo) {
                contenido.append(filaTabla(p.getNombre(), String.valueOf(p.getStockActual()), String.valueOf(p.getStockMinimo())));
            }
            contenido.append("</table>");
        }

        if (!porVencer.isEmpty()) {
            contenido.append("<p style=\"font-weight:bold;color:#d97706;margin-bottom:0;\">⏰ Lotes próximos a vencer (")
                    .append(porVencer.size()).append(")</p>")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">")
                    .append(encabezadoTabla("Producto", "Lote", "Vence", "Cantidad"));
            for (LoteProducto l : porVencer) {
                contenido.append(filaTabla(l.getProductoNombre(), l.getNumeroLote(),
                        String.valueOf(l.getFechaVencimiento()), String.valueOf(l.getCantidadActual())));
            }
            contenido.append("</table>");
        }

        if (!bajaRotacion.isEmpty()) {
            contenido.append("<p style=\"font-weight:bold;color:#64748b;margin-bottom:0;\">📉 Productos sin ventas recientes (")
                    .append(bajaRotacion.size()).append(")</p>")
                    .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;\">")
                    .append(encabezadoTabla("Producto", "Stock actual"));
            for (Producto p : bajaRotacion) {
                contenido.append(filaTabla(p.getNombre(), String.valueOf(p.getStockActual())));
            }
            contenido.append("</table>");
        }

        contenido.append("<p style=\"color:#64748b;font-size:12px;margin-top:18px;\">Ingrese al sistema para ver el detalle completo y tomar acción.</p>");

        String html = plantillaHtml("Resumen diario de alertas de inventario", contenido.toString());
        for (String correo : correosAdmins) {
            enviarHtml(correo, "Resumen diario de alertas de inventario — Bodega TAMBITO", html);
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

    /**
     * Envía un correo en HTML (con la misma identidad visual granate del sistema), usado por
     * las notificaciones "cheveres" (resumen de alertas, solicitudes de desactivación). Nunca
     * lanza más allá de {@link MessagingException}, igual que el resto de la clase.
     */
    private void enviarHtml(String destinatario, String asunto, String cuerpoHtml) throws MessagingException {
        MimeMessage mensaje = new MimeMessage(sesion);
        mensaje.setFrom(new InternetAddress(remitente));
        mensaje.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensaje.setSubject(asunto);
        mensaje.setContent(cuerpoHtml, "text/html; charset=UTF-8");
        Transport.send(mensaje);
    }

    private static final String COLOR_ACENTO = "#C00000";

    /** Envoltorio HTML común (header con marca + pie) para todos los correos "cheveres" nuevos. */
    private String plantillaHtml(String tituloHeader, String contenidoInternoHtml) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f1f5f9;padding:24px 0;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"580\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);\">"
                + "<tr><td style=\"background:" + COLOR_ACENTO + ";padding:20px 28px;\">"
                + "<span style=\"color:#ffffff;font-size:18px;font-weight:bold;\">Bodega TAMBITO</span><br>"
                + "<span style=\"color:rgba(255,255,255,0.85);font-size:13px;\">" + tituloHeader + "</span>"
                + "</td></tr>"
                + "<tr><td style=\"padding:24px 28px;color:#1e293b;font-size:14px;line-height:1.55;\">"
                + contenidoInternoHtml
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 28px;background:#f8fafc;color:#94a3b8;font-size:11px;\">"
                + "Correo automático del sistema de gestión — Bodega TAMBITO. No responder a este mensaje."
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private String filaTabla(String... celdas) {
        StringBuilder fila = new StringBuilder("<tr>");
        for (String celda : celdas) {
            fila.append("<td style=\"padding:6px 8px;border-bottom:1px solid #f1f5f9;font-size:13px;\">")
                    .append(celda).append("</td>");
        }
        return fila.append("</tr>").toString();
    }

    private String encabezadoTabla(String... columnas) {
        StringBuilder fila = new StringBuilder(
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:10px 0 18px;border-collapse:collapse;\"><tr>");
        for (String col : columnas) {
            fila.append("<th style=\"text-align:left;padding:6px 8px;background:#fef2f2;color:")
                    .append(COLOR_ACENTO).append(";font-size:11px;text-transform:uppercase;\">").append(col).append("</th>");
        }
        return fila.append("</tr>").toString();
    }

    /**
     * Notifica a los administradores que un vendedor solicitó desactivar un elemento
     * (producto, proveedor, categoría o cliente) y necesita su aprobación.
     */
    public void notificarNuevaSolicitudDesactivacion(List<String> correosAdmins, String tipoEntidadLegible,
                                                       String entidadNombre, String solicitanteNombre, String motivo)
            throws MessagingException {
        String contenido = "<p>El usuario <strong>" + solicitanteNombre + "</strong> solicitó desactivar "
                + "el/la siguiente " + tipoEntidadLegible.toLowerCase() + ":</p>"
                + "<p style=\"background:#f8fafc;border-left:3px solid " + COLOR_ACENTO + ";padding:10px 14px;margin:12px 0;\">"
                + "<strong>" + entidadNombre + "</strong><br>"
                + "<span style=\"color:#64748b;font-size:12px;\">Motivo: " + (motivo == null || motivo.isBlank() ? "(sin motivo indicado)" : motivo) + "</span>"
                + "</p>"
                + "<p>Ingrese al sistema (módulo Solicitudes) para aprobar o rechazar esta solicitud.</p>";

        String html = plantillaHtml("Nueva solicitud de desactivación pendiente", contenido);
        for (String correo : correosAdmins) {
            enviarHtml(correo, "Solicitud pendiente: desactivar " + entidadNombre + " — Bodega TAMBITO", html);
        }
    }

    /**
     * Notifica al vendedor que solicitó una desactivación el resultado (aprobada/rechazada).
     */
    public void notificarResolucionSolicitudDesactivacion(String correoSolicitante, String tipoEntidadLegible,
                                                            String entidadNombre, boolean aprobada,
                                                            String resueltoPorNombre, String motivoRechazo)
            throws MessagingException {
        String colorEstado = aprobada ? "#059669" : "#dc2626";
        String textoEstado = aprobada ? "APROBADA" : "RECHAZADA";
        String contenido = "<p>Su solicitud de desactivar el/la " + tipoEntidadLegible.toLowerCase()
                + " <strong>" + entidadNombre + "</strong> fue:</p>"
                + "<p style=\"display:inline-block;background:" + colorEstado + ";color:#fff;padding:6px 14px;border-radius:999px;"
                + "font-size:13px;font-weight:bold;margin:6px 0 14px;\">" + textoEstado + "</p>"
                + "<p style=\"color:#64748b;font-size:13px;\">Resuelto por: " + resueltoPorNombre + "</p>"
                + (!aprobada && motivoRechazo != null && !motivoRechazo.isBlank()
                    ? "<p style=\"background:#fef2f2;border-left:3px solid #dc2626;padding:10px 14px;margin:12px 0;\">"
                      + "<strong>Motivo del rechazo:</strong> " + motivoRechazo + "</p>"
                    : "");

        String html = plantillaHtml("Resultado de su solicitud de desactivación", contenido);
        enviarHtml(correoSolicitante, "Solicitud " + textoEstado.toLowerCase() + ": " + entidadNombre + " — Bodega TAMBITO", html);
    }

    /**
     * Envía el código de 6 dígitos para el restablecimiento autoservicio de contraseña (ver
     * {@code PasswordResetService}). El código se muestra grande y espaciado para que sea
     * fácil de leer y copiar, e incluye el nombre de usuario (para que quien lo recibe
     * confirme que el correo corresponde a su propia cuenta) y el tiempo de validez.
     *
     * @param correoDestino      correo registrado del usuario
     * @param nombreUsuario      nombre de usuario de la cuenta (solo para mostrarlo en el correo)
     * @param codigo             código de 6 dígitos en texto plano (nunca se guarda así en la base de datos)
     * @param minutosValidez     minutos de validez del código, para mostrarlo en el cuerpo del correo
     * @throws MessagingException si falla el envío
     */
    public void enviarCodigoRecuperacionPassword(String correoDestino, String nombreUsuario, String codigo,
                                                  int minutosValidez) throws MessagingException {
        String contenido = "<p>Recibimos una solicitud para restablecer la contraseña de la cuenta "
                + "<strong>" + nombreUsuario + "</strong>. Use el siguiente código para continuar:</p>"
                + "<p style=\"text-align:center;margin:22px 0;\">"
                + "<span style=\"display:inline-block;background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;"
                + "padding:14px 22px;font-size:28px;font-weight:bold;letter-spacing:8px;color:" + COLOR_ACENTO + ";\">"
                + codigo + "</span></p>"
                + "<p style=\"color:#64748b;font-size:13px;\">Este código vence en " + minutosValidez + " minutos y solo "
                + "puede usarse una vez.</p>"
                + "<p style=\"color:#64748b;font-size:13px;\">Si usted no solicitó este cambio, puede ignorar este "
                + "correo — su contraseña actual seguirá funcionando con normalidad.</p>";

        String html = plantillaHtml("Código de verificación — restablecer contraseña", contenido);
        enviarHtml(correoDestino, "Código para restablecer su contraseña — Bodega TAMBITO", html);
    }
}
