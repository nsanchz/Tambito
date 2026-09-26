package com.bodega.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lote de un producto recibido en una fecha determinada, con su propia fecha de
 * vencimiento y cantidad. Las ventas y salidas de inventario consumen los lotes
 * en orden FEFO (el de fecha de vencimiento más próxima primero).
 */
public class LoteProducto {

    private Integer id;
    private String numeroLote;
    private Integer productoId;
    private String productoNombre;
    private LocalDate fechaVencimiento;
    private int cantidadInicial;
    private int cantidadActual;
    private BigDecimal precioCompra;
    private Integer ordenCompraId;
    private Integer usuarioId;
    private LocalDateTime fechaIngreso;

    public LoteProducto() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroLote() {
        return numeroLote;
    }

    public void setNumeroLote(String numeroLote) {
        this.numeroLote = numeroLote;
    }

    public Integer getProductoId() {
        return productoId;
    }

    public void setProductoId(Integer productoId) {
        this.productoId = productoId;
    }

    public String getProductoNombre() {
        return productoNombre;
    }

    public void setProductoNombre(String productoNombre) {
        this.productoNombre = productoNombre;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public void setFechaVencimiento(LocalDate fechaVencimiento) {
        this.fechaVencimiento = fechaVencimiento;
    }

    public int getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(int cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }

    public int getCantidadActual() {
        return cantidadActual;
    }

    public void setCantidadActual(int cantidadActual) {
        this.cantidadActual = cantidadActual;
    }

    public java.math.BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(java.math.BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public Integer getOrdenCompraId() {
        return ordenCompraId;
    }

    public void setOrdenCompraId(Integer ordenCompraId) {
        this.ordenCompraId = ordenCompraId;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }
}
