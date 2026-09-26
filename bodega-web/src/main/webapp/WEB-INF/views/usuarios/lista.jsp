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
                        <td class="px-4 py-3 text-slate-500">
                            <c:choose>
                                <c:when test="${not empty u.ultimoAcceso}"><c:out value="${u.ultimoAcceso}"/></c:when>
                                <c:otherwise>Nunca</c:otherwise>
                            </c:choose>
                        </td>
                        <td class="px-4 py-3 text-right">
                            <div class="flex items-center justify-end gap-2">
                                <button type="button" class="text-slate-500 hover:text-indigo-600" title="Editar nombre y correo"
                                        onclick="abrirModalEditarUsuario(this)"
                                        data-id="${u.id}" data-nombres="${fn:escapeXml(u.nombres)}"
                                        data-apellidos="${fn:escapeXml(u.apellidos)}" data-correo="${fn:escapeXml(u.correo)}"
                                        data-telefono="${fn:escapeXml(u.telefono)}">Editar</button>
                                <c:if test="${u.bloqueado}">
                                    <form method="post" action="${pageContext.request.contextPath}/usuarios" class="inline">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="accion" value="desbloquear">
                                        <input type="hidden" name="usuarioId" value="${u.id}">
                                        <button type="submit" class="text-slate-500 hover:text-emerald-600" title="Levantar el bloqueo por intentos fallidos">Desbloquear</button>
                                    </form>
                                </c:if>
                                <form method="post" action="${pageContext.request.contextPath}/usuarios" class="inline">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="accion" value="restablecerPassword">
                                    <input type="hidden" name="usuarioId" value="${u.id}">
                                    <button type="submit" class="text-slate-500 hover:text-blue-600" title="Restablecer contraseña">Clave</button>
                                </form>
                                <form method="post" action="${pageContext.request.contextPath}/usuarios" class="inline">
                                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                    <input type="hidden" name="accion" value="cambiarEstado">
                                    <input type="hidden" name="usuarioId" value="${u.id}">
                                    <input type="hidden" name="nuevoEstado" value="${u.estado == 'ACTIVO' ? 'INACTIVO' : 'ACTIVO'}">
                                    <button type="submit" class="text-slate-500 hover:text-red-600" title="Cambiar estado">
                                        <c:out value="${u.estado == 'ACTIVO' ? 'Desactivar' : 'Reactivar'}"/>
                                    </button>
                                </form>
                            </div>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty usuarios}">
                    <tr><td colspan="8" class="px-4 py-6 text-center text-slate-400">No se encontraron usuarios con los filtros aplicados.</td></tr>
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
                    <div class="relative">
                        <input type="password" id="password-nuevo-usuario" name="password" required minlength="8"
                               class="h-9 px-3 pr-16 border border-slate-300 rounded-lg text-sm w-full">
                        <button type="button" onclick="alternarVisibilidadPassword('password-nuevo-usuario', this)"
                                class="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-blue-600">Ver</button>
                    </div>
                </div>
                <div class="flex flex-col gap-1">
                    <label class="text-sm font-medium">Confirmar contraseña *</label>
                    <div class="relative">
                        <input type="password" id="password-confirmar-nuevo-usuario" name="confirmarPassword" required minlength="8"
                               class="h-9 px-3 pr-16 border border-slate-300 rounded-lg text-sm w-full">
                        <button type="button" onclick="alternarVisibilidadPassword('password-confirmar-nuevo-usuario', this)"
                                class="absolute right-2 top-1/2 -translate-y-1/2 text-xs font-medium text-slate-500 hover:text-blue-600">Ver</button>
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

<script>
    function alternarVisibilidadPassword(idCampo, boton) {
        var campo = document.getElementById(idCampo);
        var esOculta = campo.type === 'password';
        campo.type = esOculta ? 'text' : 'password';
        boton.textContent = esOculta ? 'Ocultar' : 'Ver';
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
