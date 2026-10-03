<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verificar código</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Dancing+Script:wght@600&family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
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

        #contenedor-casillas { display: flex; justify-content: space-between; }
        .casilla-codigo {
            width: 44px; height: 56px;
            background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.14); color: #f1f5f9;
            border-radius: 12px; text-align: center; font-size: 1.25rem;
            transition: transform 0.7s cubic-bezier(.2,.8,.2,1), border-color 0.2s, box-shadow 0.2s;
        }
        .casilla-codigo:focus { outline: none; border-color: #C00000; box-shadow: 0 0 0 3px rgba(192,0,0,0.25); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(1) { transform: translate(4px, 10px) rotate(-16deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(2) { transform: translate(-6px, -14px) rotate(11deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(3) { transform: translate(8px, 8px) rotate(-9deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(4) { transform: translate(-8px, -6px) rotate(14deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(5) { transform: translate(6px, 12px) rotate(-12deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(6) { transform: translate(-4px, -10px) rotate(8deg); }
        #contenedor-casillas.acomodado .casilla-codigo { transform: translate(0, 0) rotate(0deg); }

        .pantalla { transition: opacity 0.35s ease, transform 0.35s ease; }
        .pantalla-oculta { opacity: 0; position: absolute; inset: 0; pointer-events: none; transform: scale(0.98); }
        .anillo-progreso { transform: rotate(-90deg); }
        .anillo-progreso circle { transition: stroke-dashoffset 0.15s linear; }

        @keyframes aparecerCheck { 0% { transform: scale(0); opacity: 0; } 60% { transform: scale(1.15); opacity: 1; } 100% { transform: scale(1); opacity: 1; } }
        .check-exito { animation: aparecerCheck 0.5s cubic-bezier(.2,.9,.3,1.2) forwards; }
        @keyframes sacudir { 0%, 100% { transform: translateX(0); } 20%, 60% { transform: translateX(-6px); } 40%, 80% { transform: translateX(6px); } }
        .sacudida { animation: sacudir 0.4s ease; }
    </style>
</head>
<body class="bg-black min-h-screen relative overflow-x-hidden">

<div class="absolute inset-0 fondo-nubes pointer-events-none"></div>

<div class="relative min-h-screen flex items-center justify-center px-6 py-12 overflow-y-auto">
  <div class="max-w-sm w-full bg-gradient-to-br from-slate-900/90 to-black/90 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-8 sm:p-10">
    <div class="max-w-sm w-full mx-auto relative" style="min-height: 420px;">

        <!-- ================= PANTALLA 1: ingreso del código ================= -->
        <div id="pantalla-ingreso" class="pantalla">
            <h1 class="titulo-cursiva text-4xl text-white mb-3">Verificar código</h1>
            <p class="text-sm text-slate-400 mb-8">
                Enviamos un código de 6 dígitos a
                <strong class="text-slate-200"><c:out value="${sessionScope.resetCorreoMostrado}"/></strong>.
                Revise también la carpeta de spam.
            </p>

            <div id="banner-error" class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm ${empty error ? 'hidden' : ''}">
                <c:out value="${error}"/>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/recuperar-password-codigo" id="form-codigo" class="flex flex-col gap-8">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <div class="flex" id="contenedor-casillas">
                    <c:forEach var="i" begin="0" end="5">
                        <input type="text" inputmode="numeric" maxlength="1"
                               class="casilla-codigo casilla" ${i == 0 ? 'autofocus' : ''}>
                    </c:forEach>
                </div>
                <input type="hidden" name="codigo" id="codigo-completo">

                <button type="submit" id="btn-verificar"
                        class="h-12 bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                               flex items-center justify-center transition-colors disabled:opacity-50">
                    Verificar
                </button>
            </form>

            <p class="text-center text-xs text-slate-500 mt-8">
                ¿No le llegó el código?
                <a href="${pageContext.request.contextPath}/recuperar-password" class="text-slate-300 hover:text-white">Solicitar uno nuevo</a>
            </p>
        </div>

        <!-- ================= PANTALLA 2: verificando ================= -->
        <div id="pantalla-verificando" class="pantalla pantalla-oculta flex flex-col items-center justify-center text-center" style="min-height: 420px;">
            <h1 class="titulo-cursiva text-4xl text-white mb-2">Verificando…</h1>
            <p class="text-sm text-slate-400 mb-10">Comprobando su código de forma segura.</p>
            <svg width="110" height="110" viewBox="0 0 110 110" class="anillo-progreso">
                <circle cx="55" cy="55" r="48" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="8"/>
                <circle id="circulo-progreso" cx="55" cy="55" r="48" fill="none" stroke="#C00000" stroke-width="8"
                        stroke-linecap="round" stroke-dasharray="301.6" stroke-dashoffset="301.6"/>
            </svg>
        </div>

        <!-- ================= PANTALLA 3: verificado ================= -->
        <div id="pantalla-exito" class="pantalla pantalla-oculta flex flex-col items-center justify-center text-center" style="min-height: 420px;">
            <div class="check-exito h-20 w-20 rounded-full bg-emerald-500 flex items-center justify-center mb-6">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                </svg>
            </div>
            <h1 class="titulo-cursiva text-4xl text-white mb-2">Código verificado</h1>
            <p class="text-sm text-slate-400">Redirigiendo…</p>
        </div>

    </div>
  </div>
</div>

<script>
    var contenedor = document.getElementById('contenedor-casillas');
    var casillas = document.querySelectorAll('.casilla');
    var formCodigo = document.getElementById('form-codigo');
    var btnVerificar = document.getElementById('btn-verificar');

    contenedor.classList.add('disperso');
    setTimeout(function () {
        contenedor.classList.remove('disperso');
        contenedor.classList.add('acomodado');
    }, 450);

    casillas.forEach(function (casilla, indice) {
        casilla.addEventListener('input', function () {
            casilla.value = casilla.value.replace(/\D/g, '').slice(0, 1);
            if (casilla.value && indice < casillas.length - 1) {
                casillas[indice + 1].focus();
            }
            if (indice === casillas.length - 1 && casilla.value && todasCompletas()) {
                formCodigo.requestSubmit();
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
                if (todasCompletas()) { formCodigo.requestSubmit(); }
            }
        });
    });

    function todasCompletas() {
        return Array.from(casillas).every(function (c) { return c.value.length === 1; });
    }

    function mostrarPantalla(id) {
        ['pantalla-ingreso', 'pantalla-verificando', 'pantalla-exito'].forEach(function (otroId) {
            document.getElementById(otroId).classList.toggle('pantalla-oculta', otroId !== id);
        });
    }

    function animarProgreso(duracionMs, callback) {
        var circulo = document.getElementById('circulo-progreso');
        var circunferencia = 301.6;
        var inicio = performance.now();
        function paso(ahora) {
            var avance = Math.min(1, (ahora - inicio) / duracionMs);
            circulo.setAttribute('stroke-dashoffset', String(circunferencia * (1 - avance)));
            if (avance < 1) { requestAnimationFrame(paso); } else if (callback) { callback(); }
        }
        requestAnimationFrame(paso);
    }

    formCodigo.addEventListener('submit', function (e) {
        e.preventDefault();
        var codigo = Array.from(casillas).map(function (c) { return c.value; }).join('');
        document.getElementById('codigo-completo').value = codigo;
        btnVerificar.disabled = true;

        mostrarPantalla('pantalla-verificando');
        document.getElementById('circulo-progreso').setAttribute('stroke-dashoffset', '301.6');

        var datos = new URLSearchParams();
        datos.set('codigo', codigo);
        datos.set('csrfToken', document.querySelector('input[name=csrfToken]').value);

        var peticion = fetch(formCodigo.action, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: datos.toString(),
            credentials: 'same-origin'
        });

        Promise.all([peticion, new Promise(function (r) { animarProgreso(900, r); })])
            .then(function (resultados) {
                var respuesta = resultados[0];
                var urlFinal = respuesta.url || '';
                var esExito = respuesta.redirected && urlFinal.indexOf('/recuperar-password-codigo') === -1
                        && urlFinal.indexOf('/recuperar-password') === -1;

                if (esExito) {
                    mostrarPantalla('pantalla-exito');
                    setTimeout(function () { window.location.href = urlFinal; }, 900);
                    return;
                }
                if (respuesta.redirected) {
                    window.location.href = urlFinal;
                    return;
                }

                respuesta.text().then(function (html) {
                    var doc = new DOMParser().parseFromString(html, 'text/html');
                    var errorNuevo = doc.getElementById('banner-error');
                    var mensaje = errorNuevo ? errorNuevo.textContent.trim() : 'Código incorrecto. Intente nuevamente.';

                    mostrarPantalla('pantalla-ingreso');
                    btnVerificar.disabled = false;

                    var bannerActual = document.getElementById('banner-error');
                    bannerActual.textContent = mensaje;
                    bannerActual.classList.remove('hidden');

                    casillas.forEach(function (c) { c.value = ''; });
                    casillas[0].focus();
                    contenedor.classList.add('sacudida');
                    setTimeout(function () { contenedor.classList.remove('sacudida'); }, 400);
                });
            })
            .catch(function () {
                mostrarPantalla('pantalla-ingreso');
                btnVerificar.disabled = false;
                var bannerActual = document.getElementById('banner-error');
                bannerActual.textContent = 'No se pudo verificar en este momento. Intente nuevamente.';
                bannerActual.classList.remove('hidden');
            });
    });
</script>

</body>
</html>
