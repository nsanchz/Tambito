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
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Dancing+Script:wght@600&family=Inter:wght@400;500;600&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/estilos.css">
    <style>
        body { font-family: 'Inter', sans-serif; }
        .titulo-cursiva { font-family: 'Dancing Script', cursive; }

        /* Fondo tipo "nubes": degradados radiales suaves que derivan lentamente (CSS puro,
           sin librerías 3D), en vez de la malla de líneas anterior. Se mantiene oscuro y
           atmosférico a propósito, para contrastar con la interfaz clara del sistema una vez
           dentro (ver layout/cabecera.jspf, fondo bg-slate-50). */
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

        /* Indicador deslizante del selector Administración/Tienda: una sola pastilla blanca
           que se desplaza de un lado a otro (en vez de dos fondos independientes
           apareciendo/desapareciendo), con un ligero rebote al asentarse que evoca el
           vaivén del agua. */
        #terminal-indicador {
            transition: transform 550ms cubic-bezier(0.34, 1.56, 0.64, 1);
        }

        /* Botón "Iniciar sesión": al enviarse, un relleno gris sube desde abajo como si se
           llenara de agua, en vez del spinner genérico del resto de la app (esta pantalla
           no incluye pie.jspf). El brillo superior se desliza para simular un pequeño
           oleaje mientras se llena. */
        #btn-login-relleno {
            transition: height 900ms cubic-bezier(0.22, 1, 0.36, 1);
        }
        #btn-login-relleno::before {
            content: "";
            position: absolute;
            top: 0; left: 0; right: 0;
            height: 3px;
            background: rgba(255,255,255,0.6);
            animation: ondaAgua 1.1s ease-in-out infinite;
        }
        @keyframes ondaAgua {
            0%, 100% { transform: translateX(-15%); opacity: 0.4; }
            50% { transform: translateX(15%); opacity: 0.9; }
        }

        /* Botón "ojito" animado del campo de contraseña (igual que en el resto de la app,
           ver layout/cabecera.jspf — esta pantalla no lo incluye, así que se duplica aquí). */
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

<!-- Fondo animado a pantalla completa (antes era un panel decorativo lateral; ahora el
     formulario va centrado encima de él, como una tarjeta). -->
<div class="absolute inset-0 fondo-nubes pointer-events-none"></div>

<div class="relative min-h-screen flex items-center justify-center px-6 py-12">

    <div class="max-w-sm w-full bg-gradient-to-br from-slate-900/90 to-black/90 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-8 sm:p-10">
        <div class="flex flex-col items-center mb-8">
            <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logotipo de la tienda"
                 class="h-14" onerror="this.style.display='none'">
        </div>

        <h1 class="titulo-cursiva text-5xl text-white mb-8 text-center">Iniciar sesión</h1>

        <c:if test="${not empty error}">
            <div class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm">
                <c:out value="${error}"/>
            </div>
        </c:if>
        <c:if test="${param.reestablecida == '1'}">
            <div class="bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 rounded-lg p-3 mb-5 text-sm">
                Contraseña restablecida correctamente. Ya puede iniciar sesión.
            </div>
        </c:if>

        <form method="post" action="${pageContext.request.contextPath}/login" class="flex flex-col gap-5"
              onsubmit="return iniciarLoginConCarga(this)">
            <div class="flex flex-col gap-1.5">
                <label for="username" class="text-sm text-slate-300">Usuario o correo</label>
                <input type="text" id="username" name="j_username" required autofocus
                       value="${fn:escapeXml(usuarioIngresado)}"
                       class="h-12 px-4 rounded-lg text-sm campo-oscuro transition-shadow">
            </div>

            <div class="flex flex-col gap-1.5">
                <div class="flex items-center justify-between">
                    <label for="password" class="text-sm text-slate-300">Contraseña</label>
                    <a href="${pageContext.request.contextPath}/recuperar-password" class="text-xs text-slate-500 hover:text-slate-300">¿Olvidaste tu contraseña?</a>
                </div>
                <div class="relative contenedor-password">
                    <input type="password" id="password" name="j_password" required
                           class="input-password h-12 px-4 pr-11 rounded-lg text-sm campo-oscuro transition-shadow w-full">
                    <div class="velo-password"></div>
                    <button type="button" onclick="alternarVisibilidadPassword('password', this)"
                            class="btn-ojo" tabindex="-1" aria-label="Mostrar u ocultar contraseña">
                        <svg class="icono-ojo-abierto" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"/><path stroke-linecap="round" stroke-linejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>
                        <svg class="icono-ojo-cerrado" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"/></svg>
                    </button>
                </div>
            </div>

            <div class="flex flex-col gap-1.5">
                <span class="text-sm text-slate-300">Terminal</span>
                <div class="relative grid grid-cols-2 p-1 rounded-lg campo-oscuro">
                    <div id="terminal-indicador" class="absolute top-1 bottom-1 left-1 rounded-md bg-white"
                         style="width: calc(50% - 4px); transform: translateX(0%);"></div>
                    <label class="relative z-10 cursor-pointer">
                        <input type="radio" name="terminalId" value="ADMINISTRACION" class="peer sr-only" checked
                               onchange="moverIndicadorTerminal(this)">
                        <span class="flex items-center justify-center h-9 rounded-md text-xs font-medium text-slate-400
                                     peer-checked:text-slate-900 cursor-pointer transition-colors">
                            Administración
                        </span>
                    </label>
                    <label class="relative z-10 cursor-pointer">
                        <input type="radio" name="terminalId" value="TIENDA" class="peer sr-only"
                               onchange="moverIndicadorTerminal(this)">
                        <span class="flex items-center justify-center h-9 rounded-md text-xs font-medium text-slate-400
                                     peer-checked:text-slate-900 cursor-pointer transition-colors">
                            Tienda
                        </span>
                    </label>
                </div>
            </div>

            <button type="submit" id="btn-login"
                    class="mt-3 h-12 relative overflow-hidden bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                           flex items-center justify-center transition-colors">
                <span id="btn-login-relleno" class="absolute inset-x-0 bottom-0 h-0 bg-slate-300/80"></span>
                <span id="btn-login-texto" class="relative z-10">Iniciar sesión</span>
            </button>
        </form>

        <p class="text-center text-xs text-slate-500 mt-8">
            Acceso exclusivo para personal autorizado de <c:out value="${nombreEmpresa}" default="Bodega TAMBITO"/>.
        </p>
    </div>
</div>

<script>
    /** Desliza la pastilla del selector Administración/Tienda hacia el lado elegido. */
    function moverIndicadorTerminal(radio) {
        document.getElementById('terminal-indicador').style.transform =
            radio.value === 'TIENDA' ? 'translateX(100%)' : 'translateX(0%)';
    }

    /**
     * En vez del spinner genérico (esta pantalla no incluye pie.jspf), el botón se "llena"
     * de gris de abajo hacia arriba como si fuera agua, mientras el formulario se envía de
     * verdad. Siempre retorna true para que el submit continúe con normalidad.
     */
    function iniciarLoginConCarga(form) {
        var boton = document.getElementById('btn-login');
        if (boton.disabled) {
            return true;
        }
        boton.disabled = true;
        document.getElementById('btn-login-texto').textContent = 'Ingresando...';
        document.getElementById('btn-login-relleno').style.height = '100%';
        return true;
    }

    /** Mismo botón "ojito" animado del resto de la app (ver layout/pie.jspf). */
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
</script>

</body>
</html>
