package com.bodega.model;

/** Estado fiscal de una venta. Solo un ADMINISTRADOR puede anular (ver VentaService). */
public enum EstadoVenta {
    COMPLETADA,
    ANULADA
}
