package com.bodega.service;

import com.bodega.dao.DashboardDAO;
import com.bodega.dao.ProductoDAO;
import com.bodega.dao.VentaDAO;
import com.bodega.model.Producto;
import com.bodega.model.Venta;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/** Agrega la información resumida que muestran los paneles de control de administrador y vendedor. */
public class DashboardService {

    private final DashboardDAO dashboardDAO;
    private final ProductoDAO productoDAO;
    private final VentaDAO ventaDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
        this.productoDAO = new ProductoDAO();
        this.ventaDAO = new VentaDAO();
    }

    /**
     * DTO expuesto a JSP vía JSTL/EL: aunque los campos son públicos para uso interno
     * cómodo desde Java, EL (BeanELResolver) SOLO resuelve "${resumen.ventasHoy}" a través
     * de un getter con convención JavaBean (getVentasHoy()); los campos públicos por sí
     * solos no son visibles para la vista.
     */
    public static class ResumenAdministrador {
        public BigDecimal ventasHoy;
        public BigDecimal ventasMes;
        public int productosActivos;
        public int productosStockBajo;
        public int clientesActivos;
        public int ordenesCompraPendientes;
        public int ventasAnuladasMes;
        public List<Producto> productosStockBajoDetalle;
        public List<Venta> ultimasVentas;

        public BigDecimal getVentasHoy() {
            return ventasHoy;
        }

        public BigDecimal getVentasMes() {
            return ventasMes;
        }

        public int getProductosActivos() {
            return productosActivos;
        }

        public int getProductosStockBajo() {
            return productosStockBajo;
        }

        public int getClientesActivos() {
            return clientesActivos;
        }

        public int getOrdenesCompraPendientes() {
            return ordenesCompraPendientes;
        }

        public int getVentasAnuladasMes() {
            return ventasAnuladasMes;
        }

        public List<Producto> getProductosStockBajoDetalle() {
            return productosStockBajoDetalle;
        }

        public List<Venta> getUltimasVentas() {
            return ultimasVentas;
        }
    }

    public static class ResumenVendedor {
        public BigDecimal ventasHoy;
        public int cantidadVentasHoy;
        public List<Venta> ultimasVentasPropias;

        public BigDecimal getVentasHoy() {
            return ventasHoy;
        }

        public int getCantidadVentasHoy() {
            return cantidadVentasHoy;
        }

        public List<Venta> getUltimasVentasPropias() {
            return ultimasVentasPropias;
        }
    }

    /**
     * @return KPIs y listas resumidas para el panel de control del ADMINISTRADOR
     *         (ventas globales de hoy/mes, catálogo, stock bajo, clientes, órdenes de compra pendientes)
     * @throws SQLException si falla alguna de las consultas agregadas
     */
    public ResumenAdministrador obtenerResumenAdministrador() throws SQLException {
        ResumenAdministrador r = new ResumenAdministrador();
        r.ventasHoy = dashboardDAO.totalVentasHoy(null);
        r.ventasMes = dashboardDAO.totalVentasMes(null);
        r.productosActivos = dashboardDAO.contarProductosActivos();
        r.productosStockBajo = dashboardDAO.contarProductosStockBajo();
        r.clientesActivos = dashboardDAO.contarClientesActivos();
        r.ordenesCompraPendientes = dashboardDAO.contarOrdenesCompraPendientes();
        r.ventasAnuladasMes = dashboardDAO.contarVentasAnuladasMes();
        r.productosStockBajoDetalle = productoDAO.listarConStockBajoMinimo();
        r.ultimasVentas = ventaDAO.listar(null, null, null, null).stream().limit(8).toList();
        return r;
    }

    /**
     * @param usuarioId id del vendedor en sesión
     * @return KPIs y últimas ventas acotados a las propias del vendedor (nunca las de otros)
     * @throws SQLException si falla alguna de las consultas agregadas
     */
    public ResumenVendedor obtenerResumenVendedor(int usuarioId) throws SQLException {
        ResumenVendedor r = new ResumenVendedor();
        r.ventasHoy = dashboardDAO.totalVentasHoy(usuarioId);
        r.cantidadVentasHoy = dashboardDAO.contarVentasHoy(usuarioId);
        r.ultimasVentasPropias = ventaDAO.listar(null, null, usuarioId, null).stream().limit(5).toList();
        return r;
    }
}
