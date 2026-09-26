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

    // MFA: sesión "pendiente de segundo factor" — no lleva permisos de negocio, solo permite
    // completar /login-mfa. Nunca se combina con SESSION_USUARIO_ID (o hay sesión completa,
    // o hay sesión pendiente de MFA, nunca ambas a la vez).
    public static final String SESSION_MFA_PENDIENTE_USUARIO_ID = "mfaPendienteUsuarioId";
    public static final String SESSION_MFA_PENDIENTE_EXPIRA = "mfaPendienteExpira";
    public static final int MFA_PENDIENTE_MINUTOS = 2;

    // Atributos de request para mensajes hacia las vistas JSP
    public static final String ATTR_ERROR = "error";
    public static final String ATTR_MENSAJE = "mensaje";

    // Política de bloqueo de cuentas
    public static final int MAX_INTENTOS_FALLIDOS = 3;
    public static final int MINUTOS_BLOQUEO = 15;

    // Política de bloqueo específica para el paso de código MFA (más estricta: un código de
    // 6 dígitos es más corto que una contraseña, así que se tolera menos intentos).
    public static final int MAX_INTENTOS_MFA_FALLIDOS = 5;
    public static final int MINUTOS_BLOQUEO_MFA = 15;
}
