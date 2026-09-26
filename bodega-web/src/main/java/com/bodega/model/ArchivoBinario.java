package com.bodega.model;

/** Archivo binario de configuración (ej. logo de la tienda) leído de archivos_configuracion. */
public class ArchivoBinario {

    private byte[] contenido;
    private String contentType;
    private String nombreOriginal;

    public ArchivoBinario() {
    }

    public ArchivoBinario(byte[] contenido, String contentType, String nombreOriginal) {
        this.contenido = contenido;
        this.contentType = contentType;
        this.nombreOriginal = nombreOriginal;
    }

    public byte[] getContenido() {
        return contenido;
    }

    public void setContenido(byte[] contenido) {
        this.contenido = contenido;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }
}
