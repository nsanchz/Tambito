package com.bodega.scheduler;

import com.bodega.config.BackupConfig;
import com.bodega.model.TipoRespaldo;
import com.bodega.service.BackupService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job de respaldo automático diario de la base de datos. Se inicia desde
 * {@link com.bodega.listener.AplicacionContextListener#contextInitialized} y corre en un
 * único hilo daemon (no bloquea el apagado de Tomcat), ejecutando
 * {@link BackupService#ejecutarRespaldo} a la hora configurada
 * (database.properties -> db.backup.hora) y luego cada 24 horas.
 */
public final class BackupScheduler {

    private static ScheduledExecutorService executor;
    private static final BackupService BACKUP_SERVICE = new BackupService();

    private BackupScheduler() {
        // Clase utilitaria: no instanciable
    }

    /** Programa la primera ejecución (hoy o mañana, según la hora configurada) y las siguientes cada 24 horas. */
    public static synchronized void iniciar() {
        if (executor != null && !executor.isShutdown()) {
            return; // ya estaba iniciado (evita doble programación ante un redeploy sin destroy previo)
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "backup-scheduler");
            hilo.setDaemon(true);
            return hilo;
        });

        long retrasoInicialSegundos = calcularRetrasoHastaProximaEjecucion();
        executor.scheduleAtFixedRate(
                () -> BACKUP_SERVICE.ejecutarRespaldo(TipoRespaldo.AUTOMATICO, null),
                retrasoInicialSegundos,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );
    }

    /** Detiene el scheduler. Debe invocarse al detener/redesplegar la aplicación. */
    public static synchronized void detener() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    /** @return segundos desde ahora hasta la próxima ocurrencia de la hora programada */
    private static long calcularRetrasoHastaProximaEjecucion() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime proximaEjecucion = ahora
                .withHour(BackupConfig.getHoraProgramada())
                .withMinute(0).withSecond(0).withNano(0);
        if (!proximaEjecucion.isAfter(ahora)) {
            proximaEjecucion = proximaEjecucion.plusDays(1);
        }
        return Duration.between(ahora, proximaEjecucion).getSeconds();
    }
}
