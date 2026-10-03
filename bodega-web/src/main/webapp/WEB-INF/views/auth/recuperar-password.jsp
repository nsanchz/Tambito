<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Recuperar contraseña</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Dancing+Script:wght@600&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/estilos.css">
    <style>
        body { font-family: 'Inter', sans-serif; }
        .titulo-cursiva { font-family: 'Dancing Script', cursive; }
        .fondo-nubes {
            background-color: #000;
            background-image:
                radial-gradient(ellipse 55% 40% at 15% 20%, rgba(192,0,0,0.20), transparent 70%),
                radial-gradient(ellipse 50% 45% at 85% 15%, rgba(100,116,139,0.30), transparent 70%),
                radial-gradient(ellipse 60% 50% at 50% 90%, rgba(30,41,59,0.55), transparent 70%),
                radial-gradient(ellipse 40% 35% at 80% 75%, rgba(192,0,0,0.14), transparent 70%);
            background-repeat: no-repeat;
            animation: derivarNubes 32s ease-in-out infinite;
        }
        @keyframes derivarNubes {
            0%   { background-position: 0% 0%, 100% 0%, 50% 100%, 80% 70%; }
            50%  { background-position: 12% 15%, 85% 20%, 42% 85%, 65% 60%; }
            100% { background-position: 0% 0%, 100% 0%, 50% 100%, 80% 70%; }
        }
        .campo-oscuro {
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.12);
            color: #f1f5f9;
        }
        .campo-oscuro::placeholder { color: rgba(241,245,249,0.4); }
        .campo-oscuro:focus {
            outline: none;
            border-color: #C00000;
            box-shadow: 0 0 0 3px rgba(192,0,0,0.25);
        }
    </style>
</head>
<body class="bg-black min-h-screen relative overflow-x-hidden">

<div class="absolute inset-0 fondo-nubes pointer-events-none"></div>

<div class="relative min-h-screen flex items-center justify-center px-6 py-12">
    <div class="max-w-sm w-full bg-gradient-to-br from-slate-900/90 to-black/90 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-8 sm:p-10">
        <div class="flex flex-col items-center gap-2 mb-8">
            <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logotipo de la tienda"
                 class="h-14" onerror="this.style.display='none'">
            <span class="text-slate-400 text-sm font-medium tracking-wide uppercase">
                <c:out value="${nombreEmpresa}" default="Bodega TAMBITO"/>
            </span>
        </div>

        <h1 class="titulo-cursiva text-4xl text-white mb-3 text-center">Recuperar contraseña</h1>
        <p class="text-sm text-slate-400 mb-8 text-center">
            Escriba su usuario o correo registrado. Si la cuenta existe, le enviaremos un código
            de verificación de 6 dígitos a su correo.
        </p>

        <c:if test="${not empty error}">
            <div class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/recuperar-password" class="flex flex-col gap-5">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <div class="flex flex-col gap-1.5">
                <label for="login" class="text-sm text-slate-300">Usuario o correo</label>
                <input type="text" id="login" name="login" required autofocus autocomplete="off"
                       class="h-12 px-4 rounded-lg text-sm campo-oscuro transition-shadow">
            </div>

            <button type="submit"
                    class="mt-3 h-12 bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                           flex items-center justify-center transition-colors">
                Enviar código
            </button>
        </form>

        <p class="text-center text-xs text-slate-500 mt-8">
            <a href="${pageContext.request.contextPath}/login" class="hover:text-slate-300">← Volver a iniciar sesión</a>
        </p>
    </div>
</div>

</body>
</html>
