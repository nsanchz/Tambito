# Contexto del proyecto — Bodega TAMBITO

> Resumen de todo el trabajo realizado con Claude en este proyecto. Última actualización: 2026-10-24.

## 1. Qué es este proyecto

Sistema de Ventas y Control de Inventario para **Bodega TAMBITO** (negocio real de abarrotes en San Martín de Porres, Lima), desarrollado como Avance de Proyecto Final (APF1 + APF2) del curso **Integrador II** (UTP). Java EE clásico: Servlets + JSP + JDBC/HikariCP + MySQL, desplegado en AWS EC2.

- **Repositorio**: `nsanchz/Tambito` (rama `main`)
- **Producción**: `https://tambito.duckdns.org/bodega-web/`
- **Servidor**: AWS EC2 t3.micro (1 vCPU útil, 1 GB RAM), Amazon Linux 2023, Tomcat 11 + Java 17 (Corretto), MySQL 8.0 en el mismo servidor
- **Equipo**: Asís López Jaime, Amaya Lupu César, Cuevas Palma Domingo, Sánchez Rodas Neyder (Scrum Master), Huarag Aranda Joel, Yalle Gonzales Jesús

---

## 2. Documento académico (Word) — `Avance 2 - Integrador.docx`

Ubicación: `~/Desktop/Int2/Avance 2 - Integrador.docx` (sincronizado también en OneDrive `INTEGRADOR II/Informes/Avance 2/`).

Trabajo realizado:
- Arreglados los 3 índices (Tabla de Contenido, Tablas, Figuras) para que Word los recalcule bien al abrir (`w:updateFields` + conversión de captions hardcodeadas a campos `SEQ` reales), verificado con automatización real de Microsoft Word vía AppleScript.
- Validadas las 4 retrospectivas de sprint (una por cada uno de los 4 sprints); se encontró y corrigió la del Sprint 1, que no tenía el rótulo "Retrospectiva" y tenía un caption mal puesto ("Daily 1").
- Reorganización: Sprint 4 movido a Anexos (junto a Sprints 1-3), bibliografía consolidada en una sola lista numerada (sin sección separada "Avance 2").
- Se agregó un bloque completo de "Conclusiones Generales" con datos reales del proyecto.
- Se quitó el Burndown Chart / tabla de métricas que se había agregado de más en Sprint 3 y 4 (quedó el formato simple Daily Scrum → Retrospectiva, igual que Sprint 1/2).

---

## 3. Material de presentación

### Canva (deck maestro, ~69 páginas)
Link: `https://www.canva.com/design/DAHXXy7LHh0/...`. Se agregaron como **elementos nativos editables** (texto/formas, nunca imágenes planas):
- Daily 4, Burndown Chart Sprint 4, Retrospectiva Sprint 4
- Conclusiones Generales con datos reales del proyecto

Hubo un intento de generar un deck completo nuevo replicando el estilo de un deck más chico que le gustó al usuario (`DAHXjsn4H1w`, estilo navy/cyan/garnet), pero falló por **cuota de créditos de Canva agotada** (`quota_exceeded`, límite de la cuenta, no resoluble desde la sesión).

### PowerPoint — `~/Desktop/Int2/Avance_Integrador_Completo.pptx`
Como alternativa al límite de Canva, se generó un PPTX nativo (pptxgenjs) replicando el mismo estilo visual (navy `#051D40`, cyan `#5CE1E6`, garnet `#B74662`/`#8C3B4A`), con datos 100% reales del proyecto (commits de Git, burndown, métricas de backend, pruebas funcionales, despliegue AWS).

- Primera versión: 23 diapositivas (resumen condensado).
- **Versión final: 49 diapositivas**, reestructurada para tener una profundidad comparable al deck de Canva de 69 páginas: 7 divisores de capítulo, cada uno de los 4 sprints desglosado individualmente (Daily Scrum + Retrospectiva por separado, con Burndown real en Sprint 3 y gráfico de commits reales en Sprint 4), 4 wireframes individuales, evidencia de código real (`VentaService.registrarVenta()`, transacción ACID), etc.
- Validado con `validate.py` (sin errores) y revisado visualmente diapositiva por diapositiva.

### Propuesta de API (ver sección 5) documentada en PDF
- `~/Desktop/Int2/Propuesta_API_Solo_Lectura_TAMBITO.pdf` (propuesta inicial, antes de implementar)
- `~/Desktop/Int2/TAMBITO_API_Solo_Lectura_Documentacion.pdf` (documentación final, con ejemplos reales de respuesta ya en producción)

---

## 4. Infraestructura del servidor (AWS EC2)

### Incidente: servidor caído tras reinicio de instancia
La instancia EC2 se reinició (sin IP elástica, por eso cambió a `52.90.165.5`; DuckDNS se autoactualizó solo). Tras el reinicio, **Tomcat quedó en loop de fallo**: un PID file obsoleto (`/opt/tomcat/temp/tomcat.pid`) apuntaba a un PID que, tras el reboot, pasó a pertenecer a otro proceso (`gssproxy`), y `startup.sh` creía que Tomcat ya corría.

**Fix aplicado**: detener el servicio, borrar el PID file obsoleto, reiniciar `tomcat.service`. Verificado con `https://tambito.duckdns.org/` → 302 → `/bodega-web/login` → "Iniciar Sesión - TAMBITO".

### Prueba de carga con k6 (`stress-login.js`)
El usuario corrió un stress test contra `/bodega-web/login` escalando 100 → 300 → 600 → 1000 VUs. El servidor empezó a fallar (timeouts masivos) al entrar en la etapa de 600 VUs (~segundo 272 del test).

**Diagnóstico** (verificado en el servidor, no teórico):
| Recurso | Configuración encontrada | Problema bajo carga |
|---|---|---|
| RAM del EC2 (t3.micro) | 1 GB total, solo ~117 MB libres en reposo | Primer cuello de botella |
| Tomcat `maxThreads` (connector 8443) | 150 | Excesivo para 1 vCPU real: puro context-switching |
| HikariCP (pool a MySQL) | `maxSize=10` (ya estaba bien ajustado) | — |
| JVM heap | sin `-Xmx` explícito | Sin control, compite por RAM sin límite claro |

### Mitigaciones aplicadas (gratis, sin cambiar de instancia)
1. **Swap de 9.3 GiB** en `/swapfile`, persistente en `/etc/fstab`, `vm.swappiness=10` (solo como red de seguridad, no agresivo).
2. **JVM heap acotado**: `/opt/tomcat/bin/setenv.sh` con `-Xms256m -Xmx448m -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError` (si el JVM se queda sin memoria de verdad, muere limpio y `systemd` lo reinicia solo).
3. **Tomcat connector 8443** (`/opt/tomcat/conf/server.xml`, con backup `server.xml.bak-*`): `maxThreads` bajado de 150 a 75, agregado `minSpareThreads=10` y `acceptCount=200` (cola de espera en vez de saturar todo de golpe).

Resultado esperado: el servidor ya no muere de golpe ante un pico de carga — degrada gradualmente (cola + swap) en vez de colapsar.

### Sobre subir a t3.small
Se discutió subir de t3.micro (1 GB RAM) a t3.small (2 GB RAM, mismos 2 vCPU burstable). Conclusión: resolvería el cuello de botella de RAM identificado (duplicaría el margen libre, permitiría subir heap/threads/pool más), pero **no cambia la cantidad de vCPU**, así que no hay garantía de aguantar los 1000 VUs completos sin volver a probar. Tiene costo (~USD 15-17/mes, no es Free Tier). **No se implementó** — quedó como decisión pendiente del usuario.

### Protección anti-flood / tipo Cloudflare (discutido, NO implementado aún)
Como DuckDNS no permite delegar DNS a Cloudflare (no se controla la zona `duckdns.org`), se descartó Cloudflare clásico. Opciones evaluadas, de más a menos recomendada para este proyecto:
1. **Rate limiting en `nftables`** (ya configurado en el servidor) — limitar conexiones nuevas por IP/segundo. **Recomendada, pendiente de implementar.**
2. **Nginx con `limit_req`** delante de Tomcat — rate limiting más fino a nivel de petición HTTP.
3. **AWS CloudFront** (Free Tier) + AWS Shield Standard (gratis, automático) — da URL `*.cloudfront.net` sin dominio propio, pero solo cachea contenido estático (la app es mayormente dinámica).
4. Cloudflare Tunnel "Quick Tunnel" (gratis, sin dominio, pero URL aleatoria que cambia en cada reinicio — no apto para este proyecto).
5. Comprar un dominio barato (~$1-3/año) y usar Cloudflare de verdad — descartado por ahora (no es 100% gratis y el proyecto no lo necesita a esta escala).

---

## 5. API de solo lectura (`/api/public/v1/*`)

Implementada y **desplegada en producción**, a pedido explícito del usuario: *"necesito que solo esas consultas api permitan el get, nada de push delete o alter"*.

### Diseño de seguridad
- Protegida por header **`X-API-Key`** (no por sesión de navegador) — mecanismo completamente aislado del resto de la app.
- Filtro nuevo `com.bodega.filter.ApiKeyFilter`, mapeado únicamente a `/api/public/*` en `web.xml` (antes de `AuthenticationFilter`).
- `AuthenticationFilter` tiene `/api/public/` agregado a sus rutas públicas (la sesión ya no aplica ahí, la API Key hace su propio control).
- **Todas las clases son estrictamente `doGet`** — no existe `doPost`/`doPut`/`doDelete` en ninguna. Verificado en producción: un POST a `/productos` con la key correcta devuelve **403** (bloqueado por los filtros del proyecto antes de llegar al servlet).
- Cada endpoint usa un **DTO propio** (nunca serializa entidades crudas) — nunca expone `passwordHash`, `mfaSecret`, `precioCompra`, direcciones cifradas de clientes, ni datos de Usuario/Venta/Cliente individuales ni Auditoría.
- La clave vive en `database.properties` → `api.public.key` (rotarla invalida cualquier integración vieja al instante).

### Endpoints activos
Base: `https://tambito.duckdns.org/bodega-web/api/public/v1`

| Endpoint | Auth | Descripción |
|---|---|---|
| `GET /estado` | Pública | Healthcheck |
| `GET /productos?categoriaId=&texto=` | X-API-Key | Catálogo (sin costos ni stock exacto) |
| `GET /producto?id=` | X-API-Key | Detalle de un producto (404 si no existe/inactivo) |
| `GET /categorias` | X-API-Key | Categorías activas |
| `GET /proveedores` | X-API-Key | Proveedores activos (sin correo) |
| `GET /dashboard-resumen` | X-API-Key | Métricas agregadas del negocio |
| `GET /inventario-stock-bajo` | X-API-Key | Productos bajo su mínimo |
| `GET /inventario-por-vencer?dias=` | X-API-Key | Lotes próximos a vencer (default 7 días) |

### Archivos de código nuevos/modificados
- `src/main/java/com/bodega/filter/ApiKeyFilter.java` (nuevo)
- `src/main/java/com/bodega/filter/AuthenticationFilter.java` (agregada excepción `/api/public/`)
- `src/main/webapp/WEB-INF/web.xml` (registrado el nuevo filtro)
- `src/main/resources/database.properties` (agregada `api.public.key`)
- 8 servlets nuevos en `src/main/java/com/bodega/servlet/`: `PublicEstadoServlet`, `PublicProductosServlet`, `PublicProductoDetalleServlet`, `PublicCategoriasServlet`, `PublicProveedoresServlet`, `PublicDashboardResumenServlet`, `PublicStockBajoServlet`, `PublicPorVencerServlet`

### Deploy
Compilado con Maven (`mvn clean package`), desplegado al EC2 reemplazando `bodega-web.war` (con backup `.bak-*` del WAR anterior), Tomcat reiniciado, los 8 endpoints probados uno por uno contra producción.

### Entregables para consumirla
- `~/Desktop/Int2/TAMBITO_API_Solo_Lectura.postman_collection.json` — colección importable directo en Postman (File → Import), con la API Key y la URL base ya cargadas como variables.
- `~/Desktop/Int2/TAMBITO_API_Solo_Lectura_Documentacion.pdf` — documentación con ejemplos reales de respuesta.

---

## 6. Pendientes / decisiones abiertas

- [ ] Implementar rate limiting en `nftables` (o nginx `limit_req`) para mitigar floods tipo el del stress test — discutido, no implementado.
- [ ] Decidir si se sube la instancia EC2 a t3.small (tiene costo, no es Free Tier).

> El flujo de aprobación de Órdenes de Compra + envío de comprobantes por correo (Gmail SMTP) **ya está implementado completo** (`EmailService`, `OrdenCompraServlet` con `aprobar`/`rechazar`) — confirmado en código, no es pendiente.

---

## 7. Dónde está todo

| Qué | Dónde |
|---|---|
| Código fuente | `bodega-web/src/main/java/com/bodega/` |
| Config (credenciales, API key, SMTP) | `bodega-web/src/main/resources/database.properties` |
| Filtros de seguridad | `bodega-web/src/main/java/com/bodega/filter/` |
| Servidor producción | EC2 `52.90.165.5` (SSH con `~/Desktop/admin.pem`, usuario `ec2-user`) |
| Dominio | `tambito.duckdns.org` (DuckDNS, se autoactualiza con la IP del EC2) |
| Entregables académicos | `~/Desktop/Int2/` (Word, PPTX, PDFs, colección Postman) |
