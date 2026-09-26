package com.bodega.util;

/** Constantes compartidas: nombres de atributos de sesión y parámetros de seguridad. */
public final class Constantes {

    private Constantes() {
    }

    // Atributos de HttpSession
    public static final String SESSION_USUARIO_ID = "usuarioId";
    public static final String SESSION_USUARIO_NOMBRE = "usuarioNombre";
    public static final String SESSION_USUARIO_ROL = "usuarioRol";
    public static final String SESSION_TERMINAL_ID = "terminalId";
    public static final String SESSION_NOMBRE_EMPRESA = "nombreEmpresa";
    public static final String SESSION_CSRF_TOKEN = "csrfToken";

    // Atributos de request para mensajes hacia las vistas JSP
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_MENSAJE = "mensaje";

    // Política de bloqueo de cuentas
    public static final int MAX_INTENTOS_FALLIDOS = 3;
    public static final int MINUTOS_BLOQUEO = 15;
}
