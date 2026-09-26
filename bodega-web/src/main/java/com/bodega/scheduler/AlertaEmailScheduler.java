package com.bodega.scheduler;

import com.bodega.model.EstadoCuenta;
import com.bodega.model.LoteProducto;
import com.bodega.model.Producto;
import com.bodega.model.Rol;
import com.bodega.model.Usuario;
import com.bodega.dao.UsuarioDAO;
import com.bodega.service.AlertaService;
import com.bodega.service.EmailService;
import com.bodega.service.ProductoService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Job diario que envía a los administradores activos un resumen por correo de las alertas
 * de inventario (stock bajo, lotes próximos a vencer, productos de baja rotación). Se inicia
 * desde {@link com.bodega.listener.AplicacionContextListener#contextInitialized} y corre en un
 * único hilo daemon, a la hora configurada (database.properties -> alertas.email.hora) y
 * luego cada 24 horas — mismo patrón que {@link BackupScheduler}.
 * <p>
 * Es un resumen diario (no una alerta por evento) precisamente porque no existe ningún
 * mecanismo de "ya se avisó esto" en el sistema: un solo correo consolidado al día evita
 * mandar el mismo aviso de stock bajo cada vez que se revisa, sin necesitar una tabla nueva
 * de seguimiento.
 */
public final class AlertaEmailScheduler {

    private static ScheduledExecutorService executor;

    private static final ProductoService PRODUCTO_SERVICE = new ProductoService();
    private static final AlertaService ALERTA_SERVICE = new AlertaService();
    private static final UsuarioDAO USUARIO_DAO = new UsuarioDAO();
    private static final EmailService EMAIL_SERVICE = new EmailService();

    private AlertaEmailScheduler() {
        // Clase utilitaria: no instanciable
    }

    /** Programa la primera ejecución (hoy o mañana, según la hora configurada) y las siguientes cada 24 horas. */
    public static synchronized void iniciar() {
        if (executor != null && !executor.isShutdown()) {
            return; // ya estaba iniciado (evita doble programación ante un redeploy sin destroy previo)
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread hilo = new Thread(runnable, "alerta-email-scheduler");
            hilo.setDaemon(true);
            return hilo;
        });

        long retrasoInicialSegundos = calcularRetrasoHastaProximaEjecucion();
        executor.scheduleAtFixedRate(
                AlertaEmailScheduler::enviarResumenDiario,
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

    /**
     * Reúne las tres alertas de inventario y envía el resumen a los administradores activos.
     * Cualquier fallo (de consulta o de envío) solo se registra en el log: nunca debe tumbar
     * el hilo del scheduler.
     */
    private static void enviarResumenDiario() {
        try {
            List<Producto> stockBajo = PRODUCTO_SERVICE.listarConStockBajoMinimo();
            List<LoteProducto> porVencer = ALERTA_SERVICE.lotesPorVencer();
            List<Producto> bajaRotacion = ALERTA_SERVICE.productosBajaRotacion();

            List<String> correosAdmins = USUARIO_DAO.listar(Rol.ADMINISTRADOR, EstadoCuenta.ACTIVO, null).stream()
                    .map(Usuario::getCorreo)
                    .filter(correo -> correo != null && !correo.isBlank())
                    .toList();

            EMAIL_SERVICE.notificarResumenAlertasInventario(correosAdmins, stockBajo, porVencer, bajaRotacion);
        } catch (Exception e) {
            System.err.println("Error al enviar el resumen diario de alertas de inventario: " + e.getMessage());
        }
    }

    /** @return segundos desde ahora hasta la próxima ocurrencia de la hora programada */
    private static long calcularRetrasoHastaProximaEjecucion() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime proximaEjecucion = ahora
                .withHour(obtenerHoraProgramada())
                .withMinute(0).withSecond(0).withNano(0);
        if (!proximaEjecucion.isAfter(ahora)) {
            proximaEjecucion = proximaEjecucion.plusDays(1);
        }
        return Duration.between(ahora, proximaEjecucion).getSeconds();
    }

    /** @return hora del día (0-23, hora local) configurada en database.properties -> alertas.email.hora (7 por defecto) */
    private static int obtenerHoraProgramada() {
        Properties props = new Properties();
        try (InputStream input = AlertaEmailScheduler.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException ignorado) {
            // Si falla la lectura, se usa la hora por defecto de abajo.
        }
        return Integer.parseInt(props.getProperty("alertas.email.hora", "7"));
    }
}
