package com.bodega.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Turno de caja: apertura con monto inicial y cierre con arqueo (efectivo esperado vs. contado). */
public class TurnoCaja {

    private Integer id;
    private Integer usuarioId;
    private String usuarioNombre;
    private String terminalId;
    private BigDecimal montoInicial;
    private BigDecimal montoEfectivoCalculado;
    private BigDecimal montoDeclarado;
    private BigDecimal diferencia;
    private EstadoTurno estado;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;

    public TurnoCaja() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public BigDecimal getMontoEfectivoCalculado() {
        return montoEfectivoCalculado;
    }

    public void setMontoEfectivoCalculado(BigDecimal montoEfectivoCalculado) {
        this.montoEfectivoCalculado = montoEfectivoCalculado;
    }

    public BigDecimal getMontoDeclarado() {
        return montoDeclarado;
    }

    public void setMontoDeclarado(BigDecimal montoDeclarado) {
        this.montoDeclarado = montoDeclarado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurno estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
}
