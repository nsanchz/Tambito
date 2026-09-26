<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Iniciar Sesión - <c:out value="${nombreEmpresa}" default="BodegaControl"/></title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/estilos.css">
</head>
<body class="bg-slate-50 min-h-screen flex items-center justify-center p-4">

<div class="w-full max-w-md bg-white rounded-xl shadow-md p-8">
    <div class="flex flex-col items-center mb-6">
        <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logotipo de la tienda" class="h-12 mb-2" onerror="this.style.display='none'">
        <h1 class="text-2xl font-bold text-slate-900">Iniciar Sesión</h1>
        <p class="text-sm text-slate-500"><c:out value="${nombreEmpresa}" default="Sistema de Gestión de Bodega"/></p>
    </div>

    <c:if test="${not empty error}">
        <div class="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 mb-4 text-sm">
            <c:out value="${error}"/>
        </div>
    </c:if>

    <form method="post" action="${pageContext.request.contextPath}/login" class="flex flex-col gap-4">
        <div class="flex flex-col gap-1">
            <label for="username" class="text-sm font-medium text-slate-700">Usuario o correo</label>
            <input type="text" id="username" name="j_username" required
                   value="${fn:escapeXml(usuarioIngresado)}"
                   class="h-11 px-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
        </div>

        <div class="flex flex-col gap-1">
            <label for="password" class="text-sm font-medium text-slate-700">Contraseña</label>
            <input type="password" id="password" name="j_password" required
                   class="h-11 px-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
        </div>

        <div class="flex flex-col gap-1">
            <label for="terminal-select" class="text-sm font-medium text-slate-700">Terminal / Caja</label>
            <select id="terminal-select" name="terminalId"
                    class="h-11 px-3 border border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
                <option value="ADMINISTRACION">Administración</option>
                <option value="TIENDA">Tienda</option>
            </select>
        </div>

        <button type="submit"
                class="h-11 mt-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium transition-colors">
            Iniciar sesión
        </button>
    </form>
</div>

</body>
</html>
