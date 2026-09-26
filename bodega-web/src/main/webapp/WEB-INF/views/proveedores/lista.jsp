<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
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

    <div class="flex items-center justify-between">
        <div>
            <h2 class="text-2xl font-bold">Gestión de Proveedores</h2>
            <p class="text-sm text-slate-500">Directorio de proveedores asociados a los productos del catálogo.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nuevo-proveedor').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nuevo proveedor
        </button>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/proveedores"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por razón social, RUC o contacto..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">RUC</th>
                    <th class="px-4 py-3">Razón social</th>
                    <th class="px-4 py-3">Contacto</th>
                    <th class="px-4 py-3">Teléfono</th>
                    <th class="px-4 py-3">Correo</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="prov" items="${proveedores}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${prov.ruc}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${prov.razonSocial}"/></td>
                        <td class="px-4 py-3"><c:out value="${prov.contactoNombre}"/></td>
                        <td class="px-4 py-3"><c:out value="${prov.telefono}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${prov.correo}"/></td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${prov.estado == 'ACTIVO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${prov.estado == 'ACTIVO' ? 'Activo' : 'Inactivo'}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <form method="post" action="${pageContext.request.contextPath}/proveedores" class="inline">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="accion" value="${prov.estado == 'ACTIVO' ? 'desactivar' : 'reactivar'}">
                                <input type="hidden" name="proveedorId" value="${prov.id}">
                                <button type="submit" class="text-slate-500 hover:text-red-600">
                                    <c:out value="${prov.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty proveedores}">
                    <tr><td colspan="7" class="px-4 py-6 text-center text-slate-400">No se encontraron proveedores registrados.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div id="modal-nuevo-proveedor" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-lg rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Nuevo proveedor</h3>
            <button type="button" onclick="document.getElementById('modal-nuevo-proveedor').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/proveedores" class="p-6 flex flex-col gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">
            <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">RUC</label>
                    <input type="text" name="ruc" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Razón social *</label>
                    <input type="text" name="razonSocial" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Nombre de contacto</label>
                    <input type="text" name="contactoNombre" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Teléfono</label>
                    <input type="tel" name="telefono" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Correo</label>
                    <input type="email" name="correo" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Dirección</label>
                    <input type="text" name="direccion" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
            </div>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nuevo-proveedor').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar proveedor</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
