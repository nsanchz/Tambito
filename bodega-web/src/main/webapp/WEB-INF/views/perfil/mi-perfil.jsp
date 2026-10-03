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
                        <div class="relative contenedor-password">
                            <input type="password" id="password-actual" name="passwordActual" required
                                   class="input-password h-9 px-3 pr-10 border border-slate-300 rounded-lg text-sm w-full">
                            <div class="velo-password"></div>
                            <button type="button" onclick="alternarVisibilidadPassword('password-actual', this)"
                                    class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                                <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                                <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                            </button>
                        </div>
                    </div>
                    <div class="grid grid-cols-2 gap-4">
                        <div class="flex flex-col gap-1">
                            <label class="text-sm font-medium">Nueva contraseña *</label>
                            <div class="relative contenedor-password">
                                <input type="password" id="password-nueva" name="passwordNueva" required minlength="8"
                                       oninput="actualizarFortalezaPassword('password-nueva', 'fortaleza-nueva')"
                                       class="input-password h-9 px-3 pr-10 border border-slate-300 rounded-lg text-sm w-full">
                                <div class="velo-password"></div>
                                <button type="button" onclick="alternarVisibilidadPassword('password-nueva', this)"
                                        class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                                    <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                                    <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                                </button>
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
                            <div class="relative contenedor-password">
                                <input type="password" id="password-confirmar" name="confirmarPasswordNueva" required minlength="8"
                                       class="input-password h-9 px-3 pr-10 border border-slate-300 rounded-lg text-sm w-full">
                                <div class="velo-password"></div>
                                <button type="button" onclick="alternarVisibilidadPassword('password-confirmar', this)"
                                        class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                                    <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                                    <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                                </button>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end">
                        <button type="submit" id="btn-cambiar-password" class="h-9 px-5 bg-slate-800 hover:bg-slate-900 text-white rounded-lg text-sm font-medium transition-colors">Cambiar contraseña</button>
                    </div>
                </form>
                <script>
                    // alternarVisibilidadPassword(idCampo, boton) ahora vive en layout/pie.jspf
                    // (compartida por toda la app, con la animación del velo).

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
