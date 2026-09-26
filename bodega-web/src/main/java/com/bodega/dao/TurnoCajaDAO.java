package com.bodega.dao;

import com.bodega.config.DatabaseConfig;
import com.bodega.model.EstadoTurno;
import com.bodega.model.TurnoCaja;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Acceso a datos de los turnos de caja (apertura, cierre y arqueo). */
public class TurnoCajaDAO {

    /**
     * Abre un nuevo turno de caja en estado ABIERTO.
     *
     * @param usuarioId    id del cajero que abre el turno
     * @param terminalId   terminal/caja física asociada a la sesión (ej. "TIENDA")
     * @param montoInicial fondo de caja con el que se abre el turno
     * @return el id autogenerado del turno
     * @throws SQLException si falla la inserción o no se pudo recuperar el id generado
     */
    public int abrir(int usuarioId, String terminalId, BigDecimal montoInicial) throws SQLException {
        String sql = "INSERT INTO turnos_caja (usuario_id, terminal_id, monto_inicial, estado, fecha_apertura) " +
                "VALUES (?, ?, ?, 'ABIERTO', ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, usuarioId);
            ps.setString(2, terminalId);
            ps.setBigDecimal(3, montoInicial);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("No se pudo obtener el ID generado al abrir el turno de caja.");
    }

    /**
     * Busca el turno actualmente abierto de un usuario, si existe. El POS exige que
     * exista uno antes de permitir vender (ver PosServlet).
     *
     * @param usuarioId id del usuario
     * @return el turno abierto más reciente, o {@link Optional#empty()} si no tiene ninguno
     * @throws SQLException si falla la consulta
     */
    public Optional<TurnoCaja> buscarTurnoAbierto(int usuarioId) throws SQLException {
        String sql = "SELECT t.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos FROM turnos_caja t " +
                "JOIN usuarios u ON u.id = t.usuario_id WHERE t.usuario_id = ? AND t.estado = 'ABIERTO' " +
                "ORDER BY t.fecha_apertura DESC LIMIT 1";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * @param id id del turno
     * @return el turno si existe, o {@link Optional#empty()} si no
     * @throws SQLException si falla la consulta
     */
    public Optional<TurnoCaja> buscarPorId(int id) throws SQLException {
        String sql = "SELECT t.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos FROM turnos_caja t " +
                "JOIN usuarios u ON u.id = t.usuario_id WHERE t.id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Cierra un turno de caja con el resultado del arqueo ya calculado por
     * {@link com.bodega.service.TurnoCajaService}.
     *
     * @param turnoId                id del turno a cerrar
     * @param montoEfectivoCalculado efectivo esperado según el sistema (fondo inicial + ventas en efectivo del turno)
     * @param montoDeclarado         efectivo realmente contado por el cajero
     * @param diferencia             {@code montoDeclarado - montoEfectivoCalculado} (negativo = faltante, positivo = sobrante)
     * @throws SQLException si falla la actualización
     */
    public void cerrar(int turnoId, BigDecimal montoEfectivoCalculado, BigDecimal montoDeclarado, BigDecimal diferencia) throws SQLException {
        String sql = "UPDATE turnos_caja SET estado = 'CERRADO', monto_efectivo_calculado = ?, " +
                "monto_declarado = ?, diferencia = ?, fecha_cierre = ? WHERE id = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, montoEfectivoCalculado);
            ps.setBigDecimal(2, montoDeclarado);
            ps.setBigDecimal(3, diferencia);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(5, turnoId);
            ps.executeUpdate();
        }
    }

    /**
     * Suma de ventas en efectivo completadas por un usuario dentro de un rango (base del arqueo).
     * Las ventas anuladas y las pagadas con otro método no se cuentan como efectivo.
     *
     * @param usuarioId id del cajero
     * @param desde     inicio del rango (normalmente la fecha de apertura del turno), inclusive
     * @param hasta     fin del rango (normalmente el instante del cierre), inclusive
     * @return total en efectivo vendido en el rango
     * @throws SQLException si falla la consulta
     */
    public BigDecimal sumarVentasEfectivoEntre(int usuarioId, LocalDateTime desde, LocalDateTime hasta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(total), 0) FROM ventas WHERE usuario_id = ? AND metodo_pago = 'EFECTIVO' " +
                "AND estado = 'COMPLETADA' AND fecha_creacion BETWEEN ? AND ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, usuarioId);
            ps.setTimestamp(2, Timestamp.valueOf(desde));
            ps.setTimestamp(3, Timestamp.valueOf(hasta));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }

    /**
     * Lista turnos de caja aplicando filtros opcionales, para el historial visible al administrador.
     *
     * @param usuarioId    id de cajero a filtrar, o {@code null} para listar de todos
     * @param filtroEstado estado a filtrar (ABIERTO/CERRADO), o {@code null} para no filtrar
     * @return hasta 100 turnos que cumplen los filtros, ordenados por fecha de apertura descendente
     * @throws SQLException si falla la consulta
     */
    public List<TurnoCaja> listar(Integer usuarioId, EstadoTurno filtroEstado) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT t.*, u.nombres AS usuario_nombres, u.apellidos AS usuario_apellidos FROM turnos_caja t " +
                        "JOIN usuarios u ON u.id = t.usuario_id WHERE 1=1");
        List<Object> parametros = new ArrayList<>();

        if (usuarioId != null) {
            sql.append(" AND t.usuario_id = ?");
            parametros.add(usuarioId);
        }
        if (filtroEstado != null) {
            sql.append(" AND t.estado = ?");
            parametros.add(filtroEstado.name());
        }
        sql.append(" ORDER BY t.fecha_apertura DESC LIMIT 100");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<TurnoCaja> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
                return resultado;
            }
        }
    }

    private TurnoCaja mapear(ResultSet rs) throws SQLException {
        TurnoCaja t = new TurnoCaja();
        t.setId(rs.getInt("id"));
        t.setUsuarioId(rs.getInt("usuario_id"));
        t.setUsuarioNombre(rs.getString("usuario_nombres") + " " + rs.getString("usuario_apellidos"));
        t.setTerminalId(rs.getString("terminal_id"));
        t.setMontoInicial(rs.getBigDecimal("monto_inicial"));
        t.setMontoEfectivoCalculado(rs.getBigDecimal("monto_efectivo_calculado"));
        t.setMontoDeclarado(rs.getBigDecimal("monto_declarado"));
        t.setDiferencia(rs.getBigDecimal("diferencia"));
        t.setEstado(EstadoTurno.valueOf(rs.getString("estado")));

        Timestamp fechaApertura = rs.getTimestamp("fecha_apertura");
        t.setFechaApertura(fechaApertura != null ? fechaApertura.toLocalDateTime() : null);

        Timestamp fechaCierre = rs.getTimestamp("fecha_cierre");
        t.setFechaCierre(fechaCierre != null ? fechaCierre.toLocalDateTime() : null);

        return t;
    }
}
