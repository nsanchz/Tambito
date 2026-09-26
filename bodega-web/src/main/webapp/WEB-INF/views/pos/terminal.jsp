<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<c:if test="${not empty sessionScope.error}">
    <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm mb-4">
        <c:out value="${sessionScope.error}"/>
    </div>
    <c:remove var="error" scope="session"/>
</c:if>

<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

    <!-- Catálogo de productos -->
    <div class="lg:col-span-2 flex flex-col gap-4">
        <form method="get" action="${pageContext.request.contextPath}/pos" class="flex gap-2">
            <input type="text" name="busqueda" id="pos-search-input" autofocus
                   placeholder="Buscar por nombre, SKU o código de barras..."
                   value="${fn:escapeXml(param.busqueda)}"
                   class="flex-1 h-11 px-4 border border-slate-300 rounded-lg text-sm">
            <button type="submit" class="h-11 px-5 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm font-medium">Buscar</button>
        </form>

        <div class="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-3">
            <c:forEach var="p" items="${productos}">
                <button type="button" onclick="agregarAlCarrito(${p.id}, '${fn:escapeXml(p.nombre)}', ${p.precioVenta}, ${p.stockActual})"
                        class="bg-white rounded-xl shadow-sm p-3 text-left hover:shadow-md transition-shadow flex flex-col gap-1 ${p.stockActual == 0 ? 'opacity-40 pointer-events-none' : ''}">
                    <span class="text-sm font-medium line-clamp-2"><c:out value="${p.nombre}"/></span>
                    <span class="text-xs text-slate-400">Stock: <c:out value="${p.stockActual}"/></span>
                    <span class="text-blue-600 font-semibold">S/ <c:out value="${p.precioVenta}"/></span>
                </button>
            </c:forEach>
            <c:if test="${empty productos}">
                <p class="text-slate-400 text-sm col-span-full text-center py-10">No se encontraron productos.</p>
            </c:if>
        </div>
    </div>

    <!-- Carrito / Panel de venta -->
    <div class="bg-white rounded-xl shadow-sm p-4 flex flex-col gap-4 h-fit sticky top-20">
        <h3 class="font-semibold text-slate-900">Venta actual</h3>

        <!-- Datos del cliente -->
        <div class="flex flex-col gap-2">
            <div class="grid grid-cols-2 gap-1 bg-slate-100 rounded-lg p-1">
                <label class="text-center text-xs py-1.5 rounded-md cursor-pointer has-[:checked]:bg-white has-[:checked]:font-medium has-[:checked]:shadow-sm">
                    <input type="radio" name="clienteTipo" value="final" class="hidden" onclick="mostrarCamposCliente(false)" checked> Cliente varios
                </label>
                <label class="text-center text-xs py-1.5 rounded-md cursor-pointer has-[:checked]:bg-white has-[:checked]:font-medium has-[:checked]:shadow-sm">
                    <input type="radio" name="clienteTipo" value="registrado" class="hidden" onclick="mostrarCamposCliente(true)"> Cliente registrado
                </label>
            </div>
            <div id="campos-cliente" class="hidden flex-col gap-2">
                <select name="clienteTipoDocumento" id="pos-tipo-documento" class="h-8 px-2 border border-slate-300 rounded-lg text-xs">
                    <option value="DNI">DNI</option>
                    <option value="RUC">RUC</option>
                </select>
                <div class="flex gap-1 min-w-0">
                    <input type="text" name="clienteNumeroDocumento" id="pos-numero-documento" placeholder="N° de documento"
                           inputmode="numeric" class="h-8 px-2 border border-slate-300 rounded-lg text-xs flex-1 min-w-0">
                    <button type="button" id="pos-btn-buscar" onclick="buscarDocumentoClientePos()"
                            class="h-8 px-2 bg-slate-100 hover:bg-slate-200 rounded-lg text-xs font-medium whitespace-nowrap shrink-0">Buscar</button>
                </div>
                <span id="pos-mensaje-busqueda" class="text-xs text-slate-500"></span>
                <input type="text" name="clienteNombre" id="pos-nombre" placeholder="Nombre / Razón social" class="h-8 px-2 border border-slate-300 rounded-lg text-xs">
                <input type="email" name="clienteCorreo" id="pos-correo" placeholder="Correo (para enviar la boleta) *" class="h-8 px-2 border border-slate-300 rounded-lg text-xs">
                <input type="tel" name="clienteTelefono" placeholder="Teléfono (opcional)" class="h-8 px-2 border border-slate-300 rounded-lg text-xs">
            </div>
        </div>

        <!-- Líneas del carrito -->
        <div id="lineas-carrito" class="flex flex-col gap-2 max-h-64 overflow-y-auto"></div>
        <p id="carrito-vacio" class="text-xs text-slate-400 text-center py-4">Aún no ha agregado productos.</p>

        <!-- Comprobante y descuento -->
        <div class="grid grid-cols-2 gap-2">
            <select name="tipoComprobante" id="tipo-comprobante" class="h-9 px-2 border border-slate-300 rounded-lg text-sm">
                <option value="BOLETA">Boleta</option>
                <option value="FACTURA" id="opcion-factura" disabled>Factura</option>
            </select>
            <input type="number" step="0.01" min="0" id="descuento" placeholder="Descuento S/" value="0"
                   class="h-9 px-2 border border-slate-300 rounded-lg text-sm" oninput="recalcularTotales()">
        </div>

        <!-- Totales -->
        <div class="flex flex-col gap-1 text-sm border-t border-slate-100 pt-3">
            <div class="flex justify-between text-slate-500"><span>Subtotal imponible</span><span id="label-subtotal">S/ 0.00</span></div>
            <div class="flex justify-between text-slate-500"><span>Descuento</span><span id="label-descuento">S/ 0.00</span></div>
            <div class="flex justify-between text-slate-500"><span>IGV (18%)</span><span id="label-igv">S/ 0.00</span></div>
            <div class="flex justify-between text-base font-bold text-slate-900 pt-1"><span>TOTAL A PAGAR</span><span id="label-total">S/ 0.00</span></div>
        </div>

        <!-- Método de pago -->
        <div class="grid grid-cols-4 gap-1">
            <button type="button" data-metodo="EFECTIVO" onclick="seleccionarMetodoPago(this)" class="metodo-pago-btn h-9 rounded-lg text-xs bg-blue-600 text-white">Efectivo</button>
            <button type="button" data-metodo="TARJETA" onclick="seleccionarMetodoPago(this)" class="metodo-pago-btn h-9 rounded-lg text-xs bg-slate-100">Tarjeta</button>
            <button type="button" data-metodo="YAPE_PLIN" onclick="seleccionarMetodoPago(this)" class="metodo-pago-btn h-9 rounded-lg text-xs bg-slate-100">Yape/Plin</button>
            <button type="button" data-metodo="TRANSFERENCIA" onclick="seleccionarMetodoPago(this)" class="metodo-pago-btn h-9 rounded-lg text-xs bg-slate-100">Transf.</button>
        </div>

        <!-- Pago en dólares en efectivo: solo aplica con EFECTIVO. -->
        <div id="bloque-pago-usd" class="flex flex-col gap-2 bg-emerald-50 rounded-lg p-3">
            <label class="flex items-center gap-2 text-xs font-medium text-emerald-800">
                <input type="checkbox" id="check-pagar-usd" onchange="alternarPagoUsd()">
                Cliente paga en efectivo en dólares (USD)
            </label>
            <div id="detalle-pago-usd" class="hidden flex-col gap-2">
                <p id="texto-tipo-cambio" class="text-xs text-emerald-700">Consultando tipo de cambio…</p>
                <div class="flex items-center justify-between text-sm font-semibold text-emerald-900">
                    <span>Equivalente a cobrar</span>
                    <span id="label-total-usd">$ 0.00</span>
                </div>
                <input type="number" step="0.01" min="0" id="monto-recibido-usd" placeholder="Monto recibido en USD"
                       class="h-9 px-2 border border-emerald-300 rounded-lg text-sm" oninput="actualizarVueltoUsd()">
                <p id="texto-vuelto-usd" class="text-xs text-emerald-700"></p>
            </div>
        </div>

        <!-- QR de referencia para pago con Yape/Plin (no es una pasarela real: es un código
             visual con el monto a transferir, para que el cliente confirme antes de pagar). -->
        <div id="contenedor-qr-yape" class="hidden flex-col items-center gap-2 bg-slate-50 rounded-lg p-3">
            <img id="img-qr-yape" src="" alt="Código QR para pago con Yape/Plin" class="w-32 h-32">
            <span class="text-xs text-slate-500">Escanee para pagar <strong id="label-monto-qr-yape">S/ 0.00</strong> con Yape/Plin</span>
        </div>

        <button type="button" onclick="confirmarVenta()"
                class="h-11 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold text-sm">
            Cobrar venta (F12)
        </button>
    </div>
</div>

<!-- Formulario oculto que se envía al confirmar la venta -->
<form id="form-venta" method="post" action="${pageContext.request.contextPath}/pos"></form>

<script>
    var carrito = [];
    var metodoPagoSeleccionado = 'EFECTIVO';
    var tipoCambioDia = null; // {compra, venta} cargado una sola vez desde /api/tipo-cambio
    var totalActualSoles = 0;

    function mostrarCamposCliente(mostrar) {
        document.getElementById('campos-cliente').classList.toggle('hidden', !mostrar);
        document.getElementById('campos-cliente').classList.toggle('flex', mostrar);

        // "Cliente varios" (sin documento) solo puede recibir Boleta: la Factura requiere
        // un cliente identificado con RUC. Se fuerza y se bloquea la opción en el <select>.
        var comprobanteSelect = document.getElementById('tipo-comprobante');
        var opcionFactura = document.getElementById('opcion-factura');
        opcionFactura.disabled = !mostrar;
        if (!mostrar) {
            comprobanteSelect.value = 'BOLETA';
        }
    }

    function agregarAlCarrito(id, nombre, precio, stockDisponible) {
        var existente = carrito.find(function (item) { return item.id === id; });
        if (existente) {
            if (existente.cantidad < stockDisponible) {
                existente.cantidad++;
            }
        } else {
            carrito.push({id: id, nombre: nombre, precio: precio, cantidad: 1, stockDisponible: stockDisponible});
        }
        renderizarCarrito();
    }

    function cambiarCantidad(id, delta) {
        var item = carrito.find(function (i) { return i.id === id; });
        if (!item) return;
        item.cantidad += delta;
        if (item.cantidad <= 0) {
            carrito = carrito.filter(function (i) { return i.id !== id; });
        } else if (item.cantidad > item.stockDisponible) {
            item.cantidad = item.stockDisponible;
        }
        renderizarCarrito();
    }

    function renderizarCarrito() {
        var contenedor = document.getElementById('lineas-carrito');
        var vacio = document.getElementById('carrito-vacio');
        contenedor.innerHTML = '';
        vacio.style.display = carrito.length === 0 ? 'block' : 'none';

        carrito.forEach(function (item) {
            var fila = document.createElement('div');
            fila.className = 'flex items-center justify-between gap-2 text-sm';
            fila.innerHTML =
                '<span class="flex-1 truncate">' + item.nombre + '</span>' +
                '<div class="flex items-center gap-1">' +
                '  <button type="button" onclick="cambiarCantidad(' + item.id + ', -1)" class="w-6 h-6 bg-slate-100 rounded">-</button>' +
                '  <span class="w-6 text-center">' + item.cantidad + '</span>' +
                '  <button type="button" onclick="cambiarCantidad(' + item.id + ', 1)" class="w-6 h-6 bg-slate-100 rounded">+</button>' +
                '</div>' +
                '<span class="w-16 text-right font-medium">S/ ' + (item.precio * item.cantidad).toFixed(2) + '</span>';
            contenedor.appendChild(fila);
        });

        recalcularTotales();
    }

    var NOMBRE_EMPRESA_QR = '<c:out value="${sessionScope.nombreEmpresa}" default="BodegaControl"/>';

    function recalcularTotales() {
        var subtotal = carrito.reduce(function (acc, item) { return acc + item.precio * item.cantidad; }, 0);
        var descuento = parseFloat(document.getElementById('descuento').value) || 0;
        if (descuento > subtotal) descuento = subtotal;
        var base = subtotal - descuento;
        var igv = base * 0.18;
        var total = base + igv;

        document.getElementById('label-subtotal').textContent = 'S/ ' + subtotal.toFixed(2);
        document.getElementById('label-descuento').textContent = 'S/ ' + descuento.toFixed(2);
        document.getElementById('label-igv').textContent = 'S/ ' + igv.toFixed(2);
        document.getElementById('label-total').textContent = 'S/ ' + total.toFixed(2);
        totalActualSoles = total;

        actualizarQrYape(total);
        actualizarEquivalenteUsd();
    }

    function actualizarQrYape(total) {
        var contenedor = document.getElementById('contenedor-qr-yape');
        if (metodoPagoSeleccionado !== 'YAPE_PLIN' || total <= 0) {
            contenedor.classList.add('hidden');
            contenedor.classList.remove('flex');
            return;
        }
        contenedor.classList.remove('hidden');
        contenedor.classList.add('flex');
        document.getElementById('label-monto-qr-yape').textContent = 'S/ ' + total.toFixed(2);

        var base = document.querySelector('meta[name="context-path"]').getAttribute('content');
        var texto = NOMBRE_EMPRESA_QR + ' - Pago Yape/Plin - S/ ' + total.toFixed(2);
        document.getElementById('img-qr-yape').src = base + '/api/qr?texto=' + encodeURIComponent(texto);
    }

    function seleccionarMetodoPago(btn) {
        document.querySelectorAll('.metodo-pago-btn').forEach(function (b) {
            b.classList.remove('bg-blue-600', 'text-white');
            b.classList.add('bg-slate-100');
        });
        btn.classList.remove('bg-slate-100');
        btn.classList.add('bg-blue-600', 'text-white');
        metodoPagoSeleccionado = btn.getAttribute('data-metodo');

        var bloqueUsd = document.getElementById('bloque-pago-usd');
        if (metodoPagoSeleccionado === 'EFECTIVO') {
            bloqueUsd.classList.remove('hidden');
            bloqueUsd.classList.add('flex');
        } else {
            bloqueUsd.classList.add('hidden');
            bloqueUsd.classList.remove('flex');
            document.getElementById('check-pagar-usd').checked = false;
            alternarPagoUsd();
        }

        recalcularTotales();
    }

    function alternarPagoUsd() {
        var activo = document.getElementById('check-pagar-usd').checked;
        var detalle = document.getElementById('detalle-pago-usd');
        detalle.classList.toggle('hidden', !activo);
        detalle.classList.toggle('flex', activo);

        if (activo && !tipoCambioDia) {
            fetch('${pageContext.request.contextPath}/api/tipo-cambio')
                .then(function (r) { return r.json(); })
                .then(function (data) {
                    var texto = document.getElementById('texto-tipo-cambio');
                    if (data.disponible) {
                        tipoCambioDia = data;
                        texto.textContent = 'Tipo de cambio (compra): S/ ' + data.compra;
                        actualizarEquivalenteUsd();
                    } else {
                        texto.textContent = 'Tipo de cambio no disponible en este momento. No se puede cobrar en USD.';
                        document.getElementById('check-pagar-usd').checked = false;
                        alternarPagoUsd();
                    }
                })
                .catch(function () {
                    document.getElementById('texto-tipo-cambio').textContent = 'No se pudo consultar el tipo de cambio.';
                    document.getElementById('check-pagar-usd').checked = false;
                    alternarPagoUsd();
                });
        }
        if (activo) { actualizarEquivalenteUsd(); }
    }

    function actualizarEquivalenteUsd() {
        if (!document.getElementById('check-pagar-usd').checked || !tipoCambioDia) return;
        var equivalente = totalActualSoles / parseFloat(tipoCambioDia.compra);
        document.getElementById('label-total-usd').textContent = '$ ' + equivalente.toFixed(2);
        actualizarVueltoUsd();
    }

    function actualizarVueltoUsd() {
        if (!tipoCambioDia) return;
        var equivalente = totalActualSoles / parseFloat(tipoCambioDia.compra);
        var recibido = parseFloat(document.getElementById('monto-recibido-usd').value) || 0;
        var texto = document.getElementById('texto-vuelto-usd');
        if (recibido <= 0) {
            texto.textContent = '';
        } else if (recibido < equivalente) {
            texto.textContent = 'Falta: $ ' + (equivalente - recibido).toFixed(2);
        } else {
            texto.textContent = 'Vuelto: $ ' + (recibido - equivalente).toFixed(2);
        }
    }

    function confirmarVenta() {
        if (carrito.length === 0) {
            alert('Agregue al menos un producto al carrito antes de cobrar.');
            return;
        }

        var form = document.getElementById('form-venta');
        form.innerHTML = '';

        function agregarCampoOculto(nombre, valor) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = nombre;
            input.value = valor;
            form.appendChild(input);
        }

        agregarCampoOculto('csrfToken', '${sessionScope.csrfToken}');

        carrito.forEach(function (item) {
            agregarCampoOculto('carritoProductoId', item.id);
            agregarCampoOculto('carritoCantidad', item.cantidad);
        });

        agregarCampoOculto('tipoComprobante', document.getElementById('tipo-comprobante').value);
        agregarCampoOculto('metodoPago', metodoPagoSeleccionado);
        agregarCampoOculto('descuento', document.getElementById('descuento').value || '0');

        var pagaEnUsd = metodoPagoSeleccionado === 'EFECTIVO' && document.getElementById('check-pagar-usd').checked;
        if (pagaEnUsd) {
            agregarCampoOculto('monedaPago', 'USD');
            agregarCampoOculto('montoRecibidoUsd', document.getElementById('monto-recibido-usd').value || '0');
        }

        var clienteTipo = document.querySelector('input[name="clienteTipo"]:checked').value;
        agregarCampoOculto('clienteTipo', clienteTipo);
        if (clienteTipo === 'registrado') {
            ['clienteTipoDocumento', 'clienteNumeroDocumento', 'clienteNombre', 'clienteCorreo', 'clienteTelefono'].forEach(function (campo) {
                var el = document.querySelector('[name="' + campo + '"]');
                agregarCampoOculto(campo, el ? el.value : '');
            });
        }

        form.submit();
    }

    function buscarDocumentoClientePos() {
        var tipo = document.getElementById('pos-tipo-documento').value;
        var numero = document.getElementById('pos-numero-documento').value.trim();
        var mensaje = document.getElementById('pos-mensaje-busqueda');
        var boton = document.getElementById('pos-btn-buscar');

        var formatoValido = tipo === 'DNI' ? /^\d{8}$/.test(numero) : /^\d{11}$/.test(numero);
        if (!formatoValido) {
            mensaje.textContent = tipo === 'DNI' ? 'El DNI debe tener 8 dígitos.' : 'El RUC debe tener 11 dígitos.';
            mensaje.className = 'text-xs text-red-600';
            return;
        }

        boton.disabled = true;
        boton.textContent = '...';

        fetch('${pageContext.request.contextPath}/api/consulta-documento?numero=' + encodeURIComponent(numero) + '&tipo=' + tipo)
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (data.encontrado) {
                    document.getElementById('pos-nombre').value = data.nombre || '';
                    if (data.correo) {
                        document.getElementById('pos-correo').value = data.correo;
                    }
                    mensaje.textContent = data.yaRegistrado
                        ? 'Cliente ya registrado: se usarán sus datos.'
                        : 'Datos encontrados.';
                    mensaje.className = 'text-xs text-emerald-600';
                } else {
                    mensaje.textContent = data.mensaje || 'No se encontró información. Complete los datos manualmente.';
                    mensaje.className = 'text-xs text-slate-500';
                }
            })
            .catch(function () {
                mensaje.textContent = 'No se pudo consultar en este momento.';
                mensaje.className = 'text-xs text-slate-500';
            })
            .finally(function () {
                boton.disabled = false;
                boton.textContent = 'Buscar';
            });
    }

    document.addEventListener('keydown', function (e) {
        if (e.key === 'F12') {
            e.preventDefault();
            confirmarVenta();
        }
    });
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
