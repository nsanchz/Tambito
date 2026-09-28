<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6">
    <div class="bg-white rounded-xl shadow-sm p-6 flex items-center justify-between">
        <div>
            <h2 class="text-2xl font-bold">Bienvenido, <c:out value="${sessionScope.usuarioNombre}"/></h2>
            <p class="text-sm text-slate-500">Turno de mostrador activo. Resumen de tus ventas de hoy.</p>
        </div>
        <a href="${pageContext.request.contextPath}/pos"
           class="group h-11 px-6 btn-primario text-white rounded-lg font-semibold flex items-center gap-2">
            Ir al Punto de Venta (F12)
            <span class="inline-block transition-transform group-hover:translate-x-1">&rarr;</span>
        </a>
    </div>

    <div class="grid grid-cols-2 gap-4">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Total vendido hoy</span>
            <span class="text-2xl font-bold">S/ <span class="kpi-contador" data-formato="moneda" data-valor="${resumen.ventasHoy}">0</span></span>
        </div>
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-1">
            <span class="text-xs text-slate-500 uppercase">Comprobantes emitidos hoy</span>
            <span class="text-2xl font-bold"><span class="kpi-contador" data-valor="${resumen.cantidadVentasHoy}">0</span></span>
        </div>
    </div>

    <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
        <div class="flex items-center justify-between">
            <h3 class="font-semibold">Mis últimas ventas realizadas</h3>
            <a href="${pageContext.request.contextPath}/ventas" class="text-xs text-blue-600 hover:underline">Ver todo mi historial</a>
        </div>
        <table class="w-full text-left text-sm">
            <thead>
            <tr class="text-slate-500 text-xs uppercase">
                <th class="py-1">Comprobante</th>
                <th class="py-1">Cliente</th>
                <th class="py-1 text-right">Total</th>
                <th class="py-1 text-center">Estado</th>
            </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
            <c:forEach var="v" items="${resumen.ultimasVentasPropias}">
                <tr>
                    <td class="py-1.5 font-mono text-xs"><c:out value="${v.numeroComprobante}"/></td>
                    <td class="py-1.5"><c:out value="${v.clienteNombre}"/></td>
                    <td class="py-1.5 text-right font-medium">S/ <c:out value="${v.total}"/></td>
                    <td class="py-1.5 text-center">
                        <span class="px-2 py-0.5 rounded-full text-xs ${v.estado == 'COMPLETADA' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                            <c:out value="${v.estado == 'COMPLETADA' ? 'OK' : 'Anulada'}"/>
                        </span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${empty resumen.ultimasVentasPropias}">
                <tr><td colspan="4" class="py-4 text-center text-slate-400">Aún no has registrado ventas hoy.</td></tr>
            </c:if>
            </tbody>
        </table>
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
