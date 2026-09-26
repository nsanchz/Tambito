package com.bodega.service;

import com.bodega.dao.TurnoCajaDAO;
import com.bodega.dao.VentaDAO;
import com.bodega.model.TurnoCaja;
import com.bodega.model.Venta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio de turnos de caja: apertura con monto inicial y cierre con arqueo
 * (efectivo esperado según las ventas del sistema vs. efectivo realmente contado por
 * el cajero). El Punto de Venta exige un turno abierto antes de permitir vender
 * (ver PosServlet), igual que una caja física real.
 */
public class TurnoCajaService {

    private final TurnoCajaDAO turnoCajaDAO;
    private final VentaDAO ventaDAO;

    public TurnoCajaService() {
        this.turnoCajaDAO = new TurnoCajaDAO();
        this.ventaDAO = new VentaDAO();
    }

    /** Resultado de abrir o cerrar un turno de caja. */
    public static class ResultadoOperacion {
        public final boolean exitoso;
        public final String mensaje;
        public final Integer turnoId;

        public ResultadoOperacion(boolean exitoso, String mensaje, Integer turnoId) {
            this.exitoso = exitoso;
            this.mensaje = mensaje;
            this.turnoId = turnoId;
        }
    }

    /**
     * @param usuarioId id del usuario
     * @return el turno abierto del usuario, o {@link Optional#empty()} si no tiene ninguno
     * @throws SQLException si falla la consulta
     */
    public Optional<TurnoCaja> obtenerTurnoAbierto(int usuarioId) throws SQLException {
        return turnoCajaDAO.buscarTurnoAbierto(usuarioId);
    }

    /**
     * Abre un nuevo turno de caja, rechazando la operación si el usuario ya tiene uno abierto.
     *
     * @param usuarioId    id del cajero que abre el turno
     * @param terminalId   terminal/caja física asociada
     * @param montoInicial fondo de caja inicial (no puede ser negativo)
     * @return resultado exitoso con el id del turno creado, o un mensaje de error de negocio
     *         (ya tiene un turno abierto, monto negativo) si no
     * @throws SQLException si falla la operación de base de datos
     */
    public ResultadoOperacion abrirTurno(int usuarioId, String terminalId, BigDecimal montoInicial) throws SQLException {
        if (turnoCajaDAO.buscarTurnoAbierto(usuarioId).isPresent()) {
            return new ResultadoOperacion(false, "Ya tiene un turno de caja abierto. Debe cerrarlo antes de abrir uno nuevo.", null);
        }
        if (montoInicial == null || montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            return new ResultadoOperacion(false, "El monto inicial de caja no puede ser negativo.", null);
        }

        int id = turnoCajaDAO.abrir(usuarioId, terminalId, montoInicial);
        return new ResultadoOperacion(true, "Turno de caja abierto correctamente con S/ " + montoInicial + " de fondo inicial.", id);
    }

    /**
     * Cierra un turno de caja calculando el arqueo: efectivo esperado (fondo inicial +
     * ventas en efectivo del turno) contra el efectivo declarado por el cajero.
     *
     * @param turnoId        id del turno a cerrar
     * @param montoDeclarado efectivo realmente contado por el cajero (no puede ser negativo)
     * @return resultado exitoso con el resumen de la diferencia (sobrante/faltante/cuadrado),
     *         o un mensaje de error de negocio (turno inexistente, ya cerrado, monto negativo) si no
     * @throws SQLException si falla alguna operación de base de datos
     */
    public ResultadoOperacion cerrarTurno(int turnoId, BigDecimal montoDeclarado) throws SQLException {
        Optional<TurnoCaja> turnoOpt = turnoCajaDAO.buscarPorId(turnoId);
        if (turnoOpt.isEmpty()) {
            return new ResultadoOperacion(false, "El turno de caja no existe.", null);
        }

        TurnoCaja turno = turnoOpt.get();
        if (turno.getEstado() == com.bodega.model.EstadoTurno.CERRADO) {
            return new ResultadoOperacion(false, "Este turno ya se encuentra cerrado.", null);
        }
        if (montoDeclarado == null || montoDeclarado.compareTo(BigDecimal.ZERO) < 0) {
            return new ResultadoOperacion(false, "El monto contado no puede ser negativo.", null);
        }

        BigDecimal ventasEfectivo = turnoCajaDAO.sumarVentasEfectivoEntre(
                turno.getUsuarioId(), turno.getFechaApertura(), LocalDateTime.now());
        BigDecimal montoEfectivoCalculado = turno.getMontoInicial().add(ventasEfectivo);
        BigDecimal diferencia = montoDeclarado.subtract(montoEfectivoCalculado);

        turnoCajaDAO.cerrar(turnoId, montoEfectivoCalculado, montoDeclarado, diferencia);

        String resumenDiferencia = diferencia.compareTo(BigDecimal.ZERO) == 0
                ? "Caja cuadrada exactamente."
                : diferencia.compareTo(BigDecimal.ZERO) > 0
                    ? "Sobrante de caja: S/ " + diferencia.abs()
                    : "Faltante de caja: S/ " + diferencia.abs();

        return new ResultadoOperacion(true, "Turno cerrado correctamente. Efectivo esperado: S/ "
                + montoEfectivoCalculado + ". " + resumenDiferencia, turnoId);
    }

    /**
     * @param usuarioId    id de cajero a filtrar, o {@code null} para listar de todos
     * @param filtroEstado estado a filtrar, o {@code null} para no filtrar
     * @return hasta 100 turnos que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<TurnoCaja> listar(Integer usuarioId, com.bodega.model.EstadoTurno filtroEstado) throws SQLException {
        return turnoCajaDAO.listar(usuarioId, filtroEstado);
    }

    /**
     * @param turnoId id del turno
     * @return el turno si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<TurnoCaja> buscarPorId(int turnoId) throws SQLException {
        return turnoCajaDAO.buscarPorId(turnoId);
    }

    /**
     * Ventas registradas por el cajero durante un turno específico, para el detalle expandible
     * del historial de turnos (visible solo para ADMINISTRADOR). El rango va desde la apertura
     * del turno hasta su cierre, o hasta el instante actual si el turno sigue abierto.
     *
     * @param turnoId id del turno a detallar
     * @return las ventas del turno, o lista vacía si el turno no existe
     * @throws SQLException si falla alguna consulta
     */
    public List<Venta> listarVentasDelTurno(int turnoId) throws SQLException {
        Optional<TurnoCaja> turnoOpt = turnoCajaDAO.buscarPorId(turnoId);
        if (turnoOpt.isEmpty()) {
            return List.of();
        }
        TurnoCaja turno = turnoOpt.get();
        LocalDateTime hasta = turno.getFechaCierre() != null ? turno.getFechaCierre() : LocalDateTime.now();
        return ventaDAO.listarPorUsuarioEntreFechas(turno.getUsuarioId(), turno.getFechaApertura(), hasta);
    }
}
