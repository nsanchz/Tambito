package com.bodega.servlet;

import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Verificación de estado de la API pública de solo lectura. Único endpoint bajo
 * "/api/public/*" que NO exige el header X-API-Key (ver ApiKeyFilter), pensado como
 * primer test desde Postman antes de usar la clave en el resto de endpoints.
 * Solo implementa doGet: no expone ninguna ruta de escritura.
 */
@WebServlet("/api/public/v1/estado")
public class PublicEstadoServlet extends HttpServlet {

    private final Gson gson = new Gson();

    /** DTO de respuesta: estado fijo "ok" (si el servlet responde, la app está arriba). */
    public static class EstadoDTO {
        public String estado = "ok";
        public String servidor = "TAMBITO";
        public String version = "1.0";
        public String horaServidor;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        EstadoDTO dto = new EstadoDTO();
        dto.horaServidor = OffsetDateTime.now().toString();
        resp.getWriter().write(gson.toJson(dto));
    }
}
