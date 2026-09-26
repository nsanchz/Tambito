package com.bodega.servlet;

import com.bodega.model.TipoDocumentoCliente;
import com.bodega.service.ConsultaDocumentoService;
import com.bodega.service.ConsultaDocumentoService.ResultadoConsulta;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Endpoint JSON consultado desde el botón "Buscar" del modal de Clientes y del registro de
 * cliente nuevo en el POS: dado un DNI/RUC ya validado en formato por el navegador, consulta
 * apisperu.com (con caché local) para autocompletar nombre/razón social y dirección.
 * Protegido por {@link com.bodega.filter.AuthenticationFilter} como el resto de la app
 * (requiere sesión activa); no requiere rol de administrador.
 */
@WebServlet("/api/consulta-documento")
public class ConsultaDocumentoServlet extends HttpServlet {

    private final ConsultaDocumentoService consultaDocumentoService = new ConsultaDocumentoService();
    private final Gson gson = new Gson();

    /** DTO serializado a JSON con el resultado de la consulta. */
    public static class ConsultaDTO {
        public boolean encontrado;
        public String nombre;
        public String direccion;
        public String mensaje;
    }

    /**
     * @param req  petición HTTP con los parámetros {@code numero} (documento) y {@code tipo} (DNI/RUC)
     * @param resp respuesta HTTP: cuerpo JSON {@code {"encontrado":bool,"nombre":str,"direccion":str}}
     * @throws IOException si falla la escritura de la respuesta
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        ConsultaDTO dto = new ConsultaDTO();

        String numero = req.getParameter("numero");
        String tipoParam = req.getParameter("tipo");

        TipoDocumentoCliente tipo;
        try {
            tipo = TipoDocumentoCliente.valueOf(tipoParam);
        } catch (IllegalArgumentException | NullPointerException e) {
            dto.encontrado = false;
            dto.mensaje = "Tipo de documento no reconocido.";
            resp.getWriter().write(gson.toJson(dto));
            return;
        }

        // Mismo formato exigido por ClienteService.validar antes de gastar cuota de la API.
        boolean formatoValido = numero != null &&
                ((tipo == TipoDocumentoCliente.DNI && numero.matches("\\d{8}")) ||
                 (tipo == TipoDocumentoCliente.RUC && numero.matches("\\d{11}")));

        if (!formatoValido) {
            dto.encontrado = false;
            dto.mensaje = tipo == TipoDocumentoCliente.DNI
                    ? "El DNI debe tener 8 dígitos."
                    : "El RUC debe tener 11 dígitos.";
            resp.getWriter().write(gson.toJson(dto));
            return;
        }

        ResultadoConsulta resultado = consultaDocumentoService.consultar(numero, tipo);
        dto.encontrado = resultado.encontrado;
        dto.nombre = resultado.nombreORazonSocial;
        dto.direccion = resultado.direccion;
        if (!resultado.encontrado) {
            dto.mensaje = "No se encontró información para este documento. Puede completar los datos manualmente.";
        }
        resp.getWriter().write(gson.toJson(dto));
    }
}
