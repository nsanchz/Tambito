<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" isErrorPage="false" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Acceso Denegado - BodegaControl</title>
    <script src="https://cdn.tailwindcss.com"></script>
</head>
<body class="bg-slate-50 min-h-screen flex items-center justify-center p-4">
<div class="w-full max-w-md bg-white rounded-xl shadow-md p-8 text-center flex flex-col items-center gap-3">
    <div class="w-16 h-16 rounded-full bg-red-100 text-red-600 flex items-center justify-center text-3xl font-bold">
        403
    </div>
    <h1 class="text-xl font-bold text-slate-900">Acceso denegado</h1>
    <p class="text-sm text-slate-600">
        No tiene permisos para acceder a este módulo. Esta sección está restringida a usuarios
        con el rol de Administrador.
    </p>
    <a href="${pageContext.request.contextPath}/panel-de-control"
       class="mt-2 h-10 px-5 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-medium flex items-center justify-center transition-colors">
        Volver al panel de control
    </a>
</div>
</body>
</html>
