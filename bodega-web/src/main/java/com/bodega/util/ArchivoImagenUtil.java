package com.bodega.util;

import java.util.Set;

/**
 * Validación de archivos de imagen subidos por el usuario (ej. logo de la tienda), para no
 * confiar únicamente en el {@code Content-Type} que declara el navegador del cliente (un
 * atacante puede mandar cualquier valor ahí). Verifica también los primeros bytes del
 * archivo ("magic bytes") contra la firma real del formato.
 */
public final class ArchivoImagenUtil {

    /** Tipos MIME de imagen aceptados en toda la aplicación (logo, y futuras imágenes de producto). */
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/png", "image/jpeg", "image/webp");

    /** Tamaño máximo aceptado para una imagen subida por el usuario (2 MB). */
    public static final long TAMANO_MAXIMO_BYTES = 2L * 1024 * 1024;

    private ArchivoImagenUtil() {
        // Clase utilitaria: no instanciable
    }

    /**
     * @param contentType tipo MIME declarado por el navegador (puede ser falso)
     * @return {@code true} si el tipo declarado está en la whitelist de imágenes aceptadas
     */
    public static boolean esTipoPermitido(String contentType) {
        return contentType != null && TIPOS_PERMITIDOS.contains(contentType.toLowerCase());
    }

    /**
     * Verifica que los primeros bytes del contenido correspondan realmente a la firma binaria
     * del formato declarado, sin confiar en el {@code Content-Type} ni en la extensión del
     * nombre de archivo (ambos los controla quien sube el archivo).
     *
     * @param contenido   bytes del archivo subido
     * @param contentType tipo MIME declarado (debe ser uno de {@link #esTipoPermitido})
     * @return {@code true} si los bytes iniciales coinciden con la firma esperada para ese tipo
     */
    public static boolean coincideConFirmaBinaria(byte[] contenido, String contentType) {
        if (contenido == null || contentType == null) {
            return false;
        }
        return switch (contentType.toLowerCase()) {
            case "image/png" -> empiezaCon(contenido, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/jpeg" -> empiezaCon(contenido, 0xFF, 0xD8, 0xFF);
            case "image/webp" -> contenido.length >= 12
                    && empiezaCon(contenido, 0x52, 0x49, 0x46, 0x46) // "RIFF"
                    && contenido[8] == 0x57 && contenido[9] == 0x45 && contenido[10] == 0x42 && contenido[11] == 0x50; // "WEBP"
            default -> false;
        };
    }

    private static boolean empiezaCon(byte[] contenido, int... firma) {
        if (contenido.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((contenido[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
