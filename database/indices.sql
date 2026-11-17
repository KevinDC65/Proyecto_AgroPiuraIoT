-- =====================================================================
-- AgroPiura IoT - Índices y restricciones de integridad complementarios
-- para bases de datos creadas con el DDL anterior del informe. Una BD nueva
-- se crea con database/esquema.sql, que ya los incluye.
--
-- Idempotente: se puede ejecutar varias veces.
--   psql -h localhost -U postgres -d agropiura -f database/indices.sql
--
-- PRODUCCIÓN: ejecutar fuera de horario punta. Para tablas grandes, cambiar
-- CREATE INDEX por CREATE INDEX CONCURRENTLY (no admite transacción, ejecutar
-- sentencia por sentencia). Antes de los UNIQUE, verificar que no existan
-- duplicados (consultas al final del archivo).
-- =====================================================================

-- PostgreSQL no indexa automáticamente las claves foráneas: sin estos
-- índices, los JOIN y los ON DELETE CASCADE recorren la tabla completa.

-- Lecturas: última lectura por sensor (dashboard) e histórico por fechas.
CREATE INDEX IF NOT EXISTS idx_lecturas_sensor_fecha
    ON lecturas (id_sensor, fecha_hora DESC);

CREATE INDEX IF NOT EXISTS idx_sensores_dispositivo
    ON sensores (id_dispositivo);

CREATE INDEX IF NOT EXISTS idx_sensores_tipo
    ON sensores (id_tipo_sensor);

-- Relación cultivo <-> dispositivo (sin duplicados).
CREATE UNIQUE INDEX IF NOT EXISTS uq_cultivo_dispositivo
    ON cultivo_dispositivo (id_cultivo, id_dispositivo);

CREATE INDEX IF NOT EXISTS idx_cultivo_dispositivo_dispositivo
    ON cultivo_dispositivo (id_dispositivo);

-- Un umbral por cultivo y variable (el servicio hace upsert sobre este par).
CREATE UNIQUE INDEX IF NOT EXISTS uq_umbrales_cultivo_tipo
    ON umbrales (id_cultivo, id_tipo_sensor);

CREATE INDEX IF NOT EXISTS idx_umbrales_tipo
    ON umbrales (id_tipo_sensor);

-- Alertas: listado por fecha, conteo de activas y cascadas.
CREATE INDEX IF NOT EXISTS idx_alertas_umbral_atendida
    ON alertas (id_umbral, atendida);

CREATE INDEX IF NOT EXISTS idx_alertas_lectura
    ON alertas (id_lectura);

CREATE INDEX IF NOT EXISTS idx_alertas_fecha
    ON alertas (fecha_alerta DESC);

-- Notificaciones: contador de no leídas del usuario (se consulta en cada página).
CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_no_leidas
    ON notificacion_usuario (id_usuario)
    WHERE leido = false;

CREATE INDEX IF NOT EXISTS idx_notificacion_usuario_alerta
    ON notificacion_usuario (id_alerta);

-- Accesos (RF-07): un rol por usuario y cultivo; se consulta en cada autorización.
CREATE UNIQUE INDEX IF NOT EXISTS uq_usuario_cultivo
    ON usuario_cultivo (id_usuario, id_cultivo);

CREATE INDEX IF NOT EXISTS idx_usuario_cultivo_cultivo
    ON usuario_cultivo (id_cultivo);

CREATE INDEX IF NOT EXISTS idx_usuario_cultivo_rol
    ON usuario_cultivo (id_rol);

CREATE INDEX IF NOT EXISTS idx_usuario_dispositivo_usuario
    ON usuario_dispositivo (id_usuario);

CREATE INDEX IF NOT EXISTS idx_usuario_dispositivo_dispositivo
    ON usuario_dispositivo (id_dispositivo);

-- Verificación previa de duplicados (deben devolver 0 filas):
-- SELECT id_usuario, id_cultivo, count(*) FROM usuario_cultivo GROUP BY 1, 2 HAVING count(*) > 1;
-- SELECT id_cultivo, id_tipo_sensor, count(*) FROM umbrales GROUP BY 1, 2 HAVING count(*) > 1;
-- SELECT id_cultivo, id_dispositivo, count(*) FROM cultivo_dispositivo GROUP BY 1, 2 HAVING count(*) > 1;
