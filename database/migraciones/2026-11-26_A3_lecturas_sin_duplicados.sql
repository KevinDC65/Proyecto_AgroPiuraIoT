-- A3 · Robustez IoT (#11)
-- Una lectura queda identificada por su sensor y su fecha y hora: si el nodo
-- reenvía el mismo lote (por ejemplo tras un timeout), no se duplica.
-- Reemplaza el índice idx_lecturas_sensor_fecha por uno ÚNICO con las mismas
-- columnas (sirve igual para "última lectura por sensor").
-- Idempotente: se puede ejecutar más de una vez.
--
-- Uso:
--   psql -h localhost -U postgres -d agropiura -f database/migraciones/2026-11-26_A3_lecturas_sin_duplicados.sql

DO $$
DECLARE
    repetidas integer;
BEGIN
    SELECT count(*) INTO repetidas
    FROM (SELECT 1 FROM public.lecturas
          GROUP BY id_sensor, fecha_hora
          HAVING count(*) > 1) d;
    IF repetidas > 0 THEN
        RAISE EXCEPTION 'Hay % combinaciones (sensor, fecha_hora) repetidas en lecturas. '
            'Revísalas con la consulta de docs/04-base-de-datos.md (sección Migraciones) antes de aplicar este script.',
            repetidas;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lecturas_sensor_fecha
    ON public.lecturas USING btree (id_sensor, fecha_hora DESC);

DROP INDEX IF EXISTS public.idx_lecturas_sensor_fecha;
