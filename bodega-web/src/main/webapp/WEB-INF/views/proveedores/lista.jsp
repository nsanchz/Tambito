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
        <button type="button" onclick="abrirModalConTransicion('modal-nuevo-proveedor')"
                class="h-10 px-4 btn-primario rounded-lg text-sm font-medium">
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
                                <button type="submit" title="${prov.estado == 'ACTIVO' ? 'Desactivar proveedor' : 'Reactivar proveedor'}"
                                        class="inline-flex items-center gap-1.5 transition-colors ${prov.estado == 'ACTIVO' ? 'text-slate-500 hover:text-red-600' : 'text-slate-500 hover:text-emerald-600'}">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5.636 5.636a9 9 0 1012.728 0M12 3v9"/></svg>
                                    <c:out value="${prov.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty proveedores}">
                    <tr><td colspan="7" class="px-4 py-10 text-center text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 mx-auto mb-2 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.75h16.5m-16.5 0V6.108c0-.621.504-1.125 1.125-1.125h14.25c.621 0 1.125.504 1.125 1.125V9.75m-16.5 0v8.25c0 .621.504 1.125 1.125 1.125h14.25c.621 0 1.125-.504 1.125-1.125V9.75M9 12.75h6"/></svg>
                        <p>No se encontraron proveedores registrados.</p>
                    </td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<div id="modal-nuevo-proveedor" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="tarjeta-modal transition-all duration-150 ease-out opacity-0 scale-95 bg-white w-full max-w-lg rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Nuevo proveedor</h3>
            <button type="button" onclick="cerrarModalConTransicion('modal-nuevo-proveedor')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/proveedores" class="p-6 flex flex-col gap-4"
              onsubmit="return iniciarEnvioConCarga(this, 'btn-guardar-proveedor', 'Guardando...')">
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
                <button type="button" onclick="cerrarModalConTransicion('modal-nuevo-proveedor')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" id="btn-guardar-proveedor" class="h-9 px-5 btn-primario rounded-lg text-sm font-medium">Guardar proveedor</button>
            </div>
        </form>
    </div>
</div>

<script>
    /**
     * Da una transición simple de entrada/salida (fade + scale) a los modales de esta vista,
     * en vez de que aparezcan/desaparezcan de golpe al togglear la clase "hidden".
     */
    function abrirModalConTransicion(idModal) {
        var modal = document.getElementById(idModal);
        var tarjeta = modal.querySelector('.tarjeta-modal');
        modal.classList.remove('hidden');
        requestAnimationFrame(function () {
            tarjeta.classList.remove('opacity-0', 'scale-95');
            tarjeta.classList.add('opacity-100', 'scale-100');
        });
    }

    function cerrarModalConTransicion(idModal) {
        var modal = document.getElementById(idModal);
        var tarjeta = modal.querySelector('.tarjeta-modal');
        tarjeta.classList.remove('opacity-100', 'scale-100');
        tarjeta.classList.add('opacity-0', 'scale-95');
        setTimeout(function () { modal.classList.add('hidden'); }, 150);
    }
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
