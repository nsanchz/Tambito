<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6">
    <div>
        <h2 class="text-2xl font-bold">Reportes Gerenciales y Análisis Financiero</h2>
        <p class="text-sm text-slate-500">Indicadores clave y reportes descargables en PDF.</p>
    </div>

    <!-- KPIs -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas de hoy</span>
            <span class="text-2xl font-bold">S/ <c:out value="${resumen.ventasHoy}"/></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas del mes</span>
            <span class="text-2xl font-bold">S/ <c:out value="${resumen.ventasMes}"/></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas anuladas (mes)</span>
            <span class="text-2xl font-bold text-red-600"><c:out value="${resumen.ventasAnuladasMes}"/></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Productos con stock bajo</span>
            <span class="text-2xl font-bold text-amber-600"><c:out value="${resumen.productosStockBajo}"/></span>
        </div>
    </div>

    <!-- Reporte de ventas -->
    <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
        <h3 class="font-semibold">Reporte de Ventas (PDF)</h3>
        <p class="text-sm text-slate-500">Detalle de comprobantes emitidos en un rango de fechas, con totales facturados y anulados.</p>
        <form method="get" action="${pageContext.request.contextPath}/reportes" target="_blank" class="flex flex-wrap items-end gap-3">
            <input type="hidden" name="tipo" value="ventas-pdf">
            <div class="flex flex-col gap-1">
                <label class="text-xs font-medium text-slate-500">Desde</label>
                <input type="date" name="desde" value="${hace7dias}" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-xs font-medium text-slate-500">Hasta</label>
                <input type="date" name="hasta" value="${hoy}" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
                Generar PDF
            </button>
        </form>
    </div>

    <!-- Reporte de stock bajo -->
    <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
        <h3 class="font-semibold">Reporte de Productos con Stock Bajo (PDF)</h3>
        <p class="text-sm text-slate-500">Listado de productos por debajo de su stock mínimo, útil para planificar reposición.</p>
        <a href="${pageContext.request.contextPath}/reportes?tipo=stock-bajo-pdf" target="_blank"
           class="self-start h-9 px-5 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-sm font-medium flex items-center">
            Generar PDF
        </a>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
