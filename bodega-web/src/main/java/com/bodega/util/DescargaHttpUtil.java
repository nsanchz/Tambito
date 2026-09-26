package com.bodega.util;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Envío de archivos generados (PDF, Excel) como descarga HTTP, reutilizado por varios Servlets. */
public final class DescargaHttpUtil {

    private DescargaHttpUtil() {
        // Clase utilitaria: no instanciable
    }

    /**
     * @param resp          respuesta HTTP donde se escribe el archivo
     * @param contenidoXlsx bytes del archivo .xlsx ya generado
     * @param nombreArchivo nombre sugerido del archivo (usado en {@code Content-Disposition})
     * @throws IOException si falla la escritura en el stream de salida
     */
    public static void enviarExcel(HttpServletResponse resp, byte[] contenidoXlsx, String nombreArchivo) throws IOException {
        resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + nombreArchivo + "\"");
        resp.setContentLength(contenidoXlsx.length);
        resp.getOutputStream().write(contenidoXlsx);
        resp.getOutputStream().flush();
    }
}
