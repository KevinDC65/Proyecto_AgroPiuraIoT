# 11 · Plan de sprints para el cierre del proyecto

Plan para llevar AgroPiura IoT de su estado actual (RF-01 a RF-08 funcionando, lecturas
simuladas) a un producto **completo y defendible**: lecturas reales desde los nodos, reglas
de negocio agronómicas, cuentas seguras, reportes útiles para el agricultor y una entrega
desplegable y documentada.

El trabajo está dividido en **15 issues** en GitHub. Cada integrante toma **como máximo 3**.

---

## 1. Cómo elegir y trabajar los issues

### Organización

- **5 frentes × 3 sprints = 15 issues.** Cada issue es un paquete de trabajo completo
  (backend, base de datos, interfaz, pruebas y documentación de esa parte).
- Códigos: la letra es el frente y el número el sprint. `B2` = frente B, sprint 2.

| Frente | Sprint 1 · Fundamentos | Sprint 2 · Funcionalidad | Sprint 3 · Entrega |
|---|---|---|---|
| **A · Telemetría IoT** | [A1](#a1) Base de la telemetría | [A2](#a2) API de lecturas y estado de nodos | [A3](#a3) Robustez y documentación IoT |
| **B · Motor agronómico** | [B1](#b1) Alertas por episodio y severidad | [B2](#b2) Plantillas, recomendaciones y atención | [B3](#b3) Validación agronómica |
| **C · Cuentas y seguridad** | [C1](#c1) Reglas de cuenta y auditoría | [C2](#c2) Perfil, auditoría en uso y CSP | [C3](#c3) Cierre de seguridad |
| **D · Reportes e indicadores** | [D1](#d1) Histórico escalable y datos válidos | [D2](#d2) Indicadores de gestión y reportes | [D3](#d3) Verificación y rendimiento |
| **E · Calidad y entrega** | [E1](#e1) CI, producción y datos demo | [E2](#e2) Docker y revisiones cruzadas | [E3](#e3) Aceptación y presentación |

### Reglas para elegir

1. **Máximo 3 issues por persona.** Para tomar uno: asignártelo en GitHub (*Assignees*) y
   comentar "Lo tomo".
2. **Se recomienda tomar los 3 de un mismo frente** (por ejemplo A1, A2 y A3): cada sprint
   se apoya en lo que construiste en el anterior. Mezclar frentes está permitido si se
   respetan las dependencias.
3. **No empezar un issue del sprint 2 o 3 si su dependencia no está integrada en `main`**
   (ver "Depende de" en cada issue).
4. Los 5 issues de un sprint deben tener dueño antes de que empiece ese sprint.

### Flujo de trabajo de cada issue

1. Actualizar `main` y crear la rama indicada en el issue (`feat/A1-base-telemetria`, …).
2. Leer [09-reglas-del-proyecto.md](09-reglas-del-proyecto.md) (zonas protegidas),
   [02-arquitectura.md](02-arquitectura.md) y [05-seguridad-y-permisos.md](05-seguridad-y-permisos.md).
3. Trabajar los **pasos** del issue en orden. Cada paso con su prueba automática.
4. Commits pequeños con prefijo (`Feat:`, `Fix:`, `Test:`, `Docs:`) y el código del issue:
   `Feat: A1 token de autenticación por nodo`.
5. Pull request a `main` con `Closes #N`, la lista de criterios marcada y capturas si hay
   pantallas. Lo revisa **otro** integrante; con CI en verde se integra.
6. Actualizar `docs/` y añadir la entrada en [08-registro-de-cambios.md](08-registro-de-cambios.md).

### Cambios de base de datos

- Cada cambio va en un script nuevo `database/migraciones/AAAA-MM-DD_codigo_descripcion.sql`
  (por ejemplo `2026-11-24_A1_token_nodo.sql`), idempotente (`IF NOT EXISTS`).
- El mismo cambio se aplica en `database/esquema.sql` y en la entidad de `model/entity/`
  **en el mismo pull request**.
- `esquema.sql` lo tocan varios frentes: avisar en el grupo antes de integrarlo y, si hay
  conflicto, resolverlo conservando los cambios de ambos.

### Dependencias entre issues

```
Sprint 1          Sprint 2          Sprint 3
A1 ─────────────▶ A2 ─────────────▶ A3
 └──▶ B1 ───────▶ B2 ─────────────▶ B3
      (rangos)     │  (atención)
C1 ─────────────▶ C2 ─────────────▶ C3
 (auditoría) ───▶ A2, B2, D2 la usan
D1 ─────────────▶ D2 ◀── A2 (estado de nodos), B2 (atención de alertas)
                   └──────────────▶ D3
E1 ─────────────▶ E2 ─────────────▶ E3 (integra y valida todo)
```

---

## 2. Sprint 1 · Fundamentos y reglas de negocio (semanas 1–2)

**Meta:** el modelo de datos y las reglas de negocio de cada frente funcionando en el
backend, con pruebas. CI funcionando.

<a id="a1"></a>
### A1 · Base de la telemetría IoT

**Objetivo:** preparar todo lo necesario para que un nodo ESP32 real pueda enviar lecturas
de forma segura y con datos válidos.

**Por qué importa al negocio:** hoy las lecturas solo existen por el simulador. Sin nodos
autenticados, cualquiera podría enviar datos falsos; sin validación, un sensor dañado
generaría alertas falsas que el agricultor dejaría de creer.

**Pasos:**
1. **Contrato de la API.** Crear `docs/12-api-de-lecturas.md` con el JSON que envía el nodo
   (`idNodo`, lista de `{tipoSensor, valor}`, `fechaHora` opcional), las respuestas y los
   códigos (200, 400, 401, 404). Pedir revisión al dueño de B1 antes de seguir.
2. **Token por nodo.** Migración: columna `token_hash` en `dispositivo_iot`. Al registrar un
   nodo (`CultivoService.registrarDispositivo`) se genera un token aleatorio seguro, se
   guarda cifrado con BCrypt y se muestra **una sola vez** al administrador en el detalle
   del cultivo (mensaje flash).
3. **Validación física de lecturas.** Crear una clase de dominio (por ejemplo
   `RangoFisico`) con los rangos posibles por variable: humedad 0–100 %, pH 0–14,
   temperatura −10 a 60 °C. Será usada también por B1: dejarla en un lugar compartido
   (`model/` o `service/`).
4. **Última conexión.** Migración: columna `ultima_conexion` en `dispositivo_iot`; se
   actualiza cada vez que el nodo envía lecturas.

**Archivos involucrados:** `CultivoService`, `DispositivoIoT`, `cultivos/detalle.html`,
`database/esquema.sql`, migraciones, `docs/12-api-de-lecturas.md`.

**Pruebas obligatorias:** generación y verificación de token; lectura válida, fuera de
rango físico y de un tipo de sensor que el nodo no tiene; actualización de `ultima_conexion`.

**Criterios de aceptación:**
- [ ] Contrato documentado y aprobado por B1 (comentario en el issue).
- [ ] Un token incorrecto no valida; el token nunca aparece en logs ni en texto plano en la BD.
- [ ] Una lectura físicamente imposible se rechaza y queda en el log como posible falla de sensor.
- [ ] Migraciones, `esquema.sql` y entidades actualizados; pruebas en verde.

**Depende de:** nada. **Desbloquea:** A2 y la validación de umbrales de B1.
**Esfuerzo estimado:** alto.

<a id="b1"></a>
### B1 · Alertas por episodio y severidad

**Objetivo:** que las alertas sean pocas, claras y priorizadas.

**Por qué importa al negocio:** hoy **cada** lectura fuera de rango crea una alerta nueva.
Con un nodo enviando cada 15 minutos, una sequía de un día genera ~96 alertas iguales y el
agricultor deja de mirarlas. Además, no se distingue una desviación leve de una crítica.

**Pasos:**
1. **Episodios.** Migración en `alertas`: `estado` (`abierta`/`cerrada`), `valor_extremo`,
   `cantidad_lecturas`, `fecha_cierre`. En `AlertasService.evaluar(...)`: si ya hay una
   alerta abierta para el mismo cultivo, variable y dirección (por debajo/por encima),
   se actualiza en lugar de crear otra y **no** se vuelve a notificar. Cuando la variable
   vuelve al rango, la alerta abierta se cierra.
2. **Severidad.** Columna `severidad` (`leve`, `moderada`, `critica`). Se calcula según
   cuánto se aleja del umbral respecto al ancho del rango (por ejemplo < 10 % leve,
   > 25 % crítica) y sube de nivel si el episodio dura más de un tiempo configurable.
   Porcentajes y tiempo en `application.properties` (`agro.alertas.severidad.*`).
3. **Umbrales válidos por variable.** `UmbralService.guardar` usa los rangos físicos de A1:
   un umbral de pH 20 o humedad 150 muestra error en el formulario. Si A1 aún no integró
   la clase de rangos, acordar su nombre y crearla juntos.
4. **Punto único de evaluación.** Verificar que el simulador y la futura API pasen por
   `AlertasService.evaluar(...)`. Mostrar la severidad en `alertas/lista.html` (con forma y
   texto, no solo color).

**Archivos involucrados:** `AlertasService`, `Alerta`, `UmbralService`, `SimuladorService`,
`alertas/lista.html`, `dashboard.html`, migraciones, `esquema.sql`.

**Pruebas obligatorias:** 10 lecturas seguidas fuera de rango = 1 alerta y 1 notificación
por usuario; cierre al volver al rango; nueva salida = nueva alerta; cada nivel de
severidad; umbral fuera del rango físico rechazado.

**Criterios de aceptación:**
- [ ] La lista de alertas ya no se llena de repetidas; muestra severidad y estado.
- [ ] El contador del menú cuenta episodios abiertos, no lecturas.
- [ ] Pruebas actualizadas en `AlertasServiceTest` y `UmbralServiceTest`; todas en verde.

**Depende de:** A1 (rangos físicos; se puede coordinar en paralelo).
**Desbloquea:** B2, D2. **Esfuerzo estimado:** alto.

<a id="c1"></a>
### C1 · Reglas de cuenta y auditoría

**Objetivo:** que las cuentas sigan reglas claras y que las acciones importantes queden
registradas.

**Por qué importa al negocio:** la columna `usuarios.estado` existe pero el login la
ignora (un usuario inactivo puede entrar), no hay protección contra adivinar contraseñas y
no queda registro de quién borró un cultivo o cambió un umbral.

**Pasos:**
1. **Estado del usuario.** `AppUserDetailsService` marca como deshabilitado a un usuario
   `inactivo`. Mensaje en el login que no revele si la cuenta existe.
2. **Bloqueo por intentos.** Migración: `intentos_fallidos` y `bloqueado_hasta` en
   `usuarios`. Escuchar los eventos de autenticación de Spring Security (éxito y fallo):
   5 fallos bloquean 15 minutos (configurable); un ingreso correcto reinicia el contador.
3. **Política de contraseñas.** En `RegistroRequest`: mínimo 8 caracteres con letras y
   números; error bajo el campo en `registro.html`.
4. **Auditoría.** Migración: tabla `auditoria` (id, id_usuario, accion, entidad,
   id_entidad, detalle, fecha). Servicio `AuditoriaService.registrar(usuario, accion,
   entidad, id, detalle)`. Documentar su uso en `docs/02-arquitectura.md` para A, B y D.
   Empezar registrando: inicio de sesión, fallo y bloqueo.

**Archivos involucrados:** `AppUserDetailsService`, `SecurityConfig` (**zona protegida**:
el pull request lo revisan dos personas), `UsuarioService`, `RegistroRequest`,
`login.html`, `registro.html`, migraciones, `esquema.sql`.

**Pruebas obligatorias:** usuario inactivo no entra; bloqueo tras 5 fallos; desbloqueo
por tiempo; reinicio del contador; contraseña débil rechazada; registro de auditoría.

**Criterios de aceptación:**
- [ ] Las cuentas existentes (ver [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md))
      siguen pudiendo entrar.
- [ ] Ningún mensaje revela si un correo está registrado.
- [ ] `AuditoriaService` documentado y con pruebas.

**Depende de:** nada. **Desbloquea:** C2 y la auditoría de A2, B2 y D2.
**Esfuerzo estimado:** medio-alto.

<a id="d1"></a>
### D1 · Histórico escalable y datos válidos

**Objetivo:** que el histórico funcione con meses de datos y que los cultivos tengan datos
coherentes.

**Por qué importa al negocio:** con nodos reales enviando cada pocos minutos, el histórico
y las alertas tendrán miles de filas; hoy se cargan todas a la vez. Además, el sistema
acepta coordenadas en cualquier parte del mundo (el cultivo "Papa" de desarrollo está fuera
de Perú) y muestra un plan de pago sin ninguna lógica.

**Pasos:**
1. **Paginación.** Histórico y alertas con 50 filas por página (`Pageable` de Spring Data).
   Al cambiar de página se conservan los filtros de fecha; la exportación a Excel respeta el
   filtro, no la página.
2. **Agregación diaria.** Si el rango supera N días (configurable, por ejemplo 14), la
   gráfica usa promedio, mínimo y máximo por día con una consulta agrupada en la BD
   (`date_trunc('day', fecha_hora)`), no en Java.
3. **Coordenadas en Piura.** Validación en `CultivoForm`/`CultivoService` con los límites
   de la región (configurables en `application.properties`). Corregir el cultivo "Papa"
   en la BD de desarrollo.
4. **Decisión sobre "Pagos" y "Plan Productor S/ 129".** Llevar al equipo una propuesta
   escrita: quitarlos o definir qué limita el plan (cultivos, nodos, historial). Registrar
   el acuerdo en un comentario del issue. Se aplica en D2.

**Archivos involucrados:** `HistoricoService`, `HistoricoController`, `LecturaRepository`,
`AlertasService`, `AlertaController`, `historico/lista.html`, `alertas/lista.html`,
`CultivoForm`, `CultivoService`, `components/layout.html` (solo para la decisión).

**Pruebas obligatorias:** paginación con filtros; consulta agrupada por día; coordenadas
fuera de la región rechazadas.

**Criterios de aceptación:**
- [ ] Un rango de 90 días carga en menos de 1 segundo con datos simulados.
- [ ] Paginación usable en celular (390px).
- [ ] Decisión sobre pagos registrada con el acuerdo del equipo.

**Depende de:** nada. **Desbloquea:** D2. **Esfuerzo estimado:** medio-alto.

<a id="e1"></a>
### E1 · Integración continua, producción y datos demo

**Objetivo:** que ningún cambio roto llegue a `main` y que la app esté lista para mostrarse.

**Por qué importa al negocio:** con 5 personas integrando en paralelo, sin pruebas
automáticas en cada pull request los errores se descubren tarde. La presentación necesita
datos creíbles, no un cultivo vacío.

**Pasos:**
1. **GitHub Actions.** Flujo `.github/workflows/ci.yml`: JDK 25, servicio PostgreSQL,
   crear la BD con `database/esquema.sql`, ejecutar `mvnw test` con `DB_PASSWORD` de
   ejemplo. Corre en cada pull request y en `main`. Activar en GitHub la regla de rama que
   exige CI en verde para integrar.
2. **Perfil de producción.** `application-prod.properties.example`: administrador demo
   desactivado, sin trazas, logs a archivo, todo secreto por variable de entorno.
3. **Datos de demostración.** `database/datos-demo.sql`: 3 o 4 cultivos reales de Piura
   (mango en Tambogrande, limón en Chulucanas, banano en Sullana, arroz en el Bajo Piura)
   con coordenadas correctas, nodos con sensores, umbrales y 30 días de lecturas con
   algunos episodios fuera de rango. Usuarios anotados en
   [10-usuarios-de-prueba.md](10-usuarios-de-prueba.md).
4. **Plan de pruebas de aceptación.** En [07-pruebas-y-verificacion.md](07-pruebas-y-verificacion.md),
   un caso por requerimiento (RF-01 a RF-08) con pasos y resultado esperado. Los demás
   frentes añaden los suyos en el sprint 2.

**Archivos involucrados:** `.github/workflows/ci.yml`, `src/main/resources/`, `database/`,
`docs/01`, `docs/07`, `docs/10`.

**Criterios de aceptación:**
- [ ] Un pull request con una prueba rota queda en rojo y no se puede integrar.
- [ ] `datos-demo.sql` se ejecuta sin errores sobre una BD recién creada.
- [ ] Arrancar con el perfil `prod` no crea el administrador demo.

**Depende de:** nada. **Desbloquea:** E2 y la verificación de todos los demás.
**Esfuerzo estimado:** medio.

---

## 3. Sprint 2 · Funcionalidad completa (semanas 3–4)

**Meta:** todo lo diseñado se usa desde la interfaz y las lecturas llegan desde un nodo.

<a id="a2"></a>
### A2 · API de lecturas y estado de nodos

**Objetivo:** recibir lecturas reales y mostrar si cada nodo está funcionando.

**Pasos:**
1. **Endpoint `POST /api/lecturas`** según el contrato de A1. Autenticación por token del
   nodo (cabecera, por ejemplo `Authorization: Bearer <token>`), fuera de la sesión web.
   Solo para `/api/**`: sin CSRF y sin redirección al login (responde 401 en JSON). Cada
   lectura válida se guarda y pasa por `AlertasService.evaluar(...)`.
   El cambio en `SecurityConfig` es **zona protegida**: el pull request lo revisan dos personas.
2. **Estado del nodo.** *En línea* si reportó en los últimos N minutos (por defecto 30,
   configurable); *sin conexión* si no. Mostrarlo en el detalle del cultivo y en el
   dashboard (forma + texto). Un nodo sin conexión más de N horas genera una alerta de tipo
   "nodo sin conexión" (acordar con B2 cómo se representa).
3. **Gestión del nodo** (solo administrador del cultivo): activar/desactivar, regenerar
   token (invalida el anterior), ver última conexión. Registrar en la auditoría de C1.
4. **Cliente del nodo.** Carpeta `iot/` con un sketch de Arduino para ESP32 (o un script de
   PC que simule uno o varios nodos) que envíe lecturas cada X minutos, con README de
   conexión de sensores, token y URL.

**Pruebas obligatorias (MockMvc):** token válido; inválido; nodo inactivo; lote mixto
(válidas e inválidas); que `/api/**` no rompa CSRF ni login del resto de rutas; cálculo
del estado.

**Criterios de aceptación:**
- [ ] Con el cliente corriendo, las lecturas aparecen en el dashboard y generan alertas.
- [ ] Un colaborador no ve las acciones de gestión del nodo y recibe 403 si las intenta.
- [ ] El resto de la aplicación sigue exigiendo sesión y CSRF.

**Depende de:** A1, B1 (evaluación), C1 (auditoría). **Desbloquea:** A3, D2.
**Esfuerzo estimado:** alto.

<a id="b2"></a>
### B2 · Plantillas de umbrales, recomendaciones y atención de alertas

**Objetivo:** que la app no solo avise, sino que **oriente** al agricultor.

**Pasos:**
1. **Plantillas por tipo de cultivo.** Migración: tabla `plantilla_umbral` (tipo de
   cultivo, id_tipo_sensor, mínimo, máximo, fuente). Valores para mango, limón, banano,
   arroz y papa investigados en fuentes agronómicas (INIA, MIDAGRI, FAO, universidades) con
   la **fuente citada** en la columna `fuente` y en `docs/`. No usar valores inventados.
   Al crear un cultivo se ofrece aplicar la plantilla; el administrador puede ajustarla.
2. **Recomendaciones.** Catálogo (tabla o archivo de configuración, no texto en el código)
   por variable y dirección: humedad de suelo baja → regar; alta → revisar drenaje;
   temperatura alta → riego en horas frescas; pH fuera de rango → análisis de suelo. Se
   muestra en la alerta y en la notificación.
3. **Atención con responsable.** Migración: `id_usuario_atencion`, `fecha_atencion` y
   `comentario` en `alertas`. Al marcar "atendida" se puede escribir un comentario; la
   lista muestra quién y cuándo. Registrar en la auditoría.
4. *(Opcional)* **Clima en la recomendación:** si OpenWeatherMap pronostica lluvia, la
   recomendación de riego lo advierte. Sin API key funciona igual.

**Pruebas obligatorias:** aplicar plantilla al crear cultivo; recomendación por cada
combinación variable/dirección; atención guarda responsable y fecha; (opcional) clima
falso con lluvia.

**Criterios de aceptación:**
- [ ] Cada valor de plantilla tiene su fuente citada.
- [ ] Toda alerta muestra una recomendación.
- [ ] D2 puede calcular el tiempo de atención con los datos guardados.

**Depende de:** B1. **Desbloquea:** B3, D2. **Esfuerzo estimado:** alto.

<a id="c2"></a>
### C2 · Perfil, auditoría en uso y cabeceras de seguridad

**Objetivo:** que cada persona gestione su cuenta y que el administrador pueda ver qué
pasó en su cultivo.

**Pasos:**
1. **Mi perfil** (`/perfil`): ver y editar nombre; cambiar contraseña pidiendo la actual y
   aplicando la política de C1. Enlace desde el menú (zona del usuario en `layout.html`).
2. **Auditoría en uso.** Registrar cambios de accesos, eliminación de cultivos, cambios de
   umbrales (coordinar con A2, B2 y D2 para sus propias acciones). Vista "Actividad" por
   cultivo, solo para su administrador (`@permisos.puedeAdministrar`).
3. **Desactivar la propia cuenta**, solo si no es el último administrador de ningún
   cultivo (mismo criterio que `AccesoService`); si lo es, indicar en qué cultivos.
4. **Content-Security-Policy** en `SecurityConfig` compatible con Leaflet (unpkg),
   OpenStreetMap, Chart.js local y Google Fonts. Revisar la consola del navegador en
   **todas** las pantallas.
5. *(Opcional)* Recuperación de contraseña por correo con token de un solo uso y caducidad.

**Pruebas obligatorias:** cambio de contraseña con actual incorrecta; vista de actividad
con usuario ajeno (403); desactivación bloqueada si es último administrador; cabecera CSP
presente.

**Criterios de aceptación:**
- [ ] Ninguna pantalla se rompe con la CSP activa.
- [ ] Una cuenta desactivada no puede iniciar sesión.

**Depende de:** C1. **Desbloquea:** C3. **Esfuerzo estimado:** medio-alto.

<a id="d2"></a>
### D2 · Indicadores de gestión y reportes

**Objetivo:** convertir los datos en información para decidir.

**Pasos:**
1. **Indicadores en el dashboard** (cálculo en `DashboardService`, consultas en la BD):
   - % de lecturas dentro de rango por cultivo en los últimos 7 días;
   - tiempo medio entre que se abre una alerta y se atiende (datos de B2);
   - disponibilidad de nodos: % del tiempo en línea (datos de A2).
2. **Tendencia por variable** en el histórico: sube, baja o se mantiene respecto a la
   semana anterior (con texto e icono).
3. **Reporte por cultivo en Excel** con hojas: resumen e indicadores, lecturas, alertas con
   su atención. Respeta permisos (`@permisos.puedeVer`). *(Opcional: PDF.)*
4. **Aplicar la decisión sobre "Pagos" y el plan** tomada en D1.

**Pruebas obligatorias:** cada indicador con datos conocidos; tendencia en sus tres casos;
exportador del reporte; reporte de cultivo ajeno → 403.

**Criterios de aceptación:**
- [ ] Los indicadores se ven en el dashboard con el sistema visual existente.
- [ ] Ninguna pantalla muestra funciones sin lógica.

**Depende de:** D1, A2 (estado de nodos), B2 (atención). Si esos aún no están, empezar
por la tendencia y el reporte. **Desbloquea:** D3. **Esfuerzo estimado:** alto.

<a id="e2"></a>
### E2 · Docker y revisiones cruzadas

**Objetivo:** que el proyecto se levante en cualquier equipo y que la calidad se revise
desde fuera de cada frente.

**Pasos:**
1. **Docker.** `Dockerfile` (compila con JDK 25 y ejecuta el JAR) y `docker-compose.yml`
   (app + PostgreSQL con `esquema.sql` al iniciar). Credenciales por variables de entorno.
   Guía en `docs/01-instalacion-y-ejecucion.md`.
2. **Revisión de seguridad cruzada.** Recorrer cada ruta nueva de A2, C2 y D2 con un
   usuario ajeno y con un colaborador. Cada acceso indebido → issue de bug etiquetado.
3. **Accesibilidad** de las pantallas nuevas: uso solo con teclado, foco visible,
   contraste AA, etiquetas en formularios.
4. *(Opcional)* **Pruebas de punta a punta** en navegador (Playwright o Selenium) del
   flujo registro → cultivo → nodo → umbral → lectura → alerta → atención, integradas en CI.

**Criterios de aceptación:**
- [ ] `docker compose up` deja la app funcionando en un equipo sin Java ni PostgreSQL.
- [ ] Lista de rutas revisadas en un comentario del issue; bugs reportados.

**Depende de:** E1. **Desbloquea:** E3. **Esfuerzo estimado:** medio.

---

## 4. Sprint 3 · Integración, estabilización y entrega (semanas 5–6)

**Meta:** sin funcionalidades grandes nuevas. Integrar, corregir, medir, documentar y
preparar la presentación.

<a id="a3"></a>
### A3 · Robustez y documentación IoT

**Pasos:**
1. **Prueba de carga:** script en `iot/` con 10 nodos enviando cada minuto durante 1 hora.
   Registrar tiempos de respuesta y errores en `docs/12-api-de-lecturas.md`.
2. **Lecturas repetidas o atrasadas:** si llega una lectura con el mismo nodo, sensor y
   fecha, no se duplica (índice único + manejo en el servicio). Una lectura atrasada se
   guarda pero no reabre episodios ya cerrados.
3. **Documentación final:** API completa y diagrama de conexión de sensores al ESP32.

**Criterios de aceptación:**
- [ ] 1 hora de carga sin errores; resultados documentados.
- [ ] Otra persona del equipo monta o simula un nodo siguiendo solo la documentación.

**Depende de:** A2. **Esfuerzo estimado:** medio.

<a id="b3"></a>
### B3 · Validación agronómica

**Pasos:**
1. Preparar 5 escenarios con datos (script o simulador): sequía, exceso de riego, ola de
   calor, sensor dañado (valores imposibles), nodo caído.
2. Ejecutarlos y comprobar alertas, severidad, recomendaciones y cierre de episodios.
3. Ajustar porcentajes de severidad según los resultados.
4. Documento `docs/13-reglas-agronomicas.md`: umbrales por cultivo con fuentes, severidad,
   episodios, recomendaciones.

**Criterios de aceptación:**
- [ ] Resultado de cada escenario registrado en el issue.
- [ ] Documento enlazado desde `docs/README.md`.

**Depende de:** B2 (y A2 para el escenario de nodo caído). **Esfuerzo estimado:** medio.

<a id="c3"></a>
### C3 · Cierre de seguridad

**Pasos:**
1. Revisión final: rutas, CSRF, permisos por cultivo, `/api/**`, secretos fuera del
   repositorio, cabeceras.
2. Dependencias sin vulnerabilidades conocidas (plugin OWASP Dependency-Check de Maven o
   herramienta equivalente); actualizar las que corresponda **solo** dentro de la misma
   versión mayor.
3. Corregir (o coordinar la corrección de) los bugs de seguridad reportados por E2.
4. Actualizar [05-seguridad-y-permisos.md](05-seguridad-y-permisos.md) con todo lo nuevo.

**Criterios de aceptación:**
- [ ] Informe en el issue; todos los bugs de seguridad cerrados o justificados.
- [ ] Documentación de seguridad coincide con el código.

**Depende de:** C2, E2. **Esfuerzo estimado:** medio.

<a id="d3"></a>
### D3 · Verificación de indicadores y rendimiento

**Pasos:**
1. Calcular a mano 2 o 3 casos de cada indicador con los datos de demostración y
   compararlos con la app.
2. Medir dashboard e histórico con 30 días de datos de varios cultivos; corregir consultas
   lentas (índices, consultas agrupadas).
3. Actualizar [03-funcionalidades-y-rutas.md](03-funcionalidades-y-rutas.md) con pantallas
   y rutas nuevas de **todos** los frentes.

**Criterios de aceptación:**
- [ ] Indicadores verificados (resultados en el issue).
- [ ] Dashboard e histórico en menos de 1 segundo.
- [ ] Tabla de rutas igual a los controladores.

**Depende de:** D2. **Esfuerzo estimado:** medio.

<a id="e3"></a>
### E3 · Aceptación, instalación limpia y presentación

**Pasos:**
1. **Congelamiento de código** a mitad del sprint (anunciar la fecha): después solo se
   integran correcciones.
2. Ejecutar el plan de pruebas de aceptación completo; registrar resultados en
   [07-pruebas-y-verificacion.md](07-pruebas-y-verificacion.md). Cada fallo → issue.
3. **Instalación limpia:** alguien que no haya tocado esa parte instala desde cero
   siguiendo solo `docs/`; corregir los pasos que fallen.
4. **Presentación:** guion de la demostración, datos de demo cargados, plan B sin red
   (mapa y fuentes) o sin nodo físico (cliente simulado de A2).
5. **Versión 1.0.0:** revisar índice y registro de cambios de `docs/` y subir la versión en
   `pom.xml`.

**Criterios de aceptación:**
- [ ] Todos los casos de aceptación ejecutados y registrados.
- [ ] Demostración ensayada al menos una vez con todo el equipo.
- [ ] Versión 1.0.0 en `main`.

**Depende de:** E2 y del cierre de los demás issues. **Esfuerzo estimado:** medio.

---

## 5. Definición de "proyecto terminado"

- [ ] Un nodo envía lecturas reales (o simuladas por el cliente externo) por la API autenticada.
- [ ] Las alertas se agrupan por episodio y tienen severidad, recomendación y responsable de atención.
- [ ] Cada cultivo puede partir de una plantilla de umbrales con fuente agronómica citada.
- [ ] Las cuentas respetan estado, bloqueo por intentos y política de contraseñas; hay perfil y auditoría.
- [ ] El dashboard muestra indicadores de gestión verificados; histórico y alertas paginados.
- [ ] Ninguna pantalla muestra funciones sin lógica.
- [ ] CI en verde en `main`; el proyecto se levanta con Docker o con la guía de instalación.
- [ ] Pruebas de aceptación ejecutadas y registradas.
- [ ] `docs/` actualizado y coherente con el código.

## 6. Seguimiento

| Issue | Responsable | Estado |
|---|---|---|
| A1 · Base de la telemetría IoT | | ☐ |
| B1 · Alertas por episodio y severidad | | ☐ |
| C1 · Reglas de cuenta y auditoría | | ☐ |
| D1 · Histórico escalable y datos válidos | | ☐ |
| E1 · Integración continua, producción y datos demo | | ☐ |
| A2 · API de lecturas y estado de nodos | | ☐ |
| B2 · Plantillas, recomendaciones y atención | | ☐ |
| C2 · Perfil, auditoría en uso y CSP | | ☐ |
| D2 · Indicadores de gestión y reportes | | ☐ |
| E2 · Docker y revisiones cruzadas | | ☐ |
| A3 · Robustez y documentación IoT | | ☐ |
| B3 · Validación agronómica | | ☐ |
| C3 · Cierre de seguridad | | ☐ |
| D3 · Verificación de indicadores y rendimiento | | ☐ |
| E3 · Aceptación, instalación limpia y presentación | | ☐ |

El tablero de GitHub (issues y milestones) es la fuente de verdad del estado; esta tabla
se actualiza al cerrar cada sprint.
