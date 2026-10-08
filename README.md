# Bodega TAMBITO

Sistema web de gestión de bodega y punto de venta (POS), desarrollado como proyecto del curso **Integrador II** — Universidad Tecnológica del Perú.

Cubre el ciclo completo de una bodega/minimarket: catálogo de productos, control de inventario por lotes con fecha de vencimiento, punto de venta, órdenes de compra a proveedores con flujo de aprobación, clientes, reportes, auditoría y administración de usuarios con control de acceso por rol.

---

## Tabla de contenidos

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Módulos del sistema](#módulos-del-sistema)
- [Seguridad](#seguridad)
- [Base de datos](#base-de-datos)
- [Despliegue](#despliegue)
- [Cómo correrlo en local](#cómo-correrlo-en-local)

---

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje / runtime | Java 17 |
| Backend | Jakarta Servlets 6.0, JSP + JSTL |
| Acceso a datos | JDBC + HikariCP (pool de conexiones) |
| Base de datos | MySQL 8.0 |
| Build | Maven (empaquetado WAR) |
| Frontend | JSP renderizado en servidor + Tailwind CSS (CDN) + JavaScript vanilla |
| Contenedor de servlets | Apache Tomcat |
| Autenticación | Hash de contraseñas con BCrypt (jBCrypt), MFA con TOTP (compatible con Google Authenticator) |
| Correo | Jakarta Mail + Angus Mail (SMTP) |
| PDF | OpenPDF (comprobantes de venta, reportes) |
| Excel | Apache POI (exportación de reportes e inventario) |
| Códigos QR | ZXing (pagos Yape/Plin) |
| Infraestructura | AWS EC2, systemd, DuckDNS (dominio), respaldos automáticos programados |

## Arquitectura

El sistema sigue una arquitectura en capas, clásica de aplicaciones Java EE:

```
Navegador
   │
   ▼
Filtros (AuthenticationFilter, AuthorizationFilter, CsrfFilter, SecurityHeadersFilter)
   │
   ▼
Servlets (controladores — uno por módulo, ej. ProductoServlet, VentaServlet)
   │
   ▼
Servicios (reglas de negocio — ej. VentaService, InventarioService)
   │
   ▼
DAO (acceso a datos vía JDBC — ej. ProductoDAO, VentaDAO)
   │
   ▼
MySQL 8.0
```

- **Filtros**: se ejecutan antes que cualquier servlet; validan sesión, rol, token CSRF y agregan cabeceras de seguridad a toda respuesta.
- **Servlets**: reciben la petición HTTP, delegan la lógica a un Service y reenvían (`forward`) a la vista JSP correspondiente.
- **Services**: contienen las reglas de negocio (ej. validar stock antes de vender, calcular FEFO, enviar notificaciones por correo). No conocen HTTP.
- **DAO**: encapsulan el SQL. Cada entidad principal tiene su propio DAO.
- **Modelos**: clases planas (POJOs) que representan las entidades del negocio.

Las vistas (JSP) comparten un layout común (`WEB-INF/views/layout/`) con cabecera, barra lateral de navegación (filtrada según el rol de la sesión) y pie de página.

## Estructura del proyecto

```
bodega-web/
└── src/main/java/com/bodega/
    ├── config/      Configuración de la aplicación (conexión a BD, respaldos)
    ├── dao/         Acceso a datos (18 clases, una por entidad principal)
    ├── filter/      Filtros de seguridad (autenticación, autorización, CSRF, cabeceras)
    ├── listener/    Listener de arranque de la aplicación (programa tareas periódicas)
    ├── model/       Entidades del dominio (29 clases)
    ├── scheduler/   Tareas programadas (respaldos automáticos, alertas)
    ├── service/     Lógica de negocio (24 clases)
    ├── servlet/     Controladores HTTP (30 servlets, uno o más por módulo)
    └── util/        Utilidades transversales (cifrado, constantes, registro de sesiones)
```

19 módulos de vista en `src/main/webapp/WEB-INF/views/`: autenticación, panel de control, POS, turno de caja, ventas, productos, inventario, proveedores, categorías, clientes, órdenes de compra, usuarios, reportes, auditoría, respaldos, configuración y perfil.

## Módulos del sistema

| # | Módulo | Rol requerido |
|---|---|---|
| 1 | Autenticación y Seguridad (login, MFA, recuperación de contraseña) | Todos |
| 2 | Panel de Control | Todos |
| 3 | Punto de Venta (POS) | Administrador / Vendedor |
| 4 | Turno de Caja | Administrador / Vendedor |
| 5 | Ventas | Administrador / Vendedor |
| 6 | Productos | Administrador / Vendedor |
| 7 | Inventario | Administrador / Vendedor |
| 8 | Proveedores | Administrador / Vendedor |
| 9 | Categorías | Administrador / Vendedor |
| 10 | Clientes | Administrador / Vendedor |
| 11 | Órdenes de Compra (con flujo de aprobación) | Administrador / Vendedor |
| 12 | Usuarios y Roles de Seguridad | Administrador |
| 13 | Reportes | Administrador |
| 14 | Auditoría | Administrador |
| 15 | Respaldos de Base de Datos | Administrador |
| 16 | Configuración del Sistema | Administrador |

La explicación funcional detallada de cada módulo (qué hace, cómo funciona por dentro y qué reglas de negocio aplica) está en el documento `Documentacion Tecnica Bodega TAMBITO.docx`.

## Seguridad

- **Contraseñas**: nunca se almacenan en texto plano; se guardan con hash BCrypt.
- **Segundo factor (MFA)**: TOTP compatible con Google Authenticator, activable por usuario.
- **Cambio de contraseña obligatorio**: toda cuenta nueva o restablecida por un administrador queda confinada al módulo "Mi Perfil" hasta cambiar la contraseña temporal.
- **Bloqueo por intentos fallidos**: 3 intentos incorrectos bloquean la cuenta temporalmente.
- **CSRF**: todas las acciones que modifican datos exigen un token de sincronización por sesión.
- **Control de acceso por rol**: filtros dedicados (`AuthenticationFilter`, `AuthorizationFilter`) validan sesión y rol en cada petición, no solo se ocultan opciones en la interfaz.
- **Cifrado de datos sensibles**: campos como la dirección del cliente se almacenan cifrados en la base de datos.
- **Auditoría**: cada acción sensible (login, aprobaciones, cambios de configuración, restablecimientos de contraseña) queda registrada con usuario, fecha/hora e IP, en un registro de solo lectura.
- **Cabeceras de seguridad HTTP** aplicadas a nivel global (`SecurityHeadersFilter`): `Cache-Control: no-store`, `X-Content-Type-Options: nosniff`, entre otras.
- **Validación estricta de archivos subidos** (ej. logotipo): se valida el contenido real del archivo, no solo la extensión.
- **Gestión de dependencias**: versiones de librerías revisadas y actualizadas para cerrar vulnerabilidades conocidas (CVE) detectadas por escaneo automático de seguridad.

## Base de datos

MySQL 8.0, con las siguientes tablas y vistas principales:

- **Core de negocio**: `productos`, `categorias`, `proveedores`, `clientes`, `lotes_producto`
- **Operación**: `ventas`, `detalle_venta`, `turnos_caja`, `movimientos_inventario`
- **Compras**: `ordenes_compra`, `detalle_orden_compra`
- **Seguridad y sistema**: `usuarios`, `auditoria`, `configuracion`, `archivos_configuracion`, `bitacora_respaldos`
- **Caché**: `cache_tipo_cambio`, `cache_consulta_documento`
- **Vistas**: `vista_productos_stock_bajo`, `vista_top_productos_categoria`, `vista_ventas_hoy`

El acceso se hace mediante un pool de conexiones HikariCP. El esquema completo (con comentarios) está en `bodega-web/src/main/resources/schema.sql`.

## Despliegue

- Alojado en una instancia **AWS EC2**, corriendo **Apache Tomcat** como servicio `systemd`.
- Dominio propio vía **DuckDNS**, con el acceso servido exclusivamente por HTTPS.
- **Respaldos automáticos completos** de la base de datos cada 4 horas (alineados a horas en punto), con retención de 30 días; también soporta respaldo manual y descarga directa del archivo desde el panel de administración.
- Zona horaria del servidor configurada en hora de Perú (America/Lima), tanto a nivel de sistema operativo como de MySQL y de la JVM.
- Variables de conexión y credenciales fuera del control de versiones (`database.properties`, excluido por `.gitignore`; se provee `database.properties.example` como plantilla).

## Cómo correrlo en local

Requisitos: JDK 17, Maven, MySQL 8.0, un contenedor de servlets compatible con Jakarta EE 10 (ej. Tomcat 10+).

```bash
# 1. Clonar el repositorio
git clone <url-del-repo>
cd stitch_sistema_bodega_java_web/bodega-web

# 2. Crear la base de datos y cargar el esquema
mysql -u root -p -e "CREATE DATABASE bodega_db"
mysql -u root -p bodega_db < src/main/resources/schema.sql

# 3. Configurar la conexión
cp src/main/resources/database.properties.example src/main/resources/database.properties
# editar database.properties con tus credenciales locales

# 4. Compilar y generar el WAR
mvn clean package

# 5. Desplegar el .war generado (target/bodega-web.war) en tu Tomcat
```

---

