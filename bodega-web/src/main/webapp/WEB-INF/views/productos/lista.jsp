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
            <h2 class="text-2xl font-bold">Gestión de Productos y Catálogo</h2>
            <p class="text-sm text-slate-500">Catálogo maestro de precios, stock y clasificación comercial.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nuevo-producto').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nuevo producto
        </button>
    </div>

    <form method="get" action="${pageContext.request.contextPath}/productos"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por SKU, código de barras, nombre o marca..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="categoriaId" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="">Todas las categorías</option>
            <c:forEach var="cat" items="${categorias}">
                <option value="${cat.id}" ${param.categoriaId == cat.id ? 'selected' : ''}><c:out value="${cat.nombre}"/></option>
            </c:forEach>
        </select>
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
                    <th class="px-4 py-3">SKU</th>
                    <th class="px-4 py-3">Producto</th>
                    <th class="px-4 py-3">Marca</th>
                    <th class="px-4 py-3 text-right">Precio compra</th>
                    <th class="px-4 py-3 text-right">Precio venta</th>
                    <th class="px-4 py-3 text-center">Stock</th>
                    <th class="px-4 py-3">Vencimiento</th>
                    <th class="px-4 py-3 text-center">Estado</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="p" items="${productos}">
                    <tr class="hover:bg-slate-50 ${not empty productoSeleccionado and productoSeleccionado.id == p.id ? 'bg-blue-50' : ''}">
                        <td class="px-4 py-3 font-mono text-slate-500"><c:out value="${p.sku}"/></td>
                        <td class="px-4 py-3 font-medium"><c:out value="${p.nombre}"/></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${p.marca}"/></td>
                        <td class="px-4 py-3 text-right">S/ <c:out value="${p.precioCompra}"/></td>
                        <td class="px-4 py-3 text-right font-medium">S/ <c:out value="${p.precioVenta}"/></td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded text-xs font-medium ${p.stockBajoMinimo ? 'bg-red-100 text-red-700' : 'bg-slate-100 text-slate-600'}">
                                <c:out value="${p.stockActual}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-slate-500">
                            <c:out value="${not empty proximosVencimientos[p.id] ? proximosVencimientos[p.id] : 'No perecible'}"/>
                        </td>
                        <td class="px-4 py-3 text-center">
                            <span class="px-2 py-0.5 rounded-full text-xs font-medium ${p.estado == 'ACTIVO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${p.estado == 'ACTIVO' ? 'Activo' : 'Inactivo'}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3 text-right whitespace-nowrap">
                            <a href="${pageContext.request.contextPath}/productos?ver=${p.id}" class="text-blue-600 hover:underline mr-3">Ver detalle</a>
                            <form method="post" action="${pageContext.request.contextPath}/productos" class="inline">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="accion" value="${p.estado == 'ACTIVO' ? 'desactivar' : 'reactivar'}">
                                <input type="hidden" name="productoId" value="${p.id}">
                                <button type="submit" class="text-slate-500 hover:text-red-600">
                                    <c:out value="${p.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                </button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty productos}">
                    <tr><td colspan="9" class="px-4 py-6 text-center text-slate-400">No se encontraron productos con los filtros aplicados.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>

    <!-- Detalle de un producto seleccionado -->
    <c:if test="${not empty productoSeleccionado}">
        <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
            <div class="flex items-center justify-between">
                <h3 class="text-lg font-semibold">Detalle: <c:out value="${productoSeleccionado.nombre}"/></h3>
                <a href="${pageContext.request.contextPath}/productos" class="text-xs text-slate-500 hover:underline">Cerrar detalle</a>
            </div>

            <c:choose>
                <c:when test="${sessionScope.usuarioRol == 'ADMINISTRADOR'}">
                    <form method="post" action="${pageContext.request.contextPath}/productos" class="grid grid-cols-2 gap-4">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" name="accion" value="editar">
                        <input type="hidden" name="productoId" value="${productoSeleccionado.id}">
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Código interno (SKU)</label>
                            <input type="text" value="${productoSeleccionado.sku}" disabled
                                   class="h-9 px-3 border border-slate-200 bg-slate-50 rounded-lg text-sm text-slate-500">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Código de barras</label>
                            <input type="text" name="codigoBarras" value="${productoSeleccionado.codigoBarras}"
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="col-span-2 flex flex-col gap-1">
                            <label class="text-sm font-medium">Nombre del producto *</label>
                            <input type="text" name="nombre" value="${productoSeleccionado.nombre}" required
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="col-span-2 flex flex-col gap-1">
                            <label class="text-sm font-medium">Descripción detallada</label>
                            <textarea name="descripcion" rows="2"
                                      class="p-3 border border-slate-300 rounded-lg text-sm resize-none">${productoSeleccionado.descripcion}</textarea>
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Categoría *</label>
                            <select name="categoriaId" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                                <c:forEach var="cat" items="${categorias}">
                                    <option value="${cat.id}" ${cat.id == productoSeleccionado.categoriaId ? 'selected' : ''}><c:out value="${cat.nombre}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Marca *</label>
                            <input type="text" name="marca" value="${productoSeleccionado.marca}" required
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Proveedor</label>
                            <select name="proveedorId" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                                <option value="">Sin proveedor asignado</option>
                                <c:forEach var="prov" items="${proveedores}">
                                    <option value="${prov.id}" ${prov.id == productoSeleccionado.proveedorId ? 'selected' : ''}><c:out value="${prov.razonSocial}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Precio de compra (S/) *</label>
                            <input type="number" step="0.01" name="precioCompra" value="${productoSeleccionado.precioCompra}" required
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Precio de venta (S/) *</label>
                            <input type="number" step="0.01" name="precioVenta" value="${productoSeleccionado.precioVenta}" required
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Stock actual</label>
                            <input type="text" value="${productoSeleccionado.stockActual}" disabled
                                   class="h-9 px-3 border border-slate-200 bg-slate-50 rounded-lg text-sm text-slate-500">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Stock mínimo de alerta *</label>
                            <input type="number" name="stockMinimo" value="${productoSeleccionado.stockMinimo}" required
                                   class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Unidad de medida *</label>
                            <select name="unidadMedida" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                                <c:forEach var="unidad" items="${unidadesMedida}">
                                    <option value="${unidad}" ${unidad == productoSeleccionado.unidadMedida ? 'selected' : ''}><c:out value="${unidad}"/></option>
                                </c:forEach>
                            </select>
                        </div>
                        <div class="col-span-2 flex items-center justify-end gap-3 pt-2">
                            <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar cambios</button>
                        </div>
                    </form>
                </c:when>
                <c:otherwise>
                    <!-- VENDEDOR: solo lectura, la edición está reservada al administrador -->
                    <div class="grid grid-cols-2 gap-4 text-sm">
                        <div><span class="text-slate-400">SKU:</span> <c:out value="${productoSeleccionado.sku}"/></div>
                        <div><span class="text-slate-400">Código de barras:</span> <c:out value="${not empty productoSeleccionado.codigoBarras ? productoSeleccionado.codigoBarras : '-'}"/></div>
                        <div class="col-span-2"><span class="text-slate-400">Descripción:</span> <c:out value="${not empty productoSeleccionado.descripcion ? productoSeleccionado.descripcion : '-'}"/></div>
                        <div><span class="text-slate-400">Categoría:</span> <c:out value="${categoriaSeleccionada.nombre}"/></div>
                        <div><span class="text-slate-400">Marca:</span> <c:out value="${productoSeleccionado.marca}"/></div>
                        <div><span class="text-slate-400">Proveedor:</span> <c:out value="${not empty proveedorSeleccionado ? proveedorSeleccionado.razonSocial : 'Sin proveedor asignado'}"/></div>
                        <div><span class="text-slate-400">Precio de compra:</span> S/ <c:out value="${productoSeleccionado.precioCompra}"/></div>
                        <div><span class="text-slate-400">Precio de venta:</span> S/ <c:out value="${productoSeleccionado.precioVenta}"/></div>
                        <div><span class="text-slate-400">Stock actual:</span> <c:out value="${productoSeleccionado.stockActual}"/></div>
                        <div><span class="text-slate-400">Stock mínimo:</span> <c:out value="${productoSeleccionado.stockMinimo}"/></div>
                        <div><span class="text-slate-400">Unidad de medida:</span> <c:out value="${productoSeleccionado.unidadMedida}"/></div>
                    </div>
                    <p class="text-xs text-slate-400">Solo un administrador puede editar los datos del producto.</p>
                </c:otherwise>
            </c:choose>

            <c:if test="${not empty lotesDelProducto}">
                <div class="pt-2 border-t border-slate-100 flex flex-col gap-2">
                    <h4 class="text-sm font-semibold">Lotes</h4>
                    <table class="w-full text-left text-sm">
                        <thead>
                        <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                            <th class="px-3 py-2">N° Lote</th>
                            <th class="px-3 py-2">Vencimiento</th>
                            <th class="px-3 py-2 text-right">Cantidad actual</th>
                        </tr>
                        </thead>
                        <tbody class="divide-y divide-slate-100">
                        <c:forEach var="l" items="${lotesDelProducto}">
                            <tr>
                                <td class="px-3 py-2 font-mono text-slate-500"><c:out value="${l.numeroLote}"/></td>
                                <td class="px-3 py-2"><c:out value="${not empty l.fechaVencimiento ? l.fechaVencimiento : 'No perecible'}"/></td>
                                <td class="px-3 py-2 text-right"><c:out value="${l.cantidadActual}"/></td>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </c:if>
        </div>
    </c:if>
</div>

<!-- Modal: Nuevo producto -->
<div id="modal-nuevo-producto" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-3xl max-h-[90vh] overflow-y-auto rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Registrar nuevo producto</h3>
            <button type="button" onclick="document.getElementById('modal-nuevo-producto').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/productos" class="p-6 grid grid-cols-2 gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Código interno (SKU) *</label>
                <input type="text" name="sku" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Código de barras EAN-13</label>
                <input type="text" name="codigoBarras" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="col-span-2 flex flex-col gap-1">
                <label class="text-sm font-medium">Nombre del producto *</label>
                <input type="text" name="nombre" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="col-span-2 flex flex-col gap-1">
                <label class="text-sm font-medium">Descripción detallada</label>
                <textarea name="descripcion" rows="2" class="p-3 border border-slate-300 rounded-lg text-sm resize-none"></textarea>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Categoría *</label>
                <select name="categoriaId" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="">Seleccione una categoría</option>
                    <c:forEach var="cat" items="${categorias}">
                        <option value="${cat.id}"><c:out value="${cat.nombre}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Marca *</label>
                <input type="text" name="marca" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Proveedor</label>
                <select name="proveedorId" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="">Sin proveedor asignado</option>
                    <c:forEach var="prov" items="${proveedores}">
                        <option value="${prov.id}"><c:out value="${prov.razonSocial}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Precio de compra (S/) *</label>
                <input type="number" step="0.01" name="precioCompra" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Precio de venta (S/) *</label>
                <input type="number" step="0.01" name="precioVenta" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Stock inicial *</label>
                <input type="number" name="stockInicial" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Stock mínimo de alerta *</label>
                <input type="number" name="stockMinimo" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Unidad de medida *</label>
                <select name="unidadMedida" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <c:forEach var="unidad" items="${unidadesMedida}">
                        <option value="${unidad}"><c:out value="${unidad}"/></option>
                    </c:forEach>
                </select>
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Estado inicial</label>
                <select name="estadoInicial" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    <option value="ACTIVO">Activo (disponible para venta)</option>
                    <option value="INACTIVO">Inactivo (no disponible en POS)</option>
                </select>
            </div>
            <div class="col-span-2 flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nuevo-producto').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar producto</button>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
