package com.bodega.servlet;

import com.bodega.service.DashboardService;
import com.bodega.service.ReporteService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Reportes Gerenciales y Análisis Financiero. Ruta exclusiva de ADMINISTRADOR
 * (ver AuthorizationFilter: prefijo "/reportes"). Los reportes descargables se
 * generan como PDF real con OpenPDF, no como una simulación en pantalla.
 */
@WebServlet("/reportes")
public class ReporteServlet extends HttpServlet {

    private final ReporteService reporteService = new ReporteService();
    private final DashboardService dashboardService = new DashboardService();

    /**
     * Según el parámetro {@code tipo}, descarga un reporte PDF ({@code ventas-pdf} con
     * {@code desde}/{@code hasta}, o {@code stock-bajo-pdf}) o muestra el panel con KPIs
     * y los formularios de descarga.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la generación del reporte o el cálculo de KPIs
     * @throws IOException      si falla el envío del PDF o el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String tipo = req.getParameter("tipo");

        try {
            if ("ventas-pdf".equals(tipo)) {
                LocalDate desde = LocalDate.parse(req.getParameter("desde"));
                LocalDate hasta = LocalDate.parse(req.getParameter("hasta"));
                enviarPdf(resp, reporteService.generarReporteVentas(desde, hasta), "reporte-ventas.pdf");
                return;
            }

            if ("stock-bajo-pdf".equals(tipo)) {
                enviarPdf(resp, reporteService.generarReporteStockBajo(), "reporte-stock-bajo.pdf");
                return;
            }

            req.setAttribute("resumen", dashboardService.obtenerResumenAdministrador());
            req.setAttribute("hoy", LocalDate.now().toString());
            req.setAttribute("hace7dias", LocalDate.now().minusDays(7).toString());
            req.setAttribute("tituloPagina", "Reportes Gerenciales y Análisis Financiero");
            req.setAttribute("moduloActivo", "reportes");
            req.getRequestDispatcher("/WEB-INF/views/reportes/panel.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al generar el reporte solicitado.", e);
        }
    }

    /**
     * @param resp          respuesta HTTP donde se escribe el PDF
     * @param contenidoPdf  bytes del PDF ya generado
     * @param nombreArchivo nombre sugerido del archivo (usado en {@code Content-Disposition})
     * @throws IOException si falla la escritura en el stream de salida
     */
    private void enviarPdf(HttpServletResponse resp, byte[] contenidoPdf, String nombreArchivo) throws IOException {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "inline; filename=\"" + nombreArchivo + "\"");
        resp.setContentLength(contenidoPdf.length);
        resp.getOutputStream().write(contenidoPdf);
        resp.getOutputStream().flush();
    }
}
