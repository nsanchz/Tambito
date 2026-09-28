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
            <h2 class="text-2xl font-bold">Inventario y Movimientos de Almacén (Kárdex)</h2>
            <p class="text-sm text-slate-500">Control de stock físico, entradas, salidas, ajustes y mermas.</p>
        </div>
        <button type="button" onclick="abrirModalConTransicion('modal-movimiento')"
                class="h-10 px-4 btn-primario rounded-lg text-sm font-medium">
            + Registrar movimiento
        </button>
    </div>

    <c:if test="${not empty productosStockBajo}">
        <div class="bg-amber-50 border border-amber-200 text-amber-800 rounded-xl p-4 text-sm flex items-start gap-2">
            <span class="text-amber-500 text-lg leading-none animate-pulse" title="${fn:length(productosStockBajo)} producto(s) con stock bajo">&#9888;</span>
            <span><strong>Alerta de stock bajo (${fn:length(productosStockBajo)}):</strong>
            <c:forEach var="p" items="${productosStockBajo}" varStatus="st">
                <c:out value="${p.nombre}"/> (${p.stockActual} uds)<c:if test="${!st.last}">, </c:if>
            </c:forEach>
            </span>
        </div>
    </c:if>

    <!-- Filtros -->
    <form method="get" action="${pageContext.request.contextPath}/inventario"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Filtrar por SKU, nombre, motivo o N° movimiento..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="tipo" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todos los tipos</option>
            <c:forEach var="t" items="${tiposMovimiento}">
                <option value="${t}" ${param.tipo == t ? 'selected' : ''}><c:out value="${t}"/></option>
            </c:forEach>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
        <a href="${pageContext.request.contextPath}/inventario?exportar=excel&busqueda=${fn:escapeXml(param.busqueda)}&tipo=${fn:escapeXml(param.tipo)}"
           class="h-9 px-4 bg-emerald-50 hover:bg-emerald-100 text-emerald-700 rounded-lg text-sm font-medium flex items-center">
            Exportar a Excel
        </a>
    </form>

    <!-- Lotes de un producto específico -->
    <c:if test="${not empty lotesProducto}">
        <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-3">
            <div class="flex items-center justify-between">
                <h3 class="text-sm font-semibold">Lotes del producto</h3>
                <a href="${pageContext.request.contextPath}/inventario?productoId=${productoLotesId}&exportar=excel"
                   class="text-xs text-emerald-700 hover:underline">Exportar kárdex de este producto a Excel</a>
            </div>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-3 py-2">N° Lote</th>
                    <th class="px-3 py-2">Vencimiento</th>
                    <th class="px-3 py-2 text-right">Cantidad inicial</th>
                    <th class="px-3 py-2 text-right">Cantidad actual</th>
                    <th class="px-3 py-2">Ingreso</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="l" items="${lotesProducto}">
                    <tr>
                        <td class="px-3 py-2 font-mono text-slate-500"><c:out value="${l.numeroLote}"/></td>
                        <td class="px-3 py-2"><c:out value="${not empty l.fechaVencimiento ? l.fechaVencimiento : 'No perecible'}"/></td>
                        <td class="px-3 py-2 text-right"><c:out value="${l.cantidadInicial}"/></td>
                        <td class="px-3 py-2 text-right font-semibold"><c:out value="${l.cantidadActual}"/></td>
                        <td class="px-3 py-2 text-slate-500"><c:out value="${l.fechaIngreso}"/></td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <!-- Tabla del Kárdex -->
    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">N° Movimiento</th>
                    <th class="px-4 py-3">Fecha</th>
                    <th class="px-4 py-3">Producto</th>
                    <th class="px-4 py-3">Lote</th>
                    <th class="px-4 py-3">Tipo</th>
                    <th class="px-4 py-3 text-right">Cantidad</th>
                    <th class="px-4 py-3 text-right">Stock ant.</th>
                    <th class="px-4 py-3 text-right">Stock result.</th>
                    <th class="px-4 py-3">Motivo</th>
                    <th class="px-4 py-3">Usuario</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="m" items="${movimientos}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${m.numeroMovimiento}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${m.fecha}"/></td>
                        <td class="px-4 py-3 font-medium">
                            <c:out value="${m.productoNombre}"/>
                            <a href="${pageContext.request.contextPath}/inventario?verLotes=${m.productoId}"
                               class="block text-xs text-blue-600 hover:underline">Ver lotes</a>
                        </td>
                        <td class="px-4 py-3 text-slate-500 font-mono text-xs"><c:out value="${not empty m.numeroLote ? m.numeroLote : '-'}"/></td>
                        <td class="px-4 py-3">
                            <span class="px-2 py-0.5 rounded text-xs font-medium ${m.tipo.incremento ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                                <c:out value="${m.tipo}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right font-medium ${m.tipo.incremento ? 'text-emerald-600' : 'text-red-600'}">
                            <c:out value="${m.tipo.incremento ? '+' : '-'}"/><c:out value="${m.cantidad}"/>
                        </td>
                        <td class="px-4 py-3 text-right text-slate-500"><c:out value="${m.stockAnterior}"/></td>
                        <td class="px-4 py-3 text-right font-semibold"><c:out value="${m.stockResultante}"/></td>
                        <td class="px-4 py-3 text-slate-500 max-w-xs truncate"><c:out value="${m.motivo}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${m.usuarioNombre}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty movimientos}">
                    <tr><td colspan="10" class="px-4 py-10 text-center text-slate-400">
                        <svg xmlns="http://www.w3.org/2000/svg" class="h-9 w-9 mx-auto mb-2 text-slate-300" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path stroke-linecap="round" stroke-linejoin="round" d="M3.75 9.75h16.5m-16.5 0V6.108c0-.621.504-1.125 1.125-1.125h14.25c.621 0 1.125.504 1.125 1.125V9.75m-16.5 0v8.25c0 .621.504 1.125 1.125 1.125h14.25c.621 0 1.125-.504 1.125-1.125V9.75M9 12.75h6"/></svg>
                        <p>No se encontraron movimientos con los filtros aplicados.</p>
                    </td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal: Registrar movimiento de inventario -->
<div id="modal-movimiento" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="tarjeta-modal transition-all duration-150 ease-out opacity-0 scale-95 bg-white w-full max-w-lg rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Registrar movimiento de inventario</h3>
            <button type="button" onclick="cerrarModalConTransicion('modal-movimiento')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/inventario" class="p-6 flex flex-col gap-4" id="form-movimiento"
              onsubmit="return iniciarEnvioConCarga(this, 'btn-guardar-movimiento', 'Guardando...')">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Producto *</label>
                <select name="productoId" id="select-producto" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="">Seleccione un producto</option>
                    <c:forEach var="p" items="${productos}">
                        <option value="${p.id}" data-stock="${p.stockActual}"><c:out value="${p.nombre}"/> (Stock: ${p.stockActual})</option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Tipo de movimiento *</label>
                <select name="tipoMovimiento" id="select-tipo" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="ENTRADA_COMPRA">Entrada por compra (+)</option>
                    <option value="AJUSTE_POSITIVO">Ajuste positivo (+)</option>
                    <option value="DEVOLUCION">Devolución de cliente (+)</option>
                    <option value="AJUSTE_NEGATIVO">Ajuste negativo (-)</option>
                    <option value="MERMA">Merma / Dañado (-)</option>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Cantidad a mover *</label>
                <input type="number" name="cantidad" id="input-cantidad" min="1" step="1" required
                       class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1" id="campo-fecha-vencimiento">
                <label class="text-sm font-medium">Fecha de vencimiento del lote (opcional)</label>
                <input type="date" name="fechaVencimiento" id="input-fecha-vencimiento"
                       class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <span class="text-xs text-slate-400">Deja vacío si el producto no es perecible.</span>
            </div>
            <div class="grid grid-cols-2 gap-4 bg-slate-50 rounded-lg p-3">
                <div class="flex flex-col">
                    <span class="text-xs text-slate-500 uppercase">Stock actual</span>
                    <span id="label-stock-actual" class="font-semibold">-</span>
                </div>
                <div class="flex flex-col">
                    <span class="text-xs text-slate-500 uppercase">Stock resultante</span>
                    <span id="label-stock-resultante" class="font-semibold text-blue-600">-</span>
                </div>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Motivo de la operación *</label>
                <input type="text" name="motivo" required placeholder="Ej. Recepción proveedor Alicorp S.A.A."
                       class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Observaciones / N° de guía o factura</label>
                <textarea name="observaciones" rows="2" class="p-3 border border-slate-300 rounded-lg text-sm resize-none"></textarea>
            </div>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="cerrarModalConTransicion('modal-movimiento')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" id="btn-guardar-movimiento" class="h-9 px-5 btn-primario rounded-lg text-sm font-medium">Registrar movimiento</button>
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

    var TIPOS_INCREMENTO = ['ENTRADA_COMPRA', 'AJUSTE_POSITIVO', 'DEVOLUCION'];

    function recalcularStock() {
        var selectProducto = document.getElementById('select-producto');
        var selectTipo = document.getElementById('select-tipo');
        var inputCantidad = document.getElementById('input-cantidad');
        var opcionProducto = selectProducto.options[selectProducto.selectedIndex];

        var stockActual = parseInt(opcionProducto.getAttribute('data-stock'), 10);
        if (isNaN(stockActual)) {
            document.getElementById('label-stock-actual').textContent = '-';
            document.getElementById('label-stock-resultante').textContent = '-';
            return;
        }

        var cantidad = parseInt(inputCantidad.value, 10) || 0;
        var esIncremento = TIPOS_INCREMENTO.indexOf(selectTipo.value) !== -1;
        var stockResultante = esIncremento ? stockActual + cantidad : stockActual - cantidad;

        document.getElementById('label-stock-actual').textContent = stockActual + ' uds';
        document.getElementById('label-stock-resultante').textContent = stockResultante + ' uds';
    }

    function actualizarVisibilidadFechaVencimiento() {
        var selectTipo = document.getElementById('select-tipo');
        var campoFecha = document.getElementById('campo-fecha-vencimiento');
        var esIncremento = TIPOS_INCREMENTO.indexOf(selectTipo.value) !== -1;
        campoFecha.hidden = !esIncremento;
        if (!esIncremento) {
            document.getElementById('input-fecha-vencimiento').value = '';
        }
    }

    document.getElementById('select-producto').addEventListener('change', recalcularStock);
    document.getElementById('select-tipo').addEventListener('change', recalcularStock);
    document.getElementById('input-cantidad').addEventListener('input', recalcularStock);
    document.getElementById('select-tipo').addEventListener('change', actualizarVisibilidadFechaVencimiento);
    actualizarVisibilidadFechaVencimiento();
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
