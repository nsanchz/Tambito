package com.bodega.model;

/** Tipo de movimiento de inventario (Kárdex). El signo determina si suma o resta stock. */
public enum TipoMovimiento {
    ENTRADA_COMPRA(1),
    SALIDA_VENTA(-1),
    AJUSTE_POSITIVO(1),
    AJUSTE_NEGATIVO(-1),
    DEVOLUCION(1),
    MERMA(-1),
    ANULACION_VENTA(1);

    private final int factor;

    TipoMovimiento(int factor) {
        this.factor = factor;
    }

    /** @return {@code +1} si el tipo incrementa stock, {@code -1} si lo descuenta */
    public int getFactor() {
        return factor;
    }

    /**
     * Nombrado "isIncremento" (no "esIncremento") a propósito: JSTL/EL en las JSP solo
     * resuelve "${m.tipo.incremento}" a través de la convención JavaBean isXxx()/getXxx(),
     * no reconoce nombres de método arbitrarios aunque sean semánticamente equivalentes.
     */
    public boolean isIncremento() {
        return factor > 0;
    }
}
