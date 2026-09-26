package com.bodega.servlet;

import com.bodega.service.TipoCambioService;
import com.google.gson.Gson;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Endpoint JSON consultado desde el POS al activar "Pagar en USD": entrega el tipo de
 * cambio del día (tasa "compra" de SUNAT) para mostrarle al cajero el equivalente en
 * dólares del total antes de cobrar. Protegido por {@link com.bodega.filter.AuthenticationFilter}
 * como el resto de la app (requiere sesión activa).
 */
@WebServlet("/api/tipo-cambio")
public class TipoCambioServlet extends HttpServlet {

    private final TipoCambioService tipoCambioService = new TipoCambioService();
    private final Gson gson = new Gson();

    /** DTO serializado a JSON con el tipo de cambio del día. */
    public static class TipoCambioDTO {
        public boolean disponible;
        public String compra;
        public String venta;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        TipoCambioDTO dto = new TipoCambioDTO();

        Optional<TipoCambioService.TipoCambio> tipoCambioOpt = tipoCambioService.obtenerTipoCambioDeHoy();
        if (tipoCambioOpt.isPresent()) {
            dto.disponible = true;
            dto.compra = tipoCambioOpt.get().compra.toPlainString();
            dto.venta = tipoCambioOpt.get().venta.toPlainString();
        } else {
            dto.disponible = false;
        }
        resp.getWriter().write(gson.toJson(dto));
    }
}
