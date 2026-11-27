# 04 · Base de datos

PostgreSQL, base `agropiura`, esquema `public`, **15 tablas**.

## Scripts

| Script | Cuándo usarlo |
|---|---|
| `database/esquema.sql` | BD **nueva y vacía**. Crea tablas, secuencias, claves, relaciones con sus reglas de borrado e índices. Es la referencia oficial del esquema |
| `database/indices.sql` | BD **ya existente** creada con el DDL anterior del informe. Añade los índices y restricciones de unicidad. Se puede ejecutar varias veces (`IF NOT EXISTS`) |
| `database/migracion-datos-anteriores.sql` | BD **con datos creados antes de los permisos por cultivo**. Nombra administrador al usuario indicado en los cultivos que no tienen ninguno (si no, nadie los ve) y crea los sensores de los nodos que no tienen. Uso: `psql ... -v correo=tu@correo.com -f database/migracion-datos-anteriores.sql`. Se puede ejecutar varias veces; si el correo no existe no aplica nada |
| `database/migraciones/2026-11-24_C1_reglas_cuenta_auditoria.sql` | BD **existente** (C1). Añade `intentos_fallidos` y `bloqueado_hasta` en `usuarios`, crea la tabla `auditoria` y sus índices. Idempotente (`IF NOT EXISTS`) |
| `database/migraciones/2026-11-24_B1_alertas_episodios_severidad.sql` | BD **existente** (B1). Añade `estado`, `severidad`, `direccion`, `valor_extremo`, `cantidad_lecturas`, `fecha_cierre` e índice `idx_alertas_umbral_estado` en `alertas`, rellenando filas previas. Idempotente |
| `database/migraciones/2026-11-24_A2_api_telemetria_y_nodos.sql` | BD **existente** (A1/A2). Añade `token_hash` y `ultima_conexion` en `dispositivo_iot`, crea la tabla `alerta_nodo` y sus índices. Idempotente (`IF NOT EXISTS`) |

Al arrancar, la aplicación avisa en el log si hay cultivos sin administrador
(`N cultivo(s) sin administrador, invisibles para todos los usuarios`).

Los catálogos y el administrador de demostración los inserta la aplicación al arrancar
(`config/DataSeeder`) solo si las tablas están vacías:

- `roles`: `administrador`, `colaborador`
- `tipo_sensor`: `humedad_suelo` (%), `temperatura_ambiente` (°C), `humedad_ambiente` (%), `ph_suelo` (pH)
- usuario `admin@agropiura.com`, administrador del cultivo "Parcela Demo - Piura"

**Hibernate no crea ni modifica tablas** (`ddl-auto=validate`). Para cambiar el esquema:

1. Escribir el `ALTER TABLE` (o `CREATE`) y probarlo en una copia local.
2. Actualizar `database/esquema.sql` para que una instalación nueva quede igual.
3. Si afecta a BD existentes, añadir el script en `database/` con nombre descriptivo y
   documentarlo aquí.
4. Ajustar la entidad en `model/entity/` y correr `.\mvnw.cmd test`.

## Tablas

| Tabla | Qué guarda | Columnas clave |
|---|---|---|
| `usuarios` | Cuentas | `correo` (único), `contrasena` (hash BCrypt), `estado`, `fecha_registro`, `intentos_fallidos`, `bloqueado_hasta` |
| `auditoria` | Trazabilidad de eventos críticos | `id`, `id_usuario` (`ON DELETE SET NULL`), `accion`, `entidad`, `id_entidad`, `detalle`, `fecha` |
| `roles` | Catálogo de roles por cultivo | `nombre_rol` (único) |
| `usuario_cultivo` | Qué usuario accede a qué cultivo y con qué rol | `id_usuario`, `id_cultivo`, `id_rol` (único por usuario y cultivo) |
| `cultivo` | Parcelas | `nombre`, `tipo_cultivo`, `ubicacion`, `latitud`, `longitud`, `area_ha` |
| `dispositivo_iot` | Nodos ESP32 | `estado` (`activo`/`inactivo`), `fecha_instalacion`, `token_hash` (BCrypt), `ultima_conexion` |
| `cultivo_dispositivo` | Asignación de nodos a cultivos (muchos a muchos) | `id_cultivo`, `id_dispositivo` (único por par) |
| `usuario_dispositivo` | Vinculación de usuarios a nodos. Viene del modelo del informe; hoy no tiene entidad JPA ni se usa | `id_usuario`, `id_dispositivo`, `estado` |
| `tipo_sensor` | Catálogo de variables medidas | `nombre_tipo`, `unidad_medida` |
| `sensores` | Sensor de un tipo dentro de un nodo | `id_dispositivo`, `id_tipo_sensor` |
| `lecturas` | Mediciones | `id_sensor`, `valor`, `fecha_hora` |
| `umbrales` | Rango aceptable por cultivo y variable | `id_cultivo`, `id_tipo_sensor` (único por par), `valor_minimo`, `valor_maximo` |
| `alertas` | Episodios fuera de rango | `id_lectura`, `id_umbral`, `mensaje`, `fecha_alerta`, `atendida`, `estado`, `severidad`, `direccion`, `valor_extremo`, `cantidad_lecturas`, `fecha_cierre` |
| `alerta_nodo` | Episodios de desconexión de nodos IoT | `id_dispositivo`, `id_cultivo`, `tipo` (`nodo_sin_conexion`), `estado` (`abierta`/`cerrada`), `severidad`, `mensaje`, `fecha_alerta`, `fecha_cierre`, `atendida` |
| `notificacion_usuario` | Aviso de una alerta para cada miembro del cultivo | `id_alerta`, `id_usuario`, `leido`, `fecha_lectura` |

## Relaciones

```
usuarios ─┬─< usuario_cultivo >─── cultivo ─┬─< umbrales >── tipo_sensor
          │          │                      │        │            │
          │        roles                    │        └──< alertas │
          │                                 │              │      │
          └─< notificacion_usuario >────────┼──────────────┘      │
                                            │                     │
                                 cultivo_dispositivo              │
                                            │                     │
                                     dispositivo_iot ─< sensores >┘
                                                          │
                                                       lecturas ─< alertas
```

## Reglas de borrado

Todas las claves foráneas usan `ON DELETE CASCADE`, salvo `usuario_cultivo.id_rol`
(`RESTRICT`: no se puede borrar un rol en uso).

| Al borrar… | Se borra también |
|---|---|
| un **usuario** | sus accesos y notificaciones |
| un **cultivo** | sus umbrales (y sus alertas), accesos y asignaciones de nodos. La aplicación además borra los nodos que solo estaban en ese cultivo |
| un **nodo** | sus sensores, sus lecturas y las alertas de esas lecturas |
| una **lectura** | sus alertas y las notificaciones de esas alertas |

## Índices

PostgreSQL no indexa automáticamente las claves foráneas. `indices.sql` (y `esquema.sql`)
crean 17 índices, entre ellos:

- `idx_lecturas_sensor_fecha (id_sensor, fecha_hora DESC)`: última lectura por sensor
  (dashboard) e histórico por fechas.
- `idx_alertas_umbral_atendida`, `idx_notificacion_usuario_no_leidas`: contadores del menú
  y lista de alertas.
- Restricciones únicas `uq_umbrales_cultivo_tipo`, `uq_usuario_cultivo`,
  `uq_cultivo_dispositivo`: evitan duplicados aunque falle la validación de la aplicación.

Antes de aplicar `indices.sql` en una BD con datos, ejecutar las consultas de verificación
de duplicados que están al final del script. En tablas grandes, crear los índices con
`CREATE INDEX CONCURRENTLY` fuera de horario punta (sentencia por sentencia).

## Copias de seguridad

```powershell
pg_dump -h localhost -U postgres -d agropiura -F c -f agropiura.backup     # respaldo
pg_restore -h localhost -U postgres -d agropiura --clean agropiura.backup  # restaurar
```

Los respaldos no se suben al repositorio.
