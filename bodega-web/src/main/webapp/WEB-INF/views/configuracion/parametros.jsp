<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6 max-w-2xl">

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
        <h2 class="text-2xl font-bold">Configuración del Sistema</h2>
        <p class="text-sm text-slate-500">Parámetros generales usados en ventas, seguridad e inventario.</p>
    </div>

    <form method="post" action="${pageContext.request.contextPath}/configuracion" enctype="multipart/form-data"
          class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

        <div class="flex flex-col gap-2 pb-2 border-b border-slate-100">
            <label class="text-sm font-medium" for="param-logo">Logo de la tienda</label>
            <div class="flex items-center gap-4">
                <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logo actual"
                     class="h-14 w-14 rounded-lg object-cover border border-slate-200"
                     onerror="this.style.display='none'">
                <input type="file" id="param-logo" name="logo" accept="image/*"
                       class="text-sm file:mr-3 file:h-9 file:px-3 file:rounded-lg file:border-0 file:bg-slate-100 file:text-sm">
            </div>
            <span class="text-xs text-slate-400">Se muestra en el login y en el panel. Dejar vacío para conservar el logo actual.</span>
        </div>

        <c:forEach var="p" items="${parametros}">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium" for="param-${p.clave}"><c:out value="${p.etiqueta}"/></label>
                <input type="${p.tipo}" id="param-${p.clave}" name="${p.clave}"
                       value="${valores[p.clave]}"
                       ${p.tipo == 'number' ? 'step=0.01' : ''}
                       class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <span class="text-xs text-slate-400"><c:out value="${p.descripcion}"/></span>
            </div>
        </c:forEach>

        <div class="flex justify-end pt-2">
            <button type="submit" class="h-10 px-6 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
                Guardar configuración
            </button>
        </div>
    </form>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
