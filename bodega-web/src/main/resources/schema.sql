-- Esquema de base de datos: bodega_db
-- Se irá completando módulo por módulo a medida que se procesan las 17 carpetas.

CREATE DATABASE IF NOT EXISTS bodega_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE bodega_db;

-- =====================================================================
-- MÓDULO: Gestión de Usuarios y Roles de Seguridad / Autenticación
-- =====================================================================
CREATE TABLE usuarios (
    id                     INT AUTO_INCREMENT PRIMARY KEY,
    nombres                VARCHAR(100)  NOT NULL,
    apellidos              VARCHAR(100)  NOT NULL,
    nombre_usuario         VARCHAR(50)   NOT NULL UNIQUE,
    correo                 VARCHAR(150)  NOT NULL UNIQUE,
    telefono               VARCHAR(20),
    password_hash          VARCHAR(60)   NOT NULL,
    rol                    ENUM('ADMINISTRADOR', 'VENDEDOR') NOT NULL,
    estado                 ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    debe_cambiar_password  BOOLEAN       NOT NULL DEFAULT TRUE,
    intentos_fallidos      INT           NOT NULL DEFAULT 0,
    bloqueado_hasta        DATETIME      NULL,
    ultimo_acceso          DATETIME      NULL,
    fecha_creacion         DATETIME      NOT NULL,
    creado_por_id          INT           NULL,
    -- MFA (TOTP / Google Authenticator): activación y baja son exclusivas del administrador,
    -- nunca autoservicio. mfa_secret va cifrado con CifradoUtil (AES-256-GCM), igual que
    -- clientes.direccion.
    mfa_habilitado         BOOLEAN       NOT NULL DEFAULT FALSE,
    mfa_secret             VARCHAR(255)  NULL,
    mfa_activado_por_id    INT           NULL,
    mfa_fecha_activacion   DATETIME      NULL,
    -- Restablecimiento autoservicio de contraseña por correo (ver PasswordResetService):
    -- código de 6 dígitos hasheado con BCrypt (igual que password_hash, nunca en claro),
    -- válido por tiempo limitado y de un solo uso. reset_password_verificado solo se activa
    -- tras validar el código, habilitando recién ahí el paso de elegir la nueva contraseña.
    reset_password_codigo_hash VARCHAR(255) NULL,
    reset_password_expira      DATETIME     NULL,
    reset_password_intentos    INT          NOT NULL DEFAULT 0,
    reset_password_verificado  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_usuario_creado_por FOREIGN KEY (creado_por_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_mfa_activado_por FOREIGN KEY (mfa_activado_por_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_usuarios_rol ON usuarios (rol);
CREATE INDEX idx_usuarios_estado ON usuarios (estado);

-- =====================================================================
-- MÓDULO: Gestión de Categorías de Productos y Clasificación POS
-- =====================================================================
CREATE TABLE categorias (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    codigo           VARCHAR(20)   NOT NULL UNIQUE,
    nombre           VARCHAR(100)  NOT NULL,
    descripcion      VARCHAR(255),
    icono            VARCHAR(50)   NOT NULL DEFAULT 'category',
    margen_sugerido  DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    estado           ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion   DATETIME      NOT NULL
) ENGINE = InnoDB;

-- =====================================================================
-- MÓDULO: Proveedores (soporte de Órdenes de Compra)
-- =====================================================================
CREATE TABLE proveedores (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    ruc              VARCHAR(20),
    razon_social     VARCHAR(150)  NOT NULL,
    contacto_nombre  VARCHAR(100),
    telefono         VARCHAR(20),
    correo           VARCHAR(150),
    direccion        VARCHAR(255),
    estado           ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion   DATETIME      NOT NULL
) ENGINE = InnoDB;

-- =====================================================================
-- MÓDULO: Gestión de Productos, Catálogo e Inventario
-- =====================================================================
CREATE TABLE productos (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    sku              VARCHAR(30)   NOT NULL UNIQUE,
    codigo_barras    VARCHAR(20)   NULL UNIQUE,
    nombre           VARCHAR(150)  NOT NULL,
    descripcion      VARCHAR(500),
    categoria_id     INT           NOT NULL,
    proveedor_id     INT           NULL,
    marca            VARCHAR(80)   NOT NULL,
    precio_compra    DECIMAL(10,2) NOT NULL,
    precio_venta     DECIMAL(10,2) NOT NULL,
    stock_actual     INT           NOT NULL DEFAULT 0,
    stock_minimo     INT           NOT NULL DEFAULT 10,
    unidad_medida    ENUM('UNIDADES', 'KILOGRAMOS', 'LITROS', 'PAQUETES', 'CAJAS') NOT NULL DEFAULT 'UNIDADES',
    estado           ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    imagen_url       VARCHAR(255),
    fecha_creacion   DATETIME      NOT NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id)
) ENGINE = InnoDB;

CREATE INDEX idx_productos_categoria ON productos (categoria_id);
CREATE INDEX idx_productos_proveedor ON productos (proveedor_id);
CREATE INDEX idx_productos_estado ON productos (estado);
CREATE INDEX idx_productos_nombre ON productos (nombre);
-- Índice compuesto: ProductoDAO.listar(categoriaId, filtroEstado, ...) filtra por
-- ambas columnas a la vez (catálogo del POS filtrado por categoría + solo activos).
CREATE INDEX idx_productos_categoria_estado ON productos (categoria_id, estado);

-- =====================================================================
-- MÓDULO: Órdenes de Compra a Proveedores (recepción de mercadería)
-- Al recibir mercadería se incrementa el stock del producto y se inserta
-- un movimiento ENTRADA_COMPRA en el Kárdex referenciando compra_id,
-- todo dentro de la misma transacción ACID (ver OrdenCompraService).
-- Se crea antes que Lotes de Producto y Kárdex porque ambas la referencian.
-- =====================================================================
CREATE TABLE ordenes_compra (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    numero             VARCHAR(20)   NULL UNIQUE,
    proveedor_id       INT           NOT NULL,
    usuario_id         INT           NOT NULL,
    -- APROBADA/RECHAZADA: etapa de aprobación por un ADMINISTRADOR entre la creación
    -- (PENDIENTE) y la recepción de mercadería, que ahora solo se permite en estado
    -- APROBADA (ver OrdenCompraService.recibirMercaderia).
    estado             ENUM('PENDIENTE', 'APROBADA', 'RECHAZADA', 'RECIBIDA_PARCIAL', 'RECIBIDA_COMPLETA', 'CANCELADA') NOT NULL DEFAULT 'PENDIENTE',
    observaciones      VARCHAR(500),
    fecha_creacion     DATETIME      NOT NULL,
    fecha_ultima_recepcion DATETIME  NULL,
    aprobado_por_id    INT           NULL,
    fecha_aprobacion   DATETIME      NULL,
    motivo_rechazo     VARCHAR(500)  NULL,
    CONSTRAINT fk_orden_compra_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_orden_compra_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_orden_compra_aprobado_por FOREIGN KEY (aprobado_por_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE TABLE detalle_orden_compra (
    id                   INT AUTO_INCREMENT PRIMARY KEY,
    orden_compra_id      INT           NOT NULL,
    producto_id          INT           NOT NULL,
    cantidad_pedida      INT           NOT NULL,
    cantidad_recibida    INT           NOT NULL DEFAULT 0,
    precio_unitario      DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_compra_orden FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_detalle_compra_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE = InnoDB;

CREATE INDEX idx_ordenes_compra_estado ON ordenes_compra (estado);
CREATE INDEX idx_detalle_compra_orden ON detalle_orden_compra (orden_compra_id);

-- =====================================================================
-- MÓDULO: Lotes de Producto (trazabilidad de vencimiento para FEFO)
-- Cada recepción de mercadería (orden de compra o entrada manual) crea un
-- lote con su propia fecha de vencimiento. Las ventas y salidas descuentan
-- automáticamente primero del lote con fecha de vencimiento más próxima
-- (First-Expired-First-Out). fecha_vencimiento NULL = producto no perecible.
-- =====================================================================
CREATE TABLE lotes_producto (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    numero_lote        VARCHAR(20)   NULL UNIQUE,
    producto_id        INT           NOT NULL,
    fecha_vencimiento  DATE          NULL,
    cantidad_inicial   INT           NOT NULL,
    cantidad_actual    INT           NOT NULL,
    precio_compra      DECIMAL(10,2) NULL,
    orden_compra_id    INT           NULL,
    usuario_id         INT           NOT NULL,
    fecha_ingreso      DATETIME      NOT NULL,
    CONSTRAINT fk_lote_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_lote_orden_compra FOREIGN KEY (orden_compra_id) REFERENCES ordenes_compra (id),
    CONSTRAINT fk_lote_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_lotes_producto ON lotes_producto (producto_id);
CREATE INDEX idx_lotes_vencimiento ON lotes_producto (fecha_vencimiento);
CREATE INDEX idx_lotes_cantidad_disponible ON lotes_producto (producto_id, cantidad_actual);

-- =====================================================================
-- MÓDULO: Inventario y Movimientos de Almacén (Kárdex)
-- Tabla de solo inserción: ningún Servlet ni DAO expone UPDATE/DELETE sobre ella,
-- ya que cada fila es un hecho histórico inmutable del inventario.
-- =====================================================================
CREATE TABLE movimientos_inventario (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    numero_movimiento  VARCHAR(20)   NULL UNIQUE,
    producto_id        INT           NOT NULL,
    lote_id            INT           NULL,
    tipo               ENUM('ENTRADA_COMPRA', 'SALIDA_VENTA', 'AJUSTE_POSITIVO', 'AJUSTE_NEGATIVO',
                             'DEVOLUCION', 'MERMA', 'ANULACION_VENTA') NOT NULL,
    cantidad           INT           NOT NULL,
    stock_anterior     INT           NOT NULL,
    stock_resultante   INT           NOT NULL,
    motivo             VARCHAR(200)  NOT NULL,
    observaciones      VARCHAR(500),
    venta_id           INT           NULL,
    compra_id          INT           NULL,
    usuario_id         INT           NOT NULL,
    fecha              DATETIME      NOT NULL,
    CONSTRAINT fk_movimiento_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_movimiento_lote FOREIGN KEY (lote_id) REFERENCES lotes_producto (id),
    CONSTRAINT fk_movimiento_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_movimientos_producto ON movimientos_inventario (producto_id);
CREATE INDEX idx_movimientos_lote ON movimientos_inventario (lote_id);
CREATE INDEX idx_movimientos_tipo ON movimientos_inventario (tipo);
CREATE INDEX idx_movimientos_fecha ON movimientos_inventario (fecha);
-- Índice compuesto: MovimientoInventarioDAO.listarPorProducto() y el filtro por
-- productoId de listar() siempre acompañan la condición con ORDER BY fecha (kárdex
-- de un producto específico, el acceso más frecuente al módulo de Inventario).
CREATE INDEX idx_movimientos_producto_fecha ON movimientos_inventario (producto_id, fecha);

-- =====================================================================
-- MÓDULO: Gestión de Clientes y Fidelización
--
-- CIFRADO DE DATOS SENSIBLES (decisión documentada):
-- La columna "direccion" se cifra a nivel de APLICACIÓN (Java, AES-256-GCM,
-- ver com.bodega.util.CifradoUtil) antes de persistirse — ClienteDAO cifra
-- en crear()/actualizar() y descifra en mapear(). Se eligió AES-256-GCM en
-- Java, no AES_ENCRYPT() de MySQL, por 3 razones:
--   1) La clave de cifrado nunca viaja dentro de una sentencia SQL (evita
--      que quede expuesta en el binlog, en logs de consultas lentas o en
--      backups de mysqldump en texto plano).
--   2) GCM es cifrado autenticado (detecta manipulación del dato cifrado);
--      AES_ENCRYPT() de MySQL usa ECB/CBC sin autenticación.
--   3) Consistencia con el resto del proyecto, que ya centraliza en Java
--      todo lo relacionado a criptografía (BCrypt para password_hash).
-- Se amplía a VARCHAR(500) porque el texto cifrado (IV + texto + tag de
-- autenticación, en Base64) ocupa más espacio que la dirección en claro.
--
-- Columnas de PII que se EVALUARON y se decidió NO cifrar, y por qué:
--   - clientes.numero_documento (DNI/RUC): ClienteDAO.listar() lo busca con
--     LIKE '%...%' para la búsqueda rápida en el mostrador (POS); un cifrado
--     determinístico rompe el LIKE y uno no determinístico rompe hasta la
--     igualdad exacta. Mitigación aplicada en su lugar: acceso restringido
--     por rol (RBAC) + auditoría de consultas a clientes.
--   - usuarios.correo: se usa tanto para login (igualdad exacta) como para
--     búsqueda parcial del administrador en Gestión de Usuarios (LIKE);
--     cifrarlo rompería el inicio de sesión por correo.
--   - proveedores.direccion: es un dato de una persona JURÍDICA (empresa),
--     de menor sensibilidad que el domicilio de una persona natural; se
--     mantiene en claro para simplificar el alcance de esta iteración.
-- =====================================================================
CREATE TABLE clientes (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento    ENUM('DNI', 'RUC') NOT NULL,
    numero_documento  VARCHAR(20)   NOT NULL UNIQUE,
    nombre_completo   VARCHAR(150)  NOT NULL,
    telefono          VARCHAR(20),
    correo            VARCHAR(150),
    direccion         VARCHAR(500),  -- cifrado en Java con AES-256-GCM (ver comentario del módulo)
    estado            ENUM('ACTIVO', 'INACTIVO') NOT NULL DEFAULT 'ACTIVO',
    puntos_fidelizacion INT         NOT NULL DEFAULT 0,
    fecha_creacion    DATETIME      NOT NULL
) ENGINE = InnoDB;

-- Caché de resultados de la consulta externa de RUC/DNI (apisperu.com), para no repetir
-- llamadas al mismo número y cuidar la cuota gratuita del servicio de terceros. Solo guarda
-- datos ya públicos (nombre/razón social, dirección de RUC), nunca el token de la API.
CREATE TABLE cache_consulta_documento (
    numero_documento     VARCHAR(20) PRIMARY KEY,
    tipo_documento       ENUM('DNI', 'RUC') NOT NULL,
    nombre_o_razon_social VARCHAR(255) NOT NULL,
    direccion            VARCHAR(500) NULL,
    fecha_consulta       DATETIME     NOT NULL
) ENGINE = InnoDB;

-- Caché del tipo de cambio USD del día (apisperu.com, fuente SUNAT), para no consultar la API
-- en cada venta pagada en dólares — se refresca automáticamente al cambiar la fecha.
CREATE TABLE cache_tipo_cambio (
    fecha           DATE PRIMARY KEY,
    compra          DECIMAL(10,4) NOT NULL,
    venta           DECIMAL(10,4) NOT NULL,
    fecha_consulta  DATETIME      NOT NULL
) ENGINE = InnoDB;

-- =====================================================================
-- MÓDULO: Punto de Venta (POS) / Historial de Ventas y Comprobantes
-- El registro y la anulación de una venta son transacciones ACID controladas
-- por VentaService: cabecera + detalle + stock + Kárdex se confirman o
-- revierten como una sola unidad (connection.setAutoCommit(false)).
-- =====================================================================
CREATE TABLE ventas (
    id                    INT AUTO_INCREMENT PRIMARY KEY,
    numero_comprobante    VARCHAR(20)   NULL UNIQUE,
    tipo_comprobante      ENUM('BOLETA', 'FACTURA') NOT NULL,
    cliente_id            INT           NULL,
    usuario_id            INT           NOT NULL,
    terminal_id           VARCHAR(50),
    subtotal_imponible    DECIMAL(10,2) NOT NULL,
    descuento             DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    igv                   DECIMAL(10,2) NOT NULL,
    total                 DECIMAL(10,2) NOT NULL,
    metodo_pago           ENUM('EFECTIVO', 'TARJETA', 'YAPE_PLIN', 'TRANSFERENCIA') NOT NULL,
    -- Pago en dólares (solo tiene sentido con metodo_pago = EFECTIVO): el total/subtotal/igv
    -- de arriba SIEMPRE quedan en soles (fuente de verdad contable); estas 3 columnas solo
    -- registran en qué moneda pagó físicamente el cliente, para el ticket y la conciliación
    -- de caja, nunca para recalcular el total de la venta.
    moneda_pago           ENUM('PEN', 'USD') NOT NULL DEFAULT 'PEN',
    tipo_cambio_aplicado  DECIMAL(10,4) NULL,
    monto_pagado_usd      DECIMAL(10,2) NULL,
    estado                ENUM('COMPLETADA', 'ANULADA') NOT NULL DEFAULT 'COMPLETADA',
    motivo_anulacion      VARCHAR(300)  NULL,
    usuario_anulacion_id  INT           NULL,
    fecha_anulacion       DATETIME      NULL,
    fecha_creacion        DATETIME      NOT NULL,
    CONSTRAINT fk_venta_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_venta_usuario_anulacion FOREIGN KEY (usuario_anulacion_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE TABLE detalle_venta (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    venta_id         INT           NOT NULL,
    producto_id      INT           NOT NULL,
    cantidad         INT           NOT NULL,
    precio_unitario  DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_detalle_venta_venta FOREIGN KEY (venta_id) REFERENCES ventas (id),
    CONSTRAINT fk_detalle_venta_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
) ENGINE = InnoDB;

CREATE INDEX idx_ventas_estado ON ventas (estado);
CREATE INDEX idx_ventas_usuario ON ventas (usuario_id);
CREATE INDEX idx_ventas_fecha ON ventas (fecha_creacion);
-- Índice compuesto: VentaDAO.listar(filtroEstado, ...) filtra por estado y ordena por
-- fecha_creacion DESC; también sustenta la vista vista_ventas_hoy (estado + rango de fecha).
CREATE INDEX idx_ventas_estado_fecha ON ventas (estado, fecha_creacion);
CREATE INDEX idx_detalle_venta_venta ON detalle_venta (venta_id);

-- =====================================================================
-- MÓDULO: Auditoría del Sistema y Control de Seguridad
-- Tabla de solo inserción: evidencia histórica inmutable de acciones sensibles.
-- =====================================================================
CREATE TABLE auditoria (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id     INT           NULL,
    accion         VARCHAR(60)   NOT NULL,
    entidad        VARCHAR(40)   NOT NULL,
    entidad_id     INT           NULL,
    detalle        VARCHAR(500)  NOT NULL,
    direccion_ip   VARCHAR(45),
    fecha          DATETIME      NOT NULL,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_auditoria_accion ON auditoria (accion);
CREATE INDEX idx_auditoria_fecha ON auditoria (fecha);

-- =====================================================================
-- MÓDULO: Configuración del Sistema (Parámetros Generales)
-- =====================================================================
CREATE TABLE configuracion (
    clave   VARCHAR(60)  PRIMARY KEY,
    valor   VARCHAR(255) NOT NULL
) ENGINE = InnoDB;

-- Archivos binarios de configuración (ej. logo de la tienda). Tabla separada
-- de "configuracion" porque su columna "valor" es VARCHAR(255) y no puede
-- alojar un BLOB sin romper el patrón clave-valor textual genérico.
CREATE TABLE archivos_configuracion (
    clave                VARCHAR(60)   PRIMARY KEY,
    contenido            LONGBLOB      NOT NULL,
    content_type         VARCHAR(100)  NOT NULL,
    nombre_original      VARCHAR(255)  NULL,
    fecha_actualizacion  DATETIME      NOT NULL
) ENGINE = InnoDB;

-- =====================================================================
-- MÓDULO: Turno de Caja (apertura, cierre y arqueo)
-- =====================================================================
CREATE TABLE turnos_caja (
    id                       INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id               INT           NOT NULL,
    terminal_id              VARCHAR(50),
    monto_inicial            DECIMAL(10,2) NOT NULL,
    monto_efectivo_calculado DECIMAL(10,2) NULL,
    monto_declarado          DECIMAL(10,2) NULL,
    diferencia               DECIMAL(10,2) NULL,
    estado                   ENUM('ABIERTO', 'CERRADO') NOT NULL DEFAULT 'ABIERTO',
    fecha_apertura           DATETIME      NOT NULL,
    fecha_cierre             DATETIME      NULL,
    CONSTRAINT fk_turno_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_turnos_caja_usuario ON turnos_caja (usuario_id);
CREATE INDEX idx_turnos_caja_estado ON turnos_caja (estado);
-- Índice compuesto: TurnoCajaDAO.buscarTurnoAbierto() filtra exactamente por
-- (usuario_id, estado) y se ejecuta en cada carga del POS (exige turno abierto).
CREATE INDEX idx_turnos_caja_usuario_estado ON turnos_caja (usuario_id, estado);

INSERT INTO configuracion (clave, valor) VALUES
    ('nombre_empresa', 'BodegaControl E.I.R.L.'),
    ('ruc_empresa', '20123456789'),
    ('moneda', 'S/'),
    ('tasa_igv', '18'),
    ('stock_minimo_defecto', '10'),
    ('max_intentos_login', '3'),
    ('minutos_bloqueo_login', '15'),
    ('timeout_sesion_admin_minutos', '15'),
    ('timeout_sesion_vendedor_minutos', '30'),
    ('dias_rotacion_minima', '15'),
    ('dias_alerta_vencimiento', '15');

-- =====================================================================
-- MÓDULO: Respaldos de Base de Datos (bitácora)
-- Cada ejecución del job de respaldo (automático diario o manual desde el
-- panel de administración) inserta una fila aquí. Tabla de solo inserción,
-- igual que auditoria/movimientos_inventario: es evidencia histórica.
-- Ver com.bodega.service.BackupService / com.bodega.scheduler.BackupScheduler.
--
-- POLÍTICA DE RETENCIÓN (documentada aquí porque no hay un lugar de
-- configuración de infraestructura fuera de este script):
--   - Frecuencia: 1 respaldo automático diario (madrugada, hora configurable
--     en database.properties -> db.backup.hora).
--   - Destino: carpeta local configurable (db.backup.dir), por defecto
--     "<home del usuario>/bodega_backups/". En un entorno cloud real este
--     destino debe apuntar a almacenamiento externo (S3, bucket, etc.), no
--     al mismo disco del servidor de aplicación.
--   - Retención: se conservan los últimos 30 días de respaldos en disco
--     (db.backup.retencion.dias); BackupService borra los .sql más antiguos
--     que ese umbral en cada ejecución, pero NUNCA borra las filas de esta
--     bitácora (se conserva el historial completo aunque el archivo físico
--     ya haya sido eliminado por retención).
-- =====================================================================
CREATE TABLE bitacora_respaldos (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    tipo              ENUM('AUTOMATICO', 'MANUAL') NOT NULL,
    estado            ENUM('EXITOSO', 'FALLIDO') NOT NULL,
    fecha_inicio      DATETIME      NOT NULL,
    fecha_fin         DATETIME      NULL,
    tamano_bytes      BIGINT        NULL,
    ruta_destino      VARCHAR(500)  NOT NULL,
    mensaje_error     VARCHAR(500)  NULL,
    usuario_id        INT           NULL,  -- NULL si lo disparó el scheduler automático
    CONSTRAINT fk_bitacora_respaldo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX idx_bitacora_respaldos_fecha ON bitacora_respaldos (fecha_inicio);
CREATE INDEX idx_bitacora_respaldos_estado ON bitacora_respaldos (estado);

-- =====================================================================
-- VISTAS: reportes recurrentes
-- Se crean como vistas (en vez de repetir el WHERE en cada DAO) para que
-- la regla de negocio de cada reporte viva en un solo lugar. Los DAO de
-- Java (ProductoDAO, DashboardDAO, ReporteDAO) consultan estas vistas
-- directamente con "SELECT ... FROM vista_xxx".
-- =====================================================================

-- Ventas completadas del día en curso (usada por DashboardDAO.totalVentasHoy/
-- contarVentasHoy). CURDATE() se reevalúa en cada consulta a la vista.
CREATE OR REPLACE VIEW vista_ventas_hoy AS
SELECT v.id, v.numero_comprobante, v.tipo_comprobante, v.cliente_id, v.usuario_id,
       v.terminal_id, v.metodo_pago, v.total, v.fecha_creacion
FROM ventas v
WHERE v.estado = 'COMPLETADA'
  AND v.fecha_creacion >= CURDATE()
  AND v.fecha_creacion < CURDATE() + INTERVAL 1 DAY;

-- Productos activos con stock igual o menor a su mínimo configurado (usada
-- por ProductoDAO.listarConStockBajoMinimo(), reemplaza el WHERE que antes
-- vivía duplicado en el DAO).
CREATE OR REPLACE VIEW vista_productos_stock_bajo AS
SELECT p.*
FROM productos p
WHERE p.stock_actual <= p.stock_minimo
  AND p.estado = 'ACTIVO'
ORDER BY p.stock_actual ASC;

-- Ranking de productos más vendidos por categoría (ventas COMPLETADA,
-- histórico completo). Usa función de ventana (MySQL 8+) para calcular la
-- posición de cada producto dentro de su categoría sin una subconsulta
-- aparte. Consumida por ReporteDAO.topProductosPorCategoria(topN), que
-- filtra "WHERE ranking_categoria <= ?".
CREATE OR REPLACE VIEW vista_top_productos_categoria AS
SELECT
    c.id                              AS categoria_id,
    c.nombre                          AS categoria_nombre,
    p.id                              AS producto_id,
    p.nombre                          AS producto_nombre,
    SUM(dv.cantidad)                  AS cantidad_vendida,
    SUM(dv.cantidad * dv.precio_unitario) AS total_vendido,
    RANK() OVER (
        PARTITION BY c.id
        ORDER BY SUM(dv.cantidad) DESC
    )                                 AS ranking_categoria
FROM detalle_venta dv
JOIN ventas v      ON v.id = dv.venta_id
JOIN productos p   ON p.id = dv.producto_id
JOIN categorias c  ON c.id = p.categoria_id
WHERE v.estado = 'COMPLETADA'
GROUP BY c.id, c.nombre, p.id, p.nombre;

-- =====================================================================
-- SEGURIDAD: usuarios y privilegios mínimos a nivel de motor MySQL
-- Reemplaza el uso de "root" desde la aplicación (visto en database.properties
-- de versiones anteriores) por 3 cuentas separadas por función, cada una con
-- el mínimo privilegio que necesita para su tarea (principio de menor
-- privilegio). Host restringido a 'localhost' porque la aplicación y el
-- servidor MySQL corren en la misma máquina; en un despliegue cloud real
-- esto debe ajustarse a la red interna del servidor de aplicación, nunca '%'.
--
-- Este script NUNCA debe llevar contraseñas reales: se versiona en git (y este
-- proyecto ya vive en un repositorio de GitHub), así que cualquier valor real
-- escrito aquí queda expuesto en el historial. Reemplaza los placeholders
-- CAMBIAR_CONTRASENA_* de abajo por contraseñas reales SOLO al ejecutar este
-- script a mano contra el servidor (o bórralas de tu copia local antes de
-- hacer commit); nunca las dejes así en el archivo que subes a git.
-- =====================================================================

-- 1) Usuario de APLICACIÓN (el que usa HikariCP en com.bodega.config.DatabaseConfig).
--    Solo CRUD de datos. Nunca DDL (CREATE/ALTER/DROP/INDEX), nunca GRANT.
CREATE USER IF NOT EXISTS 'bodega_app'@'localhost' IDENTIFIED BY 'CAMBIAR_CONTRASENA_APP';
GRANT SELECT, INSERT, UPDATE, DELETE ON bodega_db.* TO 'bodega_app'@'localhost';

-- 2) Usuario ADMINISTRADOR / de MIGRACIONES (DDL). Se usa solo desde
--    herramientas administrativas (ej. al aplicar este propio script,
--    o un cliente SQL de un DBA) — la aplicación web NUNCA se conecta con él.
CREATE USER IF NOT EXISTS 'bodega_dba'@'localhost' IDENTIFIED BY 'CAMBIAR_CONTRASENA_DBA';
GRANT ALL PRIVILEGES ON bodega_db.* TO 'bodega_dba'@'localhost';

-- 3) Usuario de RESPALDOS (usado únicamente por com.bodega.service.BackupService
--    para invocar mysqldump). Privilegio mínimo para poder volcar la base de
--    forma consistente: SELECT (leer datos), LOCK TABLES (mysqldump con
--    --single-transaction/--lock-tables), SHOW VIEW (para las 3 vistas de
--    arriba) y EVENT/TRIGGER (para que el dump incluya objetos programables
--    si se agregan más adelante). Sin INSERT/UPDATE/DELETE: este usuario
--    jamás debería poder modificar datos.
CREATE USER IF NOT EXISTS 'bodega_backup'@'localhost' IDENTIFIED BY 'CAMBIAR_CONTRASENA_BACKUP';
GRANT SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER ON bodega_db.* TO 'bodega_backup'@'localhost';

FLUSH PRIVILEGES;
