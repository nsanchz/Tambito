package com.bodega.dao;

import com.bodega.config.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Consultas de reportes recurrentes respaldadas por vistas SQL (ver schema.sql),
 * para no reconstruir el agregado en cada llamada. Se agrupan en un DAO propio,
 * igual que {@link DashboardDAO}, porque no pertenecen a ninguna entidad concreta.
 */
public class ReporteDAO {

    /** Fila de la vista vista_top_productos_categoria: un producto y su posición dentro de su categoría. */
    public record TopProductoCategoria(int categoriaId, String categoriaNombre, int productoId,
                                        String productoNombre, int cantidadVendida,
                                        BigDecimal totalVendido, int rankingCategoria) {
    }

    /**
     * @param topN cantidad de productos a devolver por categoría (ej. 5 = "top 5 por categoría")
     * @return los productos con {@code ranking_categoria <= topN} de la vista
     *         vista_top_productos_categoria, ordenados por categoría y posición
     * @throws SQLException si falla la consulta
     */
    public List<TopProductoCategoria> topProductosPorCategoria(int topN) throws SQLException {
        String sql = "SELECT * FROM vista_top_productos_categoria WHERE ranking_categoria <= ? " +
                "ORDER BY categoria_nombre ASC, ranking_categoria ASC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                List<TopProductoCategoria> resultado = new ArrayList<>();
                while (rs.next()) {
                    resultado.add(new TopProductoCategoria(
                            rs.getInt("categoria_id"),
                            rs.getString("categoria_nombre"),
                            rs.getInt("producto_id"),
                            rs.getString("producto_nombre"),
                            rs.getInt("cantidad_vendida"),
                            rs.getBigDecimal("total_vendido"),
                            rs.getInt("ranking_categoria")));
                }
                return resultado;
            }
        }
    }
}
