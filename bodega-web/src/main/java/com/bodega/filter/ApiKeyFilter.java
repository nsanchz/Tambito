package com.bodega.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Protege exclusivamente las rutas bajo "/api/public/*": en vez de exigir una sesión de
 * navegador (como el resto de la aplicación, ver AuthenticationFilter), exige el header
 * "X-API-Key" con el valor configurado en database.properties. Permite consumir estos
 * endpoints de solo lectura desde herramientas externas (Postman, otra app) sin pasar por
 * el login HTML. Mapeado únicamente a "/api/public/*" en web.xml, nunca a "/*".
 */
public class ApiKeyFilter implements Filter {

    private static final String HEADER_API_KEY = "X-API-Key";

    /** Único endpoint bajo /api/public/* que no exige la clave: healthcheck simple. */
    private static final String RUTA_SIN_KEY = "/api/public/v1/estado";

    private String apiKeyConfigurada;

    /** Carga "api.public.key" de database.properties, igual que el resto de credenciales del proyecto. */
    @Override
    public void init(FilterConfig filterConfig) {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            System.err.println("ApiKeyFilter: error al leer database.properties: " + e.getMessage());
        }
        apiKeyConfigurada = props.getProperty("api.public.key", "").trim();
        if (apiKeyConfigurada.isEmpty()) {
            System.err.println("ApiKeyFilter: falta configurar api.public.key en database.properties. "
                    + "Todas las peticiones a /api/public/* serán rechazadas.");
        }
    }

    /**
     * Compara el header "X-API-Key" de la petición contra la clave configurada. Si coincide,
     * deja pasar la petición (que luego atraviesa AuthenticationFilter, donde "/api/public/"
     * está en la lista de rutas públicas y por tanto no exige sesión). Si no coincide, o la
     * clave no está configurada en el servidor, responde 401 sin tocar el resto de la cadena.
     *
     * @param request  petición entrante, convertida a {@link HttpServletRequest}
     * @param response respuesta saliente, convertida a {@link HttpServletResponse}
     * @param chain    resto de la cadena de filtros/servlet a invocar si la clave es válida
     * @throws IOException      si falla la escritura de la respuesta 401
     * @throws ServletException si falla la continuación de la cadena de filtros
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String contextPath = req.getContextPath();
        String rutaSolicitada = req.getRequestURI().substring(contextPath.length());

        if (RUTA_SIN_KEY.equals(rutaSolicitada)) {
            chain.doFilter(request, response);
            return;
        }

        String claveRecibida = req.getHeader(HEADER_API_KEY);

        if (apiKeyConfigurada.isEmpty() || claveRecibida == null || !apiKeyConfigurada.equals(claveRecibida)) {
            resp.setContentType("application/json;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"error\":\"No autorizado. Falta o es inválido el header X-API-Key.\"}");
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
