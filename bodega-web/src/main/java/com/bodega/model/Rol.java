package com.bodega.model;

/**
 * Roles de seguridad del sistema. El rol ADMINISTRADOR tiene privilegios totales
 * (incluyendo la creación de otros administradores); VENDEDOR está restringido
 * al módulo de Punto de Venta, clientes y consulta de inventario.
 */
public enum Rol {
    ADMINISTRADOR,
    VENDEDOR
}
