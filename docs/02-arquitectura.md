# 02 · Arquitectura

Aplicación web monolítica **MVC por capas** con Spring Boot. Las vistas se renderizan en
el servidor con Thymeleaf; no hay API REST pública ni build de frontend.

```
Navegador ──HTTP──▶ Controller ──▶ Service ──▶ Repository ──▶ PostgreSQL
                       │              │
                       │              └──▶ port/ClienteClima ──▶ OpenWeatherMap
                       └──▶ Thymeleaf (templates/) + static/ (CSS, JS)
```

## Capas y responsabilidades

| Paquete | Responsabilidad | Regla |
|---|---|---|
| `controller/` | Recibir la petición, validar el formulario, aplicar permisos (`@PreAuthorize`), elegir la vista | Sin lógica de negocio ni consultas directas |
| `service/` | Reglas de negocio y transacciones (`@Transactional`) | No conoce HTTP ni vistas |
| `repository/` | Acceso a datos con Spring Data JPA | Solo consultas; nada de reglas |
| `model/entity/` | 13 entidades JPA (incluye `Auditoria` y `AlertaNodo`). `cultivo_dispositivo` se mapea como `@JoinTable` dentro de `Cultivo`; `usuario_dispositivo` no tiene entidad | Deben coincidir exactamente con `database/esquema.sql` |
| `model/` | `RangoFisico`: límites físicos por variable compartidos entre telemetría (A1/A2) y umbrales (B1) | Sin dependencias de infraestructura |
| `model/dto/` | Formularios (`CultivoForm`, `UmbralForm`, `RegistroRequest`), telemetría (`TelemetriaRequest`, `TelemetriaResponse`) y datos de vista (`DashboardResumen`, `LecturaPunto`, `ResumenVariable`, `ClimaActual`) | Validación con Bean Validation / dominio |
| `security/` | `AppUserDetailsService` (login, estado y bloqueo), `EventosAutenticacionListener` (bloqueo y auditoría), `TokenNodoService` / `TokenNodoAuthenticationFilter` (API de nodos) y `PermisosCultivo` (bean `@permisos`) | Toda autorización por cultivo, cuenta y nodo pasa por aquí |
| `exception/` | `RecursoNoEncontradoException` (404) y `ReglaNegocioException` (400) | Los servicios lanzan estas, no `RuntimeException` genéricas |
| `port/` | Interfaz `ClienteClima` | El negocio depende de la interfaz, no de OpenWeatherMap |
| `integration/clima/` | `OpenWeatherCliente`, implementación del puerto | Si falla o no hay API key, responde vacío |
| `config/` | `SecurityConfig`, `DataSeeder` (catálogos y admin demo), `NotificacionesAdvice` (contador de notificaciones en todas las vistas) | — |
| `util/excel/` | `CultivoExcelExporter`, `HistoricoExcelExporter` (Apache POI) | Devuelven `byte[]`; el controlador arma la descarga |

## Servicios

| Servicio | RF | Qué hace |
|---|---|---|
| `UsuarioService` | RF-01, C1 | Registro con BCrypt, política de contraseñas (mín. 8 caracteres, letras y números) y correo único |
| `AuditoriaService` | C1 | Registro transversal de acciones críticas en la tabla `auditoria` para todos los frentes |
| `CultivoService` | RF-02 | CRUD de cultivos; registrar y asignar nodos; el creador queda como administrador; al eliminar, borra los nodos que se quedan sin ningún cultivo |
| `NodoService` | A2 | Estado de nodos (`En línea`/`Sin conexión`/`Inactivo`), activación/desactivación, regeneración de token (auditado en C1) y monitor periódico de alertas `nodo_sin_conexion` |
| `TelemetriaService` | A1, A2 | Ingesta autenticada de lecturas en `POST /api/telemetria`, validación física (`RangoFisico`), lotes mixtos, actualización de `ultima_conexion` y evaluación vía `AlertasService` |
| `UmbralService` | RF-03, B1 | Un umbral por cultivo y variable (si existe, se actualiza); valida mínimo ≤ máximo y rangos físicos (`RangoFisico`) |
| `DashboardService` | RF-04, B1 | Resumen por cultivo con la última lectura de cada sensor (consulta `LIMIT 1`) y episodios de alerta abiertos |
| `AlertasService` | RF-05, B1 | Motor de alertas por episodio (`abierta`/`cerrada`) y severidad (`leve`/`moderada`/`critica`); agrupa lecturas consecutivas fuera de rango sin re-notificar y cierra al volver al rango |
| `ClimaService` | RF-06 | Clima por ubicación con caché Guava (30 min por defecto) |
| `AccesoService` | RF-07 | Asigna, cambia o quita roles por cultivo; nunca deja un cultivo sin administrador |
| `HistoricoService` | RF-08 | Lecturas por rango de fechas (cada extremo es opcional), puntos de la gráfica, resumen mín/máx/promedio |
| `SimuladorService` | Demo | Genera una lectura por sensor activo y la pasa por el motor de alertas |

## Flujo de una lectura

1. Llega una lectura (hoy, desde **Simular lectura**; en producción, desde el ESP32).
2. `SimuladorService` la guarda en `lecturas` y llama a `AlertasService.evaluar(lectura)`.
3. `AlertasService` busca los umbrales del nodo en una **caché Guava** (5 minutos) para no
   consultar la BD en cada lectura.
4. Si el valor está fuera de `[valor_minimo, valor_maximo]`:
   - Si ya existe un episodio abierto para el mismo cultivo, variable y dirección (`bajo`/`alto`),
     se actualiza `valor_extremo`, `cantidad_lecturas` y `severidad` (por porcentaje de desviación
     y duración en `agro.alertas.severidad.*`) **sin volver a notificar**.
   - Si no existe, abre un nuevo episodio en `alertas` y crea una fila en `notificacion_usuario`
     por cada usuario con acceso al cultivo.
   - Cuando la variable regresa dentro del rango, el episodio abierto se cierra (`estado = 'cerrada'`,
     `fecha_cierre`).
5. `NotificacionesAdvice` expone el número de **episodios abiertos** del usuario a todas las vistas
   (contador del menú lateral).

La caché de umbrales se invalida al guardar o eliminar un umbral, al asignar un nodo y al
eliminar un cultivo. Si se agrega otro punto que cambie umbrales o asignaciones, **debe**
invalidar la caché (`AlertasService.invalidarCacheUmbrales`).

## Auditoría (`AuditoriaService`)

Disponible para todos los frentes (**A2**, **B2**, **C2** y **D2**) mediante inyección por
constructor de `AuditoriaService`:

```java
// Cuando ya se tiene la entidad Usuario (puede ser null si el correo no existe):
auditoriaService.registrar(usuario, "NODO_TOKEN_REGENERADO", "dispositivo", idDispositivo,
        "Token regenerado para nodo #" + idDispositivo);

// Cuando solo se dispone del correo del usuario autenticado (Authentication.getName()):
auditoriaService.registrarPorCorreo(correo, "ALERTA_ATENDIDA", "alerta", idAlerta,
        "Atendida con revisión de riego");
```

**Convenciones de uso:**
- `accion`: código en `MAYUSCULAS_CON_GUION_BAJO` (máx. 80 caracteres). Ejemplos:
  - **C1 (autenticación):** `LOGIN_EXITOSO`, `LOGIN_FALLIDO`, `CUENTA_BLOQUEADA`.
  - **A2 (telemetría):** `NODO_ACTIVADO`, `NODO_DESACTIVADO`, `NODO_TOKEN_REGENERADO`.
  - **B2 (alertas/umbrales):** `ALERTA_ATENDIDA`, `PLANTILLA_UMBRAL_APLICADA`.
  - **C2 (cuentas/cultivos):** `ACCESO_ASIGNADO`, `ACCESO_REVOCADO`, `CULTIVO_ELIMINADO`, `UMBRAL_ACTUALIZADO`.
  - **D2 (reportes):** `REPORTE_EXPORTADO`.
- `entidad`: nombre de la entidad afectada en minúscula singular (`usuario`, `cultivo`, `dispositivo`, `umbral`, `alerta`).
- `idEntidad`: identificador numérico de la entidad afectada (opcional).
- `detalle`: texto breve y legible (máx. 500 caracteres). **Nunca** incluir contraseñas, hashes ni tokens.

## Decisiones técnicas

- **`ddl-auto=validate`:** el esquema lo gobierna `database/esquema.sql`. Hibernate solo
  valida al arrancar; si una entidad no coincide con la tabla, la app no arranca. Así se
  evita que un cambio en una entidad altere la BD sin control.
- **`open-in-view=false`:** las consultas terminan dentro del servicio. Las relaciones que
  usan las vistas están en `FetchType.EAGER` o se cargan en el servicio.
- **Borrados en cascada en la BD (`ON DELETE CASCADE`)**, no en JPA. Al eliminar un cultivo
  se van sus umbrales, accesos y asignaciones de nodos; los nodos exclusivos de ese cultivo
  se borran con una consulta nativa antes de borrar el cultivo (ver `DispositivoIoTRepository`).
- **Permisos por recurso** con `@PreAuthorize("@permisos...")` en cada método de
  controlador, no por URL. Ver [05-seguridad-y-permisos.md](05-seguridad-y-permisos.md).
- **Errores tipados:** los servicios lanzan `ReglaNegocioException` (el controlador la
  muestra en el formulario) o `RecursoNoEncontradoException` (404). Nunca se muestran trazas
  al usuario (`server.error.include-stacktrace=never`).
- **Puerto de clima:** `ClienteClima` permite cambiar de proveedor o usar un simulado en
  pruebas sin tocar el negocio.
- **Frontend sin build:** un único `app.css` con variables de diseño, un `app.js` pequeño
  (menú móvil, confirmaciones) y Chart.js servido localmente.

## Convenciones de código

- Idioma del dominio: **español** (clases, métodos, variables, mensajes, vistas).
- Inyección de dependencias por constructor; campos `private final`.
- Un controlador por área funcional; rutas con id numérico validado (`{id:\\d+}`).
- Toda acción que modifica datos es `POST` con token CSRF (Thymeleaf lo añade solo con
  `th:action`).
- Logs con SLF4J (`log.info`, `log.warn`); nunca `System.out.println`.
- Javadoc en clases y métodos públicos de servicios explicando el *por qué*, no el *qué*.
