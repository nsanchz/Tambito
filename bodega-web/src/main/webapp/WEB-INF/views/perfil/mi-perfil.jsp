<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ include file="/WEB-INF/views/layout/cabecera.jspf" %>

<div class="flex flex-col gap-6 max-w-2xl">

    <c:if test="${cambioObligatorio}">
        <div class="bg-amber-50 border border-amber-200 text-amber-800 rounded-lg p-3 text-sm">
            Por política de seguridad, debe cambiar su contraseña temporal antes de continuar usando el sistema.
        </div>
    </c:if>
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
        <h2 class="text-2xl font-bold">Mi Perfil de Usuario</h2>
        <p class="text-sm text-slate-500">Rol: <c:out value="${usuario.rol}"/> · Usuario: <c:out value="${usuario.nombreUsuario}"/></p>
    </div>

    <!-- Datos personales -->
    <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
        <h3 class="font-semibold">Datos personales</h3>
        <c:choose>
            <c:when test="${usuario.rol == 'VENDEDOR'}">
                <div class="grid grid-cols-2 gap-4 text-sm">
                    <div><span class="text-slate-400">Nombres:</span> <c:out value="${usuario.nombres}"/></div>
                    <div><span class="text-slate-400">Apellidos:</span> <c:out value="${usuario.apellidos}"/></div>
                    <div><span class="text-slate-400">Correo:</span> <c:out value="${usuario.correo}"/></div>
                    <div><span class="text-slate-400">Teléfono:</span> <c:out value="${not empty usuario.telefono ? usuario.telefono : '-'}"/></div>
                </div>
                <p class="text-xs text-slate-400">Solo un administrador puede corregir estos datos, desde el módulo de Usuarios.</p>
            </c:when>
            <c:otherwise>
                <form method="post" action="${pageContext.request.contextPath}/perfil" class="grid grid-cols-2 gap-4"
                      onsubmit="return iniciarEnvioConCarga(this, 'btn-guardar-datos', 'Guardando...')">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="accion" value="actualizarDatos">
                    <div class="flex flex-col gap-1">
                        <label class="text-sm font-medium">Nombres</label>
                        <input type="text" name="nombres" value="${usuario.nombres}" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    </div>
                    <div class="flex flex-col gap-1">
                        <label class="text-sm font-medium">Apellidos</label>
                        <input type="text" name="apellidos" value="${usuario.apellidos}" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    </div>
                    <div class="flex flex-col gap-1">
                        <label class="text-sm font-medium">Correo</label>
                        <input type="email" name="correo" value="${usuario.correo}" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    </div>
                    <div class="flex flex-col gap-1">
                        <label class="text-sm font-medium">Teléfono</label>
                        <input type="tel" name="telefono" value="${usuario.telefono}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                    </div>
                    <div class="col-span-2 flex justify-end">
                        <button type="submit" id="btn-guardar-datos" class="h-9 px-5 btn-primario text-white rounded-lg text-sm font-medium">Guardar datos</button>
                    </div>
                </form>
            </c:otherwise>
        </c:choose>
    </div>

    <!-- Cambio de contraseña -->
    <div class="bg-white rounded-xl shadow-sm p-6 flex flex-col gap-4">
        <h3 class="font-semibold">Seguridad de credenciales</h3>
        <c:choose>
            <c:when test="${usuario.rol == 'VENDEDOR' and not usuario.debeCambiarPassword}">
                <p class="text-sm text-slate-500">
                    Ya cambió su contraseña temporal. Solo puede volver a cambiarla la primera vez que
                    inicia sesión o cuando un administrador se la restablezca.
                </p>
            </c:when>
            <c:otherwise>
                <form method="post" action="${pageContext.request.contextPath}/perfil" class="flex flex-col gap-4"
                      onsubmit="return iniciarEnvioConCarga(this, 'btn-cambiar-password', 'Guardando...')">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" name="accion" value="cambiarPassword">
                    <div class="flex flex-col gap-1">
                        <label class="text-sm font-medium">Contraseña actual *</label>
                        <div class="relative">
                            <input type="password" id="password-actual" name="passwordActual" required
                                   class="h-9 px-3 pr-16 border border-slate-300 rounded-lg text-sm w-full">
                            <button type="button" onclick="alternarVisibilidadPassword('password-actual', this)"
                                    class="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-blue-600">Ver</button>
                        </div>
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Nueva contraseña *</label>
                            <div class="relative">
                                <input type="password" id="password-nueva" name="passwordNueva" required minlength="8"
                                       oninput="actualizarFortalezaPassword('password-nueva', 'fortaleza-nueva')"
                                       class="h-9 px-3 pr-16 border border-slate-300 rounded-lg text-sm w-full">
                                <button type="button" onclick="alternarVisibilidadPassword('password-nueva', this)"
                                        class="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-blue-600">Ver</button>
                            </div>
                            <div class="mt-1" id="fortaleza-nueva">
                                <div class="flex items-center justify-between text-xs mb-1">
                                    <span class="text-slate-500">Seguridad de la contraseña</span>
                                    <span class="fortaleza-label font-medium text-slate-400">—</span>
                                </div>
                                <div class="h-1.5 bg-slate-100 rounded-full overflow-hidden">
                                    <div class="fortaleza-barra h-full bg-slate-300 transition-all" style="width:0%"></div>
                                </div>
                                <div class="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs">
                                    <span data-req="len" class="flex items-center gap-1 text-slate-400">○ 8+ caracteres</span>
                                    <span data-req="mayus" class="flex items-center gap-1 text-slate-400">○ A-Z</span>
                                    <span data-req="minus" class="flex items-center gap-1 text-slate-400">○ a-z</span>
                                    <span data-req="digito" class="flex items-center gap-1 text-slate-400">○ 123</span>
                                </div>
                            </div>
                        </div>
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Confirmar nueva contraseña *</label>
                            <div class="relative">
                                <input type="password" id="password-confirmar" name="confirmarPasswordNueva" required minlength="8"
                                       class="h-9 px-3 pr-16 border border-slate-300 rounded-lg text-sm w-full">
                                <button type="button" onclick="alternarVisibilidadPassword('password-confirmar', this)"
                                        class="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-blue-600">Ver</button>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end">
                        <button type="submit" id="btn-cambiar-password" class="h-9 px-5 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-sm font-medium transition-colors">Cambiar contraseña</button>
                    </div>
                </form>
                <script>
                    function alternarVisibilidadPassword(idCampo, boton) {
                        var campo = document.getElementById(idCampo);
                        var esOculta = campo.type === 'password';
                        campo.type = esOculta ? 'text' : 'password';
                        boton.textContent = esOculta ? 'Ocultar' : 'Ver';
                    }

                    // Refleja en vivo la misma política que valida el servidor
                    // (PasswordUtil.cumplePoliticaMinima: 8+ caracteres, mayúscula, minúscula, dígito).
                    function actualizarFortalezaPassword(idCampo, idIndicador) {
                        var valor = document.getElementById(idCampo).value;
                        var indicador = document.getElementById(idIndicador);

                        var requisitos = {
                            len: valor.length >= 8,
                            mayus: /[A-Z]/.test(valor),
                            minus: /[a-z]/.test(valor),
                            digito: /[0-9]/.test(valor)
                        };
                        var cumplidos = Object.values(requisitos).filter(Boolean).length;

                        indicador.querySelectorAll('[data-req]').forEach(function (span) {
                            var ok = requisitos[span.dataset.req];
                            span.classList.toggle('text-emerald-600', ok);
                            span.classList.toggle('text-slate-400', !ok);
                            span.textContent = (ok ? '✓ ' : '○ ') + span.textContent.slice(2);
                        });

                        var barra = indicador.querySelector('.fortaleza-barra');
                        var label = indicador.querySelector('.fortaleza-label');
                        var niveles = [
                            {ancho: '0%', color: 'bg-slate-300', texto: '—', textoColor: 'text-slate-400'},
                            {ancho: '25%', color: 'bg-red-500', texto: 'Débil', textoColor: 'text-red-600'},
                            {ancho: '55%', color: 'bg-amber-500', texto: 'Regular', textoColor: 'text-amber-600'},
                            {ancho: '80%', color: 'bg-lime-500', texto: 'Buena', textoColor: 'text-lime-600'},
                            {ancho: '100%', color: 'bg-emerald-500', texto: 'Fuerte', textoColor: 'text-emerald-600'}
                        ];
                        var nivel = niveles[valor.length === 0 ? 0 : cumplidos];
                        barra.style.width = nivel.ancho;
                        barra.className = 'fortaleza-barra h-full transition-all ' + nivel.color;
                        label.textContent = nivel.texto;
                        label.className = 'fortaleza-label font-medium ' + nivel.textoColor;
                    }
                </script>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
