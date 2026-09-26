package com.bodega.model;

import java.math.BigDecimal;

/** Línea de detalle de una orden de compra: producto, cantidad pedida y cantidad ya recibida. */
public class DetalleOrdenCompra {

    private Integer id;
    private Integer ordenCompraId;
    private Integer productoId;
    private String productoNombre;
    private int cantidadPedida;
    private int cantidadRecibida;
    private BigDecimal precioUnitario;

    public DetalleOrdenCompra() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrdenCompraId() {
        return ordenCompraId;
    }

    public void setOrdenCompraId(Integer ordenCompraId) {
        this.ordenCompraId = ordenCompraId;
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

    public int getCantidadPedida() {
        return cantidadPedida;
    }

    public void setCantidadPedida(int cantidadPedida) {
        this.cantidadPedida = cantidadPedida;
    }

    public int getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(int cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    /** @return unidades que aún faltan por recibir (permite recepciones parciales en varias entregas) */
    public int getCantidadPendiente() {
        return cantidadPedida - cantidadRecibida;
    }

    /** @return {@code precioUnitario * cantidadPedida} */
    public BigDecimal getSubtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidadPedida));
    }
}
