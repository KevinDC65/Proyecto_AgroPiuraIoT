-- =====================================================================
-- AgroPiura IoT - Migración de datos creados antes de los permisos por
-- cultivo (versión de noviembre de 2026).
--
-- Antes cualquier usuario veía todos los cultivos. Ahora cada usuario solo
-- ve los cultivos donde tiene un acceso en usuario_cultivo, así que:
--   1. Los cultivos sin ningún administrador quedan invisibles para todos.
--      Este script nombra administrador de esos cultivos al usuario indicado.
--   2. Los nodos creados antes no tenían sensores y "Simular lectura" no
--      generaba nada. Este script les crea un sensor por cada tipo.
--
-- Uso (indicar el correo del usuario que se quedará con esos cultivos):
--   psql -h localhost -U postgres -d agropiura -v correo=tu@correo.com -f database/migracion-datos-anteriores.sql
--
-- Idempotente: se puede ejecutar varias veces. No borra datos; solo inserta
-- lo que falta (y sube a administrador al usuario indicado si ya era
-- colaborador de un cultivo sin administrador). Todo ocurre en una
-- transacción: si el correo no existe, no se aplica nada.
-- =====================================================================

\set ON_ERROR_STOP on

\if :{?correo}
\else
    \echo 'Falta el correo: agrega  -v correo=tu@correo.com'
    \quit
\endif

BEGIN;

SELECT set_config('migracion.correo', :'correo', true);

-- El correo debe existir; si no, se aborta la transacción.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE correo = current_setting('migracion.correo')) THEN
        RAISE EXCEPTION 'No existe el usuario con correo %', current_setting('migracion.correo');
    END IF;
END $$;

-- Cultivos sin ningún administrador (se calculan una sola vez).
CREATE TEMP TABLE cultivos_sin_admin ON COMMIT DROP AS
SELECT c.id_cultivo
FROM cultivo c
WHERE NOT EXISTS (
    SELECT 1 FROM usuario_cultivo uc
    JOIN roles r ON r.id_rol = uc.id_rol
    WHERE uc.id_cultivo = c.id_cultivo AND r.nombre_rol = 'administrador');

-- 1a. Si el usuario ya era colaborador de alguno, pasa a administrador.
UPDATE usuario_cultivo uc
SET id_rol = (SELECT id_rol FROM roles WHERE nombre_rol = 'administrador')
FROM usuarios u
WHERE uc.id_usuario = u.id_usuario
  AND u.correo = current_setting('migracion.correo')
  AND uc.id_cultivo IN (SELECT id_cultivo FROM cultivos_sin_admin);

-- 1b. En el resto, se le da acceso como administrador.
INSERT INTO usuario_cultivo (id_usuario, id_cultivo, id_rol)
SELECT u.id_usuario, s.id_cultivo, r.id_rol
FROM cultivos_sin_admin s
CROSS JOIN usuarios u
CROSS JOIN roles r
WHERE u.correo = current_setting('migracion.correo')
  AND r.nombre_rol = 'administrador'
  AND NOT EXISTS (
      SELECT 1 FROM usuario_cultivo uc
      WHERE uc.id_cultivo = s.id_cultivo AND uc.id_usuario = u.id_usuario);

-- 2. Nodos sin sensores -> un sensor por cada tipo del catálogo.
INSERT INTO sensores (id_dispositivo, id_tipo_sensor)
SELECT d.id_dispositivo, t.id_tipo_sensor
FROM dispositivo_iot d
CROSS JOIN tipo_sensor t
WHERE NOT EXISTS (SELECT 1 FROM sensores s WHERE s.id_dispositivo = d.id_dispositivo);

COMMIT;

-- Resultado: administrador de cada cultivo y sensores por nodo.
SELECT c.id_cultivo, c.nombre, u.correo AS administrador
FROM cultivo c
LEFT JOIN usuario_cultivo uc ON uc.id_cultivo = c.id_cultivo
     AND uc.id_rol = (SELECT id_rol FROM roles WHERE nombre_rol = 'administrador')
LEFT JOIN usuarios u ON u.id_usuario = uc.id_usuario
ORDER BY c.id_cultivo;

SELECT d.id_dispositivo, count(s.id_sensor) AS sensores
FROM dispositivo_iot d
LEFT JOIN sensores s ON s.id_dispositivo = d.id_dispositivo
GROUP BY d.id_dispositivo
ORDER BY d.id_dispositivo;
