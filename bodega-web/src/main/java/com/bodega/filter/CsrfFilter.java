package com.bodega.filter;

import com.bodega.util.Constantes;
import com.bodega.util.CsrfUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Protección contra CSRF (Cross-Site Request Forgery) mediante el patrón "synchronizer
 * token": cada {@link HttpSession} tiene un token aleatorio propio (ver {@link CsrfUtil}),
 * expuesto a las vistas JSP como {@code sessionScope.csrfToken} para incluirlo como campo
 * oculto en cada formulario. Toda petición POST debe traer ese mismo token de vuelta en el
 * parámetro {@code csrfToken}; si no coincide (o falta), la petición se rechaza con 403
 * ANTES de llegar al Servlet de destino.
 * <p>
 * Se ejecuta después de {@link AuthenticationFilter} y {@link AuthorizationFilter} (mapeado
 * al final en web.xml): para cuando llega aquí, la sesión ya está validada, así que el
 * único trabajo de este filtro es la validación anti-falsificación propiamente dicha.
 */
public class CsrfFilter implements Filter {

    /** Rutas POST que no requieren token porque ocurren antes de que exista una sesión autenticada. */
    private static final String[] RUTAS_EXENTAS = {
            "/login"
    };

    @Override
    public void init(FilterConfig filterConfig) {
        // Sin inicialización requerida
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String ruta = req.getRequestURI().substring(contextPath.length());
        HttpSession session = req.getSession(false);

        if ("POST".equalsIgnoreCase(req.getMethod()) && !esRutaExenta(ruta)) {
            String tokenSesion = session != null ? (String) session.getAttribute(Constantes.SESSION_CSRF_TOKEN) : null;
            String tokenRecibido = req.getParameter("csrfToken");

            if (!tokenValido(tokenSesion, tokenRecibido)) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Token de seguridad inválido o expirado. Recargue la página e intente nuevamente.");
                return;
            }
        }

        // Asegura que toda sesión autenticada tenga un token disponible para que las vistas
        // lo rendericen en sus formularios (se genera aquí de forma perezosa: cubre tanto la
        // sesión recién creada tras el login como una sesión antigua sin token, ej. tras un
        // redeploy que haya conservado sesiones tolerantes a cambios de clase).
        if (session != null && session.getAttribute(Constantes.SESSION_CSRF_TOKEN) == null) {
            session.setAttribute(Constantes.SESSION_CSRF_TOKEN, CsrfUtil.generarToken());
        }

        chain.doFilter(request, response);
    }

    private boolean esRutaExenta(String ruta) {
        for (String exenta : RUTAS_EXENTAS) {
            if (ruta.startsWith(exenta)) {
                return true;
            }
        }
        return false;
    }

    /** Comparación en tiempo constante: evita filtrar el token por diferencias de tiempo de respuesta. */
    private boolean tokenValido(String tokenSesion, String tokenRecibido) {
        if (tokenSesion == null || tokenRecibido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                tokenSesion.getBytes(StandardCharsets.UTF_8),
                tokenRecibido.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
        // Sin recursos que liberar
    }
}
