package com.bodega.filter;

import com.bodega.model.Rol;
import com.bodega.util.Constantes;
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
import java.util.List;

/**
 * Filtro de autorización basado en roles (RBAC). Mapeado en web.xml para ejecutarse
 * DESPUÉS de {@link AuthenticationFilter}, verifica que el rol del usuario en sesión
 * tenga permiso sobre el módulo solicitado. Si un VENDEDOR intenta acceder manualmente
 * a una URL exclusiva de ADMINISTRADOR, la petición se rechaza con un 403 y la vista
 * "Acceso denegado".
 */
public class AuthorizationFilter implements Filter {

    /** Prefijos de ruta exclusivos para el rol ADMINISTRADOR. */
    private static final List<String> RUTAS_SOLO_ADMINISTRADOR = List.of(
            "/usuarios",
            "/auditoria",
            "/configuracion",
            "/reportes",
            "/respaldos"
    );

    /**
     * No requiere inicialización: la lista de rutas restringidas es una constante fija.
     *
     * @param filterConfig configuración del filtro provista por el contenedor (no usada)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // Sin inicialización requerida
    }

    /**
     * Verifica el rol de la sesión contra la ruta solicitada. Si no hay sesión, delega
     * en {@link AuthenticationFilter} (que ya debió redirigir antes de llegar aquí) y
     * simplemente continúa la cadena; si hay sesión pero el rol no es ADMINISTRADOR y
     * la ruta es restringida, corta la petición con 403 y reenvía a la vista de error.
     *
     * @param request  petición entrante, convertida a {@link HttpServletRequest}
     * @param response respuesta saliente, convertida a {@link HttpServletResponse}
     * @param chain    resto de la cadena de filtros/servlet a invocar si el rol tiene permiso
     * @throws IOException      si falla el reenvío a la vista de error 403
     * @throws ServletException si falla la continuación de la cadena de filtros
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String rutaSolicitada = req.getRequestURI().substring(contextPath.length());

        HttpSession session = req.getSession(false);

        // Si no hay sesión, AuthenticationFilter ya se encargó de redirigir; aquí solo dejamos pasar.
        if (session == null || session.getAttribute(Constantes.SESSION_USUARIO_ROL) == null) {
            chain.doFilter(request, response);
            return;
        }

        Rol rolUsuario = Rol.valueOf((String) session.getAttribute(Constantes.SESSION_USUARIO_ROL));

        boolean requiereAdmin = RUTAS_SOLO_ADMINISTRADOR.stream().anyMatch(rutaSolicitada::startsWith);

        if (requiereAdmin && rolUsuario != Rol.ADMINISTRADOR) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            req.getRequestDispatcher("/WEB-INF/views/errores/error-403.jsp").forward(request, response);
            return;
        }

        chain.doFilter(request, response);
    }

    /** Sin recursos que liberar al detener la aplicación. */
    @Override
    public void destroy() {
        // Sin recursos que liberar
    }
}
