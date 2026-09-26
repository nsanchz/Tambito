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
            <h2 class="text-2xl font-bold">Gestión de Categorías de Productos</h2>
            <p class="text-sm text-slate-500">Clasificación del catálogo comercial para inventario y Punto de Venta.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nueva-categoria').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nueva categoría
        </button>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/categorias"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por código, nombre o descripción..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="estado" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="todos">Todos los estados</option>
            <option value="ACTIVO" ${param.estado == 'ACTIVO' ? 'selected' : ''}>Activa</option>
            <option value="INACTIVO" ${param.estado == 'INACTIVO' ? 'selected' : ''}>Inactiva</option>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">Código</th>
                    <th class="px-4 py-3">Nombre</th>
                    <th class="px-4 py-3">Descripción</th>
                    <th class="px-4 py-3 text-right">Margen sugerido</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="cat" items="${categorias}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${cat.codigo}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${cat.nombre}"/></td>
                        <td class="px-4 py-3 text-slate-500 max-w-xs truncate"><c:out value="${cat.descripcion}"/></td>
                        <td class="px-4 py-3 text-right"><c:out value="${cat.margenSugerido}"/>%</td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${cat.estado == 'ACTIVO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${cat.estado == 'ACTIVO' ? 'Activa' : 'Inactiva'}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <form method="post" action="${pageContext.request.contextPath}/categorias" class="inline">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="accion" value="${cat.estado == 'ACTIVO' ? 'desactivar' : 'reactivar'}">
                                <input type="hidden" name="categoriaId" value="${cat.id}">
                                <button type="submit" class="text-slate-500 hover:text-red-600">
                                    <c:out value="${cat.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty categorias}">
                    <tr><td colspan="6" class="px-4 py-6 text-center text-slate-400">No se encontraron categorías con los filtros aplicados.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal: Nueva categoría -->
<div id="modal-nueva-categoria" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-lg rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Nueva categoría de producto</h3>
            <button type="button" onclick="document.getElementById('modal-nueva-categoria').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/categorias" class="p-6 flex flex-col gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Nombre de la categoría *</label>
                <input type="text" name="nombre" required placeholder="Ej: Carnes y Aves Frescas"
                       class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Descripción comercial</label>
                <textarea name="descripcion" rows="2" class="p-3 border border-slate-300 rounded-lg text-sm resize-none"></textarea>
            </div>
            <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Margen sugerido (%)</label>
                    <input type="number" step="0.1" name="margenSugerido" placeholder="25.0"
                           class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Estado inicial</label>
                    <select name="estadoInicial" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        <option value="ACTIVO">Activa</option>
                        <option value="INACTIVO">Inactiva</option>
                    </select>
                </div>
            </div>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nueva-categoria').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar categoría</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
