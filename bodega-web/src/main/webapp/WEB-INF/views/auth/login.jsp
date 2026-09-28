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

        /* Malla geométrica animada (CSS puro, sin librerías 3D): un abanico de líneas que
           se desplaza suavemente, evocando el panel derecho de la referencia visual sin
           depender de WebGL/Canvas — liviano para servirse desde el EC2 free-tier. */
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
    </style>
</head>
<body class="bg-black min-h-screen flex">

<!-- Panel izquierdo: formulario -->
<div class="w-full lg:w-[42%] min-h-screen flex flex-col justify-center px-8 sm:px-16 py-12 bg-gradient-to-br from-slate-900 to-black relative">

    <div class="max-w-sm w-full mx-auto">
        <div class="flex flex-col items-center gap-2 mb-8">
            <img src="${pageContext.request.contextPath}/imagen?tipo=logo" alt="Logotipo de la tienda"
                 class="h-14" onerror="this.style.display='none'">
            <span class="text-slate-400 text-sm font-medium tracking-wide uppercase">
                <c:out value="${nombreEmpresa}" default="Bodega TAMBITO"/>
            </span>
        </div>

        <h1 class="titulo-cursiva text-5xl text-white mb-8 text-center">Iniciar sesión</h1>

        <c:if test="${not empty error}">
            <div class="bg-red-500/10 border border-red-500/30 text-red-300 rounded-lg p-3 mb-5 text-sm">
                <c:out value="${error}"/>
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
                    <span class="text-xs text-slate-500">¿Problemas para ingresar? Contacte al administrador</span>
                </div>
                <input type="password" id="password" name="j_password" required
                       class="h-12 px-4 rounded-lg text-sm campo-oscuro transition-shadow">
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

<!-- Panel derecho: gráfico decorativo (oculto en pantallas pequeñas) -->
<div class="hidden lg:block lg:w-[58%] min-h-screen bg-black malla-geometrica"></div>

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
</script>

</body>
</html>
