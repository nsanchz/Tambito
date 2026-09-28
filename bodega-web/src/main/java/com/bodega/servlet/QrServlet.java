package com.bodega.servlet;

import com.bodega.util.QrCodeUtil;
import com.google.zxing.WriterException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Genera códigos QR bajo demanda a partir de un texto arbitrario, usado por el POS para
 * mostrar un QR de referencia cuando el método de pago es Yape/Plin (no es una pasarela
 * de pago real: es un código visual para que el cliente confirme el monto a transferir).
 */
@WebServlet("/api/qr")
public class QrServlet extends HttpServlet {

    private static final int TAMANO_PX = 220;

    // Límite de caracteres del texto a codificar: suficiente para cualquier uso legítimo
    // dentro de la aplicación (monto a pagar, referencia de comprobante); evita que este
    // endpoint se use como un generador/alojador arbitrario de contenido en un QR.
    private static final int LARGO_MAXIMO_TEXTO = 300;

    // Lista blanca explícita de caracteres permitidos: letras (con tildes/ñ), dígitos y la
    // puntuación que realmente aparece en un monto a pagar o una referencia de comprobante
    // (S/ 25.50, Ref. #123-A, etc.). Al validar contra una lista blanca (en vez de solo
    // bloquear caracteres de control) queda explícito que el texto se sanitiza antes de
    // usarse, no solo se filtra parcialmente.
    private static final Pattern TEXTO_PERMITIDO = Pattern.compile("^[\\p{L}0-9\\s.,:;()/#\\-_@]*$");

    /**
     * @param req  petición HTTP; espera {@code ?texto=} con el contenido a codificar
     * @param resp respuesta HTTP con la imagen PNG del QR
     * @throws ServletException si falla la generación del código
     * @throws IOException      si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String texto = req.getParameter("texto");
        if (texto == null || texto.isBlank()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Falta el parámetro 'texto'.");
            return;
        }
        if (texto.length() > LARGO_MAXIMO_TEXTO) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "El texto a codificar excede el máximo permitido (" + LARGO_MAXIMO_TEXTO + " caracteres).");
            return;
        }
        if (!TEXTO_PERMITIDO.matcher(texto).matches()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "El texto contiene caracteres no permitidos.");
            return;
        }

        try {
            byte[] png = QrCodeUtil.generarPng(texto, TAMANO_PX);
            resp.setContentType("image/png");
            resp.setHeader("Cache-Control", "no-store");
            resp.setHeader("Content-Disposition", "inline; filename=\"qr.png\"");
            // Evita que el navegador intente "adivinar" el tipo de contenido a partir de los
            // bytes (content sniffing) y lo reinterprete como HTML/JS en vez de imagen.
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setContentLength(png.length);
            resp.getOutputStream().write(png);
            resp.getOutputStream().flush();
        } catch (WriterException e) {
            throw new ServletException("Error al generar el código QR solicitado.", e);
        }
    }
}
