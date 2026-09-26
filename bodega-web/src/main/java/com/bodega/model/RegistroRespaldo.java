package com.bodega.model;

import java.time.LocalDateTime;

/** Registro inmutable de la bitácora de respaldos de base de datos (una fila por ejecución). */
public class RegistroRespaldo {

    private Integer id;
    private TipoRespaldo tipo;
    private EstadoRespaldo estado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Long tamanoBytes;
    private String rutaDestino;
    private String mensajeError;
    private Integer usuarioId;
    private String usuarioNombre;

    public RegistroRespaldo() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public TipoRespaldo getTipo() {
        return tipo;
    }

    public void setTipo(TipoRespaldo tipo) {
        this.tipo = tipo;
    }

    public EstadoRespaldo getEstado() {
        return estado;
    }

    public void setEstado(EstadoRespaldo estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getRutaDestino() {
        return rutaDestino;
    }

    public void setRutaDestino(String rutaDestino) {
        this.rutaDestino = rutaDestino;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
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
}
