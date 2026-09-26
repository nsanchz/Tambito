package com.bodega.model;

/**
 * Moneda en la que el cliente pagó físicamente una venta. El total/subtotal/igv de la
 * {@link Venta} siempre están en soles (PEN) — esto solo registra la forma de pago para
 * el ticket y la conciliación de caja, nunca para recalcular el total.
 */
public enum MonedaPago {
    PEN,
    USD
}
