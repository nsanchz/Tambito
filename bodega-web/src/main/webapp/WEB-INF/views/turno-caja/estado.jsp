<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6 max-w-3xl">

    <c:if test="${requerido}">
        <div class="bg-amber-50 border border-amber-200 text-amber-800 rounded-lg p-3 text-sm">
            Debe abrir un turno de caja antes de operar el Punto de Venta.
        </div>
    </c:if>
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

    <div>
        <h2 class="text-2xl font-bold">Turno de Caja</h2>
        <p class="text-sm text-slate-500">Apertura con fondo inicial y cierre con arqueo de efectivo.</p>
    </div>

    <c:choose>
        <c:when test="${not empty turnoAbierto}">
            <!-- Turno abierto: mostrar estado y formulario de cierre -->
            <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
                <div class="flex items-center justify-between">
                    <div>
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-100 text-emerald-700">TURNO ABIERTO</span>
                        <h3 class="text-lg font-semibold mt-2">Terminal: <c:out value="${turnoAbierto.terminalId}"/></h3>
                        <p class="text-sm text-slate-500">Abierto el <c:out value="${turnoAbierto.fechaApertura}"/></p>
                    </div>
                    <div class="text-right">
                        <span class="text-xs text-slate-500 uppercase">Fondo inicial</span>
                        <p class="text-xl font-bold">S/ <c:out value="${turnoAbierto.montoInicial}"/></p>
                    </div>
                </div>

                <form method="post" action="${pageContext.request.contextPath}/turno-caja" class="flex flex-col gap-3 bg-slate-50 rounded-lg p-4">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="accion" value="cerrar">
                    <input type="hidden" name="turnoId" value="${turnoAbierto.id}">
                    <label class="text-sm font-medium">Monto de efectivo contado físicamente en caja (S/) *</label>
                    <input type="number" step="0.01" min="0" name="montoDeclarado" required
                           class="h-10 px-3 border border-slate-300 rounded-lg text-sm max-w-xs">
                    <p class="text-xs text-slate-400">
                        El sistema calculará automáticamente el efectivo esperado (fondo inicial + ventas en efectivo
                        de este turno) y mostrará la diferencia (sobrante o faltante) al confirmar.
                    </p>
                    <button type="submit" class="self-start h-10 px-6 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium">
                        Cerrar turno y hacer arqueo
                    </button>
                </form>
            </div>
        </c:when>
        <c:otherwise>
            <!-- Sin turno abierto: formulario de apertura -->
            <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
                <h3 class="font-semibold">Abrir nuevo turno de caja</h3>
                <form method="post" action="${pageContext.request.contextPath}/turno-caja" class="flex flex-col gap-3 max-w-xs">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="accion" value="abrir">
                    <label class="text-sm font-medium">Monto inicial de caja (S/) *</label>
                    <input type="number" step="0.01" min="0" name="montoInicial" required value="0.00"
                           class="h-10 px-3 border border-slate-300 rounded-lg text-sm">
                    <button type="submit" class="h-10 px-6 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
                        Abrir turno
                    </button>
                </form>
            </div>
        </c:otherwise>
    </c:choose>

    <!-- Historial de turnos (solo administrador) -->
    <c:if test="${not empty historialTurnos}">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
            <h3 class="font-semibold text-sm">Historial de turnos de caja</h3>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-3 py-2">Cajero</th>
                    <th class="px-3 py-2">Apertura</th>
                    <th class="px-3 py-2">Cierre</th>
                    <th class="px-3 py-2 text-right">Fondo inicial</th>
                    <th class="px-3 py-2 text-right">Efectivo esperado</th>
                    <th class="px-3 py-2 text-right">Contado</th>
                    <th class="px-3 py-2 text-right">Diferencia</th>
                    <th class="px-3 py-2 text-center">Estado</th>
                    <th class="px-3 py-2 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="t" items="${historialTurnos}">
                    <tr class="${not empty turnoDetalle and turnoDetalle.id == t.id ? 'bg-blue-50' : ''}">
                        <td class="px-3 py-2"><c:out value="${t.usuarioNombre}"/></td>
                        <td class="px-3 py-2 text-slate-500"><c:out value="${t.fechaApertura}"/></td>
                        <td class="px-3 py-2 text-slate-500"><c:out value="${t.fechaCierre}"/></td>
                        <td class="px-3 py-2 text-right">S/ <c:out value="${t.montoInicial}"/></td>
                        <td class="px-3 py-2 text-right">S/ <c:out value="${t.montoEfectivoCalculado}"/></td>
                        <td class="px-3 py-2 text-right">S/ <c:out value="${t.montoDeclarado}"/></td>
                        <td class="px-3 py-2 text-right font-medium
                            ${not empty t.diferencia and t.diferencia lt 0 ? 'text-red-600' : 'text-emerald-600'}">
                            <c:if test="${not empty t.diferencia}">S/ <c:out value="${t.diferencia}"/></c:if>
                        </td>
                        <td class="px-3 py-2 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs ${t.estado == 'ABIERTO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${t.estado}"/>
                            </span>
                        </td>
                        <td class="px-3 py-2 text-right">
                            <a href="${pageContext.request.contextPath}/turno-caja?verTurno=${t.id}"
                               class="text-blue-600 hover:underline">Ver detalle</a>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <!-- Detalle de un turno seleccionado: ventas realizadas durante ese turno -->
    <c:if test="${not empty turnoDetalle}">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
            <div class="flex items-center justify-between">
                <h3 class="font-semibold text-sm">
                    Detalle del turno de <c:out value="${turnoDetalle.usuarioNombre}"/>
                    — <c:out value="${turnoDetalle.terminalId}"/>
                </h3>
                <a href="${pageContext.request.contextPath}/turno-caja" class="text-xs text-slate-500 hover:underline">Cerrar detalle</a>
            </div>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-3 py-2">Hora</th>
                    <th class="px-3 py-2">Comprobante</th>
                    <th class="px-3 py-2">Método de pago</th>
                    <th class="px-3 py-2 text-center">Estado</th>
                    <th class="px-3 py-2 text-right">Total</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="v" items="${ventasDelTurno}">
                    <tr>
                        <td class="px-3 py-2 text-slate-500"><c:out value="${v.fechaCreacion}"/></td>
                        <td class="px-3 py-2 font-mono"><c:out value="${v.numeroComprobante}"/></td>
                        <td class="px-3 py-2"><c:out value="${v.metodoPago}"/></td>
                        <td class="px-3 py-2 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs ${v.estado == 'COMPLETADA' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${v.estado}"/>
                            </span>
                        </td>
                        <td class="px-3 py-2 text-right font-medium">S/ <c:out value="${v.total}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty ventasDelTurno}">
                    <tr><td colspan="5" class="px-3 py-4 text-center text-slate-400">Este turno no registra ventas.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </c:if>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
