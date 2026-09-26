package com.bodega.servlet;

import com.bodega.model.ArchivoBinario;
import com.bodega.service.ConfiguracionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Sirve archivos binarios de configuración almacenados en base de datos (ej. el logo de
 * la tienda). Ruta pública: debe poder mostrarse en el login antes de autenticarse
 * (ver AuthenticationFilter, que incluye "/imagen" entre las rutas públicas).
 */
@WebServlet("/imagen")
public class ImagenServlet extends HttpServlet {

    private final ConfiguracionService configuracionService = new ConfiguracionService();

    /**
     * @param req  petición HTTP; espera {@code ?tipo=logo}
     * @param resp respuesta HTTP con el binario y su Content-Type original, o 404 si no existe
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String tipo = req.getParameter("tipo");

        try {
            if (!"logo".equals(tipo)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Optional<ArchivoBinario> archivoOpt = configuracionService.obtenerLogo();
            if (archivoOpt.isEmpty()) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            ArchivoBinario archivo = archivoOpt.get();
            resp.setContentType(archivo.getContentType());
            resp.setHeader("Cache-Control", "private, max-age=300");
            resp.setContentLength(archivo.getContenido().length);
            resp.getOutputStream().write(archivo.getContenido());
            resp.getOutputStream().flush();
        } catch (SQLException e) {
            throw new ServletException("Error al consultar la imagen solicitada.", e);
        }
    }
}
