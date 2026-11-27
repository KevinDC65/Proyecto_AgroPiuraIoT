-- =====================================================================
-- Migración B1 · Alertas por episodio y severidad
--
-- Añade estado del episodio (abierta/cerrada), severidad (leve/moderada/critica),
-- dirección (bajo/alto), valor_extremo, cantidad_lecturas y fecha_cierre en alertas.
--
-- Idempotente: se puede ejecutar varias veces sin error.
--   psql -h localhost -U postgres -d agropiura -f database/migraciones/2026-11-24_B1_alertas_episodios_severidad.sql
-- =====================================================================

ALTER TABLE public.alertas
    ADD COLUMN IF NOT EXISTS estado character varying(20) NOT NULL DEFAULT 'abierta',
    ADD COLUMN IF NOT EXISTS severidad character varying(20) NOT NULL DEFAULT 'leve',
    ADD COLUMN IF NOT EXISTS direccion character varying(10) NOT NULL DEFAULT 'bajo',
    ADD COLUMN IF NOT EXISTS valor_extremo numeric(10,2),
    ADD COLUMN IF NOT EXISTS cantidad_lecturas integer NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS fecha_cierre timestamp without time zone;

-- Relleno seguro e idempotente para alertas creadas antes de B1.
UPDATE public.alertas a
   SET valor_extremo = l.valor,
       direccion = CASE
           WHEN u.valor_maximo IS NOT NULL AND l.valor > u.valor_maximo THEN 'alto'
           ELSE 'bajo'
       END,
       estado = CASE WHEN a.atendida THEN 'cerrada' ELSE 'abierta' END,
       fecha_cierre = CASE
           WHEN a.atendida AND a.fecha_cierre IS NULL THEN a.fecha_alerta
           ELSE a.fecha_cierre
       END
  FROM public.lecturas l, public.umbrales u
 WHERE a.id_lectura = l.id_lectura
   AND a.id_umbral = u.id_umbral
   AND a.valor_extremo IS NULL;

CREATE INDEX IF NOT EXISTS idx_alertas_umbral_estado
    ON public.alertas USING btree (id_umbral, estado);
