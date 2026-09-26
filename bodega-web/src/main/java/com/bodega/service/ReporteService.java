package com.bodega.service;

import com.bodega.dao.ProductoDAO;
import com.bodega.dao.VentaDAO;
import com.bodega.model.EstadoVenta;
import com.bodega.model.Producto;
import com.bodega.model.Venta;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genera los reportes gerenciales del sistema en formato PDF usando OpenPDF.
 * Todo el contenido de los reportes está en español, incluyendo etiquetas,
 * encabezados de tabla y mensajes de "sin datos".
 */
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Font FUENTE_TITULO = new Font(Font.HELVETICA, 16, Font.BOLD);
    private static final Font FUENTE_SUBTITULO = new Font(Font.HELVETICA, 10, Font.NORMAL, java.awt.Color.GRAY);
    private static final Font FUENTE_ENCABEZADO_TABLA = new Font(Font.HELVETICA, 9, Font.BOLD, java.awt.Color.WHITE);
    private static final Font FUENTE_CELDA = new Font(Font.HELVETICA, 9, Font.NORMAL);

    private final VentaDAO ventaDAO;
    private final ProductoDAO productoDAO;

    public ReporteService() {
        this.ventaDAO = new VentaDAO();
        this.productoDAO = new ProductoDAO();
    }

    /**
     * Genera el PDF del Reporte Gerencial de Ventas: detalle de comprobantes en el rango
     * de fechas indicado, con los totales facturado y anulado al pie.
     *
     * @param desde fecha inicial del rango, inclusive
     * @param hasta fecha final del rango, inclusive
     * @return el PDF generado como arreglo de bytes, listo para enviar en la respuesta HTTP
     * @throws SQLException si falla la consulta de ventas
     */
    public byte[] generarReporteVentas(LocalDate desde, LocalDate hasta) throws SQLException {
        List<Venta> ventas = ventaDAO.listar(null, null, null, null).stream()
                .filter(v -> !v.getFechaCreacion().toLocalDate().isBefore(desde)
                        && !v.getFechaCreacion().toLocalDate().isAfter(hasta))
                .toList();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Reporte Gerencial de Ventas", FUENTE_TITULO));
            documento.add(new Paragraph("Del " + desde + " al " + hasta
                    + " · Generado el " + LocalDateTime.now().format(FORMATO_FECHA), FUENTE_SUBTITULO));
            documento.add(Chunk.NEWLINE);

            PdfPTable tabla = new PdfPTable(new float[]{2.2f, 2f, 2.5f, 1.5f, 1.5f, 1.5f});
            tabla.setWidthPercentage(100);

            agregarEncabezado(tabla, "Comprobante", "Fecha", "Vendedor", "Método pago", "Estado", "Total (S/)");

            BigDecimal totalFacturado = BigDecimal.ZERO;
            BigDecimal totalAnulado = BigDecimal.ZERO;

            for (Venta v : ventas) {
                agregarCelda(tabla, v.getNumeroComprobante());
                agregarCelda(tabla, v.getFechaCreacion().format(FORMATO_FECHA));
                agregarCelda(tabla, v.getUsuarioNombre());
                agregarCelda(tabla, v.getMetodoPago().name());
                agregarCelda(tabla, v.getEstado() == EstadoVenta.COMPLETADA ? "Completada" : "Anulada");
                agregarCelda(tabla, v.getTotal().toString());

                if (v.getEstado() == EstadoVenta.COMPLETADA) {
                    totalFacturado = totalFacturado.add(v.getTotal());
                } else {
                    totalAnulado = totalAnulado.add(v.getTotal());
                }
            }

            if (ventas.isEmpty()) {
                PdfPCell celdaVacia = new PdfPCell(new Paragraph("No se registraron ventas en el rango de fechas seleccionado.", FUENTE_CELDA));
                celdaVacia.setColspan(6);
                celdaVacia.setPadding(8);
                tabla.addCell(celdaVacia);
            }

            documento.add(tabla);
            documento.add(Chunk.NEWLINE);

            documento.add(new Paragraph("Total facturado (ventas completadas): S/ " + totalFacturado, FUENTE_ENCABEZADO_TABLA_NEGRO()));
            documento.add(new Paragraph("Total anulado: S/ " + totalAnulado, FUENTE_CELDA));
            documento.add(new Paragraph("Cantidad de comprobantes: " + ventas.size(), FUENTE_CELDA));

        } finally {
            documento.close();
        }

        return salida.toByteArray();
    }

    /**
     * Genera el PDF del Reporte de Productos con Stock Bajo, útil para planificar reposición.
     *
     * @return el PDF generado como arreglo de bytes, listo para enviar en la respuesta HTTP
     * @throws SQLException si falla la consulta de productos
     */
    public byte[] generarReporteStockBajo() throws SQLException {
        List<Producto> productos = productoDAO.listarConStockBajoMinimo();

        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 54, 36);

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            documento.add(new Paragraph("Reporte de Productos con Stock Bajo", FUENTE_TITULO));
            documento.add(new Paragraph("Generado el " + LocalDateTime.now().format(FORMATO_FECHA), FUENTE_SUBTITULO));
            documento.add(Chunk.NEWLINE);

            PdfPTable tabla = new PdfPTable(new float[]{1.2f, 3f, 1.2f, 1.2f, 1.5f});
            tabla.setWidthPercentage(100);
            agregarEncabezado(tabla, "SKU", "Producto", "Stock actual", "Stock mínimo", "Diferencia");

            for (Producto p : productos) {
                agregarCelda(tabla, p.getSku());
                agregarCelda(tabla, p.getNombre());
                agregarCelda(tabla, String.valueOf(p.getStockActual()));
                agregarCelda(tabla, String.valueOf(p.getStockMinimo()));
                agregarCelda(tabla, String.valueOf(p.getStockActual() - p.getStockMinimo()));
            }

            if (productos.isEmpty()) {
                PdfPCell celdaVacia = new PdfPCell(new Paragraph("No hay productos con stock bajo el mínimo en este momento.", FUENTE_CELDA));
                celdaVacia.setColspan(5);
                celdaVacia.setPadding(8);
                tabla.addCell(celdaVacia);
            }

            documento.add(tabla);
        } finally {
            documento.close();
        }

        return salida.toByteArray();
    }

    private void agregarEncabezado(PdfPTable tabla, String... columnas) {
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Paragraph(columna, FUENTE_ENCABEZADO_TABLA));
            celda.setBackgroundColor(new java.awt.Color(30, 41, 59));
            celda.setPadding(6);
            tabla.addCell(celda);
        }
    }

    private void agregarCelda(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Paragraph(texto != null ? texto : "-", FUENTE_CELDA));
        celda.setPadding(5);
        tabla.addCell(celda);
    }

    private Font FUENTE_ENCABEZADO_TABLA_NEGRO() {
        return new Font(Font.HELVETICA, 11, Font.BOLD);
    }
}
