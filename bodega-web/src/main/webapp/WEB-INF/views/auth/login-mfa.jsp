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

        /* --- Casillas del código: nacen dispersas y rotadas, y se acomodan en fila ---
           Las 6 casillas viven SIEMPRE en su posición correcta de flexbox (justify-between);
           el efecto de dispersión es solo un `transform` visual encima de esa posición, así
           que el estado final ("acomodado" = transform identidad) queda perfectamente
           alineado por construcción, sin calcular posiciones absolutas a mano. */
        #contenedor-casillas {
            display: flex;
            justify-content: space-between;
        }
        .casilla-codigo {
            width: 44px;
            height: 56px;
            background: rgba(255,255,255,0.06);
            border: 1px solid rgba(255,255,255,0.14);
            color: #f1f5f9;
            border-radius: 12px;
            text-align: center;
            font-size: 1.25rem;
            transition: transform 0.7s cubic-bezier(.2,.8,.2,1), border-color 0.2s, box-shadow 0.2s;
        }
        .casilla-codigo:focus {
            outline: none;
            border-color: #C00000;
            box-shadow: 0 0 0 3px rgba(192,0,0,0.25);
        }
        /* Desplazamiento/rotación dispersos iniciales (uno por casilla, vía nth-child) */
        #contenedor-casillas.disperso .casilla-codigo:nth-child(1) { transform: translate(4px, 10px) rotate(-16deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(2) { transform: translate(-6px, -14px) rotate(11deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(3) { transform: translate(8px, 8px) rotate(-9deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(4) { transform: translate(-8px, -6px) rotate(14deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(5) { transform: translate(6px, 12px) rotate(-12deg); }
        #contenedor-casillas.disperso .casilla-codigo:nth-child(6) { transform: translate(-4px, -10px) rotate(8deg); }
        /* Acomodado = sin transform extra: quedan exactamente donde el flexbox las ubica. */
        #contenedor-casillas.acomodado .casilla-codigo { transform: translate(0, 0) rotate(0deg); }

        .pantalla { transition: opacity 0.35s ease, transform 0.35s ease; }
        .pantalla-oculta { opacity: 0; position: absolute; inset: 0; pointer-events: none; transform: scale(0.98); }

        .anillo-progreso {
            transform: rotate(-90deg);
        }
        .anillo-progreso circle {
            transition: stroke-dashoffset 0.15s linear;
        }

        @keyframes aparecerCheck {
            0%   { transform: scale(0); opacity: 0; }
            60%  { transform: scale(1.15); opacity: 1; }
            100% { transform: scale(1); opacity: 1; }
        }
        .check-exito { animation: aparecerCheck 0.5s cubic-bezier(.2,.9,.3,1.2) forwards; }

        @keyframes flotarConfeti {
            0%   { transform: translateY(0) scale(1); opacity: 0.9; }
            100% { transform: translateY(-40px) scale(0.6); opacity: 0; }
        }
        .confeti { animation: flotarConfeti 1.4s ease-out forwards; }

        @keyframes sacudir {
            0%, 100% { transform: translateX(0); }
            20%, 60% { transform: translateX(-6px); }
            40%, 80% { transform: translateX(6px); }
        }
        .sacudida { animation: sacudir 0.4s ease; }
    </style>
</head>
<body class="bg-black min-h-screen relative overflow-x-hidden">

<div class="absolute inset-0 fondo-nubes pointer-events-none"></div>

<div class="relative min-h-screen flex items-center justify-center px-6 py-12 overflow-y-auto">
  <div class="max-w-sm w-full bg-gradient-to-br from-slate-900/90 to-black/90 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-8 sm:p-10">
    <div class="max-w-sm w-full mx-auto relative" style="min-height: 280px;">

        <!-- ================= PANTALLA 1: ingreso del código ================= -->
        <div id="pantalla-ingreso" class="pantalla">
            <c:choose>
                <c:when test="${not empty mfaQrDataUri}">
                    <h1 class="titulo-cursiva text-4xl text-white mb-3">Activar verificación</h1>
                    <p class="text-sm text-slate-400 mb-6">
                        Un administrador activó la verificación en dos pasos para su cuenta. Abra
                        <strong class="text-slate-300">Google Authenticator</strong>, escanee este código y
                        luego ingrese los 6 dígitos generados para confirmar.
                    </p>
                    <div class="flex justify-center mb-6">
                        <img src="${mfaQrDataUri}" alt="Código QR de activación de MFA" class="rounded-lg" width="180" height="180">
                    </div>
                    <details class="text-xs text-slate-500 mb-6">
                        <summary class="cursor-pointer">¿No puede escanear? Ingreso manual</summary>
                        <code class="block mt-1 p-2 bg-white/5 rounded break-all text-slate-400"><c:out value="${mfaOtpAuthUri}"/></code>
                    </details>
                </c:when>
                <c:otherwise>
                    <h1 class="titulo-cursiva text-4xl text-white mb-3">Verificación</h1>
                    <p id="texto-instruccion" class="text-sm text-slate-400 mb-8">
                        Un código de 6 dígitos espera en su Google Authenticator.
                    </p>
                </c:otherwise>
            </c:choose>

            <div id="banner-error" class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm ${empty error ? 'hidden' : ''}">
                <c:out value="${error}"/>
            </div>

            <form method="post" action="${pageContext.request.contextPath}/login-mfa" id="form-mfa" class="flex flex-col gap-8">
                <div class="flex" id="contenedor-casillas">
                    <c:forEach var="i" begin="0" end="5">
                        <input type="text" inputmode="numeric" maxlength="1"
                               class="casilla-codigo casilla" ${i == 0 ? 'autofocus' : ''}>
                    </c:forEach>
                </div>
                <input type="hidden" name="codigo" id="codigo-completo">

                <button type="submit" id="btn-verificar"
                        class="h-12 bg-white hover:bg-slate-200 text-slate-900 rounded-full font-medium text-sm
                               flex items-center justify-center gap-2 transition-colors disabled:opacity-50">
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

        <!-- ================= PANTALLA 2: verificando ================= -->
        <div id="pantalla-verificando" class="pantalla pantalla-oculta flex flex-col items-center justify-center text-center">
            <h1 class="titulo-cursiva text-4xl text-white mb-2">Verificando…</h1>
            <p class="text-sm text-slate-400 mb-10">Comprobando su código de forma segura.</p>
            <svg width="110" height="110" viewBox="0 0 110 110" class="anillo-progreso">
                <circle cx="55" cy="55" r="48" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="8"/>
                <circle id="circulo-progreso" cx="55" cy="55" r="48" fill="none" stroke="#C00000" stroke-width="8"
                        stroke-linecap="round" stroke-dasharray="301.6" stroke-dashoffset="301.6"/>
            </svg>
        </div>

        <!-- ================= PANTALLA 3: acceso concedido ================= -->
        <div id="pantalla-exito" class="pantalla pantalla-oculta flex flex-col items-center justify-center text-center relative overflow-hidden">
            <div id="capa-confeti" class="absolute inset-0 pointer-events-none"></div>
            <div class="check-exito h-20 w-20 rounded-full bg-emerald-500 flex items-center justify-center mb-6">
                <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7"/>
                </svg>
            </div>
            <h1 class="titulo-cursiva text-4xl text-white mb-2">Acceso concedido</h1>
            <p class="text-sm text-slate-400">Redirigiendo…</p>
        </div>

    </div>
  </div>
</div>

<script>
    var contenedor = document.getElementById('contenedor-casillas');
    var casillas = document.querySelectorAll('.casilla');
    var formMfa = document.getElementById('form-mfa');
    var btnVerificar = document.getElementById('btn-verificar');

    // --- Intro: nacen dispersas y a los pocos instantes se acomodan en fila ---
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
                formMfa.requestSubmit();
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
                if (todasCompletas()) { formMfa.requestSubmit(); }
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
            if (avance < 1) {
                requestAnimationFrame(paso);
            } else if (callback) {
                callback();
            }
        }
        requestAnimationFrame(paso);
    }

    function lanzarConfeti() {
        var capa = document.getElementById('capa-confeti');
        var colores = ['#34d399', '#60a5fa', '#f472b6', '#fbbf24', '#c084fc'];
        for (var i = 0; i < 18; i++) {
            var punto = document.createElement('div');
            var color = colores[i % colores.length];
            punto.className = 'confeti';
            punto.style.position = 'absolute';
            punto.style.left = (10 + Math.random() * 80) + '%';
            punto.style.top = (30 + Math.random() * 40) + '%';
            punto.style.width = '6px';
            punto.style.height = '6px';
            punto.style.borderRadius = '50%';
            punto.style.background = color;
            punto.style.animationDelay = (Math.random() * 0.3) + 's';
            capa.appendChild(punto);
        }
    }

    formMfa.addEventListener('submit', function (e) {
        e.preventDefault();
        var codigo = Array.from(casillas).map(function (c) { return c.value; }).join('');
        document.getElementById('codigo-completo').value = codigo;
        btnVerificar.disabled = true;

        mostrarPantalla('pantalla-verificando');
        document.getElementById('circulo-progreso').setAttribute('stroke-dashoffset', '301.6');

        var datos = new URLSearchParams();
        datos.set('codigo', codigo);

        var peticion = fetch(formMfa.action, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: datos.toString(),
            credentials: 'same-origin'
        });

        // La animación del anillo dura al menos 900ms para que se sienta real, aunque el
        // servidor responda antes; si tarda más, el anillo espera igual a que termine.
        Promise.all([peticion, new Promise(function (r) { animarProgreso(900, r); })])
            .then(function (resultados) {
                var respuesta = resultados[0];
                var urlFinal = respuesta.url || '';
                var esRedirectDeExito = respuesta.redirected && urlFinal.indexOf('/login-mfa') === -1
                        && urlFinal.indexOf('/login') === -1;

                if (esRedirectDeExito) {
                    mostrarPantalla('pantalla-exito');
                    lanzarConfeti();
                    setTimeout(function () { window.location.href = urlFinal; }, 1100);
                    return;
                }

                if (respuesta.redirected) {
                    // Redirigido a /login (sesión pendiente expiró o se agotaron los intentos).
                    window.location.href = urlFinal;
                    return;
                }

                // Sin redirect: el servidor reenvió la misma pantalla con un error (código
                // incorrecto). Se relee el HTML devuelto para mostrar el mensaje real y, si
                // corresponde, refrescar el QR (reintento durante el enrolamiento).
                respuesta.text().then(function (html) {
                    var doc = new DOMParser().parseFromString(html, 'text/html');
                    var formNuevo = doc.getElementById('form-mfa');

                    if (!formNuevo) {
                        // No hay formulario en la respuesta: se agotaron los intentos y el
                        // servidor reenvió a la vista de login.
                        window.location.href = '${pageContext.request.contextPath}/login';
                        return;
                    }

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
