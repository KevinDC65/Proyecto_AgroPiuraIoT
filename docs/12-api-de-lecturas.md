# 12 · API de lecturas (telemetría IoT)

Contrato entre un nodo ESP32 (o simulador de PC) y AgroPiura IoT para enviar lecturas de
sensores. Definido en **A1 · Base de la telemetría IoT** (#1) y habilitado con
autenticación HTTP Bearer y monitoreo de nodos en **A2 · API de lecturas y estado de nodos** (#6).

> **Estado (27/11/2026):** la ruta `POST /api/telemetria` está **habilitada** con
> autenticación `Authorization: Bearer <token>` en cadena `SecurityFilterChain` stateless
> exclusiva para `/api/**` (sin sesión web ni CSRF, sin afectar la seguridad del resto de
> la aplicación).

---

## 1. Ruta

```
POST /api/telemetria
Content-Type: application/json
Authorization: Bearer <token del nodo>
```

- Sin sesión web ni CSRF: el nodo se identifica únicamente con su token Bearer.
- Un nodo solo puede enviar lecturas de **sus** sensores. Si el JSON incluye `idNodo` /
  `idDispositivo`, debe coincidir con el nodo autenticado; si se omite, se infiere del token.

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
| `idNodo` | no ¹ | ID del nodo (`dispositivo_iot.id_dispositivo`), el que muestra el detalle del cultivo. También se acepta `idDispositivo`. Si se omite en una petición autenticada, se infiere del token; si se envía, debe coincidir con el nodo del token. |
| `lecturas` | sí | Lista de lecturas. Debe tener al menos una. |
| `lecturas[].tipoSensor` | sí ² | `humedad_suelo`, `humedad_ambiente`, `temperatura_ambiente` o `ph_suelo` (sin distinguir mayúsculas). |
| `lecturas[].valor` | sí | Número decimal con punto (`6.4`), en la unidad de la sección 3. |
| `fechaHora` | no | Momento de la medición, hora local de Perú, formato ISO `AAAA-MM-DDTHH:MM:SS`. Si falta, se usa la hora de llegada al servidor. Puede ir en el lote o en cada lectura. |

¹ Obligatorio cuando se invoca el servicio directamente sin contexto HTTP autenticado.  
² En lugar de `tipoSensor` también se acepta `idSensor` o `idTipoSensor`.

**Formatos heredados** (siguen funcionando por compatibilidad, no usarlos en nodos
nuevos): una sola lectura con `tipoSensor` + `valor`, o los atajos `temperatura`,
`humedad` y `humedadSuelo` en la raíz del JSON.

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
| **400** | JSON ilegible, sin lecturas, valor nulo, sin tipo de sensor o `idNodo` distinto al del token | `error` |
| **401** | Falta el token Bearer, no es válido o fue revocado | `error` |
| **403** | El token es válido pero el nodo fue desactivado por un administrador | `error` |
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

**400 / 401 / 403 / 404**
```json
{ "status": "error", "lecturasProcesadas": 0, "alertasGeneradas": 0, "lecturasRechazadas": 0,
  "mensaje": "No se encontró sensor de tipo 'radiacion_solar' para el dispositivo 3" }
```

Un error 400 o 404 rechaza **todo** el lote (no se guarda ninguna lectura).

## 5. Token del nodo

- Se genera al pulsar **Registrar nodo nuevo** (o **Regenerar token**) en el detalle del
  cultivo: 32 bytes aleatorios (`SecureRandom`) en Base64 URL, 43 caracteres.
- Se muestra **una sola vez** en un aviso del detalle del cultivo (`tokenNodo`). Hay que
  copiarlo y grabarlo en el ESP32 o cliente simulado. No se puede volver a consultar.
- En la BD solo se guarda su hash BCrypt (`dispositivo_iot.token_hash`). Nunca aparece en
  los logs ni en la tabla `auditoria`.
- Si se pierde, el administrador del cultivo puede regenerarlo y el anterior queda
  invalidado de inmediato.
- Los nodos creados antes de A1 no tienen token: deben regenerar uno antes de enviar
  lecturas a la API.

## 6. Última conexión y estado del nodo

Cada solicitud procesada actualiza `dispositivo_iot.ultima_conexion` de los nodos que
enviaron lecturas (y cierra episodios abiertos de `nodo_sin_conexion` en `alerta_nodo`),
**aunque alguna o todas las lecturas se hayan rechazado** por valor imposible (el nodo sí
se comunicó). Una solicitud que termina en 400, 401, 403 o 404 no la actualiza.

`NodoService` clasifica cada nodo como **En línea** (reportó en los últimos
`agro.iot.nodo.minutos-en-linea`, por defecto 30 min), **Sin conexión** o **Inactivo**.

## 7. Ejemplo con curl y clientes de prueba

```bash
curl -X POST http://localhost:8080/api/telemetria \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"idNodo":3,"lecturas":[{"tipoSensor":"humedad_suelo","valor":41.5}]}'
```

Ver también los clientes listos para probar en [`iot/README.md`](../iot/README.md)
(`esp32_agropiura.ino`, `simulador_nodo.py` y `simulador-nodo.ps1`).

## 8. Archivos

| Archivo | Rol |
|---|---|
| `controller/TelemetriaController.java` | Ruta `POST /api/telemetria` y códigos de error |
| `security/TokenNodoAuthenticationFilter.java`, `security/TokenNodoService.java` | Autenticación Bearer stateless para `/api/**` |
| `service/TelemetriaService.java` | Valida, guarda, evalúa alertas y registra la conexión |
| `service/NodoTokenService.java` | Genera, guarda (hash BCrypt) y verifica el token |
| `service/NodoService.java` | Estado En línea / Sin conexión / Inactivo, auditoría y `alerta_nodo` |
| `model/RangoFisico.java` | Rangos físicos por tipo de sensor (usado en A1, A2 y B1) |
| `model/dto/TelemetriaRequest.java`, `LecturaItemRequest.java`, `TelemetriaResponse.java` | Formato JSON |
| `database/migraciones/2026-11-25_A1_token_y_conexion_nodo.sql` | Columnas `token_hash` y `ultima_conexion` |
| `database/migraciones/2026-11-24_A2_api_telemetria_y_nodos.sql` | Tabla `alerta_nodo` e índices |
