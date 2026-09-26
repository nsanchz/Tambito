package com.bodega.service;

import com.bodega.dao.CacheTipoCambioDAO;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

/**
 * Tipo de cambio USD del día (apisperu.com, fuente SUNAT: https://tipocambio.apisperu.com),
 * usado para permitir que un cliente pague en efectivo en dólares una venta cuyo total
 * siempre se calcula y se guarda en soles (ver comentario en {@code ventas.moneda_pago}
 * de schema.sql — el dólar nunca es la moneda contable, solo la forma física de pago).
 * <p>
 * Mismo criterio de resiliencia que {@link ConsultaDocumentoService}: cualquier falla
 * (token vacío, timeout, formato inesperado) se degrada a "no disponible" y el POS
 * simplemente no ofrece la opción de pagar en USD ese momento — nunca bloquea una venta.
 */
public class TipoCambioService {

    private static final String URL_SUNAT = "https://tipocambio.apisperu.com/api/v1/sunat";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String token;
    private final HttpClient httpClient;
    private final CacheTipoCambioDAO cacheDAO;

    public TipoCambioService() {
        this.token = cargarToken();
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.cacheDAO = new CacheTipoCambioDAO();
    }

    /** Tipo de cambio USD vigente (tasas "compra" y "venta" de SUNAT). */
    public static class TipoCambio {
        public final BigDecimal compra;
        public final BigDecimal venta;

        public TipoCambio(BigDecimal compra, BigDecimal venta) {
            this.compra = compra;
            this.venta = venta;
        }
    }

    /**
     * @return el tipo de cambio de hoy, usando primero la caché local (vigente todo el día
     *         calendario) y solo llamando a apisperu.com si no se ha consultado hoy;
     *         {@link Optional#empty()} si no se pudo obtener por ningún medio
     */
    public Optional<TipoCambio> obtenerTipoCambioDeHoy() {
        try {
            Optional<CacheTipoCambioDAO.Entrada> cacheado = cacheDAO.buscarDeHoy();
            if (cacheado.isPresent()) {
                return Optional.of(new TipoCambio(cacheado.get().compra, cacheado.get().venta));
            }
        } catch (SQLException e) {
            System.err.println("TipoCambioService: no se pudo leer la caché: " + e.getMessage());
        }

        if (token == null || token.isBlank()) {
            System.err.println("TipoCambioService: falta configurar api.tipocambio.token en database.properties.");
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(URL_SUNAT))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("TipoCambioService: respuesta HTTP " + response.statusCode()
                        + " al consultar el tipo de cambio.");
                return Optional.empty();
            }

            Optional<TipoCambio> resultado = parsear(response.body());
            resultado.ifPresent(tc -> {
                try {
                    cacheDAO.guardarDeHoy(tc.compra, tc.venta);
                } catch (SQLException e) {
                    System.err.println("TipoCambioService: no se pudo guardar en caché: " + e.getMessage());
                }
            });
            return resultado;

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("TipoCambioService: error de red al consultar el tipo de cambio: " + e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<TipoCambio> parsear(String cuerpoJson) {
        try {
            JsonObject json = JsonParser.parseString(cuerpoJson).getAsJsonObject();
            if (json.has("success") && !json.get("success").getAsBoolean()) {
                return Optional.empty();
            }
            JsonObject rates = json.getAsJsonObject("rates");
            if (rates == null || !rates.has("USD")) {
                return Optional.empty();
            }
            JsonObject usd = rates.getAsJsonObject("USD");
            BigDecimal compra = new BigDecimal(usd.get("buy").getAsString());
            BigDecimal venta = new BigDecimal(usd.get("sell").getAsString());
            return Optional.of(new TipoCambio(compra, venta));
        } catch (JsonSyntaxException | IllegalStateException | NumberFormatException | NullPointerException e) {
            System.err.println("TipoCambioService: respuesta con formato inesperado: " + e.getMessage());
            return Optional.empty();
        }
    }

    private String cargarToken() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                return null;
            }
            props.load(input);
            return props.getProperty("api.tipocambio.token");
        } catch (IOException e) {
            return null;
        }
    }
}
