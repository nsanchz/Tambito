package com.bodega.model;

/** Categoría de fidelización de un cliente, derivada de sus puntos acumulados. */
public enum CategoriaFidelizacion {
    NUEVO,
    FRECUENTE,
    VIP;

    private static final int UMBRAL_FRECUENTE = 100;
    private static final int UMBRAL_VIP = 500;

    /** Calcula la categoría vigente a partir del total de puntos acumulados. */
    public static CategoriaFidelizacion desdePuntos(int puntos) {
        if (puntos >= UMBRAL_VIP) {
            return VIP;
        }
        if (puntos >= UMBRAL_FRECUENTE) {
            return FRECUENTE;
        }
        return NUEVO;
    }
}
