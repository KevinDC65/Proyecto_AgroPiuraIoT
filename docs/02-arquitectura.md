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
| `model/entity/` | 11 entidades JPA. `cultivo_dispositivo` se mapea como `@JoinTable` dentro de `Cultivo`; `usuario_dispositivo` no tiene entidad | Deben coincidir exactamente con `database/esquema.sql` |
| `model/dto/` | Formularios (`CultivoForm`, `UmbralForm`, `RegistroRequest`) y datos de vista (`DashboardResumen`, `LecturaPunto`, `ResumenVariable`, `ClimaActual`) | Records inmutables; validación con Bean Validation |
| `security/` | `AppUserDetailsService` (login) y `PermisosCultivo` (bean `@permisos`) | Toda autorización por cultivo pasa por aquí |
| `exception/` | `RecursoNoEncontradoException` (404) y `ReglaNegocioException` (400) | Los servicios lanzan estas, no `RuntimeException` genéricas |
| `port/` | Interfaz `ClienteClima` | El negocio depende de la interfaz, no de OpenWeatherMap |
| `integration/clima/` | `OpenWeatherCliente`, implementación del puerto | Si falla o no hay API key, responde vacío |
| `config/` | `SecurityConfig`, `DataSeeder` (catálogos y admin demo), `NotificacionesAdvice` (contador de notificaciones en todas las vistas) | — |
| `util/excel/` | `CultivoExcelExporter`, `HistoricoExcelExporter` (Apache POI) | Devuelven `byte[]`; el controlador arma la descarga |

## Servicios

| Servicio | RF | Qué hace |
|---|---|---|
| `UsuarioService` | RF-01 | Registro con BCrypt; correo único |
| `CultivoService` | RF-02 | CRUD de cultivos; registrar y asignar nodos; el creador queda como administrador; al eliminar, borra los nodos que se quedan sin ningún cultivo |
| `UmbralService` | RF-03 | Un umbral por cultivo y variable (si existe, se actualiza); valida mínimo ≤ máximo |
| `DashboardService` | RF-04 | Resumen por cultivo con la última lectura de cada sensor (consulta `LIMIT 1`) |
| `AlertasService` | RF-05 | Motor de alertas: compara la lectura con el umbral, crea la alerta y una notificación por miembro del cultivo |
| `ClimaService` | RF-06 | Clima por ubicación con caché Guava (30 min por defecto) |
| `AccesoService` | RF-07 | Asigna, cambia o quita roles por cultivo; nunca deja un cultivo sin administrador |
| `HistoricoService` | RF-08 | Lecturas por rango de fechas (cada extremo es opcional), puntos de la gráfica, resumen mín/máx/promedio |
| `SimuladorService` | Demo | Genera una lectura por sensor activo y la pasa por el motor de alertas |

## Flujo de una lectura

1. Llega una lectura (hoy, desde **Simular lectura**; en producción, desde el ESP32).
2. `SimuladorService` la guarda en `lecturas` y llama a `AlertasService.evaluar(lectura)`.
3. `AlertasService` busca los umbrales del nodo en una **caché Guava** (5 minutos) para no
   consultar la BD en cada lectura.
4. Si el valor está fuera de `[valor_minimo, valor_maximo]`, crea una fila en `alertas` y
   una en `notificacion_usuario` por cada usuario con acceso al cultivo.
5. `NotificacionesAdvice` expone el número de notificaciones no leídas a todas las vistas
   (contador del menú lateral).

La caché de umbrales se invalida al guardar o eliminar un umbral, al asignar un nodo y al
eliminar un cultivo. Si se agrega otro punto que cambie umbrales o asignaciones, **debe**
invalidar la caché (`AlertasService.invalidarCacheUmbrales`).

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
