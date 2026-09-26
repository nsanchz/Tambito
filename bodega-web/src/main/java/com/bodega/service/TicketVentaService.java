package com.bodega.service;

import com.bodega.dao.ClienteDAO;
import com.bodega.model.Cliente;
import com.bodega.model.DetalleVenta;
import com.bodega.model.TipoComprobante;
import com.bodega.model.Venta;
import com.bodega.util.QrCodeUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Genera el comprobante de venta (ticket) individual en PDF, con el formato angosto
 * típico de una impresora térmica de 80mm, a partir de una venta ya registrada.
 * Se usa desde el Historial de Ventas para descargar/imprimir el comprobante de
 * cualquier venta puntual (a diferencia de ReporteService, que genera reportes
 * gerenciales agregados de muchas ventas).
 */
public class TicketVentaService {

    private static final float ANCHO_TICKET_80MM = 226.77f;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Font FUENTE_EMPRESA = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font FUENTE_NORMAL = new Font(Font.HELVETICA, 8, Font.NORMAL);
    private static final Font FUENTE_NEGRITA = new Font(Font.HELVETICA, 8, Font.BOLD);
    private static final Font FUENTE_TOTAL = new Font(Font.HELVETICA, 11, Font.BOLD);

    private final VentaService ventaService;
    private final ConfiguracionService configuracionService;
    private final ClienteDAO clienteDAO;

    public TicketVentaService() {
        this.ventaService = new VentaService();
        this.configuracionService = new ConfiguracionService();
        this.clienteDAO = new ClienteDAO();
    }

    /**
     * Genera el ticket PDF de una venta, con los datos de la empresa tomados de
     * Configuración del Sistema para que un cambio ahí se refleje en el próximo ticket impreso.
     *
     * @param ventaId id de la venta a imprimir
     * @return el PDF generado como arreglo de bytes, listo para enviar en la respuesta HTTP
     * @throws SQLException             si falla la consulta de la venta o de la configuración
     * @throws NoSuchElementException si la venta no existe
     */
    public byte[] generarTicket(int ventaId) throws SQLException {
        Venta venta = ventaService.buscarPorId(ventaId)
                .orElseThrow(() -> new NoSuchElementException("La venta solicitada no existe."));

        var config = configuracionService.obtenerTodos();
        String nombreEmpresa = config.getOrDefault(ConfiguracionService.CLAVE_NOMBRE_EMPRESA, "BodegaControl");
        String rucEmpresa = config.getOrDefault(ConfiguracionService.CLAVE_RUC_EMPRESA, "");
        String moneda = config.getOrDefault(ConfiguracionService.CLAVE_MONEDA, "S/");

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(new Rectangle(ANCHO_TICKET_80MM, 700f), 10, 10, 10, 10);

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            agregarCentrado(documento, nombreEmpresa, FUENTE_EMPRESA);
            if (!rucEmpresa.isBlank()) {
                agregarCentrado(documento, "RUC: " + rucEmpresa, FUENTE_NORMAL);
            }
            agregarCentrado(documento, venta.getTipoComprobante() + " ELECTRÓNICA", FUENTE_NEGRITA);
            agregarCentrado(documento, venta.getNumeroComprobante(), FUENTE_NEGRITA);
            documento.add(new Paragraph(" "));

            documento.add(new Paragraph("Fecha: " + venta.getFechaCreacion().format(FORMATO_FECHA), FUENTE_NORMAL));
            documento.add(new Paragraph("Cliente: " + venta.getClienteNombre(), FUENTE_NORMAL));
            documento.add(new Paragraph("Atendido por: " + venta.getUsuarioNombre(), FUENTE_NORMAL));
            documento.add(new Paragraph("Método de pago: " + venta.getMetodoPago(), FUENTE_NORMAL));
            documento.add(new Paragraph("--------------------------------", FUENTE_NORMAL));

            PdfPTable tabla = new PdfPTable(new float[]{3.5f, 1f, 1.5f});
            tabla.setWidthPercentage(100);
            for (DetalleVenta d : venta.getDetalles()) {
                tabla.addCell(celdaSinBorde(d.getProductoNombre(), FUENTE_NORMAL, Element.ALIGN_LEFT));
                tabla.addCell(celdaSinBorde("x" + d.getCantidad(), FUENTE_NORMAL, Element.ALIGN_CENTER));
                tabla.addCell(celdaSinBorde(moneda + " " + d.getSubtotal(), FUENTE_NORMAL, Element.ALIGN_RIGHT));
            }
            documento.add(tabla);

            documento.add(new Paragraph("--------------------------------", FUENTE_NORMAL));
            agregarLineaTotal(documento, "Subtotal:", moneda + " " + venta.getSubtotalImponible());
            agregarLineaTotal(documento, "Descuento:", moneda + " " + venta.getDescuento());
            agregarLineaTotal(documento, "IGV:", moneda + " " + venta.getIgv());
            documento.add(new Paragraph(" "));

            Paragraph total = new Paragraph("TOTAL: " + moneda + " " + venta.getTotal(), FUENTE_TOTAL);
            total.setAlignment(Element.ALIGN_RIGHT);
            documento.add(total);

            documento.add(new Paragraph(" "));
            if (venta.getEstado() == com.bodega.model.EstadoVenta.ANULADA) {
                Paragraph anulada = new Paragraph("*** COMPROBANTE ANULADO ***", FUENTE_NEGRITA);
                anulada.setAlignment(Element.ALIGN_CENTER);
                documento.add(anulada);
            }

            agregarCodigoQr(documento, venta, rucEmpresa);

            agregarCentrado(documento, "¡Gracias por su compra!", FUENTE_NORMAL);

        } finally {
            documento.close();
        }

        return salida.toByteArray();
    }

    /**
     * Agrega el código QR representativo del comprobante (formato estándar de comprobante
     * electrónico peruano: RUC, tipo, serie-correlativo, IGV, total, fecha y documento del
     * cliente), igual al que trae impreso cualquier boleta o factura electrónica real.
     *
     * @param documento  documento PDF en construcción
     * @param venta      venta ya registrada (boleta o factura, cualquier método de pago)
     * @param rucEmpresa RUC del emisor configurado en Configuración del Sistema
     * @throws SQLException                      si falla la consulta del cliente asociado
     * @throws com.lowagie.text.DocumentException si falla la inserción de la imagen en el PDF
     */
    private void agregarCodigoQr(Document documento, Venta venta, String rucEmpresa) throws SQLException, com.lowagie.text.DocumentException {
        String[] partesComprobante = venta.getNumeroComprobante().split("-");
        String serie = partesComprobante[0];
        String correlativo = partesComprobante.length > 1 ? partesComprobante[1] : "";
        String codigoTipoComprobante = venta.getTipoComprobante() == TipoComprobante.FACTURA ? "01" : "03";

        String tipoDocCliente = "-";
        String numDocCliente = "-";
        if (venta.getClienteId() != null) {
            Optional<Cliente> clienteOpt = clienteDAO.buscarPorId(venta.getClienteId());
            if (clienteOpt.isPresent()) {
                tipoDocCliente = clienteOpt.get().getTipoDocumento() == com.bodega.model.TipoDocumentoCliente.RUC ? "6" : "1";
                numDocCliente = clienteOpt.get().getNumeroDocumento();
            }
        }

        String contenidoQr = String.join("|",
                rucEmpresa, codigoTipoComprobante, serie, correlativo,
                venta.getIgv().toString(), venta.getTotal().toString(),
                venta.getFechaCreacion().toLocalDate().toString(), tipoDocCliente, numDocCliente) + "|";

        try {
            byte[] qrPng = QrCodeUtil.generarPng(contenidoQr, 200);
            Image imagenQr = Image.getInstance(qrPng);
            imagenQr.scaleToFit(90, 90);
            imagenQr.setAlignment(Element.ALIGN_CENTER);
            documento.add(imagenQr);
            agregarCentrado(documento, "Representación impresa del comprobante electrónico.", FUENTE_NORMAL);
        } catch (com.google.zxing.WriterException | IOException e) {
            throw new SQLException("Error al generar el código QR del comprobante.", e);
        }
    }

    private void agregarCentrado(Document documento, String texto, Font fuente) throws com.lowagie.text.DocumentException {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        documento.add(parrafo);
    }

    private void agregarLineaTotal(Document documento, String etiqueta, String valor) throws com.lowagie.text.DocumentException {
        PdfPTable fila = new PdfPTable(new float[]{2f, 1.5f});
        fila.setWidthPercentage(100);
        fila.addCell(celdaSinBorde(etiqueta, FUENTE_NORMAL, Element.ALIGN_LEFT));
        fila.addCell(celdaSinBorde(valor, FUENTE_NORMAL, Element.ALIGN_RIGHT));
        documento.add(fila);
    }

    private PdfPCell celdaSinBorde(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto, fuente));
        celda.setBorder(Rectangle.NO_BORDER);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(2);
        return celda;
    }
}
