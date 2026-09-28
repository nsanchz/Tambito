<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6">

    <c:if test="${not empty sessionScope.mensaje}">
        <div class="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 text-sm">
            <c:out value="${sessionScope.mensaje}"/>
        </div>
        <c:remove var="mensaje" scope="session"/>
    </c:if>
    <c:if test="${not empty sessionScope.error}">
        <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">
            <c:out value="${sessionScope.error}"/>
        </div>
        <c:remove var="error" scope="session"/>
    </c:if>

    <div class="flex items-start justify-between gap-4 flex-wrap">
        <div>
            <h2 class="text-2xl font-bold">Respaldos de Base de Datos</h2>
            <p class="text-sm text-slate-500">Historial de respaldos automáticos (diarios) y manuales de bodega_db.</p>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/respaldos"
              class="bg-white rounded-xl shadow-sm p-3 flex flex-col gap-2 w-full max-w-md"
              onsubmit="return iniciarEnvioConCarga(this, 'btn-generar-respaldo', 'Generando respaldo...')">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <label class="text-xs font-medium text-slate-600">Ruta de destino en el servidor (opcional)</label>
            <input type="text" name="rutaDestino" placeholder="${rutaPorDefecto}"
                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm font-mono">
            <p class="text-xs text-slate-400">
                Déjelo vacío para usar la ruta por defecto (<c:out value="${rutaPorDefecto}"/>).
                Debe ser una ruta accesible por el servidor (no por su computadora), ej. un disco
                externo o carpeta compartida montada en el servidor.
            </p>
            <button type="submit" id="btn-generar-respaldo" class="self-end h-10 px-4 btn-primario text-white rounded-lg text-sm font-medium">
                Generar respaldo ahora
            </button>
        </form>
    </div>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">Inicio</th>
                    <th class="px-4 py-3">Tipo</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3">Disparado por</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="r" items="${historial}" varStatus="st">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 text-slate-500 whitespace-nowrap"><c:out value="${r.fechaInicio}"/></td>
                        <td class="px-4 py-3">
                            <span class="px-2 py-0.5 rounded text-xs font-medium ${r.tipo == 'MANUAL' ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-600'}">
                                <c:out value="${r.tipo}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${r.estado == 'EXITOSO' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                                <c:out value="${r.estado}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${not empty r.usuarioNombre ? r.usuarioNombre : 'Automático (scheduler)'}"/></td>
                        <td class="px-4 py-3 text-right">
                            <button type="button" onclick="alternarDetalleRespaldo('detalle-respaldo-${st.index}')"
                                    class="text-blue-600 hover:underline">Ver detalle</button>
                        </td>
                    </tr>
                    <tr id="detalle-respaldo-${st.index}" class="hidden bg-slate-50 transition-opacity duration-200">
                        <td colspan="5" class="px-4 py-3">
                            <div class="grid grid-cols-2 gap-2 text-xs text-slate-600 max-w-2xl">
                                <div><span class="text-slate-400">Fin:</span> <c:out value="${r.fechaFin}"/></div>
                                <div><span class="text-slate-400">Tamaño:</span> <c:if test="${not empty r.tamanoBytes}">${r.tamanoBytes / 1024} KB</c:if></div>
                                <div class="col-span-2 break-all"><span class="text-slate-400">Destino:</span> <c:out value="${r.rutaDestino}"/></div>
                                <c:if test="${r.estado == 'FALLIDO' and not empty r.mensajeError}">
                                    <div class="col-span-2 bg-red-50 border border-red-200 text-red-700 rounded-lg p-2 break-all">
                                        <span class="font-medium">Error:</span> <c:out value="${r.mensajeError}"/>
                                    </div>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty historial}">
                    <tr><td colspan="5" class="px-4 py-6 text-center text-slate-400">Aún no se ha ejecutado ningún respaldo.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<script>
    function alternarDetalleRespaldo(id) {
        var fila = document.getElementById(id);
        if (fila.classList.contains('hidden')) {
            fila.classList.remove('hidden');
            fila.classList.add('opacity-0');
            requestAnimationFrame(function () {
                fila.classList.remove('opacity-0');
            });
        } else {
            fila.classList.add('hidden');
        }
    }
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
