package com.bodega.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Categoría de clasificación comercial del catálogo, usada en inventario y en el POS. */
public class Categoria {

    private Integer id;
    private String codigo;
    private String nombre;
    private String descripcion;
    private String icono;
    private BigDecimal margenSugerido;
    private EstadoCuenta estado;
    private LocalDateTime fechaCreacion;

    public Categoria() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public BigDecimal getMargenSugerido() {
        return margenSugerido;
    }

    public void setMargenSugerido(BigDecimal margenSugerido) {
        this.margenSugerido = margenSugerido;
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
