package com.bodega.listener;

import com.bodega.config.DatabaseConfig;
import com.bodega.scheduler.AlertaEmailScheduler;
import com.bodega.scheduler.BackupScheduler;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Listener del ciclo de vida de la aplicación.
 * Garantiza que el pool de conexiones HikariCP y los jobs programados (respaldo automático,
 * resumen diario de alertas por correo) arranquen/se cierren correctamente junto con el
 * ciclo de vida de Tomcat.
 */
@WebListener
public class AplicacionContextListener implements ServletContextListener {

    /**
     * Arranca los schedulers de respaldos automáticos diarios (ver {@link BackupScheduler}) y
     * de resumen diario de alertas de inventario (ver {@link AlertaEmailScheduler}). El pool
     * de HikariCP se sigue inicializando de forma perezosa al primer acceso a
     * {@link DatabaseConfig}, no aquí.
     *
     * @param sce evento de inicialización del contexto (no usado)
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        BackupScheduler.iniciar();
        AlertaEmailScheduler.iniciar();
    }

    /**
     * Detiene los schedulers y cierra el pool de conexiones HikariCP para liberar los
     * recursos de la base de datos cuando Tomcat detiene o redespliega la aplicación. Los
     * schedulers se detienen primero para no dejar un respaldo a medias, ni un envío de
     * correo a medias, mientras el pool de conexiones ya se está cerrando.
     *
     * @param sce evento de destrucción del contexto (no usado)
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        BackupScheduler.detener();
        AlertaEmailScheduler.detener();
        DatabaseConfig.closePool();
    }
}
