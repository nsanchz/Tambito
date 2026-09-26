package com.bodega.util;

import java.security.SecureRandom;
import java.util.Base64;

/** Generación de tokens anti-CSRF (patrón "synchronizer token"), ver {@link com.bodega.filter.CsrfFilter}. */
public final class CsrfUtil {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int TAMANO_BYTES = 32;

    private CsrfUtil() {
        // Clase utilitaria: no instanciable
    }

    /** @return un token aleatorio de 256 bits codificado en Base64 URL-safe, único por sesión */
    public static String generarToken() {
        byte[] bytes = new byte[TAMANO_BYTES];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
