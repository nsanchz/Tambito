<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6">
    <div>
        <h2 class="text-2xl font-bold">Auditoría del Sistema y Control de Seguridad</h2>
        <p class="text-sm text-slate-500">Bitácora inmutable de acciones sensibles: accesos, cambios de usuarios y anulaciones.</p>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/auditoria"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por detalle o usuario..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="accion" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todas las acciones</option>
            <c:forEach var="a" items="${accionesFiltrables}">
                <option value="${a}" ${param.accion == a ? 'selected' : ''}><c:out value="${a}"/></option>
            </c:forEach>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">Fecha</th>
                    <th class="px-4 py-3">Usuario</th>
                    <th class="px-4 py-3">Acción</th>
                    <th class="px-4 py-3">Entidad</th>
                    <th class="px-4 py-3">Detalle</th>
                    <th class="px-4 py-3">IP</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="r" items="${registros}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 text-slate-500 whitespace-nowrap"><c:out value="${r.fecha}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${r.usuarioNombre}"/></td>
                        <td class="px-4 py-3">
                            <span class="px-2 py-0.5 rounded text-xs font-medium
                                ${fn:contains(r.accion, 'FALLIDO') || fn:contains(r.accion, 'BLOQUEADA') || fn:contains(r.accion, 'ANULADA')
                                  ? 'bg-red-100 text-red-700 animate-pulse' : 'bg-slate-100 text-slate-600'}">
                                <c:out value="${r.accion}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-slate-500">
                            <c:choose>
                                <c:when test="${not empty r.entidadNombre}">
                                    <c:out value="${r.entidadNombre}"/>
                                </c:when>
                                <c:otherwise>
                                    <c:out value="${r.entidad}"/><c:if test="${not empty r.entidadId}"> #<c:out value="${r.entidadId}"/></c:if>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="px-4 py-3 text-slate-600 max-w-md truncate"><c:out value="${r.detalle}"/></td>
                        <td class="px-4 py-3 font-mono text-xs text-slate-400"><c:out value="${r.direccionIp}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty registros}">
                    <tr><td colspan="6" class="px-4 py-10 text-center text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 mx-auto mb-2 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.75h16.5m-16.5 0V6.108c0-.621.504-1.125 1.125-1.125h14.25c.621 0 1.125.504 1.125 1.125V9.75m-16.5 0v8.25c0 .621.504 1.125 1.125 1.125h14.25c.621 0 1.125-.504 1.125-1.125V9.75M9 12.75h6"/></svg>
                        <p>No hay eventos registrados con los filtros aplicados.</p>
                    </td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
