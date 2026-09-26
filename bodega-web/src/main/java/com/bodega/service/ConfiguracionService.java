package com.bodega.service;

import com.bodega.dao.ArchivoConfiguracionDAO;
import com.bodega.dao.ConfiguracionDAO;
import com.bodega.model.ArchivoBinario;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lógica de acceso a los parámetros generales del sistema (Configuración del Sistema).
 * Define las claves conocidas con su etiqueta y descripción para que la vista JSP
 * pueda renderizar el formulario de forma genérica sin código Java embebido.
 */
public class ConfiguracionService {

    public static final String CLAVE_NOMBRE_EMPRESA = "nombre_empresa";
    public static final String CLAVE_RUC_EMPRESA = "ruc_empresa";
    public static final String CLAVE_MONEDA = "moneda";
    public static final String CLAVE_TASA_IGV = "tasa_igv";
    public static final String CLAVE_STOCK_MINIMO_DEFECTO = "stock_minimo_defecto";
    public static final String CLAVE_MAX_INTENTOS_LOGIN = "max_intentos_login";
    public static final String CLAVE_MINUTOS_BLOQUEO_LOGIN = "minutos_bloqueo_login";
    public static final String CLAVE_TIMEOUT_SESION_ADMIN = "timeout_sesion_admin_minutos";
    public static final String CLAVE_TIMEOUT_SESION_VENDEDOR = "timeout_sesion_vendedor_minutos";
    public static final String CLAVE_DIAS_ROTACION_MINIMA = "dias_rotacion_minima";
    public static final String CLAVE_DIAS_ALERTA_VENCIMIENTO = "dias_alerta_vencimiento";

    /**
     * Metadatos de un parámetro configurable, usados por la vista JSP para renderizar
     * el formulario de forma genérica sin código Java embebido.
     *
     * @param clave       clave interna del parámetro (una de las constantes {@code CLAVE_*})
     * @param etiqueta    etiqueta legible mostrada junto al campo
     * @param descripcion texto de ayuda mostrado debajo del campo
     * @param tipo        tipo de input HTML a usar ("text" o "number")
     */
    public record DefinicionParametro(String clave, String etiqueta, String descripcion, String tipo) {
    }

    public static final DefinicionParametro[] PARAMETROS = {
            new DefinicionParametro(CLAVE_NOMBRE_EMPRESA, "Nombre de la empresa", "Razón comercial mostrada en comprobantes y reportes.", "text"),
            new DefinicionParametro(CLAVE_RUC_EMPRESA, "RUC de la empresa", "Número de RUC emisor de los comprobantes electrónicos.", "text"),
            new DefinicionParametro(CLAVE_MONEDA, "Moneda", "Símbolo de moneda usado en todo el sistema (ej. S/).", "text"),
            new DefinicionParametro(CLAVE_TASA_IGV, "Tasa de IGV (%)", "Porcentaje de impuesto aplicado a cada venta.", "number"),
            new DefinicionParametro(CLAVE_STOCK_MINIMO_DEFECTO, "Stock mínimo por defecto", "Umbral de alerta de stock bajo sugerido al crear un producto nuevo.", "number"),
            new DefinicionParametro(CLAVE_MAX_INTENTOS_LOGIN, "Intentos fallidos antes de bloquear", "Cantidad de intentos de login incorrectos que bloquean la cuenta.", "number"),
            new DefinicionParametro(CLAVE_MINUTOS_BLOQUEO_LOGIN, "Minutos de bloqueo de cuenta", "Duración del bloqueo temporal tras exceder los intentos fallidos.", "number"),
            new DefinicionParametro(CLAVE_TIMEOUT_SESION_ADMIN, "Timeout de sesión (Administrador, min)", "Minutos de inactividad antes de cerrar la sesión de un administrador.", "number"),
            new DefinicionParametro(CLAVE_TIMEOUT_SESION_VENDEDOR, "Timeout de sesión (Vendedor, min)", "Minutos de inactividad antes de cerrar la sesión de un vendedor.", "number"),
            new DefinicionParametro(CLAVE_DIAS_ROTACION_MINIMA, "Días mínimos de rotación", "Días sin venta a partir de los cuales un producto se marca como de baja rotación.", "number"),
            new DefinicionParametro(CLAVE_DIAS_ALERTA_VENCIMIENTO, "Días de alerta de vencimiento", "Días de anticipación con los que se avisa que un lote está por vencer.", "number"),
    };

    /** Clave fija del logo de la tienda en archivos_configuracion. */
    public static final String CLAVE_LOGO = "logo";

    private final ConfiguracionDAO configuracionDAO;
    private final ArchivoConfiguracionDAO archivoConfiguracionDAO;

    public ConfiguracionService() {
        this.configuracionDAO = new ConfiguracionDAO();
        this.archivoConfiguracionDAO = new ArchivoConfiguracionDAO();
    }

    /**
     * @return todos los valores actuales indexados por clave; toda clave conocida en
     *         {@link #PARAMETROS} aparece con cadena vacía si aún no existe en la base de datos
     * @throws SQLException si falla la consulta
     */
    public Map<String, String> obtenerTodos() throws SQLException {
        Map<String, String> valores = configuracionDAO.listarTodos();
        // Asegura que toda clave conocida aparezca en el formulario aunque aún no exista en BD.
        Map<String, String> resultado = new LinkedHashMap<>();
        for (DefinicionParametro def : PARAMETROS) {
            resultado.put(def.clave(), valores.getOrDefault(def.clave(), ""));
        }
        return resultado;
    }

    /**
     * Actualiza todos los parámetros presentes en el mapa recibido (las claves desconocidas
     * o ausentes del formulario se ignoran).
     *
     * @param nuevosValores mapa {@code clave -> nuevo valor} enviado desde el formulario de Configuración
     * @throws SQLException si falla alguna actualización
     */
    public void actualizarTodos(Map<String, String> nuevosValores) throws SQLException {
        for (DefinicionParametro def : PARAMETROS) {
            if (nuevosValores.containsKey(def.clave())) {
                configuracionDAO.actualizar(def.clave(), nuevosValores.get(def.clave()));
            }
        }
    }

    /**
     * Tasa de IGV vigente como fracción (ej. 0.18 para 18%), usada por VentaService en
     * cada venta para que un cambio en Configuración se aplique de inmediato.
     *
     * @return la tasa de IGV vigente como fracción decimal (18 si la clave por defecto no existe)
     * @throws SQLException si falla la consulta
     */
    public BigDecimal obtenerTasaIgvComoFraccion() throws SQLException {
        String valor = configuracionDAO.obtener(CLAVE_TASA_IGV, "18");
        return new BigDecimal(valor).movePointLeft(2);
    }

    /**
     * Días de tolerancia sin ventas antes de marcar un producto como de baja rotación.
     *
     * @return el valor vigente de {@link #CLAVE_DIAS_ROTACION_MINIMA} (15 si no está configurado)
     * @throws SQLException si falla la consulta
     */
    public int obtenerDiasRotacionMinima() throws SQLException {
        return Integer.parseInt(configuracionDAO.obtener(CLAVE_DIAS_ROTACION_MINIMA, "15"));
    }

    /**
     * Días de anticipación con los que se alerta que un lote está por vencer.
     *
     * @return el valor vigente de {@link #CLAVE_DIAS_ALERTA_VENCIMIENTO} (15 si no está configurado)
     * @throws SQLException si falla la consulta
     */
    public int obtenerDiasAlertaVencimiento() throws SQLException {
        return Integer.parseInt(configuracionDAO.obtener(CLAVE_DIAS_ALERTA_VENCIMIENTO, "15"));
    }

    /**
     * Minutos de inactividad tras los cuales debe expirar la sesión de un usuario con el
     * rol dado (política de seguridad distinta para Administrador y Vendedor). Usado por
     * {@link com.bodega.servlet.LoginServlet} para fijar {@code session.setMaxInactiveInterval}
     * al iniciar sesión, ya que el timeout fijo de web.xml no puede variar por rol.
     *
     * @param rol rol del usuario que inició sesión
     * @return minutos de timeout configurados para ese rol (15 para ADMINISTRADOR, 30 para
     *         VENDEDOR si la clave no está configurada)
     * @throws SQLException si falla la consulta
     */
    public int obtenerTimeoutSesionMinutos(com.bodega.model.Rol rol) throws SQLException {
        return rol == com.bodega.model.Rol.ADMINISTRADOR
                ? Integer.parseInt(configuracionDAO.obtener(CLAVE_TIMEOUT_SESION_ADMIN, "15"))
                : Integer.parseInt(configuracionDAO.obtener(CLAVE_TIMEOUT_SESION_VENDEDOR, "30"));
    }

    /**
     * Cantidad de intentos fallidos consecutivos que bloquean una cuenta.
     *
     * @return el valor vigente de {@link #CLAVE_MAX_INTENTOS_LOGIN} (3 si no está configurado)
     * @throws SQLException si falla la consulta
     */
    public int obtenerMaxIntentosLogin() throws SQLException {
        return Integer.parseInt(configuracionDAO.obtener(CLAVE_MAX_INTENTOS_LOGIN, "3"));
    }

    /**
     * Minutos de bloqueo temporal aplicados tras exceder {@link #obtenerMaxIntentosLogin()}.
     *
     * @return el valor vigente de {@link #CLAVE_MINUTOS_BLOQUEO_LOGIN} (15 si no está configurado)
     * @throws SQLException si falla la consulta
     */
    public int obtenerMinutosBloqueoLogin() throws SQLException {
        return Integer.parseInt(configuracionDAO.obtener(CLAVE_MINUTOS_BLOQUEO_LOGIN, "15"));
    }

    /**
     * Guarda o reemplaza el logo de la tienda.
     *
     * @param contenido      contenido binario de la imagen
     * @param contentType    tipo MIME de la imagen (ej. "image/png")
     * @param nombreOriginal nombre original del archivo subido
     * @throws SQLException si falla la inserción/actualización
     */
    public void guardarLogo(byte[] contenido, String contentType, String nombreOriginal) throws SQLException {
        archivoConfiguracionDAO.guardar(CLAVE_LOGO, contenido, contentType, nombreOriginal);
    }

    /**
     * @return el logo actual de la tienda, o {@link Optional#empty()} si nunca se subió uno
     * @throws SQLException si falla la consulta
     */
    public Optional<ArchivoBinario> obtenerLogo() throws SQLException {
        return archivoConfiguracionDAO.obtener(CLAVE_LOGO);
    }
}
