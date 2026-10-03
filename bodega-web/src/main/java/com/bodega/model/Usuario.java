package com.bodega.model;

import java.time.LocalDateTime;

/** Representa un usuario del sistema (administrador o vendedor). */
public class Usuario {

    private Integer id;
    private String nombres;
    private String apellidos;
    private String nombreUsuario;
    private String correo;
    private String telefono;
    private String passwordHash;
    private Rol rol;
    private EstadoCuenta estado;
    private boolean debeCambiarPassword;
    private int intentosFallidos;
    private LocalDateTime bloqueadoHasta;
    private LocalDateTime ultimoAcceso;
    private LocalDateTime fechaCreacion;
    private Integer creadoPorId;
    private boolean mfaHabilitado;
    private String mfaSecret;
    private Integer mfaActivadoPorId;
    private LocalDateTime mfaFechaActivacion;
    private String resetPasswordCodigoHash;
    private LocalDateTime resetPasswordExpira;
    private int resetPasswordIntentos;
    private boolean resetPasswordVerificado;

    public Usuario() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public EstadoCuenta getEstado() {
        return estado;
    }

    public void setEstado(EstadoCuenta estado) {
        this.estado = estado;
    }

    public boolean isDebeCambiarPassword() {
        return debeCambiarPassword;
    }

    public void setDebeCambiarPassword(boolean debeCambiarPassword) {
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDateTime ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Integer getCreadoPorId() {
        return creadoPorId;
    }

    public void setCreadoPorId(Integer creadoPorId) {
        this.creadoPorId = creadoPorId;
    }

    /** @return {@code true} si la cuenta está bloqueada temporalmente por intentos fallidos de login */
    public boolean isBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(LocalDateTime.now());
    }

    public boolean isMfaHabilitado() {
        return mfaHabilitado;
    }

    public void setMfaHabilitado(boolean mfaHabilitado) {
        this.mfaHabilitado = mfaHabilitado;
    }

    /** Secreto TOTP cifrado (AES-256-GCM vía CifradoUtil) tal como se guarda en BD; nunca en claro. */
    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public Integer getMfaActivadoPorId() {
        return mfaActivadoPorId;
    }

    public void setMfaActivadoPorId(Integer mfaActivadoPorId) {
        this.mfaActivadoPorId = mfaActivadoPorId;
    }

    public LocalDateTime getMfaFechaActivacion() {
        return mfaFechaActivacion;
    }

    public void setMfaFechaActivacion(LocalDateTime mfaFechaActivacion) {
        this.mfaFechaActivacion = mfaFechaActivacion;
    }

    /** Hash BCrypt del código de 6 dígitos de recuperación de contraseña vigente (nunca en claro); {@code null} si no hay ninguno pendiente. */
    public String getResetPasswordCodigoHash() {
        return resetPasswordCodigoHash;
    }

    public void setResetPasswordCodigoHash(String resetPasswordCodigoHash) {
        this.resetPasswordCodigoHash = resetPasswordCodigoHash;
    }

    public LocalDateTime getResetPasswordExpira() {
        return resetPasswordExpira;
    }

    public void setResetPasswordExpira(LocalDateTime resetPasswordExpira) {
        this.resetPasswordExpira = resetPasswordExpira;
    }

    public int getResetPasswordIntentos() {
        return resetPasswordIntentos;
    }

    public void setResetPasswordIntentos(int resetPasswordIntentos) {
        this.resetPasswordIntentos = resetPasswordIntentos;
    }

    /** {@code true} si ya se validó el código de 6 dígitos y puede pasar a elegir la nueva contraseña. */
    public boolean isResetPasswordVerificado() {
        return resetPasswordVerificado;
    }

    public void setResetPasswordVerificado(boolean resetPasswordVerificado) {
        this.resetPasswordVerificado = resetPasswordVerificado;
    }

    /** @return {@code true} si hay un código de recuperación vigente (no nulo y todavía no expiró) */
    public boolean tieneCodigoRecuperacionVigente() {
        return resetPasswordCodigoHash != null && resetPasswordExpira != null
                && resetPasswordExpira.isAfter(LocalDateTime.now());
    }
}
