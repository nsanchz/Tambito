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

    <div>
        <h2 class="text-2xl font-bold">Historial de Ventas y Comprobantes</h2>
        <p class="text-sm text-slate-500">Consulta de tickets emitidos y administración de estados fiscales.</p>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/ventas"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="N° comprobante o documento del cliente..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="estado" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todos los estados</option>
            <c:forEach var="e" items="${estadosVenta}">
                <option value="${e}" ${param.estado == e ? 'selected' : ''}><c:out value="${e}"/></option>
            </c:forEach>
        </select>
        <select name="metodoPago" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todos los métodos</option>
            <c:forEach var="m" items="${metodosPago}">
                <option value="${m}" ${param.metodoPago == m ? 'selected' : ''}><c:out value="${m}"/></option>
            </c:forEach>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
        <a href="${pageContext.request.contextPath}/ventas?exportar=excel&busqueda=${fn:escapeXml(param.busqueda)}&estado=${fn:escapeXml(param.estado)}&metodoPago=${fn:escapeXml(param.metodoPago)}"
           class="h-9 px-4 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg text-sm font-medium flex items-center">
            Exportar a Excel
        </a>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">N° Comprobante</th>
                    <th class="px-4 py-3">Cliente</th>
                    <th class="px-4 py-3">Vendedor</th>
                    <th class="px-4 py-3">Fecha</th>
                    <th class="px-4 py-3">Método de pago</th>
                    <th class="px-4 py-3 text-right">Total</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="v" items="${ventas}">
                    <tr class="hover:bg-slate-50 ${v.estado == 'ANULADA' ? 'opacity-60' : ''}">
                        <td class="px-4 py-3 font-mono ${v.estado == 'ANULADA' ? 'line-through text-slate-400' : 'text-slate-700'}">
                            <c:out value="${v.numeroComprobante}"/>
                        </td>
                        <td class="px-4 py-3"><c:out value="${v.clienteNombre}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${v.usuarioNombre}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${v.fechaCreacion}"/></td>
                        <td class="px-4 py-3"><c:out value="${v.metodoPago}"/></td>
                        <td class="px-4 py-3 text-right font-semibold">S/ <c:out value="${v.total}"/></td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${v.estado == 'COMPLETADA' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                                <c:out value="${v.estado == 'COMPLETADA' ? 'Completada' : 'Anulada'}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <button type="button" onclick="alternarMenuAcciones(this)"
                                    class="inline-flex items-center justify-center w-8 h-8 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
                                    title="Más acciones">
                                <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 24 24" fill="currentColor">
                                    <circle cx="12" cy="5" r="1.8"/><circle cx="12" cy="12" r="1.8"/><circle cx="12" cy="19" r="1.8"/>
                                </svg>
                            </button>
                            <div class="menu-acciones hidden fixed z-50 w-56 bg-white rounded-xl shadow-lg border border-slate-100 py-1.5 text-sm">
                                <a href="${pageContext.request.contextPath}/ventas?ver=${v.id}"
                                   class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-slate-600 hover:bg-slate-50">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                                    Ver
                                </a>
                                <a href="${pageContext.request.contextPath}/ventas?ticketPdf=${v.id}" target="_blank"
                                   class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-slate-600 hover:bg-slate-50">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6.72 13.829c-.24.03-.48.062-.72.096m.72-.096a42.415 42.415 0 0110.56 0m-10.56 0L6.34 18m10.94-4.171c.24.03.48.062.72.096m-.72-.096L17.66 18m0 0l.229 2.523a1.125 1.125 0 01-1.12 1.227H7.231c-.662 0-1.18-.568-1.12-1.227L6.34 18m11.32 0a5.966 5.966 0 002.08.266m-13.4-.266a5.966 5.966 0 01-2.08.266"/></svg>
                                    Ticket
                                </a>
                                <c:if test="${sessionScope.usuarioRol == 'ADMINISTRADOR' && v.estado == 'COMPLETADA'}">
                                    <div class="my-1 border-t border-slate-100"></div>
                                    <button type="button" onclick="abrirModalAnulacion(${v.id}, '${fn:escapeXml(v.numeroComprobante)}')"
                                            class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-red-600 hover:bg-red-50">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/></svg>
                                        Anular
                                    </button>
                                </c:if>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty ventas}">
                    <tr><td colspan="8" class="px-4 py-10 text-center text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 mx-auto mb-2 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.75h16.5m-16.5 0V6.108c0-.621.504-1.125 1.125-1.125h14.25c.621 0 1.125.504 1.125 1.125V9.75m-16.5 0v8.25c0 .621.504 1.125 1.125 1.125h14.25c.621 0 1.125-.504 1.125-1.125V9.75M9 12.75h6"/></svg>
                        <p>No se encontraron ventas con los filtros aplicados.</p>
                    </td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Detalle de una venta seleccionada -->
    <c:if test="${not empty ventaSeleccionada}">
        <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
            <div class="flex items-center justify-between">
                <h3 class="text-lg font-semibold">Comprobante <c:out value="${ventaSeleccionada.numeroComprobante}"/></h3>
                <div class="flex items-center gap-3">
                    <span class="text-sm text-slate-500">Cliente: <c:out value="${ventaSeleccionada.clienteNombre}"/></span>
                    <a href="${pageContext.request.contextPath}/ventas?ticketPdf=${ventaSeleccionada.id}" target="_blank"
                       class="h-8 px-4 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-xs font-medium flex items-center">
                        Descargar ticket PDF
                    </a>
                </div>
            </div>
            <c:if test="${ventaSeleccionada.estado == 'ANULADA'}">
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">
                    Venta anulada el <c:out value="${ventaSeleccionada.fechaAnulacion}"/>. Motivo: <c:out value="${ventaSeleccionada.motivoAnulacion}"/>
                </div>
            </c:if>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-3 py-2">Producto</th>
                    <th class="px-3 py-2 text-right">Cantidad</th>
                    <th class="px-3 py-2 text-right">Precio unit.</th>
                    <th class="px-3 py-2 text-right">Subtotal</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="d" items="${ventaSeleccionada.detalles}">
                    <tr>
                        <td class="px-3 py-2"><c:out value="${d.productoNombre}"/></td>
                        <td class="px-3 py-2 text-right"><c:out value="${d.cantidad}"/></td>
                        <td class="px-3 py-2 text-right">S/ <c:out value="${d.precioUnitario}"/></td>
                        <td class="px-3 py-2 text-right">S/ <c:out value="${d.subtotal}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
            <div class="flex flex-col items-end gap-1 text-sm">
                <span>Subtotal: S/ <c:out value="${ventaSeleccionada.subtotalImponible}"/></span>
                <span>Descuento: S/ <c:out value="${ventaSeleccionada.descuento}"/></span>
                <span>IGV (18%): S/ <c:out value="${ventaSeleccionada.igv}"/></span>
                <span class="text-lg font-bold">TOTAL: S/ <c:out value="${ventaSeleccionada.total}"/></span>
            </div>
        </div>
    </c:if>
</div>

<!-- Modal: Anulación de venta (solo ADMINISTRADOR) -->
<c:if test="${sessionScope.usuarioRol == 'ADMINISTRADOR'}">
    <div id="modal-anulacion" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div class="tarjeta-modal transition-all duration-150 ease-out opacity-0 scale-95 bg-white w-full max-w-md rounded-xl shadow-xl overflow-hidden">
            <div class="px-6 py-4 bg-red-50 flex items-center justify-between">
                <h3 class="font-semibold text-red-700">Anulación de venta y reversión de stock</h3>
                <button type="button" onclick="cerrarModalConTransicion('modal-anulacion')"
                        class="text-slate-400 hover:text-slate-700">Cerrar</button>
            </div>
            <form method="post" action="${pageContext.request.contextPath}/ventas" class="p-6 flex flex-col gap-4"
                  onsubmit="return iniciarEnvioConCarga(this, 'btn-confirmar-anulacion', 'Anulando...')">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <p class="text-sm text-slate-600">
                    ¿Está seguro de anular la venta <strong id="texto-numero-venta"></strong>? El stock de los
                    productos se devolverá automáticamente al inventario y se registrará un movimiento de
                    ajuste inmutable en el Kárdex.
                </p>
                <input type="hidden" name="ventaId" id="input-venta-id">
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Motivo de la anulación *</label>
                    <textarea name="motivoAnulacion" required rows="2" class="p-3 border border-slate-300 rounded-lg text-sm resize-none"></textarea>
                </div>
                <div class="flex items-center justify-end gap-3">
                    <button type="button" onclick="cerrarModalConTransicion('modal-anulacion')"
                            class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                    <button type="submit" id="btn-confirmar-anulacion" class="h-9 px-5 bg-red-600 hover:bg-red-700 text-white rounded-lg text-sm font-medium">Confirmar anulación</button>
                </div>
            </form>
        </div>
    </div>

    <script>
        function abrirModalAnulacion(ventaId, numeroComprobante) {
            document.getElementById('input-venta-id').value = ventaId;
            document.getElementById('texto-numero-venta').textContent = numeroComprobante;
            abrirModalConTransicion('modal-anulacion');
        }

        /**
         * Da una transición simple de entrada/salida (fade + scale) al modal de esta vista,
         * en vez de que aparezca/desaparezca de golpe al togglear la clase "hidden".
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
</c:if>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
