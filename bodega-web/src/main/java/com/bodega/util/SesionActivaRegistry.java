package com.bodega.util;

import jakarta.servlet.http.HttpSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro en memoria de la sesión HTTP activa de cada usuario autenticado (una sola instancia
 * de Tomcat, mismo criterio que {@link LoginRateLimiter}). Se usa para poder forzar el cierre
 * de la sesión de un usuario específico desde otra petición — en particular, cuando un
 * administrador activa el MFA de alguien: esa persona debe verse obligada a reingresar sus
 * credenciales para completar el enrolamiento, en vez de seguir navegando con la sesión que
 * ya tenía abierta.
 */
public final class SesionActivaRegistry {

    private static final Map<Integer, HttpSession> SESIONES_POR_USUARIO = new ConcurrentHashMap<>();

    private SesionActivaRegistry() {
    }

    /** Registra (o reemplaza) la sesión activa de un usuario, llamado al abrir sesión completa. */
    public static void registrar(int usuarioId, HttpSession session) {
        SESIONES_POR_USUARIO.put(usuarioId, session);
    }

    /** Quita el registro de un usuario (logout normal o expiración), sin invalidar nada. */
    public static void eliminar(int usuarioId) {
        SESIONES_POR_USUARIO.remove(usuarioId);
    }

    /**
     * Si el usuario tiene una sesión activa registrada, la invalida de inmediato (su próxima
     * petición ya no encontrará una sesión autenticada). Si la sesión ya expiró o fue
     * invalidada por otro motivo mientras tanto, no hace nada.
     */
    public static void invalidarSiExiste(int usuarioId) {
        HttpSession session = SESIONES_POR_USUARIO.remove(usuarioId);
        if (session != null) {
            try {
                session.invalidate();
            } catch (IllegalStateException yaInvalidada) {
                // La sesión ya estaba invalidada (ej. el usuario cerró sesión justo antes); no pasa nada.
            }
        }
    }
}
