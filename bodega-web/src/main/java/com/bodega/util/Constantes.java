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

    // Cacheado en sesión al iniciar sesión (ver LoginSessionHelper) para que
    // AuthenticationFilter pueda confinar al usuario al módulo "Mi Perfil" sin tener que
    // consultar la base de datos en cada petición. Se limpia a false ahí mismo apenas el
    // usuario cambia su contraseña con éxito (ver PerfilServlet), sin esperar a un nuevo login.
    public static final String SESSION_DEBE_CAMBIAR_PASSWORD = "debeCambiarPassword";

    // MFA: sesión "pendiente de segundo factor" — no lleva permisos de negocio, solo permite
    // completar /login-mfa. Nunca se combina con SESSION_USUARIO_ID (o hay sesión completa,
    // o hay sesión pendiente de MFA, nunca ambas a la vez).
    public static final String SESSION_MFA_PENDIENTE_USUARIO_ID = "mfaPendienteUsuarioId";
    public static final String SESSION_MFA_PENDIENTE_EXPIRA = "mfaPendienteExpira";
    public static final int MFA_PENDIENTE_MINUTOS = 2;

    // Distingue, dentro de la sesión pendiente de /login-mfa, si el usuario debe solo ingresar
    // su código habitual (MFA ya confirmado) o si además debe escanear el QR y confirmarlo por
    // primera vez (activación recién iniciada por un administrador, ver MfaService).
    public static final String SESSION_MFA_ENROLANDO = "mfaEnrolando";

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

    // Restablecimiento autoservicio de contraseña ("olvidé mi contraseña"): sesión "pendiente
    // de recuperación", análoga a SESSION_MFA_PENDIENTE_*, usada por los 3 pasos del flujo
    // (solicitar -> verificar código -> elegir nueva contraseña). resetUsuarioId guarda -1
    // como centinela cuando el usuario/correo ingresado no existe, para que el paso de
    // verificación siga mostrando la misma pantalla genérica sin revelar si la cuenta existe.
    public static final String SESSION_RESET_USUARIO_ID = "resetUsuarioId";
    public static final String SESSION_RESET_EXPIRA = "resetExpira";
    public static final String SESSION_RESET_VERIFICADO = "resetVerificado";
    public static final String SESSION_RESET_CORREO_MOSTRADO = "resetCorreoMostrado";
    public static final int RESET_PENDIENTE_MINUTOS = 15;
    public static final int CODIGO_RESET_VALIDO_MINUTOS = 10;
    public static final int MAX_INTENTOS_CODIGO_RESET = 5;
}
