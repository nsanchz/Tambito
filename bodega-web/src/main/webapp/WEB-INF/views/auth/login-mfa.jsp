<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verificación en dos pasos</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Dancing+Script:wght@600&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/estilos.css">
    <style>
        body { font-family: 'Inter', sans-serif; }
        .titulo-cursiva { font-family: 'Dancing Script', cursive; }
        .malla-geometrica {
            background-image: repeating-linear-gradient(100deg,
                rgba(255,255,255,0.08) 0px, rgba(255,255,255,0.08) 1px,
                transparent 1px, transparent 14px);
            background-size: 400% 400%;
            animation: desplazarMalla 18s ease-in-out infinite;
        }
        @keyframes desplazarMalla {
            0%   { background-position: 0% 50%; }
            50%  { background-position: 100% 50%; }
            100% { background-position: 0% 50%; }
        }
        .casilla-codigo {
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.12);
            color: #f1f5f9;
        }
        .casilla-codigo:focus {
            outline: none;
            border-color: #C00000;
            box-shadow: 0 0 0 3px rgba(192,0,0,0.25);
        }
    </style>
</head>
<body class="bg-black min-h-screen flex">

<div class="w-full lg:w-[42%] min-h-screen flex flex-col justify-center px-8 sm:px-16 py-12 bg-gradient-to-br from-slate-900 to-black relative overflow-y-auto">
    <div class="max-w-sm w-full mx-auto">
        <c:choose>
            <c:when test="${not empty mfaQrDataUri}">
                <h1 class="titulo-cursiva text-4xl text-white mb-3">Activar verificación</h1>
                <p class="text-sm text-slate-400 mb-6">
                    Un administrador activó la verificación en dos pasos para su cuenta. Abra
                    <strong class="text-slate-300">Google Authenticator</strong>, escanee este código y
                    luego ingrese los 6 dígitos generados para confirmar.
                </p>
                <div class="flex justify-center mb-6">
                    <img src="${mfaQrDataUri}" alt="Código QR de activación de MFA" class="rounded-lg" width="200" height="200">
                </div>
                <details class="text-xs text-slate-500 mb-6">
                    <summary class="cursor-pointer">¿No puede escanear? Ingreso manual</summary>
                    <code class="block mt-1 p-2 bg-white/5 rounded break-all text-slate-400"><c:out value="${mfaOtpAuthUri}"/></code>
                </details>
            </c:when>
            <c:otherwise>
                <h1 class="titulo-cursiva text-4xl text-white mb-3">Verificación</h1>
                <p class="text-sm text-slate-400 mb-8">
                    Abra Google Authenticator e ingrese el código de 6 dígitos generado para su cuenta.
                </p>
            </c:otherwise>
        </c:choose>

        <c:if test="${not empty error}">
            <div class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login-mfa" id="form-mfa" class="flex flex-col gap-6">
            <div class="flex justify-between gap-2" id="contenedor-casillas">
                <c:forEach var="i" begin="0" end="5">
                    <input type="text" inputmode="numeric" maxlength="1" required
                           class="casilla-codigo w-12 h-14 text-center text-xl rounded-lg transition-shadow casilla"
                           ${i == 0 ? 'autofocus' : ''}>
                </c:forEach>
            </div>
            <input type="hidden" name="codigo" id="codigo-completo">

            <button type="submit"
                    class="h-12 bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                           flex items-center justify-center gap-2 transition-colors">
                <c:out value="${not empty mfaQrDataUri ? 'Confirmar y activar' : 'Verificar'}"/>
                <svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M17 8l4 4m0 0l-4 4m4-4H3"/>
                </svg>
            </button>
        </form>

        <p class="text-center text-xs text-slate-500 mt-8">
            ¿No tiene acceso a su celular? Contacte al administrador del sistema.
        </p>
    </div>
</div>

<div class="hidden lg:block lg:w-[58%] min-h-screen bg-black malla-geometrica"></div>

<script>
    // Autoavance entre las 6 casillas y ensamblado del código en el input oculto antes de enviar.
    var casillas = document.querySelectorAll('.casilla');
    casillas.forEach(function (casilla, indice) {
        casilla.addEventListener('input', function () {
            casilla.value = casilla.value.replace(/\D/g, '').slice(0, 1);
            if (casilla.value && indice < casillas.length - 1) {
                casillas[indice + 1].focus();
            }
        });
        casilla.addEventListener('keydown', function (e) {
            if (e.key === 'Backspace' && !casilla.value && indice > 0) {
                casillas[indice - 1].focus();
            }
        });
        casilla.addEventListener('paste', function (e) {
            var texto = (e.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '');
            if (texto.length === casillas.length) {
                e.preventDefault();
                casillas.forEach(function (c, i) { c.value = texto[i]; });
                casillas[casillas.length - 1].focus();
            }
        });
    });

    document.getElementById('form-mfa').addEventListener('submit', function () {
        var codigo = Array.from(casillas).map(function (c) { return c.value; }).join('');
        document.getElementById('codigo-completo').value = codigo;
    });
</script>

</body>
</html>
