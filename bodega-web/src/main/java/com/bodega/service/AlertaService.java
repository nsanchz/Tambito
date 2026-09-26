package com.bodega.service;

import com.bodega.dao.LoteProductoDAO;
import com.bodega.dao.ProductoDAO;
import com.bodega.model.LoteProducto;
import com.bodega.model.Producto;

import java.sql.SQLException;
import java.util.List;

/**
 * Alertas de inventario derivadas de los lotes y del historial de ventas: productos de
 * baja rotación (sin ventas recientes) y lotes próximos a vencer. Los umbrales de ambas
 * alertas son configurables desde el panel de administrador (ver {@link ConfiguracionService}).
 */
public class AlertaService {

    private final ProductoDAO productoDAO;
    private final LoteProductoDAO loteProductoDAO;
    private final ConfiguracionService configuracionService;

    public AlertaService() {
        this.productoDAO = new ProductoDAO();
        this.loteProductoDAO = new LoteProductoDAO();
        this.configuracionService = new ConfiguracionService();
    }

    /**
     * @return productos activos sin ninguna venta completada en los últimos N días
     *         (N = {@link ConfiguracionService#obtenerDiasRotacionMinima()})
     * @throws SQLException si falla la consulta
     */
    public List<Producto> productosBajaRotacion() throws SQLException {
        int dias = configuracionService.obtenerDiasRotacionMinima();
        return productoDAO.listarSinVentaReciente(dias);
    }

    /**
     * @return lotes con stock disponible cuya fecha de vencimiento cae dentro de los
     *         próximos N días (N = {@link ConfiguracionService#obtenerDiasAlertaVencimiento()})
     * @throws SQLException si falla la consulta
     */
    public List<LoteProducto> lotesPorVencer() throws SQLException {
        int dias = configuracionService.obtenerDiasAlertaVencimiento();
        return loteProductoDAO.listarPorVencer(dias);
    }
}
