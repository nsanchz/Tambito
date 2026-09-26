package com.bodega.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Cabecera de una venta (comprobante electrónico), con su detalle de líneas. */
public class Venta {

    private Integer id;
    private String numeroComprobante;
    private TipoComprobante tipoComprobante;
    private Integer clienteId;
    private String clienteNombre;
    private Integer usuarioId;
    private String usuarioNombre;
    private String terminalId;
    private BigDecimal subtotalImponible;
    private BigDecimal descuento;
    private BigDecimal igv;
    private BigDecimal total;
    private MetodoPago metodoPago;
    private MonedaPago monedaPago = MonedaPago.PEN;
    private BigDecimal tipoCambioAplicado;
    private BigDecimal montoPagadoUsd;
    private EstadoVenta estado;
    private String motivoAnulacion;
    private Integer usuarioAnulacionId;
    private LocalDateTime fechaAnulacion;
    private LocalDateTime fechaCreacion;
    private List<DetalleVenta> detalles = new ArrayList<>();

    public Venta() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumeroComprobante() {
        return numeroComprobante;
    }

    public void setNumeroComprobante(String numeroComprobante) {
        this.numeroComprobante = numeroComprobante;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public BigDecimal getSubtotalImponible() {
        return subtotalImponible;
    }

    public void setSubtotalImponible(BigDecimal subtotalImponible) {
        this.subtotalImponible = subtotalImponible;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }

    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getIgv() {
        return igv;
    }

    public void setIgv(BigDecimal igv) {
        this.igv = igv;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public MonedaPago getMonedaPago() {
        return monedaPago;
    }

    public void setMonedaPago(MonedaPago monedaPago) {
        this.monedaPago = monedaPago;
    }

    /** Tipo de cambio (tasa "compra" SUNAT) aplicado al momento de la venta, o {@code null} si se pagó en soles. */
    public BigDecimal getTipoCambioAplicado() {
        return tipoCambioAplicado;
    }

    public void setTipoCambioAplicado(BigDecimal tipoCambioAplicado) {
        this.tipoCambioAplicado = tipoCambioAplicado;
    }

    /** Monto en dólares que el cliente entregó físicamente, o {@code null} si se pagó en soles. */
    public BigDecimal getMontoPagadoUsd() {
        return montoPagadoUsd;
    }

    public void setMontoPagadoUsd(BigDecimal montoPagadoUsd) {
        this.montoPagadoUsd = montoPagadoUsd;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoVenta estado) {
        this.estado = estado;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public void setMotivoAnulacion(String motivoAnulacion) {
        this.motivoAnulacion = motivoAnulacion;
    }

    public Integer getUsuarioAnulacionId() {
        return usuarioAnulacionId;
    }

    public void setUsuarioAnulacionId(Integer usuarioAnulacionId) {
        this.usuarioAnulacionId = usuarioAnulacionId;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    public void setFechaAnulacion(LocalDateTime fechaAnulacion) {
        this.fechaAnulacion = fechaAnulacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
    }
}
