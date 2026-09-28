<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6">
    <div>
        <h2 class="text-2xl font-bold">Bienvenido, <c:out value="${sessionScope.usuarioNombre}"/></h2>
        <p class="text-sm text-slate-500">Resumen gerencial de ventas, inventario y operaciones del sistema.</p>
    </div>

    <!-- KPIs -->
    <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas de hoy</span>
            <span class="text-2xl font-bold">S/ <span class="kpi-contador" data-formato="moneda" data-valor="${resumen.ventasHoy}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas del mes</span>
            <span class="text-2xl font-bold">S/ <span class="kpi-contador" data-formato="moneda" data-valor="${resumen.ventasMes}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Productos activos</span>
            <span class="text-2xl font-bold"><span class="kpi-contador" data-valor="${resumen.productosActivos}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Stock bajo</span>
            <span class="text-2xl font-bold text-amber-600"><span class="kpi-contador ${resumen.productosStockBajo > 0 ? 'animate-pulse' : ''}" data-valor="${resumen.productosStockBajo}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Clientes activos</span>
            <span class="text-2xl font-bold"><span class="kpi-contador" data-valor="${resumen.clientesActivos}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ordenes de compra pendientes</span>
            <span class="text-2xl font-bold"><span class="kpi-contador" data-valor="${resumen.ordenesCompraPendientes}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Ventas anuladas (mes)</span>
            <span class="text-2xl font-bold text-red-600"><span class="kpi-contador" data-valor="${resumen.ventasAnuladasMes}">0</span></span>
        </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <!-- Stock bajo -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
            <div class="flex items-center justify-between">
                <h3 class="font-semibold">Productos con stock bajo</h3>
                <a href="${pageContext.request.contextPath}/inventario" class="text-xs text-blue-600 hover:underline">Ir a Inventario</a>
            </div>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="text-slate-500 text-xs uppercase">
                    <th class="py-1">Producto</th>
                    <th class="py-1 text-right">Stock actual</th>
                    <th class="py-1 text-right">Stock mínimo</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="p" items="${resumen.productosStockBajoDetalle}">
                    <tr>
                        <td class="py-1.5"><c:out value="${p.nombre}"/></td>
                        <td class="py-1.5 text-right text-red-600 font-medium"><c:out value="${p.stockActual}"/></td>
                        <td class="py-1.5 text-right text-slate-500"><c:out value="${p.stockMinimo}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty resumen.productosStockBajoDetalle}">
                    <tr><td colspan="3" class="py-4 text-center text-slate-400">Sin alertas de stock por el momento.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>

        <!-- Últimas ventas -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
            <div class="flex items-center justify-between">
                <h3 class="font-semibold">Últimas ventas registradas</h3>
                <a href="${pageContext.request.contextPath}/ventas" class="text-xs text-blue-600 hover:underline">Ver historial completo</a>
            </div>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="text-slate-500 text-xs uppercase">
                    <th class="py-1">Comprobante</th>
                    <th class="py-1">Vendedor</th>
                    <th class="py-1 text-right">Total</th>
                    <th class="py-1 text-center">Estado</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="v" items="${resumen.ultimasVentas}">
                    <tr>
                        <td class="py-1.5 font-mono text-xs"><c:out value="${v.numeroComprobante}"/></td>
                        <td class="py-1.5"><c:out value="${v.usuarioNombre}"/></td>
                        <td class="py-1.5 text-right font-medium">S/ <c:out value="${v.total}"/></td>
                        <td class="py-1.5 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs ${v.estado == 'COMPLETADA' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                                <c:out value="${v.estado == 'COMPLETADA' ? 'OK' : 'Anulada'}"/>
                            </span>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty resumen.ultimasVentas}">
                    <tr><td colspan="4" class="py-4 text-center text-slate-400">Aún no hay ventas registradas.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    (function () {
        document.querySelectorAll('.kpi-contador').forEach(function (span) {
            var valorFinal = parseFloat(span.getAttribute('data-valor')) || 0;
            var esMoneda = span.getAttribute('data-formato') === 'moneda';
            var duracion = 600;
            var inicio = null;

            function animar(marca) {
                if (!inicio) inicio = marca;
                var progreso = Math.min((marca - inicio) / duracion, 1);
                var valorActual = valorFinal * progreso;
                span.textContent = esMoneda ? valorActual.toFixed(2) : Math.round(valorActual);
                if (progreso < 1) {
                    requestAnimationFrame(animar);
                } else {
                    span.textContent = esMoneda ? valorFinal.toFixed(2) : valorFinal;
                }
            }

            requestAnimationFrame(animar);
        });
    })();
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
