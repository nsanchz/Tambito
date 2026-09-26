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
            <h2 class="text-2xl font-bold">Gestión de Clientes y Fidelización</h2>
            <p class="text-sm text-slate-500">Directorio de clientes registrados para Boleta/Factura y su historial de compras.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nuevo-cliente').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nuevo cliente
        </button>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/clientes"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por nombre o documento..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="estado" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="todos">Todos los estados</option>
            <option value="ACTIVO" ${param.estado == 'ACTIVO' ? 'selected' : ''}>Activo</option>
            <option value="INACTIVO" ${param.estado == 'INACTIVO' ? 'selected' : ''}>Inactivo</option>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
    </form>

    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">Documento</th>
                    <th class="px-4 py-3">Nombre / Razón social</th>
                    <th class="px-4 py-3">Teléfono</th>
                    <th class="px-4 py-3">Correo</th>
                    <th class="px-4 py-3 text-center">Fidelización</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="cli" items="${clientes}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${cli.tipoDocumento}"/>: <c:out value="${cli.numeroDocumento}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${cli.nombreCompleto}"/></td>
                        <td class="px-4 py-3"><c:out value="${cli.telefono}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${cli.correo}"/></td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium
                                ${cli.categoriaFidelizacion == 'VIP' ? 'bg-purple-100 text-purple-700' :
                                  cli.categoriaFidelizacion == 'FRECUENTE' ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${cli.categoriaFidelizacion}"/> · <c:out value="${cli.puntosFidelizacion}"/> pts
                            </span>
                        </td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${cli.estado == 'ACTIVO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${cli.estado == 'ACTIVO' ? 'Activo' : 'Inactivo'}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <a href="${pageContext.request.contextPath}/clientes?ver=${cli.id}" class="text-blue-600 hover:underline">Ver ficha</a>
                            &nbsp;·&nbsp;
                            <form method="post" action="${pageContext.request.contextPath}/clientes" class="inline">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="accion" value="${cli.estado == 'ACTIVO' ? 'desactivar' : 'reactivar'}">
                                <input type="hidden" name="clienteId" value="${cli.id}">
                                <button type="submit" class="text-slate-500 hover:text-red-600">
                                    <c:out value="${cli.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty clientes}">
                    <tr><td colspan="7" class="px-4 py-6 text-center text-slate-400">No se encontraron clientes registrados.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Ficha de cliente / historial de compras -->
    <c:if test="${not empty clienteSeleccionado}">
        <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
            <div class="flex items-center justify-between">
                <div>
                    <div class="flex items-center gap-2">
                        <h3 class="text-lg font-semibold"><c:out value="${clienteSeleccionado.nombreCompleto}"/></h3>
                        <span class="px-2 py-0.5 rounded-full text-xs font-medium
                            ${clienteSeleccionado.categoriaFidelizacion == 'VIP' ? 'bg-purple-100 text-purple-700' :
                              clienteSeleccionado.categoriaFidelizacion == 'FRECUENTE' ? 'bg-blue-100 text-blue-700' : 'bg-slate-100 text-slate-500'}">
                            <c:out value="${clienteSeleccionado.categoriaFidelizacion}"/>
                        </span>
                    </div>
                    <p class="text-sm text-slate-500">
                        <c:out value="${clienteSeleccionado.tipoDocumento}"/>: <c:out value="${clienteSeleccionado.numeroDocumento}"/>
                        · Tel: <c:out value="${clienteSeleccionado.telefono}"/>
                        · <c:out value="${clienteSeleccionado.correo}"/>
                        · <c:out value="${clienteSeleccionado.puntosFidelizacion}"/> puntos acumulados
                    </p>
                </div>
                <div class="text-right">
                    <span class="text-xs text-slate-500 uppercase">Total histórico comprado</span>
                    <p class="text-xl font-bold">S/ <c:out value="${totalHistoricoCompras}"/></p>
                </div>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/clientes" class="grid grid-cols-2 gap-3 bg-slate-50 rounded-lg p-4">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="accion" value="actualizar">
                <input type="hidden" name="clienteId" value="${clienteSeleccionado.id}">
                <input type="text" name="nombreCompleto" value="${clienteSeleccionado.nombreCompleto}" placeholder="Nombre completo" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <input type="tel" name="telefono" value="${clienteSeleccionado.telefono}" placeholder="Teléfono" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <input type="email" name="correo" value="${clienteSeleccionado.correo}" placeholder="Correo *" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <input type="text" name="direccion" value="${clienteSeleccionado.direccion}" placeholder="Dirección" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                <div class="col-span-2 flex justify-end">
                    <button type="submit" class="h-9 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Actualizar datos</button>
                </div>
            </form>

            <h4 class="font-semibold text-sm mt-2">Historial de compras</h4>
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-3 py-2">Comprobante</th>
                    <th class="px-3 py-2">Fecha</th>
                    <th class="px-3 py-2 text-right">Total</th>
                    <th class="px-3 py-2 text-center">Estado</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="v" items="${historialCompras}">
                    <tr>
                        <td class="px-3 py-2 font-mono text-xs"><c:out value="${v.numeroComprobante}"/></td>
                        <td class="px-3 py-2 text-slate-500"><c:out value="${v.fechaCreacion}"/></td>
                        <td class="px-3 py-2 text-right font-medium">S/ <c:out value="${v.total}"/></td>
                        <td class="px-3 py-2 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs ${v.estado == 'COMPLETADA' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}">
                                <c:out value="${v.estado == 'COMPLETADA' ? 'OK' : 'Anulada'}"/>
                            </span>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty historialCompras}">
                    <tr><td colspan="4" class="px-3 py-4 text-center text-slate-400">Este cliente aún no registra compras.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </c:if>
</div>

<!-- Modal: Nuevo cliente -->
<div id="modal-nuevo-cliente" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-lg rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Nuevo cliente</h3>
            <button type="button" onclick="document.getElementById('modal-nuevo-cliente').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/clientes" class="p-6 flex flex-col gap-4" id="form-nuevo-cliente">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">
            <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Tipo de documento</label>
                    <select name="tipoDocumento" id="nc-tipo-documento" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        <option value="DNI">DNI</option>
                        <option value="RUC">RUC</option>
                    </select>
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">N° de documento *</label>
                    <div class="flex gap-2">
                        <input type="text" name="numeroDocumento" id="nc-numero-documento" required
                               inputmode="numeric" class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1">
                        <button type="button" id="nc-btn-buscar" onclick="buscarDocumentoCliente()"
                                class="h-9 px-3 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm font-medium whitespace-nowrap">
                            Buscar
                        </button>
                    </div>
                    <span id="nc-mensaje-busqueda" class="text-xs text-slate-500"></span>
                </div>
                <div class="col-span-2 flex flex-col gap-1">
                    <label class="text-sm font-medium">Nombre completo / Razón social *</label>
                    <input type="text" name="nombreCompleto" id="nc-nombre" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Teléfono</label>
                    <input type="tel" name="telefono" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Correo *</label>
                    <input type="email" name="correo" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <span class="text-xs text-slate-400">Obligatorio: se usa para enviarle la boleta de cada compra.</span>
                </div>
                <div class="col-span-2 flex flex-col gap-1">
                    <label class="text-sm font-medium">Dirección</label>
                    <input type="text" name="direccion" id="nc-direccion" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
            </div>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nuevo-cliente').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar cliente</button>
            </div>
        </form>
    </div>
</div>

<script>
    var CTX_PATH = '${pageContext.request.contextPath}';

    function buscarDocumentoCliente() {
        var tipo = document.getElementById('nc-tipo-documento').value;
        var numero = document.getElementById('nc-numero-documento').value.trim();
        var mensaje = document.getElementById('nc-mensaje-busqueda');
        var boton = document.getElementById('nc-btn-buscar');

        var formatoValido = tipo === 'DNI' ? /^\d{8}$/.test(numero) : /^\d{11}$/.test(numero);
        if (!formatoValido) {
            mensaje.textContent = tipo === 'DNI' ? 'El DNI debe tener 8 dígitos.' : 'El RUC debe tener 11 dígitos.';
            mensaje.className = 'text-xs text-red-600';
            return;
        }

        boton.disabled = true;
        boton.textContent = 'Buscando...';
        mensaje.textContent = '';

        fetch(CTX_PATH + '/api/consulta-documento?numero=' + encodeURIComponent(numero) + '&tipo=' + tipo)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.encontrado) {
                    document.getElementById('nc-nombre').value = data.nombre || '';
                    if (data.direccion) {
                        document.getElementById('nc-direccion').value = data.direccion;
                    }
                    mensaje.textContent = 'Datos encontrados. Puede corregirlos si hace falta.';
                    mensaje.className = 'text-xs text-emerald-600';
                } else {
                    mensaje.textContent = data.mensaje || 'No se encontró información. Complete los datos manualmente.';
                    mensaje.className = 'text-xs text-slate-500';
                }
            })
            .catch(function () {
                mensaje.textContent = 'No se pudo consultar en este momento. Complete los datos manualmente.';
                mensaje.className = 'text-xs text-slate-500';
            })
            .finally(function () {
                boton.disabled = false;
                boton.textContent = 'Buscar';
            });
    }

    // Deshabilita el botón "Buscar" mientras el formato no calce con el tipo elegido, para no
    // gastar cuota de la API en documentos obviamente inválidos.
    function actualizarEstadoBotonBuscar() {
        var tipo = document.getElementById('nc-tipo-documento').value;
        var numero = document.getElementById('nc-numero-documento').value.trim();
        var formatoValido = tipo === 'DNI' ? /^\d{8}$/.test(numero) : /^\d{11}$/.test(numero);
        document.getElementById('nc-btn-buscar').disabled = !formatoValido;
    }
    document.getElementById('nc-tipo-documento').addEventListener('change', actualizarEstadoBotonBuscar);
    document.getElementById('nc-numero-documento').addEventListener('input', actualizarEstadoBotonBuscar);
    actualizarEstadoBotonBuscar();
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
