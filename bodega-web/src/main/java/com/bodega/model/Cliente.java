package com.bodega.model;

import java.time.LocalDateTime;

/** Cliente registrado para ventas con Boleta/Factura y seguimiento de historial de compras. */
public class Cliente {

    private Integer id;
    private TipoDocumentoCliente tipoDocumento;
    private String numeroDocumento;
    private String nombreCompleto;
    private String telefono;
    private String correo;
    private String direccion;
    private EstadoCuenta estado;
    private int puntosFidelizacion;
    private LocalDateTime fechaCreacion;

    public Cliente() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TipoDocumentoCliente getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumentoCliente tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public int getPuntosFidelizacion() {
        return puntosFidelizacion;
    }

    public void setPuntosFidelizacion(int puntosFidelizacion) {
        this.puntosFidelizacion = puntosFidelizacion;
    }

    /** @return la categoría de fidelización vigente, derivada de los puntos acumulados (no se almacena aparte) */
    public CategoriaFidelizacion getCategoriaFidelizacion() {
        return CategoriaFidelizacion.desdePuntos(puntosFidelizacion);
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
