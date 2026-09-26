package com.bodega.service;

import com.bodega.dao.AuditoriaDAO;
import com.bodega.model.RegistroAuditoria;

import java.sql.SQLException;
import java.util.List;

/**
 * Registra y consulta la bitácora de auditoría. Se instancia desde cualquier Servlet
 * que ejecute una acción sensible (login, gestión de usuarios, anulación de ventas,
 * cambios de configuración) para dejar evidencia inmutable de quién hizo qué y cuándo.
 */
public class AuditoriaService {

    private final AuditoriaDAO auditoriaDAO;

    public AuditoriaService() {
        this.auditoriaDAO = new AuditoriaDAO();
    }

    /**
     * Registra un evento de auditoría. Nunca lanza una excepción propagable: si falla
     * la inserción, se registra en el log del servidor y se continúa, porque la
     * auditoría no debe interrumpir la operación de negocio que la originó.
     *
     * @param usuarioId   id del usuario que ejecutó la acción, o {@code null} si fue anónimo
     * @param accion      código de la acción (ej. "LOGIN_EXITOSO", "VENTA_ANULADA")
     * @param entidad     tipo de entidad afectada (ej. "USUARIO", "VENTA")
     * @param entidadId   id de la entidad afectada, o {@code null} si no aplica
     * @param detalle     descripción legible del evento
     * @param direccionIp dirección IP de origen de la petición
     */
    public void registrar(Integer usuarioId, String accion, String entidad, Integer entidadId,
                           String detalle, String direccionIp) {
        try {
            auditoriaDAO.registrar(usuarioId, accion, entidad, entidadId, detalle, direccionIp);
        } catch (SQLException e) {
            // La auditoría nunca debe interrumpir la operación de negocio que la originó;
            // si falla el registro, se deja constancia en el log del servidor y se continúa.
            System.err.println("No se pudo registrar el evento de auditoría: " + e.getMessage());
        }
    }

    /**
     * @param filtroAccion  código de acción a filtrar, o {@code null}/vacío para no filtrar
     * @param filtroEntidad tipo de entidad a filtrar, o {@code null}/vacío para no filtrar
     * @param textoBusqueda texto a buscar, o {@code null}/vacío para no filtrar
     * @return hasta 500 registros que cumplen los filtros
     * @throws SQLException si falla la consulta
     */
    public List<RegistroAuditoria> listar(String filtroAccion, String filtroEntidad, String textoBusqueda) throws SQLException {
        return auditoriaDAO.listar(filtroAccion, filtroEntidad, textoBusqueda);
    }
}
