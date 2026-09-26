package com.bodega.service;

import com.bodega.dao.CacheConsultaDocumentoDAO;
import com.bodega.model.TipoDocumentoCliente;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

/**
 * Consulta datos públicos de RUC/DNI en apisperu.com (https://dniruc.apisperu.com) para
 * autocompletar el nombre/razón social y dirección al registrar un Cliente. Es un servicio
 * gratuito de terceros (no oficial de SUNAT/RENIEC): por diseño, CUALQUIER falla de este
 * servicio (timeout, token inválido, cuota agotada, cambio de formato de respuesta) se
 * degrada de forma silenciosa a "no encontrado" y NUNCA impide el registro manual del
 * cliente — ver {@code ClienteServlet}/{@code PosServlet}, que tratan el resultado vacío
 * como "el usuario completa los datos a mano".
 * <p>
 * Antes de llamar a la API externa, se revisa {@link CacheConsultaDocumentoDAO} para no
 * repetir una consulta ya hecha recientemente (cuida la cuota mensual gratuita del servicio).
 */
public class ConsultaDocumentoService {

    private static final String BASE_URL = "https://dniruc.apisperu.com/api/v1";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final String token;
    private final HttpClient httpClient;
    private final CacheConsultaDocumentoDAO cacheDAO;

    public ConsultaDocumentoService() {
        this.token = cargarToken();
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.cacheDAO = new CacheConsultaDocumentoDAO();
    }

    /** Resultado de una consulta de documento. */
    public static class ResultadoConsulta {
        public final boolean encontrado;
        public final String nombreORazonSocial;
        public final String direccion;

        private ResultadoConsulta(boolean encontrado, String nombreORazonSocial, String direccion) {
            this.encontrado = encontrado;
            this.nombreORazonSocial = nombreORazonSocial;
            this.direccion = direccion;
        }

        static ResultadoConsulta noEncontrado() {
            return new ResultadoConsulta(false, null, null);
        }

        static ResultadoConsulta de(String nombreORazonSocial, String direccion) {
            return new ResultadoConsulta(true, nombreORazonSocial, direccion);
        }
    }

    /**
     * Consulta un DNI o RUC, usando primero la caché local (30 días de vigencia) y solo
     * llamando a apisperu.com si no hay caché vigente.
     *
     * @param numeroDocumento número exacto (8 dígitos DNI / 11 dígitos RUC; se asume ya
     *                        validado por el llamador, ver {@code ClienteService.validar})
     * @param tipo            DNI o RUC
     * @return el resultado; {@code encontrado = false} tanto si el documento no existe como
     *         si el servicio externo no está disponible por cualquier motivo
     */
    public ResultadoConsulta consultar(String numeroDocumento, TipoDocumentoCliente tipo) {
        try {
            Optional<CacheConsultaDocumentoDAO.Entrada> cacheado = cacheDAO.buscarVigente(numeroDocumento);
            if (cacheado.isPresent()) {
                return ResultadoConsulta.de(cacheado.get().nombreORazonSocial, cacheado.get().direccion);
            }
        } catch (SQLException e) {
            // Un fallo leyendo la caché no debe impedir intentar la consulta en vivo.
            System.err.println("ConsultaDocumentoService: no se pudo leer la caché: " + e.getMessage());
        }

        if (token == null || token.isBlank()) {
            System.err.println("ConsultaDocumentoService: falta configurar api.rucdni.token en database.properties.");
            return ResultadoConsulta.noEncontrado();
        }

        String endpoint = tipo == TipoDocumentoCliente.DNI
                ? BASE_URL + "/dni/" + numeroDocumento
                : BASE_URL + "/ruc/" + numeroDocumento;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(TIMEOUT)
                    .header("Authorization", "Bearer " + token)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("ConsultaDocumentoService: respuesta HTTP " + response.statusCode()
                        + " al consultar " + tipo + " (posible token inválido o cuota agotada).");
                return ResultadoConsulta.noEncontrado();
            }

            ResultadoConsulta resultado = parsear(response.body(), tipo);
            if (resultado.encontrado) {
                try {
                    cacheDAO.guardar(numeroDocumento, tipo, resultado.nombreORazonSocial, resultado.direccion);
                } catch (SQLException e) {
                    // No pasa nada si no se pudo cachear: la próxima vez simplemente se
                    // vuelve a consultar en vivo. Nunca se le muestra este error al usuario.
                    System.err.println("ConsultaDocumentoService: no se pudo guardar en caché: " + e.getMessage());
                }
            }
            return resultado;

        } catch (IOException | InterruptedException e) {
            // Timeout, DNS caído, red intermitente, etc.: se degrada a "no encontrado".
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("ConsultaDocumentoService: error de red al consultar " + tipo + ": " + e.getMessage());
            return ResultadoConsulta.noEncontrado();
        }
    }

    /**
     * Interpreta la respuesta JSON de apisperu.com de forma defensiva: como es un servicio de
     * terceros no oficial, cualquier campo inesperado o ausente se trata como "no encontrado"
     * en vez de propagar una excepción.
     */
    private ResultadoConsulta parsear(String cuerpoJson, TipoDocumentoCliente tipo) {
        try {
            JsonObject json = JsonParser.parseString(cuerpoJson).getAsJsonObject();

            if (json.has("success") && !json.get("success").getAsBoolean()) {
                return ResultadoConsulta.noEncontrado();
            }

            if (tipo == TipoDocumentoCliente.DNI) {
                String nombres = textoDe(json, "nombres");
                String apPaterno = textoDe(json, "apellidoPaterno");
                String apMaterno = textoDe(json, "apellidoMaterno");
                if (nombres == null) {
                    return ResultadoConsulta.noEncontrado();
                }
                String nombreCompleto = String.join(" ",
                        valorOVacio(nombres), valorOVacio(apPaterno), valorOVacio(apMaterno)).trim()
                        .replaceAll("\\s+", " ");
                return ResultadoConsulta.de(nombreCompleto, null);
            } else {
                String razonSocial = textoDe(json, "razonSocial");
                if (razonSocial == null) {
                    return ResultadoConsulta.noEncontrado();
                }
                String direccion = textoDe(json, "direccion");
                return ResultadoConsulta.de(razonSocial, direccion);
            }
        } catch (JsonSyntaxException | IllegalStateException e) {
            System.err.println("ConsultaDocumentoService: respuesta de apisperu.com con formato inesperado: "
                    + e.getMessage());
            return ResultadoConsulta.noEncontrado();
        }
    }

    private String textoDe(JsonObject json, String campo) {
        return json.has(campo) && !json.get(campo).isJsonNull() ? json.get(campo).getAsString() : null;
    }

    private String valorOVacio(String s) {
        return s == null ? "" : s;
    }

    /** Carga {@code api.rucdni.token} de database.properties, igual que el resto de credenciales del proyecto. */
    private String cargarToken() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                return null;
            }
            props.load(input);
            return props.getProperty("api.rucdni.token");
        } catch (IOException e) {
            return null;
        }
    }
}
