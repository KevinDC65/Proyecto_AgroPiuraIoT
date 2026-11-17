# 08 · Registro de cambios

Historial de lo que se ha construido, ordenado del más reciente al más antiguo. Cada
entrada explica **qué** cambió y **por qué**, para que cualquier integrante pueda entender
el estado actual sin leer todo el código.

---

## 17/11/2026 · Integración de los cambios del 12/11/2026

Se integraron los commits `71e0ef2` (ajustes de interfaz) y `e555019` (API de telemetría y
pruebas) sobre la revisión integral descrita más abajo. Donde ambos cambiaban lo mismo se
conservó la versión revisada; lo compatible se incorporó.

**Incorporado:**
- API de telemetría: `TelemetriaController` (`/api/telemetria`), `TelemetriaService`, DTOs
  `TelemetriaRequest`, `LecturaItemRequest`, `TelemetriaResponse` y consultas nuevas en
  `SensorRepository`. Queda **cerrada** (`denyAll` en `SecurityConfig`) hasta autenticar cada
  nodo con su token: abierta, cualquiera podía escribir lecturas en cultivos ajenos. Es el
  punto de partida de los issues A1 y A2.
- Usuarios demo `colaborador@agropiura.com` y `usuario@agropiura.com`, creados solo en modo
  demo (ver [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md)).
- Validaciones de entrada en `UsuarioService` y `AppUserDetailsService`; `Rol.esColaborador`.
- Pruebas nuevas: `ViewControllerTest`, `CultivoControllerTest`, `SecurityControllerTest`,
  `UmbralControllerTest`, `AppUserDetailsServiceTest`, `TelemetriaControllerTest` y casos
  nuevos en `UsuarioServiceTest` y `DataSeederTest`. Se adaptaron al modelo de permisos por
  cultivo (los cultivos de prueba se crean con administrador). Total: 120 pruebas, 108
  activas y 12 de telemetría desactivadas hasta el issue A2.
- Mensaje "Cultivo registrado" al crear un cultivo (las demás acciones ya lo tenían).

**No incorporado (se mantuvo la versión revisada):**
- `ddl-auto=update`: el esquema lo sigue gobernando `database/esquema.sql` (`validate`).
- Pruebas sobre H2 en memoria: siguen contra PostgreSQL con el esquema real, porque los
  borrados en cascada y las restricciones viven en la BD.
- CSRF desactivado en `/api/**` y rutas `/api/**` públicas.
- Borrado manual de datos hijos al eliminar un cultivo (lo hace la BD con `ON DELETE CASCADE`).
- Validaciones de cultivo con `IllegalArgumentException` en el servicio (terminaban en error
  500; las cubre la validación del formulario).
- "Desasignar nodo" y el contador de nodos activos del dashboard: dependían de partes de su
  versión no integradas. Pueden retomarse dentro del issue A2.
- Ajustes de estilos sobre el diseño anterior: los reemplaza el rediseño.

## 17/11/2026 · Revisión integral, seguridad, rediseño de interfaz y documentación

### Seguridad y permisos (backend)

**Problema:** bastaba con registrarse para ver, editar o borrar cualquier cultivo, cambiar
umbrales, atender alertas y exportar los datos de todos.

**Cambios:**
- Nuevo `security/PermisosCultivo` (bean `@permisos`) con `puedeVer`, `puedeAdministrar` y
  `puedeVerAlerta`, aplicado con `@PreAuthorize` en todos los controladores.
- El rol pasa a depender de cada cultivo (`usuario_cultivo`): el colaborador consulta,
  simula y atiende alertas; solo el administrador del cultivo edita, borra, gestiona nodos,
  umbrales y accesos.
- Quien crea un cultivo queda como su administrador (antes quedaba sin dueño).
- Las listas (cultivos, dashboard, mapa, alertas, exportaciones) muestran solo los
  cultivos del usuario.
- Un cultivo ajeno o inexistente responde 403 sin revelar si existe.
- No se puede quitar ni degradar al último administrador de un cultivo.
- La clave del administrador demo se puede cambiar con `ADMIN_DEMO_CLAVE` o desactivar con
  `agro.demo.admin.habilitado=false`; el arranque advierte si se usa la clave pública.
- Las vistas ocultan los botones que el usuario no puede usar.

### Manejo de errores (backend)

**Problema:** un id inexistente, un umbral con mínimo mayor que máximo o un nodo inválido
terminaban en error 500.

**Cambios:**
- Excepciones de dominio `RecursoNoEncontradoException` (404) y `ReglaNegocioException`
  (400). Los formularios muestran el mensaje en lugar de fallar.
- Páginas `error/403.html`, `error/4xx.html` y `error/5xx.html`; nunca se muestran trazas.

### Funcionalidad corregida (backend)

- **Simular lectura** no generaba nada en cultivos creados desde la web porque los nodos
  nacían sin sensores. Ahora cada nodo nuevo tiene un sensor por cada tipo del catálogo; el
  simulador usa rangos plausibles cuando no hay umbral y avisa si no hay nodos.
- **Histórico:** se ignoraba el filtro si solo se indicaba "desde" o solo "hasta", y la
  exportación a Excel perdía el filtro. Corregido; un rango invertido muestra un mensaje.
- **Caché del motor de alertas:** ahora se invalida al asignar un nodo o eliminar un
  cultivo; antes podía usar umbrales desactualizados hasta 5 minutos.
- **Eliminar un cultivo dejaba nodos huérfanos** con sus sensores y lecturas. Ahora se
  eliminan los nodos que solo pertenecían a ese cultivo (consulta nativa
  `DispositivoIoTRepository.eliminarExclusivosDeCultivo`) y se conservan los compartidos
  con otros cultivos. Prueba: `eliminarBorraLosNodosQueQuedanSinCultivoYConservaLosCompartidos`.

### Rendimiento (backend y base de datos)

- El dashboard cargaba todas las lecturas de cada sensor tres veces por cultivo; ahora
  pide solo la última lectura de cada sensor (`LIMIT 1`).
- El histórico ya no hace una consulta extra por cada sensor.
- `database/indices.sql`: 17 índices y restricciones de unicidad; ninguna clave foránea
  tenía índice.

### Base de datos

- Nuevo `database/esquema.sql`: esquema completo para crear la BD desde cero. Antes el
  repositorio remitía a un `query.sql` del informe que no estaba versionado, así que no se
  podía instalar el proyecto solo con el repositorio. Verificado creando una BD vacía,
  arrancando la app sobre ella y comparando tablas e índices con la BD de desarrollo.
- **Datos anteriores a los permisos por cultivo:** los cultivos creados antes no tenían
  administrador en `usuario_cultivo`, así que con los permisos nuevos nadie los veía (por
  ejemplo, `k@gmail.com` entraba y no veía nada). Además, sus nodos no tenían sensores.
  Nuevo `database/migracion-datos-anteriores.sql`: asigna esos cultivos como administrador
  al correo indicado y crea los sensores que faltan; es idempotente y no aplica nada si el
  correo no existe. Aplicado en la BD de desarrollo con `k@gmail.com` (Mango - Parcela A y
  Papa).
- La app avisa en el log al arrancar si hay cultivos sin administrador
  (`CultivoRepository.findSinAdministrador`).
- Nuevo [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md) con las cuentas de prueba
  y sus contraseñas.

### Interfaz (frontend)

Rediseño completo con un sistema visual propio ("Chulucanas", ver
[06-diseno-de-interfaz.md](06-diseno-de-interfaz.md)):

- **General:** menú lateral con iconos SVG (en lugar de emojis), contador de alertas sin
  leer, usuario y botón de salir; en celular, barra superior con menú desplegable. Migas de
  pan y enlace "Saltar al contenido".
- **Dashboard:** indicadores en una franja (la de alertas se pone roja si hay pendientes),
  lista de cultivos con su banda de humedad, panel de clima.
- **Cultivos, formulario y detalle:** tablas que pasan a tarjetas en celular, errores bajo
  cada campo, confirmación antes de eliminar, enlace al histórico desde el detalle.
- **Umbrales, alertas, accesos e histórico:** nombres de variable legibles ("Humedad
  suelo" en lugar de `humedad_suelo`), números con formato, estados vacíos que explican qué
  hacer.
- **Histórico:** la gráfica iba de la lectura más reciente a la más antigua; ahora va en
  orden cronológico. Encabezados de variable corregidos para no saltar niveles.
- **Mapa:** marcadores y leyenda con los colores de estado; aviso si Leaflet no carga.
- **Portada, login, registro y 404:** portada con vista de ejemplo de la app; login y
  registro a dos columnas en escritorio; `autocomplete` en los campos; aviso al cerrar sesión.
- **Celular:** campos a 16px para evitar el zoom automático, sin destello al tocar, hover
  solo con mouse.
- **Accesibilidad:** jerarquía tipográfica reforzada (título de página 36px, título de
  panel 21px), contraste AA en los textos de ejemplo de los campos, estados con forma y
  texto además de color.
- Nuevos recursos: `static/js/app.js`, `static/img/favicon.svg`,
  `templates/components/icons.html`.

### Pruebas

- De 34 a **56 pruebas**. Nueva `AutorizacionWebTest` (13 pruebas con las plantillas
  reales) y casos nuevos en servicios.
- Dependencia de pruebas `spring-boot-starter-security-test`.

### Limpieza

- Eliminados 6 métodos de repositorio sin uso: `AlertaRepository.findAllByOrderByFechaAlertaDesc`,
  `AlertaRepository.countByAtendidaFalse`, `CultivoRepository.findByNombreContainingIgnoreCase`,
  `DispositivoIoTRepository.findByEstadoIgnoreCase`,
  `LecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc`,
  `NotificacionUsuarioRepository.findByUsuarioIdUsuarioAndLeidoFalse`.
- Eliminados archivos locales sobrantes (`HELP.md` de la plantilla de Spring Initializr y
  carpetas temporales de herramientas de prueba). `.gitignore` excluye ahora las carpetas
  de configuración local de editores y herramientas.
- Documentación reorganizada en `docs/` (antes había un único README en la raíz).

### Entorno

- El proyecto requiere **JDK 25** (`JAVA_HOME`); ver
  [01-instalacion-y-ejecucion.md](01-instalacion-y-ejecucion.md).

---

## 27/10/2026 · Mapa, simulador y resumen histórico

- Mapa de cultivos con Leaflet y OpenStreetMap (latitud y longitud en el cultivo).
- Simulador de lecturas para demostración.
- Alertas marcables como atendidas.
- Resumen por variable en el histórico (mínimo, máximo, promedio).

## 25/10/2026 · Requerimientos RF-03 a RF-08

- **RF-03:** umbrales por cultivo y variable, sin duplicar (TDD).
- **RF-05:** motor de alertas con notificaciones a los usuarios del cultivo y vista de
  alertas con "marcar como leída".
- **RF-08:** histórico por cultivo y rango de fechas, gráfica temporal y exportación a Excel.
- **RF-06:** clima de OpenWeatherMap con caché Guava; si falla o no hay clave, el panel se
  oculta sin errores. Clave por variable de entorno `OPENWEATHER_API_KEY`.
- **RF-07:** accesos por cultivo con roles administrador y colaborador.
- Librerías Guava (caché de umbrales), Commons Lang3 y configuración de Logback.
- Administrador y cultivo de demostración sembrados al arrancar.
- Correcciones: fragmentos de layout mal referenciados, enlace de exportación del
  histórico, carga de Chart.js local, ruta `/weather` del cliente de clima.

## 22/10/2026 · Organización y configuración segura

- Layout aislado en `templates/components/`.
- `application.properties.example` para no subir configuraciones; datos sensibles
  retirados del repositorio.
- Página 404 personalizada.

## 13–15/10/2026 · Base del proyecto

- Proyecto Spring Boot creado desde Initializr; conexión a PostgreSQL con `ddl-auto=validate`.
- Entidades JPA de todas las tablas del modelo.
- Registro e inicio de sesión (RF-01), CRUD de cultivos con exportación a Excel (RF-02),
  dashboard con el resumen del día (RF-04).
- Primer diseño basado en los mockups del equipo.
