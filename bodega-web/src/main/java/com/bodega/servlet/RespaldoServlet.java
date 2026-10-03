package com.bodega.servlet;

import com.bodega.config.BackupConfig;
import com.bodega.model.EstadoRespaldo;
import com.bodega.model.RegistroRespaldo;
import com.bodega.model.TipoRespaldo;
import com.bodega.service.AuditoriaService;
import com.bodega.service.BackupService;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Historial de Respaldos de Base de Datos y disparo manual. Ruta exclusiva de
 * ADMINISTRADOR (ver AuthorizationFilter: prefijo "/respaldos"). Los respaldos
 * automáticos diarios los ejecuta {@link com.bodega.scheduler.BackupScheduler}; este
 * Servlet solo lee la bitácora y permite forzar una ejecución adicional bajo demanda.
 */
@WebServlet("/respaldos")
public class RespaldoServlet extends HttpServlet {

    private final BackupService backupService = new BackupService();
    private final AuditoriaService auditoriaService = new AuditoriaService();

    /**
     * Muestra el historial de respaldos (últimos 100), más reciente primero.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String idDescarga = req.getParameter("descargar");
        if (idDescarga != null) {
            descargarArchivo(req, resp, idDescarga);
            return;
        }

        try {
            List<RegistroRespaldo> historial = backupService.listarRecientes();
            req.setAttribute("historial", historial);
            req.setAttribute("rutaPorDefecto", BackupConfig.getDirectorioDestino().toString());
            req.setAttribute("tituloPagina", "Respaldos de Base de Datos");
            req.setAttribute("moduloActivo", "respaldos");
            req.getRequestDispatcher("/WEB-INF/views/respaldos/lista.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el historial de respaldos.", e);
        }
    }

    /**
     * Permite descargar directamente desde el navegador el archivo .sql de un respaldo ya
     * ejecutado, sin que el administrador tenga que entrar por SSH al servidor a buscarlo.
     * Solo se permite para registros EXITOSO cuyo archivo todavía exista en disco (la
     * política de retención borra los más antiguos del directorio por defecto, aunque la
     * fila de bitácora se conserva igual — ver BackupService.aplicarPoliticaDeRetencion).
     *
     * @param idParametro id del registro de bitácora, como texto (viene de la URL)
     */
    private void descargarArchivo(HttpServletRequest req, HttpServletResponse resp, String idParametro)
            throws ServletException, IOException {
        int idAdminSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);

        int id;
        try {
            id = Integer.parseInt(idParametro);
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Identificador de respaldo inválido.");
            return;
        }

        try {
            Optional<RegistroRespaldo> registroOpt = backupService.buscarPorId(id);
            if (registroOpt.isEmpty() || registroOpt.get().getEstado() != EstadoRespaldo.EXITOSO) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Respaldo no encontrado.");
                return;
            }

            RegistroRespaldo registro = registroOpt.get();
            Path archivo = Path.of(registro.getRutaDestino());
            if (!Files.isRegularFile(archivo)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "El archivo de este respaldo ya no existe en el servidor (pudo haberse eliminado por la política de retención).");
                return;
            }

            auditoriaService.registrar(idAdminSesion, "RESPALDO_DESCARGADO", "BITACORA_RESPALDOS", registro.getId(),
                    "Se descargó el archivo del respaldo (" + archivo.getFileName() + ")", req.getRemoteAddr());

            resp.setContentType("application/sql");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + archivo.getFileName() + "\"");
            resp.setContentLengthLong(Files.size(archivo));
            Files.copy(archivo, resp.getOutputStream());
            resp.getOutputStream().flush();
        } catch (SQLException e) {
            throw new ServletException("Error al preparar la descarga del respaldo.", e);
        }
    }

    /**
     * Dispara un respaldo manual inmediato (bloqueante: mysqldump de esta base de datos
     * de prueba tarda menos de un segundo; en una base de producción grande esto debería
     * moverse a un job asíncrono con notificación, pero excede el alcance de esta entrega).
     *
     * @param req  petición HTTP; admite el parámetro opcional {@code rutaDestino} con una
     *             carpeta del servidor distinta a la configurada por defecto (ej. un disco
     *             externo montado), solo para este respaldo puntual
     * @param resp respuesta HTTP
     * @throws IOException si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int idAdminSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String rutaDestino = req.getParameter("rutaDestino");

        RegistroRespaldo resultado = backupService.ejecutarRespaldo(TipoRespaldo.MANUAL, idAdminSesion, rutaDestino);

        auditoriaService.registrar(idAdminSesion, "RESPALDO_MANUAL", "BITACORA_RESPALDOS", resultado.getId(),
                "Respaldo manual " + resultado.getEstado() + " (" + resultado.getRutaDestino() + ")",
                req.getRemoteAddr());

        req.getSession().setAttribute(
                resultado.getEstado().name().equals("EXITOSO") ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                resultado.getEstado().name().equals("EXITOSO")
                        ? "Respaldo generado correctamente (" + resultado.getTamanoBytes() + " bytes)."
                        : "El respaldo falló: " + resultado.getMensajeError());

        resp.sendRedirect(req.getContextPath() + "/respaldos");
    }
}
