package com.bodega.model;

/** Estado del ciclo de vida de una orden de compra a proveedor. */
public enum EstadoOrdenCompra {
    PENDIENTE,
    APROBADA,
    RECHAZADA,
    RECIBIDA_PARCIAL,
    RECIBIDA_COMPLETA,
    CANCELADA
}
