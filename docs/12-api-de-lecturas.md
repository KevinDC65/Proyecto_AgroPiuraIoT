# 12 · API de lecturas de telemetría (A1 / A2)

Contrato oficial del endpoint de ingesta de lecturas para nodos ESP32 y clientes simulados.

---

## Endpoint y autenticación

- **URL:** `POST /api/telemetria`
- **Content-Type:** `application/json`
- **Autenticación obligatoria:** cabecera HTTP `Authorization: Bearer <token>`
  - Formato del token: `ap_<idDispositivo>_<secreto>` (generado al registrar un nodo o al regenerar su token; almacenado con BCrypt en `dispositivo_iot.token_hash`).
  - Funciona **fuera de la sesión web** (`STATELESS`), sin CSRF y sin redirección al formulario de login.
  - Si el JSON incluye `idDispositivo` (o alias `idNodo`), **debe coincidir** con el nodo del token; si se omite, se toma automáticamente del token autenticado.

---

## Formato de solicitud (Request)

### Opción recomendada · Lote de lecturas
```json
{
  "idNodo": 3,
  "fechaHora": "2026-11-24T19:30:00",
  "lecturas": [
    { "tipoSensor": "humedad_suelo", "valor": 48.50 },
    { "tipoSensor": "temperatura_ambiente", "valor": 27.40 },
    { "tipoSensor": "humedad_ambiente", "valor": 66.00 },
    { "tipoSensor": "ph_suelo", "valor": 6.50 }
  ]
}
```
- `idNodo` / `idDispositivo`: opcional (se infiere del token si se omite).
- `fechaHora`: opcional (formato ISO-8601 `AAAA-MM-DDTHH:MM:SS`; si se omite se usa la hora del servidor).
- Cada elemento de `lecturas` acepta `tipoSensor` (`humedad_suelo`, `temperatura_ambiente`, `humedad_ambiente`, `ph_suelo`), `idTipoSensor` o `idSensor`.

### Lectura individual (compatible con el contrato original)
```json
{
  "tipoSensor": "humedad_suelo",
  "valor": 22.40
}
```

---

## Validación física (`RangoFisico`)

Toda lectura se valida antes de guardarse contra el rango físico posible de su variable:

| Variable (`tipoSensor`) | Mínimo | Máximo | Unidad |
|---|---:|---:|---|
| `humedad_suelo` | `0` | `100` | `%` |
| `humedad_ambiente` | `0` | `100` | `%` |
| `ph_suelo` | `0` | `14` | `pH` |
| `temperatura_ambiente` | `-10` | `60` | `°C` |

- Las lecturas fuera de rango físico se **rechazan** y se registran en el log (`WARN`) como posible falla de sensor.
- En un **lote mixto** (varias lecturas donde algunas son válidas y otras inválidas), las válidas se guardan, pasan por `AlertasService.evaluar(...)`, actualizan `dispositivo_iot.ultima_conexion` y cierran alertas abiertas de `nodo_sin_conexion`; las inválidas se devuelven en `errores` y `lecturasRechazadas`.

---

## Códigos de respuesta

| Código | Cuándo ocurre | Ejemplo de cuerpo JSON |
|---:|---|---|
| `200 OK` | Todas las lecturas válidas procesadas | `{"status":"ok","lecturasProcesadas":2,"alertasGeneradas":1,"mensaje":"Telemetría procesada exitosamente"}` |
| `200 OK` | Lote mixto (algunas válidas y otras rechazadas) | `{"status":"ok","lecturasProcesadas":2,"lecturasRechazadas":1,"alertasGeneradas":0,"mensaje":"Telemetría procesada con observaciones","errores":["Lectura #3: Valor 150.00 fuera del rango físico para humedad_suelo (0 a 100 %)"]}` |
| `400 Bad Request` | JSON inválido, payload vacío, valor nulo/fuera de rango físico (lectura única o lote donde todas fallan), o `idDispositivo` distinto al del token | `{"status":"error","lecturasProcesadas":0,"alertasGeneradas":0,"mensaje":"El payload no contiene ninguna lectura válida"}` |
| `401 Unauthorized` | Falta cabecera `Authorization: Bearer` o el token es inválido/revocado | `{"status":"error","mensaje":"Token de nodo ausente o inválido"}` |
| `403 Forbidden` | Token válido pero el nodo fue desactivado por un administrador | `{"status":"error","mensaje":"El nodo se encuentra inactivo"}` |
| `404 Not Found` | Lectura individual que referencia un sensor o tipo de sensor que el nodo no tiene | `{"status":"error","lecturasProcesadas":0,"alertasGeneradas":0,"mensaje":"No se encontró sensor de tipo 'salinidad' para el dispositivo 3"}` |
