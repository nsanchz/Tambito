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
            <h2 class="text-2xl font-bold">Órdenes de Compra a Proveedores</h2>
            <p class="text-sm text-slate-500">Solicitud y recepción de mercadería; actualiza el stock e inserta movimientos en el Kárdex.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nueva-orden').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nueva orden de compra
        </button>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/ordenes-compra"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <select name="estado" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todos los estados</option>
            <c:forEach var="e" items="${estadosOrden}">
                <option value="${e}" ${param.estado == e ? 'selected' : ''}><c:out value="${e}"/></option>
            </c:forEach>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
        <a href="${pageContext.request.contextPath}/ordenes-compra?exportar=excel${not empty param.estado ? '&estado='.concat(fn:escapeXml(param.estado)) : ''}"
           class="h-9 px-4 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg text-sm font-medium flex items-center">
            Exportar a Excel
        </a>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">N° Orden</th>
                    <th class="px-4 py-3">Proveedor</th>
                    <th class="px-4 py-3">Solicitado por</th>
                    <th class="px-4 py-3">Fecha</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="o" items="${ordenes}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${o.numero}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${o.proveedorNombre}"/></td>
                        <td class="px-4 py-3"><c:out value="${o.usuarioNombre}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${o.fechaCreacion}"/></td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium
                                ${o.estado == 'RECIBIDA_COMPLETA' ? 'bg-emerald-100 text-emerald-700' :
                                  o.estado == 'RECIBIDA_PARCIAL' ? 'bg-amber-100 text-amber-700' :
                                  o.estado == 'APROBADA' ? 'bg-indigo-100 text-indigo-700' :
                                  o.estado == 'RECHAZADA' ? 'bg-red-100 text-red-700' :
                                  o.estado == 'CANCELADA' ? 'bg-slate-100 text-slate-500' : 'bg-sky-100 text-sky-700'}">
                                <c:out value="${o.estado}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <div class="flex items-center justify-end gap-2">
                                <c:if test="${sessionScope.usuarioRol == 'ADMINISTRADOR' and o.estado == 'PENDIENTE'}">
                                    <form method="post" action="${pageContext.request.contextPath}/ordenes-compra" class="inline">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="accion" value="aprobar">
                                        <input type="hidden" name="ordenId" value="${o.id}">
                                        <button type="submit" class="text-emerald-600 hover:underline">Aprobar</button>
                                    </form>
                                    <form method="post" action="${pageContext.request.contextPath}/ordenes-compra" class="inline"
                                          onsubmit="document.getElementById('motivo-rechazo-${o.id}').value = prompt('Motivo del rechazo (opcional):') || ''; return true;">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="accion" value="rechazar">
                                        <input type="hidden" name="ordenId" value="${o.id}">
                                        <input type="hidden" id="motivo-rechazo-${o.id}" name="motivo" value="">
                                        <button type="submit" class="text-red-600 hover:underline">Rechazar</button>
                                    </form>
                                </c:if>
                                <a href="${pageContext.request.contextPath}/ordenes-compra?ver=${o.id}"
                                   class="text-blue-600 hover:underline">Ver / Recibir</a>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty ordenes}">
                    <tr><td colspan="6" class="px-4 py-6 text-center text-slate-400">No hay órdenes de compra registradas.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Detalle / Recepción de una orden seleccionada -->
    <c:if test="${not empty ordenSeleccionada}">
        <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
            <div class="flex items-center justify-between">
                <h3 class="text-lg font-semibold">Orden <c:out value="${ordenSeleccionada.numero}"/> — <c:out value="${ordenSeleccionada.proveedorNombre}"/></h3>
                <span class="text-sm text-slate-500">Estado: <c:out value="${ordenSeleccionada.estado}"/></span>
            </div>

            <c:if test="${ordenSeleccionada.estado == 'PENDIENTE'}">
                <div class="bg-sky-50 border border-sky-200 text-sky-700 rounded-lg p-3 text-sm">
                    Esta orden aún está pendiente de aprobación. No se puede recibir mercadería hasta que un administrador la apruebe.
                </div>
            </c:if>
            <c:if test="${ordenSeleccionada.estado == 'RECHAZADA'}">
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">
                    Esta orden fue rechazada<c:if test="${not empty ordenSeleccionada.motivoRechazo}">: <c:out value="${ordenSeleccionada.motivoRechazo}"/></c:if>.
                </div>
            </c:if>

            <c:if test="${ordenSeleccionada.estado == 'APROBADA' or ordenSeleccionada.estado == 'RECIBIDA_PARCIAL'}">
            <form method="post" action="${pageContext.request.contextPath}/ordenes-compra" class="flex flex-col gap-3">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="accion" value="recibir">
                <input type="hidden" name="ordenId" value="${ordenSeleccionada.id}">
                <table class="w-full text-left text-sm">
                    <thead>
                    <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                        <th class="px-3 py-2">Producto</th>
                        <th class="px-3 py-2 text-right">Pedido</th>
                        <th class="px-3 py-2 text-right">Recibido</th>
                        <th class="px-3 py-2 text-right">Pendiente</th>
                        <th class="px-3 py-2 text-right">Recibir ahora</th>
                        <th class="px-3 py-2 text-right">Vencimiento del lote</th>
                    </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100">
                    <c:forEach var="d" items="${ordenSeleccionada.detalles}">
                        <tr>
                            <td class="px-3 py-2"><c:out value="${d.productoNombre}"/></td>
                            <td class="px-3 py-2 text-right"><c:out value="${d.cantidadPedida}"/></td>
                            <td class="px-3 py-2 text-right"><c:out value="${d.cantidadRecibida}"/></td>
                            <td class="px-3 py-2 text-right font-medium"><c:out value="${d.cantidadPendiente}"/></td>
                            <td class="px-3 py-2 text-right">
                                <input type="hidden" name="detalleId" value="${d.id}">
                                <input type="number" name="cantidadRecibida" min="0" max="${d.cantidadPendiente}"
                                       class="w-24 h-8 px-2 border border-slate-300 rounded-lg text-sm text-right"
                                       ${d.cantidadPendiente == 0 ? 'disabled' : ''}>
                            </td>
                            <td class="px-3 py-2 text-right">
                                <input type="date" name="fechaVencimiento"
                                       class="w-40 h-8 px-2 border border-slate-300 rounded-lg text-sm"
                                       ${d.cantidadPendiente == 0 ? 'disabled' : ''}>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <div class="flex justify-end">
                    <button type="submit" class="h-9 px-5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium">
                        Registrar recepción
                    </button>
                </div>
            </form>
            </c:if>
        </div>
    </c:if>
</div>

<!-- Modal: Nueva orden de compra -->
<div id="modal-nueva-orden" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-3xl max-h-[90vh] overflow-y-auto rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Nueva orden de compra</h3>
            <button type="button" onclick="document.getElementById('modal-nueva-orden').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/ordenes-compra" class="p-6 flex flex-col gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Proveedor *</label>
                <select name="proveedorId" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="">Seleccione un proveedor</option>
                    <c:forEach var="prov" items="${proveedores}">
                        <option value="${prov.id}"><c:out value="${prov.razonSocial}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Observaciones</label>
                <textarea name="observaciones" rows="2" class="p-3 border border-slate-300 rounded-lg text-sm resize-none"></textarea>
            </div>

            <div class="flex flex-col gap-2">
                <label class="text-sm font-medium">Productos a pedir *</label>
                <div id="contenedor-lineas" class="flex flex-col gap-2"></div>
                <button type="button" onclick="agregarLinea()" class="self-start text-sm text-blue-600 hover:underline">+ Agregar producto</button>
            </div>

            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nueva-orden').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Registrar orden</button>
            </div>
        </form>
    </div>
</div>

<script>
    var PRODUCTOS_CATALOGO = [
        <c:forEach var="p" items="${productos}" varStatus="st">
        {id: ${p.id}, nombre: "${fn:escapeXml(p.nombre)}", precioCompra: ${p.precioCompra}}<c:if test="${!st.last}">,</c:if>
        </c:forEach>
    ];

    function agregarLinea(productoIdPreseleccionado, cantidadPreseleccionada) {
        var contenedor = document.getElementById('contenedor-lineas');
        var fila = document.createElement('div');
        fila.className = 'grid grid-cols-12 gap-2 items-center';

        var opcionesProducto = PRODUCTOS_CATALOGO.map(function (p) {
            var seleccionado = (productoIdPreseleccionado && String(p.id) === String(productoIdPreseleccionado)) ? 'selected' : '';
            return '<option value="' + p.id + '" data-precio="' + p.precioCompra + '" ' + seleccionado + '>' + p.nombre + '</option>';
        }).join('');

        fila.innerHTML =
            '<select name="lineaProductoId" required class="col-span-5 h-9 px-2 border border-slate-300 rounded-lg text-sm">' +
            '<option value="">Seleccione producto</option>' + opcionesProducto +
            '</select>' +
            '<input type="number" name="lineaCantidad" min="1" required placeholder="Cantidad" value="' + (cantidadPreseleccionada || '') + '" class="col-span-2 h-9 px-2 border border-slate-300 rounded-lg text-sm">' +
            '<button type="button" onclick="aplicarSugerenciaEnFila(this)" title="Sugerir cantidad según velocidad de venta (Kárdex)" class="col-span-2 h-9 px-1 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 rounded-lg text-xs">Sugerir</button>' +
            '<input type="number" step="0.01" name="lineaPrecioUnitario" min="0" required placeholder="Precio unit." class="col-span-2 h-9 px-2 border border-slate-300 rounded-lg text-sm">' +
            '<button type="button" onclick="this.parentElement.remove()" class="col-span-1 text-red-500 hover:text-red-700 text-sm">Quitar</button>';

        contenedor.appendChild(fila);
    }

    function aplicarSugerenciaEnFila(botonSugerir) {
        var fila = botonSugerir.parentElement;
        var selectProducto = fila.querySelector('select[name="lineaProductoId"]');
        var inputCantidad = fila.querySelector('input[name="lineaCantidad"]');

        if (!selectProducto.value) {
            alert('Seleccione primero un producto para calcular la sugerencia.');
            return;
        }

        var base = document.querySelector('meta[name="context-path"]').getAttribute('content');
        botonSugerir.textContent = '...';
        fetch(base + '/api/sugerencia-reposicion?productoId=' + selectProducto.value)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                inputCantidad.value = data.cantidadSugerida;
                botonSugerir.textContent = 'Sugerir';
            })
            .catch(function () { botonSugerir.textContent = 'Sugerir'; });
    }

    <c:if test="${abrirModalNuevaOrden}">
    document.addEventListener('DOMContentLoaded', function () {
        document.getElementById('modal-nueva-orden').classList.remove('hidden');
        agregarLinea('${productoSugeridoId}', ${cantidadSugeridaInicial});
    });
    </c:if>

    if (document.getElementById('contenedor-lineas') && document.getElementById('contenedor-lineas').children.length === 0) {
        agregarLinea();
    }
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
