<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Nueva contraseña</title>
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

        /* Botón "ojito" animado (igual que login.jsp / cabecera.jspf — esta pantalla no incluye el layout). */
        .contenedor-password { position: relative; }
        .btn-ojo {
            position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
            width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
            border-radius: 9999px; color: rgba(241,245,249,0.5); cursor: pointer; background: transparent; border: none; padding: 0;
            transition: background-color .15s ease, transform .15s ease, color .15s ease;
        }
        .btn-ojo:hover { background: rgba(255,255,255,0.08); color: #f1f5f9; }
        .btn-ojo:active { transform: translateY(-50%) scale(0.88); }
        .btn-ojo svg { width: 18px; height: 18px; }
        .btn-ojo .icono-ojo-cerrado { display: none; }
        .contenedor-password.mostrando-password .icono-ojo-abierto { display: none; }
        .contenedor-password.mostrando-password .icono-ojo-cerrado { display: block; }
        .velo-password {
            position: absolute; inset: 0; pointer-events: none; border-radius: inherit;
            transform: scaleX(1); transform-origin: right center;
            transition: transform .45s cubic-bezier(.65,0,.35,1);
        }
        .contenedor-password.mostrando-password .velo-password { transform: scaleX(0); }
        @keyframes particulaDesintegrar {
            0% { transform: translate(0,0) scale(1); opacity: .9; }
            100% { transform: translate(var(--dx), var(--dy)) scale(0); opacity: 0; }
        }
        .particula-desintegrar {
            position: absolute; width: 4px; height: 4px; border-radius: 1px; pointer-events: none;
            animation: particulaDesintegrar .55s ease-out forwards;
        }
    </style>
</head>
<body class="bg-black min-h-screen relative overflow-x-hidden">

<div class="absolute inset-0 fondo-nubes pointer-events-none"></div>

<div class="relative min-h-screen flex items-center justify-center px-6 py-12">
    <div class="max-w-sm w-full bg-gradient-to-br from-slate-900/90 to-black/90 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-8 sm:p-10">
        <div class="flex flex-col items-center mb-8">
            <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logotipo de la tienda"
                 class="h-14" onerror="this.style.display='none'">
        </div>

        <h1 class="titulo-cursiva text-4xl text-white mb-3 text-center">Nueva contraseña</h1>
        <p class="text-sm text-slate-400 mb-8 text-center">Elija una nueva contraseña para su cuenta.</p>

        <c:if test="${not empty error}">
            <div class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm">
                <c:out value="${error}"/>
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/recuperar-password-nueva" class="flex flex-col gap-5">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">

            <div class="flex flex-col gap-1.5">
                <label for="password-nueva" class="text-sm text-slate-300">Nueva contraseña</label>
                <div class="contenedor-password">
                    <input type="password" id="password-nueva" name="passwordNueva" required minlength="8"
                           oninput="actualizarFortalezaPassword()"
                           class="input-password h-12 px-4 pr-11 rounded-lg text-sm campo-oscuro transition-shadow w-full">
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
                        <span class="fortaleza-label font-medium text-slate-500">—</span>
                    </div>
                    <div class="h-1.5 bg-white/10 rounded-full overflow-hidden">
                        <div class="fortaleza-barra h-full bg-slate-500 transition-all" style="width:0%"></div>
                    </div>
                    <div class="flex flex-wrap gap-x-3 gap-y-1 mt-2 text-xs">
                        <span data-req="len" class="flex items-center gap-1 text-slate-500">○ 8+ caracteres</span>
                        <span data-req="mayus" class="flex items-center gap-1 text-slate-500">○ A-Z</span>
                        <span data-req="minus" class="flex items-center gap-1 text-slate-500">○ a-z</span>
                        <span data-req="digito" class="flex items-center gap-1 text-slate-500">○ 123</span>
                    </div>
                </div>
            </div>

            <div class="flex flex-col gap-1.5">
                <label for="password-confirmar" class="text-sm text-slate-300">Confirmar nueva contraseña</label>
                <div class="contenedor-password">
                    <input type="password" id="password-confirmar" name="confirmarPassword" required minlength="8"
                           class="input-password h-12 px-4 pr-11 rounded-lg text-sm campo-oscuro transition-shadow w-full">
                    <div class="velo-password"></div>
                    <button type="button" onclick="alternarVisibilidadPassword('password-confirmar', this)"
                            class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                        <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                        <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                    </button>
                </div>
            </div>

            <button type="submit" id="btn-restablecer"
                    class="mt-3 h-12 bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                           flex items-center justify-center transition-colors">
                Restablecer contraseña
            </button>
        </form>
    </div>
</div>

<script>
    function alternarVisibilidadPassword(idCampo, boton) {
        var input = document.getElementById(idCampo);
        var contenedor = input.closest('.contenedor-password');
        var revelando = input.type === 'password';
        if (revelando) {
            input.type = 'text';
            contenedor.classList.add('mostrando-password');
        } else {
            contenedor.classList.remove('mostrando-password');
            lanzarParticulasDesintegracion(contenedor, input);
            setTimeout(function () { input.type = 'password'; }, 420);
        }
    }

    function lanzarParticulasDesintegracion(contenedor, input) {
        var rect = input.getBoundingClientRect();
        var rectContenedor = contenedor.getBoundingClientRect();
        for (var i = 0; i < 12; i++) {
            var particula = document.createElement('span');
            particula.className = 'particula-desintegrar';
            particula.style.left = (rect.left - rectContenedor.left + Math.random() * rect.width) + 'px';
            particula.style.top = (rect.top - rectContenedor.top + Math.random() * rect.height) + 'px';
            particula.style.background = 'rgba(241,245,249,0.6)';
            particula.style.setProperty('--dx', ((Math.random() - 0.5) * 60) + 'px');
            particula.style.setProperty('--dy', (-15 - Math.random() * 35) + 'px');
            particula.style.animationDelay = (Math.random() * 0.1) + 's';
            contenedor.appendChild(particula);
            (function (el) { setTimeout(function () { el.remove(); }, 700); })(particula);
        }
    }

    document.querySelectorAll('.contenedor-password').forEach(function (contenedor) {
        var input = contenedor.querySelector('.input-password');
        var velo = contenedor.querySelector('.velo-password');
        if (!input || !velo) return;
        var estilo = getComputedStyle(input);
        velo.style.background = estilo.backgroundColor;
        velo.style.borderRadius = estilo.borderRadius;
    });

    // Refleja en vivo la misma política que valida el servidor
    // (PasswordUtil.cumplePoliticaMinima: 8+ caracteres, mayúscula, minúscula, dígito).
    function actualizarFortalezaPassword() {
        var valor = document.getElementById('password-nueva').value;
        var indicador = document.getElementById('fortaleza-nueva');

        var requisitos = {
            len: valor.length >= 8,
            mayus: /[A-Z]/.test(valor),
            minus: /[a-z]/.test(valor),
            digito: /[0-9]/.test(valor)
        };
        var cumplidos = Object.values(requisitos).filter(Boolean).length;

        indicador.querySelectorAll('[data-req]').forEach(function (span) {
            var ok = requisitos[span.dataset.req];
            span.classList.toggle('text-emerald-400', ok);
            span.classList.toggle('text-slate-500', !ok);
            span.textContent = (ok ? '✓ ' : '○ ') + span.textContent.slice(2);
        });

        var barra = indicador.querySelector('.fortaleza-barra');
        var label = indicador.querySelector('.fortaleza-label');
        var niveles = [
            {ancho: '0%', color: 'bg-slate-500', texto: '—', textoColor: 'text-slate-500'},
            {ancho: '25%', color: 'bg-red-500', texto: 'Débil', textoColor: 'text-red-400'},
            {ancho: '55%', color: 'bg-amber-500', texto: 'Regular', textoColor: 'text-amber-400'},
            {ancho: '80%', color: 'bg-lime-500', texto: 'Buena', textoColor: 'text-lime-400'},
            {ancho: '100%', color: 'bg-emerald-500', texto: 'Fuerte', textoColor: 'text-emerald-400'}
        ];
        var nivel = niveles[valor.length === 0 ? 0 : cumplidos];
        barra.style.width = nivel.ancho;
        barra.className = 'fortaleza-barra h-full transition-all ' + nivel.color;
        label.textContent = nivel.texto;
        label.className = 'fortaleza-label font-medium ' + nivel.textoColor;
    }
</script>

</body>
</html>
