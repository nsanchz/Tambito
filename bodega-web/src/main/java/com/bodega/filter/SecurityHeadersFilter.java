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

/**
 * Agrega cabeceras HTTP de seguridad a toda respuesta, primera línea de defensa contra
 * clickjacking, sniffing de MIME type y para reducir el radio de daño de un XSS que
 * pudiera colarse en el futuro. Se ejecuta en todas las rutas (incluidas las públicas
 * como /login), ya que estas cabeceras protegen al navegador del cliente, no dependen
 * de si hay sesión autenticada.
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // Sin inicialización requerida
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Clickjacking: prohíbe que cualquier sitio embeba esta aplicación en un <iframe>.
        resp.setHeader("X-Frame-Options", "DENY");

        // Evita que el navegador intente "adivinar" el tipo de contenido de una respuesta
        // (ej. tratar un .txt subido como HTML ejecutable).
        resp.setHeader("X-Content-Type-Options", "nosniff");

        // No filtra la URL completa de origen a sitios de terceros al seguir un enlace saliente.
        resp.setHeader("Referrer-Policy", "same-origin");

        // Desactiva APIs del navegador que esta aplicación no usa, por si un script inyectado
        // intentara abusarlas (cámara, micrófono, geolocalización).
        resp.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Content-Security-Policy: solo el propio origen puede cargar scripts/estilos/imágenes,
        // salvo Tailwind (CDN ya usado por todas las vistas) e imágenes en base64 (QR embebidos).
        // 'unsafe-inline' en script/style es necesario porque las vistas usan JS y clases inline
        // (Tailwind vía CDN); es una limitación conocida documentada, no un descuido.
        resp.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                        + "script-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; "
                        + "style-src 'self' 'unsafe-inline' https://cdn.tailwindcss.com; "
                        + "img-src 'self' data:; "
                        + "frame-ancestors 'none'; "
                        + "base-uri 'self'; "
                        + "form-action 'self'");

        // HSTS: solo tiene efecto (y solo se envía) cuando la conexión ya llegó por HTTPS;
        // enviarlo sobre HTTP puro no tiene sentido y algunos navegadores lo ignoran igual.
        if (req.isSecure()) {
            resp.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        // Oculta la versión exacta de Tomcat expuesta por defecto en la cabecera "Server"
        // (reduce la información disponible para un atacante que haga fingerprinting).
        resp.setHeader("Server", "Servidor Web");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Sin recursos que liberar
    }
}
