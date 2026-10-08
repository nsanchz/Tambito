# Métricas de GitHub — Bodega TAMBITO

Resumen de la actividad del repositorio a lo largo del desarrollo del proyecto.

## Resumen general

| Métrica | Valor |
|---|---|
| Total de commits | 28 |
| Rango de fechas | 26 de septiembre – 7 de octubre de 2026 |
| Días con actividad | 4 |
| Líneas agregadas | ~22,300 |
| Líneas eliminadas | ~770 |
| Archivos distintos modificados | 154 |
| Ramas | `main` |

## Línea de tiempo del proyecto

```
26 sep  ██████████  10 commits   Base del sistema: MFA, consulta RUC/DNI, rediseño de login
27 sep  ██████       6 commits   Rediseño visual completo + primera ronda de seguridad (Aikido)
02 oct  ██████████  10 commits   Recuperación de contraseña, UX de autenticación, respaldos, Aikido #2
07 oct  █            1 commit    Bloqueo de navegación con cambio de contraseña pendiente
```

## Detalle de commits por etapa

### Etapa 1 — Base del sistema (26 sep, 10 commits)
- Checkpoint inicial antes de implementar MFA + consulta RUC/DNI + rediseño de login
- Autenticación de dos factores (TOTP) para administradores
- Integración con servicio externo de consulta de RUC/DNI (apisperu.com)
- Corrección del flujo de enrolamiento de MFA (propio del usuario, no del admin)
- Pago en efectivo en dólares en el Punto de Venta
- Corrección de desbordes de layout (botón Buscar, acciones de usuarios)
- Alineación de casillas de MFA y restricción de terminal por rol
- Reglas de negocio del POS: boleta obligatoria para "Cliente varios"
- Indicador visual de fortaleza de contraseña
- Rediseño del menú de acciones de Usuarios (menú desplegable)

### Etapa 2 — Rediseño visual y seguridad (27 sep, 6 commits)
- Rediseño visual moderno en toda la aplicación (paleta granate, microanimaciones, spinners)
- Primera ronda de corrección de vulnerabilidades reportadas por Aikido (escaneo automático de seguridad)
- Actualización de dependencias vulnerables: `protobuf-java`, `log4j-api`, `commons-compress`
- Eliminación de contraseñas reales que habían quedado expuestas en `schema.sql`

### Etapa 3 — Recuperación de contraseña y endurecimiento (2 oct, 10 commits)
- Flujo de restablecimiento de contraseña autoservicio por correo (3 pasos)
- Botón "ojito" animado para mostrar/ocultar contraseña
- Mejoras de UX en las pantallas de autenticación (centrado, fondo, validaciones)
- Segunda ronda de Aikido: actualización de Apache POI y commons-lang3
- Corrección de recorte del logo de la empresa (sidebar y Configuración)
- Reemplazo de `confirm()` nativo del navegador por un modal propio
- Descarga directa de archivos de respaldo desde el panel
- Cambio de la periodicidad de respaldos automáticos (de 1 vez al día a cada 4 horas)

### Etapa 4 — Seguridad de sesión (7 oct, 1 commit)
- Bloqueo de navegación a cualquier módulo mientras haya un cambio de contraseña obligatorio pendiente

## Categorías de trabajo

| Categoría | Commits aprox. | Ejemplos |
|---|---|---|
| Seguridad y autenticación | 9 | MFA, recuperación de contraseña, bloqueo por cambio pendiente |
| Seguridad de dependencias (Aikido) | 3 | Actualización de librerías vulnerables, limpieza de credenciales expuestas |
| Experiencia de usuario (UI/UX) | 10 | Rediseño visual, animaciones, corrección de layout, modal propio |
| Infraestructura y operación | 2 | Respaldos automáticos, descarga de respaldos |
| Reglas de negocio | 4 | POS, validaciones de usuario, consulta RUC/DNI |

## Nota sobre la autoría en Git

El historial de commits de este repositorio está centralizado en una sola cuenta de GitHub, usada para la integración técnica del proyecto. El resto del equipo participó en otras partes del trabajo que no quedan reflejadas en el historial de Git: documentación, diseño de presentación, pruebas funcionales manuales y la metodología Scrum del curso (dailies, retrospectivas, backlog). Esa parte del trabajo está documentada por separado en la presentación del proyecto.

---

*Generado a partir del historial real del repositorio (`git log`).*
