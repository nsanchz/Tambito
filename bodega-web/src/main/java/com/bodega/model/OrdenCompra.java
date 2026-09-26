package com.bodega.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Orden de compra a un proveedor, con sus líneas de detalle. */
public class OrdenCompra {

    private Integer id;
    private String numero;
    private Integer proveedorId;
    private String proveedorNombre;
    private Integer usuarioId;
    private String usuarioNombre;
    private String usuarioCorreo;
    private String proveedorCorreo;
    private EstadoOrdenCompra estado;
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaUltimaRecepcion;
    private Integer aprobadoPorId;
    private String aprobadoPorNombre;
    private LocalDateTime fechaAprobacion;
    private String motivoRechazo;
    private List<DetalleOrdenCompra> detalles = new ArrayList<>();

    public OrdenCompra() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Integer getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(Integer proveedorId) {
        this.proveedorId = proveedorId;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
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

    public String getUsuarioCorreo() {
        return usuarioCorreo;
    }

    public void setUsuarioCorreo(String usuarioCorreo) {
        this.usuarioCorreo = usuarioCorreo;
    }

    public String getProveedorCorreo() {
        return proveedorCorreo;
    }

    public void setProveedorCorreo(String proveedorCorreo) {
        this.proveedorCorreo = proveedorCorreo;
    }

    public EstadoOrdenCompra getEstado() {
        return estado;
    }

    public void setEstado(EstadoOrdenCompra estado) {
        this.estado = estado;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaUltimaRecepcion() {
        return fechaUltimaRecepcion;
    }

    public void setFechaUltimaRecepcion(LocalDateTime fechaUltimaRecepcion) {
        this.fechaUltimaRecepcion = fechaUltimaRecepcion;
    }

    public Integer getAprobadoPorId() {
        return aprobadoPorId;
    }

    public void setAprobadoPorId(Integer aprobadoPorId) {
        this.aprobadoPorId = aprobadoPorId;
    }

    public String getAprobadoPorNombre() {
        return aprobadoPorNombre;
    }

    public void setAprobadoPorNombre(String aprobadoPorNombre) {
        this.aprobadoPorNombre = aprobadoPorNombre;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public String getMotivoRechazo() {
        return motivoRechazo;
    }

    public void setMotivoRechazo(String motivoRechazo) {
        this.motivoRechazo = motivoRechazo;
    }

    public List<DetalleOrdenCompra> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleOrdenCompra> detalles) {
        this.detalles = detalles;
    }
}
