-- A1 · Base de la telemetría IoT (#1)
-- Token por nodo (solo el hash BCrypt) y fecha de la última conexión.
-- Idempotente: se puede ejecutar más de una vez sin error.
--
-- Uso:
--   psql -h localhost -U postgres -d agropiura -f database/migraciones/2026-11-25_A1_token_y_conexion_nodo.sql

ALTER TABLE public.dispositivo_iot
    ADD COLUMN IF NOT EXISTS token_hash character varying(100);

ALTER TABLE public.dispositivo_iot
    ADD COLUMN IF NOT EXISTS ultima_conexion timestamp without time zone;

COMMENT ON COLUMN public.dispositivo_iot.token_hash IS
    'Hash BCrypt del token del nodo. El token en texto plano solo se muestra una vez al registrarlo.';
COMMENT ON COLUMN public.dispositivo_iot.ultima_conexion IS
    'Fecha y hora de la última vez que el nodo envió lecturas a /api/telemetria.';
