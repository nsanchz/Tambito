package com.bodega.servlet;

import com.bodega.scheduler.AlertaEmailScheduler;
import com.bodega.service.AuditoriaService;
import com.bodega.service.ConfiguracionService;
import com.bodega.util.ArchivoImagenUtil;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/** Configuración del Sistema: parámetros generales y logo de la tienda, editables solo por ADMINISTRADOR. */
@WebServlet("/configuracion")
// Límite de tamaño de archivo: además de la whitelist y la verificación de bytes mágicos
// (ver ArchivoImagenUtil), esto evita que una subida enorme agote memoria/disco (DoS). El
// límite se fija un poco por encima de TAMANO_MAXIMO_BYTES para que sea el código, no el
// contenedor, quien dé el mensaje de error legible al usuario.
@MultipartConfig(maxFileSize = 3L * 1024 * 1024, maxRequestSize = 4L * 1024 * 1024)
public class ConfiguracionServlet extends HttpServlet {

    private final ConfiguracionService configuracionService = new ConfiguracionService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    /**
     * Muestra el formulario con todos los parámetros configurables y sus valores actuales.
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
            req.setAttribute("valores", configuracionService.obtenerTodos());
            req.setAttribute("parametros", ConfiguracionService.PARAMETROS);
            req.setAttribute("tituloPagina", "Configuración del Sistema");
            req.setAttribute("moduloActivo", "configuracion");
            req.getRequestDispatcher("/WEB-INF/views/configuracion/parametros.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al cargar los parámetros de configuración.", e);
        }
    }

    /**
     * Guarda los nuevos valores de todos los parámetros enviados desde el formulario
     * y registra el cambio en auditoría.
     *
     * @param req  petición HTTP con un parámetro por cada clave de {@link ConfiguracionService#PARAMETROS}
     * @param resp respuesta HTTP
     * @throws ServletException si falla la actualización en la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int idAdminSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        // Acción independiente del formulario de parámetros: envía el resumen de alertas de
        // inventario (stock bajo/vencimientos/baja rotación) en este momento, a solicitud del
        // administrador, sin esperar a la hora programada (ver AlertaEmailScheduler).
        if ("enviarResumenAlertas".equals(req.getParameter("accion"))) {
            String resultado = AlertaEmailScheduler.enviarAhora();
            auditoriaService.registrar(idAdminSesion, "ALERTAS_RESUMEN_MANUAL", "CONFIGURACION", null,
                    resultado, req.getRemoteAddr());
            req.getSession().setAttribute(Constantes.ATTR_MENSAJE, resultado);
            resp.sendRedirect(req.getContextPath() + "/configuracion");
            return;
        }

        try {
            Map<String, String> nuevosValores = new HashMap<>();
            for (var def : ConfiguracionService.PARAMETROS) {
                String valor = req.getParameter(def.clave());
                if (valor != null) {
                    nuevosValores.put(def.clave(), valor);
                }
            }

            configuracionService.actualizarTodos(nuevosValores);

            // El logo es opcional: si el admin no seleccionó un archivo nuevo, se conserva el actual.
            // Nunca se confía únicamente en el Content-Type que declara el navegador ni en el
            // nombre del archivo: se valida contra una whitelist y se verifican los bytes
            // reales del archivo (ver ArchivoImagenUtil) antes de guardar nada.
            Part logoPart = req.getPart("logo");
            if (logoPart != null && logoPart.getSize() > 0) {
                String tipoDeclarado = logoPart.getContentType();
                if (!ArchivoImagenUtil.esTipoPermitido(tipoDeclarado)) {
                    req.getSession().setAttribute(Constantes.ATTR_ERROR,
                            "El logo debe ser una imagen PNG, JPEG o WEBP.");
                    resp.sendRedirect(req.getContextPath() + "/configuracion");
                    return;
                }

                byte[] contenido = logoPart.getInputStream().readAllBytes();
                if (contenido.length > ArchivoImagenUtil.TAMANO_MAXIMO_BYTES) {
                    req.getSession().setAttribute(Constantes.ATTR_ERROR,
                            "El logo no puede superar los 2 MB.");
                    resp.sendRedirect(req.getContextPath() + "/configuracion");
                    return;
                }
                if (!ArchivoImagenUtil.coincideConFirmaBinaria(contenido, tipoDeclarado)) {
                    req.getSession().setAttribute(Constantes.ATTR_ERROR,
                            "El archivo no es una imagen válida (el contenido no coincide con su tipo declarado).");
                    resp.sendRedirect(req.getContextPath() + "/configuracion");
                    return;
                }

                configuracionService.guardarLogo(contenido, tipoDeclarado, logoPart.getSubmittedFileName());
                auditoriaService.registrar(idAdminSesion, "LOGO_ACTUALIZADO", "CONFIGURACION", null,
                        "Se actualizó el logo de la tienda.", req.getRemoteAddr());
            }

            auditoriaService.registrar(idAdminSesion, "CONFIGURACION_ACTUALIZADA", "CONFIGURACION", null,
                    "Se actualizaron los parámetros generales del sistema.", req.getRemoteAddr());

            req.getSession().setAttribute(Constantes.ATTR_MENSAJE, "Configuración actualizada correctamente.");
        } catch (SQLException e) {
            throw new ServletException("Error al actualizar la configuración.", e);
        } catch (IllegalStateException e) {
            // Lanzada por el contenedor si la subida excede @MultipartConfig(maxFileSize/maxRequestSize).
            req.getSession().setAttribute(Constantes.ATTR_ERROR, "El archivo del logo es demasiado grande (máximo 2 MB).");
            resp.sendRedirect(req.getContextPath() + "/configuracion");
            return;
        }

        resp.sendRedirect(req.getContextPath() + "/configuracion");
    }
}
