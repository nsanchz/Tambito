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
 * Job de respaldo automático de la base de datos, repetido cada N horas (en vez de una sola
 * vez al día), para tener más puntos de recuperación posibles sin la complejidad operativa
 * de respaldos incrementales reales (binary logs de MySQL + restauración punto-a-punto) —
 * decisión consciente de simplicidad para el alcance de este proyecto. Se inicia desde
 * {@link com.bodega.listener.AplicacionContextListener#contextInitialized} y corre en un
 * único hilo daemon (no bloquea el apagado de Tomcat), ejecutando
 * {@link BackupService#ejecutarRespaldo} alineado a horas en punto del día (database.properties
 * -> db.backup.intervalo.horas; con el valor por defecto de 4, corre a las 00:00, 04:00, 08:00,
 * 12:00, 16:00 y 20:00 hora local del servidor).
 */
public final class BackupScheduler {

    private static ScheduledExecutorService executor;
    private static final BackupService BACKUP_SERVICE = new BackupService();

    private BackupScheduler() {
        // Clase utilitaria: no instanciable
    }

    /** Programa la primera ejecución (en la próxima hora en punto que calce con el intervalo) y las siguientes cada N horas. */
    public static synchronized void iniciar() {
        if (executor != null && !executor.isShutdown()) {
            return; // ya estaba iniciado (evita doble programación ante un redeploy sin destroy previo)
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "backup-scheduler");
            hilo.setDaemon(true);
            return hilo;
        });

        int intervaloHoras = BackupConfig.getIntervaloHoras();
        long retrasoInicialSegundos = calcularRetrasoHastaProximaEjecucion(intervaloHoras);
        executor.scheduleAtFixedRate(
                () -> BACKUP_SERVICE.ejecutarRespaldo(TipoRespaldo.AUTOMATICO, null),
                retrasoInicialSegundos,
                TimeUnit.HOURS.toSeconds(intervaloHoras),
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

    /**
     * @param intervaloHoras cada cuántas horas debe correr (ej. 4 -> 00:00, 04:00, 08:00...)
     * @return segundos desde ahora hasta la próxima hora en punto alineada a ese intervalo
     */
    private static long calcularRetrasoHastaProximaEjecucion(int intervaloHoras) {
        LocalDateTime ahora = LocalDateTime.now();
        int horaActual = ahora.getHour();
        int horaAlineada = (horaActual / intervaloHoras) * intervaloHoras;
        LocalDateTime proximaEjecucion = ahora
                .withHour(horaAlineada).withMinute(0).withSecond(0).withNano(0);
        if (!proximaEjecucion.isAfter(ahora)) {
            proximaEjecucion = proximaEjecucion.plusHours(intervaloHoras);
        }
        return Duration.between(ahora, proximaEjecucion).getSeconds();
    }
}
