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
            <h2 class="text-2xl font-bold">Gestión de Usuarios y Roles de Seguridad</h2>
            <p class="text-sm text-slate-500">Control de credenciales y múltiples administradores del sistema.</p>
        </div>
        <button type="button" onclick="document.getElementById('modal-nuevo-usuario').classList.remove('hidden')"
                class="h-10 px-4 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">
            + Nuevo usuario
        </button>
    </div>

    <!-- Filtros -->
    <form method="get" action="${pageContext.request.contextPath}/usuarios"
          class="bg-white p-4 rounded-xl shadow-sm flex flex-wrap items-center gap-3">
        <input type="text" name="busqueda" placeholder="Buscar por nombre, usuario o correo..."
               value="${fn:escapeXml(param.busqueda)}"
               class="h-9 px-3 border border-slate-300 rounded-lg text-sm flex-1 min-w-[220px]">
        <select name="rol" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="todos">Todos los roles</option>
            <option value="ADMINISTRADOR" ${param.rol == 'ADMINISTRADOR' ? 'selected' : ''}>Administrador</option>
            <option value="VENDEDOR" ${param.rol == 'VENDEDOR' ? 'selected' : ''}>Vendedor</option>
        </select>
        <select name="estado" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            <option value="todos">Todos los estados</option>
            <option value="ACTIVO" ${param.estado == 'ACTIVO' ? 'selected' : ''}>Activo</option>
            <option value="INACTIVO" ${param.estado == 'INACTIVO' ? 'selected' : ''}>Inactivo</option>
        </select>
        <button type="submit" class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Filtrar</button>
    </form>

    <!-- Tabla -->
    <div class="bg-white rounded-xl shadow-sm overflow-hidden">
        <div class="overflow-x-auto">
            <table class="w-full text-left text-sm">
                <thead>
                <tr class="bg-slate-50 text-slate-500 uppercase text-xs">
                    <th class="px-4 py-3">Nombre y apellido</th>
                    <th class="px-4 py-3">Usuario</th>
                    <th class="px-4 py-3">Correo</th>
                    <th class="px-4 py-3">Teléfono</th>
                    <th class="px-4 py-3">Rol</th>
                    <th class="px-4 py-3">Estado</th>
                    <th class="px-4 py-3">MFA</th>
                    <th class="px-4 py-3">Último acceso</th>
                    <th class="px-4 py-3 text-right">Acciones</th>
                </tr>
                </thead>
                <tbody class="divide-y divide-slate-100">
                <c:forEach var="u" items="${usuarios}">
                    <tr class="hover:bg-slate-50">
                        <td class="px-4 py-3 font-medium"><c:out value="${u.nombreCompleto}"/></td>
                        <td class="px-4 py-3"><code class="bg-slate-100 px-2 py-0.5 rounded"><c:out value="${u.nombreUsuario}"/></code></td>
                        <td class="px-4 py-3 text-slate-500"><c:out value="${u.correo}"/></td>
                        <td class="px-4 py-3"><c:out value="${u.telefono}"/></td>
                        <td class="px-4 py-3">
                            <span class="px-2 py-0.5 rounded text-xs font-medium ${u.rol == 'ADMINISTRADOR' ? 'bg-indigo-100 text-indigo-700' : 'bg-sky-100 text-sky-700'}">
                                <c:out value="${u.rol}"/>
                            </span>
                        </td>
                        <td class="px-4 py-3">
                            <span class="px-2 py-0.5 rounded text-xs font-medium ${u.estado == 'ACTIVO' ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'}">
                                <c:out value="${u.estado}"/>
                            </span>
                            <c:if test="${u.bloqueado}">
                                <span class="px-2 py-0.5 rounded text-xs font-medium bg-red-100 text-red-700 ml-1" title="Bloqueada por intentos fallidos hasta ${u.bloqueadoHasta}">
                                    Bloqueada
                                </span>
                            </c:if>
                        </td>
                        <td class="px-4 py-3">
                            <c:choose>
                                <c:when test="${u.mfaHabilitado}">
                                    <span class="px-2 py-0.5 rounded text-xs font-medium bg-emerald-100 text-emerald-700">Activo</span>
                                </c:when>
                                <c:when test="${not empty u.mfaSecret}">
                                    <span class="px-2 py-0.5 rounded text-xs font-medium bg-amber-100 text-amber-700" title="El usuario debe escanear el QR y confirmarlo en su próximo inicio de sesión">Pendiente de 1er login</span>
                                </c:when>
                                <c:otherwise>
                                    <span class="px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-500">Inactivo</span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td class="px-4 py-3 text-slate-500">
                            <c:choose>
                                <c:when test="${not empty u.ultimoAcceso}"><c:out value="${u.ultimoAcceso}"/></c:when>
                                <c:otherwise>Nunca</c:otherwise>
                            </c:choose>
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
                                <button type="button" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-slate-600 hover:bg-slate-50"
                                        onclick="abrirModalEditarUsuario(this)"
                                        data-id="${u.id}" data-nombres="${fn:escapeXml(u.nombres)}"
                                        data-apellidos="${fn:escapeXml(u.apellidos)}" data-correo="${fn:escapeXml(u.correo)}"
                                        data-telefono="${fn:escapeXml(u.telefono)}">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                                    Editar datos
                                </button>

                                <c:if test="${u.bloqueado}">
                                    <form method="post" action="${pageContext.request.contextPath}/usuarios">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="accion" value="desbloquear">
                                        <input type="hidden" name="usuarioId" value="${u.id}">
                                        <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-emerald-600 hover:bg-emerald-50">
                                            <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M8 11V7a4 4 0 118 0m-9 4h10a1 1 0 011 1v7a1 1 0 01-1 1H7a1 1 0 01-1-1v-7a1 1 0 011-1z"/></svg>
                                            Desbloquear cuenta
                                        </button>
                                    </form>
                                </c:if>

                                <form method="post" action="${pageContext.request.contextPath}/usuarios">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="accion" value="restablecerPassword">
                                    <input type="hidden" name="usuarioId" value="${u.id}">
                                    <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-slate-600 hover:bg-slate-50">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4 text-slate-400" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M15 7a2 2 0 012 2m4 0a6 6 0 11-12 0 6 6 0 0112 0zM7 9a2 2 0 00-2 2v7a2 2 0 002 2h1"/></svg>
                                        Restablecer contraseña
                                    </button>
                                </form>

                                <div class="my-1 border-t border-slate-100"></div>

                                <c:choose>
                                    <c:when test="${u.mfaHabilitado}">
                                        <form method="post" action="${pageContext.request.contextPath}/usuarios"
                                              onsubmit="return confirmarAccion(this, '¿Desactivar el MFA de ${fn:escapeXml(u.nombreCompleto)}?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="accion" value="desactivarMfa">
                                            <input type="hidden" name="usuarioId" value="${u.id}">
                                            <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-red-600 hover:bg-red-50">
                                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                                                Desactivar MFA
                                            </button>
                                        </form>
                                    </c:when>
                                    <c:when test="${not empty u.mfaSecret}">
                                        <form method="post" action="${pageContext.request.contextPath}/usuarios"
                                              onsubmit="return confirmarAccion(this, '¿Cancelar la activación pendiente de MFA de ${fn:escapeXml(u.nombreCompleto)}?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="accion" value="desactivarMfa">
                                            <input type="hidden" name="usuarioId" value="${u.id}">
                                            <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-amber-600 hover:bg-amber-50">
                                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/></svg>
                                                Cancelar activación MFA
                                            </button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form method="post" action="${pageContext.request.contextPath}/usuarios"
                                              onsubmit="return confirmarAccion(this, 'Se cerrará la sesión actual de ${fn:escapeXml(u.nombreCompleto)} (si tiene una abierta). En su próximo inicio de sesión deberá escanear el QR de Google Authenticator. ¿Continuar?');">
                                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                            <input type="hidden" name="accion" value="activarMfa">
                                            <input type="hidden" name="usuarioId" value="${u.id}">
                                            <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 text-emerald-600 hover:bg-emerald-50">
                                                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                                                Activar MFA
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>

                                <div class="my-1 border-t border-slate-100"></div>

                                <form method="post" action="${pageContext.request.contextPath}/usuarios">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="accion" value="cambiarEstado">
                                    <input type="hidden" name="usuarioId" value="${u.id}">
                                    <input type="hidden" name="nuevoEstado" value="${u.estado == 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'}">
                                    <button type="submit" class="w-full text-left px-3 py-2 flex items-center gap-2.5 ${u.estado == 'ACTIVO' ? 'text-red-600 hover:bg-red-50' : 'text-emerald-600 hover:bg-emerald-50'}">
                                        <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M5.636 5.636a9 9 0 1012.728 0M12 3v9"/></svg>
                                        <c:out value="${u.estado == 'ACTIVO' ? 'Desactivar cuenta' : 'Reactivar cuenta'}"/>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty usuarios}">
                    <tr><td colspan="9" class="px-4 py-6 text-center text-slate-400">No se encontraron usuarios con los filtros aplicados.</td></tr>
                </c:if>
                </tbody>
            </table>
        </div>
    </div>
</div>

<!-- Modal: Nuevo usuario -->
<div id="modal-nuevo-usuario" class="${abrirModalNuevoUsuario ? '' : 'hidden'} fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-2xl rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Registrar nuevo usuario</h3>
            <button type="button" onclick="document.getElementById('modal-nuevo-usuario').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/usuarios" class="p-6 flex flex-col gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="crear">

            <c:if test="${not empty errorCreacion}">
                <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm font-medium">
                    <c:out value="${errorCreacion}"/>
                </div>
            </c:if>

            <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Nombres *</label>
                    <input type="text" name="nombres" required value="${fn:escapeXml(param.nombres)}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Apellidos *</label>
                    <input type="text" name="apellidos" required value="${fn:escapeXml(param.apellidos)}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Nombre de usuario *</label>
                    <input type="text" name="nombreUsuario" required value="${fn:escapeXml(param.nombreUsuario)}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Correo corporativo *</label>
                    <input type="email" name="correo" required value="${fn:escapeXml(param.correo)}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Teléfono</label>
                    <input type="tel" name="telefono" value="${fn:escapeXml(param.telefono)}" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Estado inicial</label>
                    <select name="estadoInicial" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
                        <option value="ACTIVO" ${param.estadoInicial != 'INACTIVO' ? 'selected' : ''}>Activo</option>
                        <option value="INACTIVO" ${param.estadoInicial == 'INACTIVO' ? 'selected' : ''}>Inactivo</option>
                    </select>
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Contraseña temporal *</label>
                    <div class="relative contenedor-password">
                        <input type="password" id="password-nuevo-usuario" name="password" required minlength="8"
                               oninput="actualizarFortalezaPassword('password-nuevo-usuario', 'fortaleza-nuevo-usuario')"
                               class="input-password h-9 px-3 pr-10 border border-slate-300 rounded-lg text-sm w-full">
                        <div class="velo-password"></div>
                        <button type="button" onclick="alternarVisibilidadPassword('password-nuevo-usuario', this)"
                                class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                            <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                            <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                        </button>
                    </div>
                    <div class="mt-1" id="fortaleza-nuevo-usuario">
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
                    <label class="text-sm font-medium">Confirmar contraseña *</label>
                    <div class="relative contenedor-password">
                        <input type="password" id="password-confirmar-nuevo-usuario" name="confirmarPassword" required minlength="8"
                               class="input-password h-9 px-3 pr-10 border border-slate-300 rounded-lg text-sm w-full">
                        <div class="velo-password"></div>
                        <button type="button" onclick="alternarVisibilidadPassword('password-confirmar-nuevo-usuario', this)"
                                class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                            <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                            <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                        </button>
                    </div>
                </div>
            </div>
            <div class="flex flex-col gap-2">
                <label class="text-sm font-medium">Rol *</label>
                <label class="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer">
                    <input type="radio" name="rolSeleccionado" value="ADMINISTRADOR" ${param.rolSeleccionado == 'ADMINISTRADOR' ? 'checked' : ''}> Administrador (privilegios totales)
                </label>
                <label class="flex items-center gap-2 p-3 border border-slate-200 rounded-lg cursor-pointer">
                    <input type="radio" name="rolSeleccionado" value="VENDEDOR" ${param.rolSeleccionado != 'ADMINISTRADOR' ? 'checked' : ''}> Vendedor (mostrador y caja)
                </label>
            </div>
            <label class="flex items-start gap-3 p-3 border border-emerald-200 bg-emerald-50 rounded-lg cursor-pointer">
                <input type="checkbox" name="activarMfaAlCrear" value="true" class="mt-0.5">
                <span class="flex flex-col">
                    <span class="text-sm font-medium text-emerald-800">Activar verificación en dos pasos (MFA) para este usuario</span>
                    <span class="text-xs text-emerald-700">
                        El usuario deberá escanear un código QR con Google Authenticator y confirmarlo en su primer
                        inicio de sesión — usted, como administrador, no verá ese código.
                    </span>
                </span>
            </label>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-nuevo-usuario').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Crear usuario</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal: Editar usuario (nombre, apellido, correo, teléfono) -->
<div id="modal-editar-usuario" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-md rounded-xl shadow-xl overflow-hidden">
        <div class="px-6 py-4 bg-slate-50 flex items-center justify-between">
            <h3 class="font-semibold">Editar datos del usuario</h3>
            <button type="button" onclick="document.getElementById('modal-editar-usuario').classList.add('hidden')"
                    class="text-slate-400 hover:text-slate-700">Cerrar</button>
        </div>
        <form method="post" action="${pageContext.request.contextPath}/usuarios" class="p-6 flex flex-col gap-4">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <input type="hidden" name="accion" value="editar">
            <input type="hidden" id="editar-usuario-id" name="usuarioId" value="">
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Nombres *</label>
                <input type="text" id="editar-nombres" name="nombres" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Apellidos *</label>
                <input type="text" id="editar-apellidos" name="apellidos" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Correo *</label>
                <input type="email" id="editar-correo" name="correo" required class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex flex-col gap-1">
                <label class="text-sm font-medium">Teléfono</label>
                <input type="tel" id="editar-telefono" name="telefono" class="h-9 px-3 border border-slate-300 rounded-lg text-sm">
            </div>
            <div class="flex items-center justify-end gap-3 pt-2">
                <button type="button" onclick="document.getElementById('modal-editar-usuario').classList.add('hidden')"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="submit" class="h-9 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium">Guardar cambios</button>
            </div>
        </form>
    </div>
</div>

<!-- Modal de confirmación genérico: reemplaza al confirm() nativo del navegador (antiestético
     y no personalizable) para cualquier acción sensible de esta pantalla (MFA, etc.). Un solo
     formulario queda "pendiente" en JS (confirmarAccion) y se reenvía manualmente si se acepta. -->
<div id="modal-confirmacion" class="hidden fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
    <div class="bg-white w-full max-w-sm rounded-xl shadow-xl overflow-hidden">
        <div class="p-6 flex flex-col gap-4">
            <p id="texto-confirmacion" class="text-sm text-slate-700"></p>
            <div class="flex items-center justify-end gap-3">
                <button type="button" onclick="cancelarConfirmacion()"
                        class="h-9 px-4 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm">Cancelar</button>
                <button type="button" onclick="aceptarConfirmacion()"
                        class="h-9 px-5 btn-primario text-white rounded-lg text-sm font-medium">Aceptar</button>
            </div>
        </div>
    </div>
</div>

<script>
    var formPendienteConfirmacion = null;

    /** Sustituye a confirm(): abre el modal estilizado y deja el formulario pendiente de reenvío. */
    function confirmarAccion(form, mensaje) {
        formPendienteConfirmacion = form;
        document.getElementById('texto-confirmacion').textContent = mensaje;
        document.getElementById('modal-confirmacion').classList.remove('hidden');
        return false;
    }

    function aceptarConfirmacion() {
        document.getElementById('modal-confirmacion').classList.add('hidden');
        if (formPendienteConfirmacion) {
            formPendienteConfirmacion.submit();
            formPendienteConfirmacion = null;
        }
    }

    function cancelarConfirmacion() {
        document.getElementById('modal-confirmacion').classList.add('hidden');
        formPendienteConfirmacion = null;
    }
</script>

<script>
    // El menú de acciones (⋮) — alternarMenuAcciones/cerrarTodosLosMenus — ahora vive como
    // función compartida en layout/pie.jspf, reutilizable desde cualquier vista con tablas.

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

    function abrirModalEditarUsuario(boton) {
        document.getElementById('editar-usuario-id').value = boton.dataset.id;
        document.getElementById('editar-nombres').value = boton.dataset.nombres;
        document.getElementById('editar-apellidos').value = boton.dataset.apellidos;
        document.getElementById('editar-correo').value = boton.dataset.correo;
        document.getElementById('editar-telefono').value = boton.dataset.telefono;
        document.getElementById('modal-editar-usuario').classList.remove('hidden');
    }
</script>

<%@ include file="/WEB-INF/views/layout/pie.jspf" %>
