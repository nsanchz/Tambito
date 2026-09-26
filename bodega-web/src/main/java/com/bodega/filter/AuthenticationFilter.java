package com.bodega.filter;

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

/**
 * Filtro de autenticación: intercepta TODAS las peticiones a la aplicación y verifica
 * que exista una HttpSession válida con un usuario autenticado. Las rutas públicas
 * (login, recursos estáticos) quedan excluidas.
 * Mapeado explícitamente en web.xml (y no vía @WebFilter) para garantizar que se
 * ejecute ANTES que AuthorizationFilter.
 */
public class AuthenticationFilter implements Filter {

    private static final String[] RUTAS_PUBLICAS = {
            "/login",
            "/assets/",
            "/imagen",
            "/WEB-INF/views/errores/"
    };

    /**
     * No requiere inicialización: las rutas públicas están fijadas como constante
     * y la verificación de sesión no depende de parámetros de despliegue.
     *
     * @param filterConfig configuración del filtro provista por el contenedor (no usada)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // Sin inicialización requerida
    }

    /**
     * Deja pasar la petición si la ruta es pública o si existe una {@link HttpSession}
     * con un usuario autenticado; en caso contrario, redirige a {@code /login}.
     *
     * @param request  petición entrante, convertida a {@link HttpServletRequest}
     * @param response respuesta saliente, convertida a {@link HttpServletResponse}
     * @param chain    resto de la cadena de filtros/servlet a invocar si la petición pasa
     * @throws IOException      si falla el redirect a login
     * @throws ServletException si falla la continuación de la cadena de filtros
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String rutaSolicitada = req.getRequestURI().substring(contextPath.length());

        if (esRutaPublica(rutaSolicitada)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = req.getSession(false);
        boolean autenticado = session != null && session.getAttribute(Constantes.SESSION_USUARIO_ID) != null;

        if (!autenticado) {
            resp.sendRedirect(contextPath + "/login?expirada=true");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * @param ruta ruta solicitada, ya sin el context path
     * @return {@code true} si la ruta empieza con alguno de los prefijos de {@link #RUTAS_PUBLICAS}
     */
    private boolean esRutaPublica(String ruta) {
        for (String publica : RUTAS_PUBLICAS) {
            if (ruta.startsWith(publica)) {
                return true;
            }
        }
        return false;
    }

    /** Sin recursos que liberar al detener la aplicación. */
    @Override
    public void destroy() {
        // Sin recursos que liberar
    }
}
