package com.bodega.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuración de la rutina de respaldos, leída de src/main/resources/database.properties
 * (claves {@code db.backup.*}). Separada de {@link DatabaseConfig} porque no tiene relación
 * con el pool de conexiones HikariCP: son credenciales y rutas para invocar {@code mysqldump}.
 */
public final class BackupConfig {

    private static final Properties PROPS = cargar();

    private BackupConfig() {
        // Clase utilitaria: no instanciable
    }

    private static Properties cargar() {
        Properties props = new Properties();
        try (InputStream input = BackupConfig.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("No se encontró database.properties en el classpath.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar database.properties: " + e.getMessage(), e);
        }
        return props;
    }

    public static String getUsername() {
        return PROPS.getProperty("db.backup.username");
    }

    public static String getPassword() {
        return PROPS.getProperty("db.backup.password");
    }

    public static String getMysqldumpPath() {
        return PROPS.getProperty("db.backup.mysqldump.path", "mysqldump");
    }

    /**
     * @return la carpeta de destino de los respaldos, ya resuelta (expande el marcador
     *         {@code ${user.home}} si está presente en la propiedad), creándola si no existe
     */
    public static Path getDirectorioDestino() {
        String valor = PROPS.getProperty("db.backup.dir", "${user.home}/bodega_backups");
        valor = valor.replace("${user.home}", System.getProperty("user.home"));
        Path directorio = Path.of(valor);
        try {
            Files.createDirectories(directorio);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear la carpeta de respaldos: " + directorio, e);
        }
        return directorio;
    }

    public static int getRetencionDias() {
        return Integer.parseInt(PROPS.getProperty("db.backup.retencion.dias", "30"));
    }

    /**
     * @return cada cuántas horas corre el respaldo automático (database.properties ->
     *         db.backup.intervalo.horas, por defecto 4: 00:00, 04:00, 08:00, 12:00, 16:00,
     *         20:00 hora local del servidor). Antes era un único respaldo diario a una hora
     *         fija; se cambió a un intervalo para tener más puntos de recuperación en el día
     *         sin la complejidad de respaldos incrementales reales (binlogs de MySQL).
     */
    public static int getIntervaloHoras() {
        return Integer.parseInt(PROPS.getProperty("db.backup.intervalo.horas", "4"));
    }

    /** @return el nombre de la base de datos a respaldar, extraído de la URL JDBC de conexión */
    public static String getNombreBaseDatos() {
        String url = PROPS.getProperty("db.url", "");
        int inicio = url.indexOf("://");
        int barra = url.indexOf('/', inicio + 3);
        int fin = url.indexOf('?', barra);
        if (barra < 0) {
            throw new RuntimeException("No se pudo determinar el nombre de la base de datos desde db.url.");
        }
        return fin > barra ? url.substring(barra + 1, fin) : url.substring(barra + 1);
    }

    /** @return host:puerto del servidor MySQL, extraído de la URL JDBC de conexión */
    public static String[] getHostYPuerto() {
        String url = PROPS.getProperty("db.url", "");
        int inicio = url.indexOf("://") + 3;
        int barra = url.indexOf('/', inicio);
        String hostPuerto = url.substring(inicio, barra);
        String[] partes = hostPuerto.split(":");
        return partes.length == 2 ? partes : new String[]{partes[0], "3306"};
    }
}
