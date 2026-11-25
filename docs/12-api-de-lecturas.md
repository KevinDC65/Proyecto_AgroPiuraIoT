# 12 · API de lecturas (telemetría IoT)

Contrato entre un nodo ESP32 y AgroPiura IoT para enviar lecturas de sensores. Lo define
el issue **A1 · Base de la telemetría IoT** (#1); **A2** (#6) abre la ruta y aplica la
autenticación por token, y **A3** agrega la prueba de carga y el diagrama de conexión.

> **Estado (25/11/2026):** el procesamiento (validación física, última conexión, token por
> nodo) está implementado y probado a nivel de servicio. La ruta sigue **cerrada**
> (`/api/**` con `denyAll` en `SecurityConfig`) hasta que A2 conecte la verificación del
> token a la petición HTTP.

---

## 1. Ruta

```
POST /api/telemetria
Content-Type: application/json
Authorization: Bearer <token del nodo>
```

- Sin sesión web ni CSRF: el nodo se identifica solo con su token (lo implementa A2).
- Un nodo solo puede enviar lecturas de **sus** sensores.

## 2. Cuerpo de la solicitud

Formato recomendado: un nodo, una lista de lecturas.

```json
{
  "idNodo": 3,
  "fechaHora": "2026-11-25T08:30:00",
  "lecturas": [
    { "tipoSensor": "humedad_suelo",        "valor": 41.5 },
    { "tipoSensor": "temperatura_ambiente", "valor": 29.8 },
    { "tipoSensor": "humedad_ambiente",     "valor": 68 },
    { "tipoSensor": "ph_suelo",             "valor": 6.4 }
  ]
}
```

| Campo | Obligatorio | Descripción |
|---|:---:|---|
| `idNodo` | sí | ID del nodo (`dispositivo_iot.id_dispositivo`), el que muestra el detalle del cultivo. También se acepta `idDispositivo`. |
| `lecturas` | sí | Lista de lecturas. Debe tener al menos una. |
| `lecturas[].tipoSensor` | sí ¹ | `humedad_suelo`, `humedad_ambiente`, `temperatura_ambiente` o `ph_suelo` (sin distinguir mayúsculas). |
| `lecturas[].valor` | sí | Número decimal con punto (`6.4`), en la unidad de la sección 3. |
| `fechaHora` | no | Momento de la medición, hora local de Perú, formato ISO `AAAA-MM-DDTHH:MM:SS`. Si falta, se usa la hora de llegada al servidor. Puede ir en el lote o en cada lectura. |

¹ En lugar de `tipoSensor` también se acepta `idSensor` o `idTipoSensor`.

**Formatos heredados** (siguen funcionando por compatibilidad, no usarlos en nodos
nuevos): una sola lectura con `idDispositivo` + `tipoSensor` + `valor`, o los atajos
`temperatura`, `humedad` y `humedadSuelo` en la raíz del JSON.

## 3. Validación física

Antes de guardar, cada lectura se compara con lo que el sensor **puede** medir
(`model/RangoFisico.java`). Un valor fuera de este rango indica un sensor dañado o mal
conectado, no un problema del cultivo:

| Tipo de sensor | Unidad | Rango físico aceptado |
|---|---|:---:|
| `humedad_suelo` | % | 0 a 100 |
| `humedad_ambiente` | % | 0 a 100 |
| `ph_suelo` | — | 0 a 14 |
| `temperatura_ambiente` | °C | −10 a 60 |

Los límites son inclusivos (0 % y 100 % son válidos). Una lectura imposible:
- **no se guarda** ni se evalúa contra los umbrales, así que no genera alertas;
- queda en el log como `WARN ... posible falla de sensor` con el nodo, el sensor y el valor;
- se informa en la respuesta (`rechazos`), y el resto del lote se procesa igual.

No confundir con los **umbrales** del cultivo (reglas agronómicas de B1/B3): el umbral dice qué es bueno para la
planta; el rango físico dice qué es posible medir.

## 4. Respuestas

Todas las respuestas son JSON.

| Código | Cuándo | `status` |
|:---:|---|---|
| **200** | Todas las lecturas se guardaron | `ok` |
| **200** | Algunas se guardaron y otras se rechazaron por valor imposible | `parcial` |
| **200** | Todas se rechazaron por valor imposible | `rechazado` |
| **400** | JSON ilegible, sin lecturas, valor nulo o lectura sin tipo de sensor | `error` |
| **401** | Falta el token, no es válido o no pertenece a `idNodo` (A2) | `error` |
| **404** | El nodo no existe o no tiene un sensor de ese tipo | `error` |

Las lecturas físicamente imposibles responden **200** a propósito: la solicitud era
correcta y reenviarla no cambiaría el resultado. El nodo no debe reintentar; el problema
está en el sensor.

**200 · todo guardado**
```json
{ "status": "ok", "lecturasProcesadas": 4, "alertasGeneradas": 1, "lecturasRechazadas": 0,
  "mensaje": "Telemetría procesada exitosamente" }
```

**200 · lote parcial**
```json
{ "status": "parcial", "lecturasProcesadas": 3, "alertasGeneradas": 0, "lecturasRechazadas": 1,
  "mensaje": "1 lectura(s) rechazada(s) por valor físicamente imposible",
  "rechazos": ["ph_suelo = 20 fuera del rango físico (0 a 14): posible falla de sensor"] }
```

**400 / 401 / 404**
```json
{ "status": "error", "lecturasProcesadas": 0, "alertasGeneradas": 0, "lecturasRechazadas": 0,
  "mensaje": "No se encontró sensor de tipo 'radiacion_solar' para el dispositivo 3" }
```

Un error 400 o 404 rechaza **todo** el lote (no se guarda ninguna lectura).

## 5. Token del nodo

- Se genera al pulsar **Registrar nodo nuevo** en el detalle del cultivo: 32 bytes
  aleatorios (`SecureRandom`) en Base64 URL, 43 caracteres.
- Se muestra **una sola vez** en un aviso del detalle del cultivo. Hay que copiarlo y
  grabarlo en el ESP32. No se puede volver a consultar.
- En la BD solo se guarda su hash BCrypt (`dispositivo_iot.token_hash`). Nunca aparece en
  los logs: el log solo registra "Token asignado al nodo N" o "Token rechazado para el nodo N".
- Si se pierde, se genera uno nuevo (botón de A2) y el anterior deja de funcionar.
- Los nodos creados antes de A1 no tienen token: deben recibir uno nuevo antes de enviar
  lecturas a la API.

Verificación: `NodoTokenService.verificar(idNodo, token)`. Responde `false` sin más detalle
si el nodo no existe, no tiene token o el token no coincide, para no revelar qué nodos
existen.

## 6. Última conexión

Cada solicitud procesada actualiza `dispositivo_iot.ultima_conexion` de los nodos que
enviaron lecturas, **aunque alguna lectura se haya rechazado** por valor imposible (el
nodo sí se comunicó). Una solicitud que termina en 400 o 404 no la actualiza. A2 usa este
dato para mostrar si el nodo está en línea.

## 7. Ejemplo con curl

Disponible cuando A2 abra la ruta:

```bash
curl -X POST http://localhost:8080/api/telemetria \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"idNodo":3,"lecturas":[{"tipoSensor":"humedad_suelo","valor":41.5}]}'
```

## 8. Archivos

| Archivo | Rol |
|---|---|
| `controller/TelemetriaController.java` | Ruta `POST /api/telemetria` y códigos de error |
| `service/TelemetriaService.java` | Valida, guarda, evalúa alertas y registra la conexión |
| `service/NodoTokenService.java` | Genera, guarda (hash) y verifica el token |
| `model/RangoFisico.java` | Rangos físicos por tipo de sensor (los usa también B1) |
| `model/dto/TelemetriaRequest.java`, `LecturaItemRequest.java`, `TelemetriaResponse.java` | Formato JSON |
| `database/migraciones/2026-11-25_A1_token_y_conexion_nodo.sql` | Columnas `token_hash` y `ultima_conexion` |
