package com.bodega.servlet;

import com.bodega.model.Rol;
import com.bodega.model.TurnoCaja;
import com.bodega.service.TurnoCajaService;
import com.bodega.service.TurnoCajaService.ResultadoOperacion;
import com.bodega.util.Constantes;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Apertura y cierre de turno de caja (arqueo). Disponible para ambos roles. */
@WebServlet("/turno-caja")
public class TurnoCajaServlet extends HttpServlet {

    private final TurnoCajaService turnoCajaService = new TurnoCajaService();

    /**
     * Muestra el estado del turno de caja del usuario (formulario de apertura si no
     * tiene uno abierto, o de cierre/arqueo si sí), y el historial completo si es ADMINISTRADOR.
     *
     * @param req  petición HTTP
     * @param resp respuesta HTTP
     * @throws ServletException si falla la consulta a la base de datos
     * @throws IOException      si falla el reenvío a la vista
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String rolSesion = (String) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ROL);

        try {
            Optional<TurnoCaja> turnoAbierto = turnoCajaService.obtenerTurnoAbierto(usuarioIdSesion);
            req.setAttribute("turnoAbierto", turnoAbierto.orElse(null));
            req.setAttribute("requerido", "true".equals(req.getParameter("requerido")));

            if (Rol.ADMINISTRADOR.name().equals(rolSesion)) {
                List<TurnoCaja> historial = turnoCajaService.listar(null, null);
                req.setAttribute("historialTurnos", historial);

                String verTurnoId = req.getParameter("verTurno");
                if (verTurnoId != null && !verTurnoId.isBlank()) {
                    int turnoId = Integer.parseInt(verTurnoId);
                    turnoCajaService.buscarPorId(turnoId).ifPresent(t -> req.setAttribute("turnoDetalle", t));
                    req.setAttribute("ventasDelTurno", turnoCajaService.listarVentasDelTurno(turnoId));
                }
            }

            req.setAttribute("tituloPagina", "Turno de Caja");
            req.setAttribute("moduloActivo", "turno-caja");
            req.getRequestDispatcher("/WEB-INF/views/turno-caja/estado.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException("Error al consultar el turno de caja.", e);
        }
    }

    /**
     * Despacha las acciones de turno de caja ({@code abrir}, {@code cerrar}). Al abrir
     * exitosamente, redirige directo al POS.
     *
     * @param req  petición HTTP con el parámetro {@code accion} y los datos propios de cada acción
     * @param resp respuesta HTTP
     * @throws ServletException si falla la operación contra la base de datos
     * @throws IOException      si falla el redirect final
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        int usuarioIdSesion = (int) req.getSession().getAttribute(Constantes.SESSION_USUARIO_ID);
        String terminalId = (String) req.getSession().getAttribute(Constantes.SESSION_TERMINAL_ID);
        String accion = req.getParameter("accion");

        try {
            ResultadoOperacion resultado = switch (accion) {
                case "abrir" -> turnoCajaService.abrirTurno(usuarioIdSesion, terminalId,
                        new BigDecimal(req.getParameter("montoInicial")));
                case "cerrar" -> turnoCajaService.cerrarTurno(Integer.parseInt(req.getParameter("turnoId")),
                        new BigDecimal(req.getParameter("montoDeclarado")));
                default -> new ResultadoOperacion(false, "Acción no reconocida.", null);
            };

            req.getSession().setAttribute(resultado.exitoso ? Constantes.ATTR_MENSAJE : Constantes.ATTR_ERROR,
                    resultado.mensaje);

            if (resultado.exitoso && "abrir".equals(accion)) {
                resp.sendRedirect(req.getContextPath() + "/pos");
                return;
            }
        } catch (SQLException e) {
            throw new ServletException("Error al procesar la operación de turno de caja.", e);
        }

        resp.sendRedirect(req.getContextPath() + "/turno-caja");
    }
}
