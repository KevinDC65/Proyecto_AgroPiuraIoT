-- =====================================================================
-- AgroPiura IoT - Esquema completo de la base de datos (PostgreSQL 15+)
--
-- Crea las 13 tablas, secuencias, claves primarias y foráneas, y los
-- índices/restricciones de database/indices.sql. Pensado para una BD vacía:
--
--   createdb -h localhost -U postgres agropiura
--   psql -h localhost -U postgres -d agropiura -f database/esquema.sql
--
-- Los catálogos (roles, tipo_sensor) y el administrador de demostración
-- los siembra la aplicación al arrancar (config/DataSeeder), no este script.
-- Hibernate solo valida el esquema (ddl-auto=validate): cualquier cambio de
-- tablas se hace aquí y en las entidades a la vez.
-- =====================================================================

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: alertas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.alertas (
    id_alerta integer NOT NULL,
    id_lectura integer NOT NULL,
    id_umbral integer NOT NULL,
    mensaje character varying(255),
    fecha_alerta timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    atendida boolean DEFAULT false NOT NULL
);


--
-- Name: alertas_id_alerta_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.alertas_id_alerta_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: alertas_id_alerta_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.alertas_id_alerta_seq OWNED BY public.alertas.id_alerta;


--
-- Name: cultivo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cultivo (
    id_cultivo integer NOT NULL,
    nombre character varying(150) NOT NULL,
    tipo_cultivo character varying(100) NOT NULL,
    ubicacion character varying(200),
    latitud numeric(9,6),
    longitud numeric(9,6),
    fecha_creacion date DEFAULT CURRENT_DATE NOT NULL,
    area_ha numeric(6,2)
);


--
-- Name: cultivo_dispositivo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.cultivo_dispositivo (
    id_cultivo_dispositivo integer NOT NULL,
    id_cultivo integer NOT NULL,
    id_dispositivo integer NOT NULL,
    fecha_asignacion date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: cultivo_dispositivo_id_cultivo_dispositivo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cultivo_dispositivo_id_cultivo_dispositivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cultivo_dispositivo_id_cultivo_dispositivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cultivo_dispositivo_id_cultivo_dispositivo_seq OWNED BY public.cultivo_dispositivo.id_cultivo_dispositivo;


--
-- Name: cultivo_id_cultivo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.cultivo_id_cultivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: cultivo_id_cultivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.cultivo_id_cultivo_seq OWNED BY public.cultivo.id_cultivo;


--
-- Name: dispositivo_iot; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dispositivo_iot (
    id_dispositivo integer NOT NULL,
    estado character varying(50) DEFAULT 'activo'::character varying NOT NULL,
    fecha_instalacion date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: dispositivo_iot_id_dispositivo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dispositivo_iot_id_dispositivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dispositivo_iot_id_dispositivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.dispositivo_iot_id_dispositivo_seq OWNED BY public.dispositivo_iot.id_dispositivo;


--
-- Name: lecturas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.lecturas (
    id_lectura integer NOT NULL,
    id_sensor integer NOT NULL,
    valor numeric(10,2) NOT NULL,
    fecha_hora timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: lecturas_id_lectura_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.lecturas_id_lectura_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: lecturas_id_lectura_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.lecturas_id_lectura_seq OWNED BY public.lecturas.id_lectura;


--
-- Name: notificacion_usuario; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificacion_usuario (
    id_notificacion integer NOT NULL,
    id_alerta integer NOT NULL,
    id_usuario integer NOT NULL,
    leido boolean DEFAULT false NOT NULL,
    fecha_lectura timestamp without time zone
);


--
-- Name: notificacion_usuario_id_notificacion_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.notificacion_usuario_id_notificacion_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: notificacion_usuario_id_notificacion_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.notificacion_usuario_id_notificacion_seq OWNED BY public.notificacion_usuario.id_notificacion;


--
-- Name: roles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.roles (
    id_rol integer NOT NULL,
    nombre_rol character varying(100) NOT NULL
);


--
-- Name: roles_id_rol_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.roles_id_rol_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: roles_id_rol_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.roles_id_rol_seq OWNED BY public.roles.id_rol;


--
-- Name: sensores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.sensores (
    id_sensor integer NOT NULL,
    id_dispositivo integer NOT NULL,
    id_tipo_sensor integer NOT NULL
);


--
-- Name: sensores_id_sensor_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.sensores_id_sensor_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: sensores_id_sensor_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.sensores_id_sensor_seq OWNED BY public.sensores.id_sensor;


--
-- Name: tipo_sensor; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipo_sensor (
    id_tipo_sensor integer NOT NULL,
    nombre_tipo character varying(100) NOT NULL,
    unidad_medida character varying(50) NOT NULL
);


--
-- Name: tipo_sensor_id_tipo_sensor_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.tipo_sensor_id_tipo_sensor_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: tipo_sensor_id_tipo_sensor_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.tipo_sensor_id_tipo_sensor_seq OWNED BY public.tipo_sensor.id_tipo_sensor;


--
-- Name: umbrales; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.umbrales (
    id_umbral integer NOT NULL,
    id_cultivo integer NOT NULL,
    id_tipo_sensor integer NOT NULL,
    valor_minimo numeric(10,2),
    valor_maximo numeric(10,2)
);


--
-- Name: umbrales_id_umbral_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.umbrales_id_umbral_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: umbrales_id_umbral_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.umbrales_id_umbral_seq OWNED BY public.umbrales.id_umbral;


--
-- Name: usuario_cultivo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_cultivo (
    id_acceso integer NOT NULL,
    id_usuario integer NOT NULL,
    id_cultivo integer NOT NULL,
    id_rol integer NOT NULL,
    fecha_asignada date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: usuario_cultivo_id_acceso_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuario_cultivo_id_acceso_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuario_cultivo_id_acceso_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuario_cultivo_id_acceso_seq OWNED BY public.usuario_cultivo.id_acceso;


--
-- Name: usuario_dispositivo; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuario_dispositivo (
    id_usuario_dispositivo integer NOT NULL,
    id_usuario integer NOT NULL,
    id_dispositivo integer NOT NULL,
    fecha_vinculacion date DEFAULT CURRENT_DATE NOT NULL,
    estado character varying(50) DEFAULT 'activo'::character varying NOT NULL
);


--
-- Name: usuario_dispositivo_id_usuario_dispositivo_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuario_dispositivo_id_usuario_dispositivo_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuario_dispositivo_id_usuario_dispositivo_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuario_dispositivo_id_usuario_dispositivo_seq OWNED BY public.usuario_dispositivo.id_usuario_dispositivo;


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id_usuario integer NOT NULL,
    nombre character varying(150) NOT NULL,
    correo character varying(150) NOT NULL,
    contrasena character varying(255) NOT NULL,
    estado character varying(50) DEFAULT 'activo'::character varying NOT NULL,
    fecha_registro date DEFAULT CURRENT_DATE NOT NULL
);


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.usuarios_id_usuario_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.usuarios_id_usuario_seq OWNED BY public.usuarios.id_usuario;


--
-- Name: alertas id_alerta; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas ALTER COLUMN id_alerta SET DEFAULT nextval('public.alertas_id_alerta_seq'::regclass);


--
-- Name: cultivo id_cultivo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo ALTER COLUMN id_cultivo SET DEFAULT nextval('public.cultivo_id_cultivo_seq'::regclass);


--
-- Name: cultivo_dispositivo id_cultivo_dispositivo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo_dispositivo ALTER COLUMN id_cultivo_dispositivo SET DEFAULT nextval('public.cultivo_dispositivo_id_cultivo_dispositivo_seq'::regclass);


--
-- Name: dispositivo_iot id_dispositivo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispositivo_iot ALTER COLUMN id_dispositivo SET DEFAULT nextval('public.dispositivo_iot_id_dispositivo_seq'::regclass);


--
-- Name: lecturas id_lectura; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturas ALTER COLUMN id_lectura SET DEFAULT nextval('public.lecturas_id_lectura_seq'::regclass);


--
-- Name: notificacion_usuario id_notificacion; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacion_usuario ALTER COLUMN id_notificacion SET DEFAULT nextval('public.notificacion_usuario_id_notificacion_seq'::regclass);


--
-- Name: roles id_rol; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles ALTER COLUMN id_rol SET DEFAULT nextval('public.roles_id_rol_seq'::regclass);


--
-- Name: sensores id_sensor; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensores ALTER COLUMN id_sensor SET DEFAULT nextval('public.sensores_id_sensor_seq'::regclass);


--
-- Name: tipo_sensor id_tipo_sensor; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_sensor ALTER COLUMN id_tipo_sensor SET DEFAULT nextval('public.tipo_sensor_id_tipo_sensor_seq'::regclass);


--
-- Name: umbrales id_umbral; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umbrales ALTER COLUMN id_umbral SET DEFAULT nextval('public.umbrales_id_umbral_seq'::regclass);


--
-- Name: usuario_cultivo id_acceso; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_cultivo ALTER COLUMN id_acceso SET DEFAULT nextval('public.usuario_cultivo_id_acceso_seq'::regclass);


--
-- Name: usuario_dispositivo id_usuario_dispositivo; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_dispositivo ALTER COLUMN id_usuario_dispositivo SET DEFAULT nextval('public.usuario_dispositivo_id_usuario_dispositivo_seq'::regclass);


--
-- Name: usuarios id_usuario; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id_usuario SET DEFAULT nextval('public.usuarios_id_usuario_seq'::regclass);


--
-- Name: alertas alertas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas
    ADD CONSTRAINT alertas_pkey PRIMARY KEY (id_alerta);


--
-- Name: cultivo_dispositivo cultivo_dispositivo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo_dispositivo
    ADD CONSTRAINT cultivo_dispositivo_pkey PRIMARY KEY (id_cultivo_dispositivo);


--
-- Name: cultivo cultivo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo
    ADD CONSTRAINT cultivo_pkey PRIMARY KEY (id_cultivo);


--
-- Name: dispositivo_iot dispositivo_iot_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dispositivo_iot
    ADD CONSTRAINT dispositivo_iot_pkey PRIMARY KEY (id_dispositivo);


--
-- Name: lecturas lecturas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturas
    ADD CONSTRAINT lecturas_pkey PRIMARY KEY (id_lectura);


--
-- Name: notificacion_usuario notificacion_usuario_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacion_usuario
    ADD CONSTRAINT notificacion_usuario_pkey PRIMARY KEY (id_notificacion);


--
-- Name: roles roles_nombre_rol_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_nombre_rol_key UNIQUE (nombre_rol);


--
-- Name: roles roles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY (id_rol);


--
-- Name: sensores sensores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensores
    ADD CONSTRAINT sensores_pkey PRIMARY KEY (id_sensor);


--
-- Name: tipo_sensor tipo_sensor_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipo_sensor
    ADD CONSTRAINT tipo_sensor_pkey PRIMARY KEY (id_tipo_sensor);


--
-- Name: umbrales umbrales_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umbrales
    ADD CONSTRAINT umbrales_pkey PRIMARY KEY (id_umbral);


--
-- Name: usuario_cultivo usuario_cultivo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_cultivo
    ADD CONSTRAINT usuario_cultivo_pkey PRIMARY KEY (id_acceso);


--
-- Name: usuario_dispositivo usuario_dispositivo_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_dispositivo
    ADD CONSTRAINT usuario_dispositivo_pkey PRIMARY KEY (id_usuario_dispositivo);


--
-- Name: usuarios usuarios_correo_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_correo_key UNIQUE (correo);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);


--
-- Name: idx_alertas_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alertas_fecha ON public.alertas USING btree (fecha_alerta DESC);


--
-- Name: idx_alertas_lectura; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alertas_lectura ON public.alertas USING btree (id_lectura);


--
-- Name: idx_alertas_umbral_atendida; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_alertas_umbral_atendida ON public.alertas USING btree (id_umbral, atendida);


--
-- Name: idx_cultivo_dispositivo_dispositivo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_cultivo_dispositivo_dispositivo ON public.cultivo_dispositivo USING btree (id_dispositivo);


--
-- Name: idx_lecturas_sensor_fecha; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_lecturas_sensor_fecha ON public.lecturas USING btree (id_sensor, fecha_hora DESC);


--
-- Name: idx_notificacion_usuario_alerta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notificacion_usuario_alerta ON public.notificacion_usuario USING btree (id_alerta);


--
-- Name: idx_notificacion_usuario_no_leidas; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_notificacion_usuario_no_leidas ON public.notificacion_usuario USING btree (id_usuario) WHERE (leido = false);


--
-- Name: idx_sensores_dispositivo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sensores_dispositivo ON public.sensores USING btree (id_dispositivo);


--
-- Name: idx_sensores_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_sensores_tipo ON public.sensores USING btree (id_tipo_sensor);


--
-- Name: idx_umbrales_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_umbrales_tipo ON public.umbrales USING btree (id_tipo_sensor);


--
-- Name: idx_usuario_cultivo_cultivo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_cultivo_cultivo ON public.usuario_cultivo USING btree (id_cultivo);


--
-- Name: idx_usuario_cultivo_rol; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_cultivo_rol ON public.usuario_cultivo USING btree (id_rol);


--
-- Name: idx_usuario_dispositivo_dispositivo; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_dispositivo_dispositivo ON public.usuario_dispositivo USING btree (id_dispositivo);


--
-- Name: idx_usuario_dispositivo_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_usuario_dispositivo_usuario ON public.usuario_dispositivo USING btree (id_usuario);


--
-- Name: uq_cultivo_dispositivo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_cultivo_dispositivo ON public.cultivo_dispositivo USING btree (id_cultivo, id_dispositivo);


--
-- Name: uq_umbrales_cultivo_tipo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_umbrales_cultivo_tipo ON public.umbrales USING btree (id_cultivo, id_tipo_sensor);


--
-- Name: uq_usuario_cultivo; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_usuario_cultivo ON public.usuario_cultivo USING btree (id_usuario, id_cultivo);


--
-- Name: alertas alertas_id_lectura_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas
    ADD CONSTRAINT alertas_id_lectura_fkey FOREIGN KEY (id_lectura) REFERENCES public.lecturas(id_lectura) ON DELETE CASCADE;


--
-- Name: alertas alertas_id_umbral_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.alertas
    ADD CONSTRAINT alertas_id_umbral_fkey FOREIGN KEY (id_umbral) REFERENCES public.umbrales(id_umbral) ON DELETE CASCADE;


--
-- Name: cultivo_dispositivo cultivo_dispositivo_id_cultivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo_dispositivo
    ADD CONSTRAINT cultivo_dispositivo_id_cultivo_fkey FOREIGN KEY (id_cultivo) REFERENCES public.cultivo(id_cultivo) ON DELETE CASCADE;


--
-- Name: cultivo_dispositivo cultivo_dispositivo_id_dispositivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.cultivo_dispositivo
    ADD CONSTRAINT cultivo_dispositivo_id_dispositivo_fkey FOREIGN KEY (id_dispositivo) REFERENCES public.dispositivo_iot(id_dispositivo) ON DELETE CASCADE;


--
-- Name: lecturas lecturas_id_sensor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.lecturas
    ADD CONSTRAINT lecturas_id_sensor_fkey FOREIGN KEY (id_sensor) REFERENCES public.sensores(id_sensor) ON DELETE CASCADE;


--
-- Name: notificacion_usuario notificacion_usuario_id_alerta_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacion_usuario
    ADD CONSTRAINT notificacion_usuario_id_alerta_fkey FOREIGN KEY (id_alerta) REFERENCES public.alertas(id_alerta) ON DELETE CASCADE;


--
-- Name: notificacion_usuario notificacion_usuario_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacion_usuario
    ADD CONSTRAINT notificacion_usuario_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: sensores sensores_id_dispositivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensores
    ADD CONSTRAINT sensores_id_dispositivo_fkey FOREIGN KEY (id_dispositivo) REFERENCES public.dispositivo_iot(id_dispositivo) ON DELETE CASCADE;


--
-- Name: sensores sensores_id_tipo_sensor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensores
    ADD CONSTRAINT sensores_id_tipo_sensor_fkey FOREIGN KEY (id_tipo_sensor) REFERENCES public.tipo_sensor(id_tipo_sensor) ON DELETE CASCADE;


--
-- Name: umbrales umbrales_id_cultivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umbrales
    ADD CONSTRAINT umbrales_id_cultivo_fkey FOREIGN KEY (id_cultivo) REFERENCES public.cultivo(id_cultivo) ON DELETE CASCADE;


--
-- Name: umbrales umbrales_id_tipo_sensor_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.umbrales
    ADD CONSTRAINT umbrales_id_tipo_sensor_fkey FOREIGN KEY (id_tipo_sensor) REFERENCES public.tipo_sensor(id_tipo_sensor) ON DELETE CASCADE;


--
-- Name: usuario_cultivo usuario_cultivo_id_cultivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_cultivo
    ADD CONSTRAINT usuario_cultivo_id_cultivo_fkey FOREIGN KEY (id_cultivo) REFERENCES public.cultivo(id_cultivo) ON DELETE CASCADE;


--
-- Name: usuario_cultivo usuario_cultivo_id_rol_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_cultivo
    ADD CONSTRAINT usuario_cultivo_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES public.roles(id_rol) ON DELETE RESTRICT;


--
-- Name: usuario_cultivo usuario_cultivo_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_cultivo
    ADD CONSTRAINT usuario_cultivo_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: usuario_dispositivo usuario_dispositivo_id_dispositivo_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_dispositivo
    ADD CONSTRAINT usuario_dispositivo_id_dispositivo_fkey FOREIGN KEY (id_dispositivo) REFERENCES public.dispositivo_iot(id_dispositivo) ON DELETE CASCADE;


--
-- Name: usuario_dispositivo usuario_dispositivo_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuario_dispositivo
    ADD CONSTRAINT usuario_dispositivo_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;
