package com.bodega.util;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limita la cantidad de intentos de login por dirección IP, independientemente del bloqueo
 * por cuenta que ya aplica {@link com.bodega.service.AutenticacionService}. El bloqueo por
 * cuenta protege una cuenta puntual; esto protege al servidor de que una sola IP martille
 * el endpoint de login (con usuarios distintos o inexistentes) a alto volumen.
 * <p>
 * Implementación en memoria (ventana deslizante por IP): suficiente para una sola instancia
 * de la aplicación como esta; si en el futuro se despliega en múltiples nodos, esto debería
 * moverse a un almacén compartido (ej. Redis).
 */
public final class LoginRateLimiter {

    private static final int MAX_INTENTOS_POR_VENTANA = 15;
    private static final long VENTANA_MS = 5 * 60 * 1000L; // 5 minutos

    private static final Map<String, Deque<Long>> INTENTOS_POR_IP = new ConcurrentHashMap<>();

    private LoginRateLimiter() {
        // Clase utilitaria: no instanciable
    }

    /**
     * Registra un intento de login desde la IP dada y determina si debe permitirse.
     *
     * @param ip dirección IP de origen de la petición
     * @return {@code true} si la IP todavía no superó el máximo de intentos en la ventana
     *         de tiempo vigente (y el intento queda registrado); {@code false} si ya lo
     *         superó (el intento NO se registra, para no extender el bloqueo indefinidamente)
     */
    public static boolean permitirIntento(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        long ahora = System.currentTimeMillis();
        Deque<Long> marcasDeTiempo = INTENTOS_POR_IP.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());

        synchronized (marcasDeTiempo) {
            while (!marcasDeTiempo.isEmpty() && ahora - marcasDeTiempo.peekFirst() > VENTANA_MS) {
                marcasDeTiempo.pollFirst();
            }
            if (marcasDeTiempo.size() >= MAX_INTENTOS_POR_VENTANA) {
                return false;
            }
            marcasDeTiempo.addLast(ahora);
            return true;
        }
    }
}
