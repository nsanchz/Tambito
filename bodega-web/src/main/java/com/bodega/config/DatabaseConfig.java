package com.bodega.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Configuración centralizada del pool de conexiones a la base de datos usando HikariCP.
 * Lee los parámetros desde src/main/resources/database.properties.
 */
public final class DatabaseConfig {

    private static final HikariDataSource DATA_SOURCE;

    static {
        Properties props = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException(
                        "No se encontró el archivo database.properties en el classpath.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar database.properties: " + e.getMessage(), e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getProperty("db.url"));
        config.setUsername(props.getProperty("db.username"));
        config.setPassword(props.getProperty("db.password"));
        config.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));

        config.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "20")));
        config.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "5")));
        config.setConnectionTimeout(Long.parseLong(props.getProperty("db.pool.connectionTimeout", "30000")));
        config.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeout", "600000")));
        config.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetime", "1800000")));
        config.setPoolName("BodegaHikariPool");

        // Optimizaciones recomendadas por HikariCP para MySQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        DATA_SOURCE = new HikariDataSource(config);
    }

    private DatabaseConfig() {
        // Clase utilitaria: no instanciable
    }

    /**
     * Obtiene una conexión del pool. El llamador es responsable de cerrarla
     * (usar try-with-resources) para que vuelva al pool.
     *
     * @return una conexión JDBC lista para usar, tomada del pool HikariCP
     * @throws SQLException si el pool no puede entregar una conexión (ej. se agotó el tiempo de espera)
     */
    public static Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /** Cierra el pool de conexiones. Debe invocarse al detener la aplicación (ver ContextListener). */
    public static void closePool() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}
