package com.bodega.service;

import com.bodega.config.BackupConfig;
import com.bodega.dao.BackupDAO;
import com.bodega.model.EstadoRespaldo;
import com.bodega.model.RegistroRespaldo;
import com.bodega.model.TipoRespaldo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Orquesta la ejecución de un respaldo completo de la base de datos invocando el binario
 * {@code mysqldump} (ver {@link BackupConfig}), aplica la política de retención en disco
 * y registra el resultado en la bitácora ({@code bitacora_respaldos}). Se usa tanto desde
 * el scheduler automático diario (com.bodega.scheduler.BackupScheduler) como desde el
 * disparo manual del panel de administración (ver RespaldoServlet).
 */
public class BackupService {

    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int LARGO_MAX_MENSAJE_ERROR = 500;

    private final BackupDAO backupDAO;

    public BackupService() {
        this.backupDAO = new BackupDAO();
    }

    /**
     * Ejecuta un respaldo completo de la base de datos en la carpeta de destino
     * configurada por defecto (database.properties -> db.backup.dir). Usado por el
     * scheduler automático diario.
     *
     * @param tipo      AUTOMATICO (scheduler) o MANUAL (disparado desde el panel)
     * @param usuarioId id del administrador que lo disparó manualmente, o {@code null} si es automático
     * @return el registro de bitácora ya persistido, con su resultado
     */
    public RegistroRespaldo ejecutarRespaldo(TipoRespaldo tipo, Integer usuarioId) {
        return ejecutarRespaldo(tipo, usuarioId, null);
    }

    /**
     * Ejecuta un respaldo completo de la base de datos y registra el resultado en la
     * bitácora, tanto si fue exitoso como si falló (nunca lanza excepción hacia el
     * llamador: un fallo de respaldo no debe tumbar el hilo del scheduler ni la petición
     * HTTP que lo disparó manualmente).
     *
     * @param tipo                    AUTOMATICO (scheduler) o MANUAL (disparado desde el panel)
     * @param usuarioId               id del administrador que lo disparó manualmente, o {@code null} si es automático
     * @param rutaDestinoPersonalizada carpeta del servidor donde guardar este respaldo puntual
     *                                 (ej. un disco externo montado, una carpeta compartida), o
     *                                 {@code null}/vacío para usar la carpeta por defecto configurada.
     *                                 Solo tiene sentido para respaldos MANUAL: el scheduler automático
     *                                 siempre usa la carpeta por defecto. La política de retención en
     *                                 disco tampoco aplica sobre una ruta personalizada (es un destino
     *                                 elegido puntualmente por el administrador, no el histórico
     *                                 gestionado automáticamente).
     * @return el registro de bitácora ya persistido, con su resultado
     */
    public RegistroRespaldo ejecutarRespaldo(TipoRespaldo tipo, Integer usuarioId, String rutaDestinoPersonalizada) {
        LocalDateTime inicio = LocalDateTime.now();
        boolean usaDirectorioPersonalizado = rutaDestinoPersonalizada != null && !rutaDestinoPersonalizada.isBlank();

        RegistroRespaldo registro = new RegistroRespaldo();
        registro.setTipo(tipo);
        registro.setFechaInicio(inicio);
        registro.setUsuarioId(usuarioId);

        Path directorio;
        try {
            directorio = usaDirectorioPersonalizado
                    ? resolverDirectorioPersonalizado(rutaDestinoPersonalizada)
                    : BackupConfig.getDirectorioDestino();
        } catch (Exception e) {
            registro.setEstado(EstadoRespaldo.FALLIDO);
            registro.setRutaDestino(rutaDestinoPersonalizada);
            registro.setMensajeError("Ruta de destino inválida: " + e.getMessage());
            registro.setFechaFin(LocalDateTime.now());
            persistirEnBitacora(registro);
            return registro;
        }

        Path destino = directorio.resolve("bodega_db_" + inicio.format(FORMATO_ARCHIVO) + ".sql");
        registro.setRutaDestino(destino.toString());

        try {
            ejecutarMysqldump(destino);
            registro.setTamanoBytes(Files.size(destino));
            registro.setEstado(EstadoRespaldo.EXITOSO);
            if (!usaDirectorioPersonalizado) {
                aplicarPoliticaDeRetencion();
            }
        } catch (Exception e) {
            registro.setEstado(EstadoRespaldo.FALLIDO);
            String mensaje = e.getMessage() != null ? e.getMessage() : e.toString();
            registro.setMensajeError(mensaje.length() > LARGO_MAX_MENSAJE_ERROR
                    ? mensaje.substring(0, LARGO_MAX_MENSAJE_ERROR) : mensaje);
        } finally {
            registro.setFechaFin(LocalDateTime.now());
        }

        persistirEnBitacora(registro);
        return registro;
    }

    /**
     * @param ruta ruta de carpeta ingresada por el administrador (se crea si no existe)
     * @return la ruta resuelta, verificada como escribible por el proceso de la aplicación
     * @throws IOException si la ruta no se puede crear o el proceso no tiene permiso de escritura
     */
    private Path resolverDirectorioPersonalizado(String ruta) throws IOException {
        Path directorio = Path.of(ruta.trim());
        Files.createDirectories(directorio);
        if (!Files.isWritable(directorio)) {
            throw new IOException("El servidor no tiene permiso de escritura en \"" + directorio + "\".");
        }
        return directorio;
    }

    private void persistirEnBitacora(RegistroRespaldo registro) {
        try {
            registro.setId(backupDAO.registrar(registro));
        } catch (SQLException e) {
            // No se relanza: perder la fila de bitácora no debe ocultar que el archivo de
            // respaldo sí se generó (o el motivo del fallo); se deja constancia en consola.
            System.err.println("No se pudo registrar el respaldo en la bitácora: " + e.getMessage());
        }
    }

    /** @return los últimos respaldos registrados, más reciente primero */
    public List<RegistroRespaldo> listarRecientes() throws SQLException {
        return backupDAO.listarRecientes();
    }

    /**
     * @param id id del registro de bitácora
     * @return el registro si existe, usado para descargar su archivo (ver RespaldoServlet)
     * @throws SQLException si falla la consulta
     */
    public java.util.Optional<RegistroRespaldo> buscarPorId(int id) throws SQLException {
        return backupDAO.buscarPorId(id);
    }

    /**
     * Invoca {@code mysqldump} con las credenciales del usuario dedicado de respaldo
     * (bodega_backup), pasadas mediante un archivo de opciones temporal en vez del
     * parámetro {@code -p<password>} en la línea de comandos, para que la contraseña
     * nunca sea visible en la lista de procesos del sistema operativo (ej. {@code ps aux}).
     *
     * @param destino ruta del archivo .sql de salida
     * @throws IOException          si falla la escritura del archivo de opciones o del volcado
     * @throws InterruptedException si el hilo es interrumpido mientras espera a mysqldump
     */
    private void ejecutarMysqldump(Path destino) throws IOException, InterruptedException {
        Path archivoOpciones = crearArchivoOpcionesTemporal();
        try {
            String[] hostPuerto = BackupConfig.getHostYPuerto();
            List<String> comando = List.of(
                    BackupConfig.getMysqldumpPath(),
                    "--defaults-extra-file=" + archivoOpciones,
                    "-h", hostPuerto[0],
                    "-P", hostPuerto[1],
                    "--single-transaction", // snapshot consistente sin bloquear escrituras (InnoDB)
                    "--routines",
                    "--triggers",
                    "--events",
                    BackupConfig.getNombreBaseDatos()
            );

            ProcessBuilder pb = new ProcessBuilder(comando);
            pb.redirectOutput(destino.toFile());
            Process proceso = pb.start();

            String salidaError;
            try (var errorStream = proceso.getErrorStream()) {
                salidaError = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            int codigoSalida = proceso.waitFor();

            if (codigoSalida != 0) {
                Files.deleteIfExists(destino);
                throw new IOException("mysqldump terminó con código " + codigoSalida
                        + (salidaError.isBlank() ? "" : ": " + salidaError.trim()));
            }
        } finally {
            Files.deleteIfExists(archivoOpciones);
        }
    }

    /**
     * @return la ruta de un archivo de opciones temporal (formato {@code my.cnf}) con
     *         permisos restringidos al usuario del proceso (equivalente a {@code chmod 600}),
     *         que el llamador debe borrar apenas termine de usarlo
     */
    private Path crearArchivoOpcionesTemporal() throws IOException {
        Path archivo;
        if (esPosix()) {
            Set<PosixFilePermission> permisos = PosixFilePermissions.fromString("rw-------");
            archivo = Files.createTempFile("bodega_backup_", ".cnf",
                    PosixFilePermissions.asFileAttribute(permisos));
        } else {
            archivo = Files.createTempFile("bodega_backup_", ".cnf");
        }
        // El valor de password va entre comillas dobles: en los archivos de opciones de
        // MySQL, "#" inicia un comentario (y trunca todo lo que sigue) si no se cita, lo
        // que corrompería silenciosamente cualquier contraseña que contenga ese carácter.
        String contenido = "[client]\n"
                + "user=" + BackupConfig.getUsername() + "\n"
                + "password=\"" + BackupConfig.getPassword() + "\"\n";
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
        return archivo;
    }

    private boolean esPosix() {
        return "/".equals(java.io.File.separator);
    }

    /**
     * Elimina del disco los archivos .sql de respaldo más antiguos que la retención
     * configurada (db.backup.retencion.dias). Las filas correspondientes en la bitácora
     * NO se eliminan: se conserva el historial completo aunque el archivo físico ya no exista.
     */
    private void aplicarPoliticaDeRetencion() throws IOException {
        Instant limite = Instant.now().minus(BackupConfig.getRetencionDias(), ChronoUnit.DAYS);
        try (var archivos = Files.list(BackupConfig.getDirectorioDestino())) {
            for (Path archivo : archivos.filter(p -> p.toString().endsWith(".sql")).toList()) {
                if (Files.getLastModifiedTime(archivo).toInstant().isBefore(limite)) {
                    Files.deleteIfExists(archivo);
                }
            }
        }
    }
}
