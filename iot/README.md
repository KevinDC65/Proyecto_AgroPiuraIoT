# Cliente IoT · AgroPiura IoT

Esta carpeta contiene dos clientes compatibles con el endpoint autenticado `POST /api/telemetria` (ver contrato completo en [`docs/12-api-de-lecturas.md`](../docs/12-api-de-lecturas.md)):

1. **`esp32_agropiura/esp32_agropiura.ino`**: sketch de Arduino para microcontrolador ESP32 real.
2. **`simulador_nodo.py`**: simulador en Python (compatible con Python 3 y 2.7, sin dependencias externas) para probar uno o varios nodos desde PC.
3. **`simulador-nodo.ps1`**: simulador nativo en PowerShell para Windows (sin requerir Python).

---

## 1. Obtener el token del nodo en la aplicación web

1. Inicia sesión como **administrador del cultivo** (por ejemplo `admin@agropiura.com`).
2. Abre el detalle del cultivo (`/cultivos/{id}`).
3. Haz clic en **Registrar nodo nuevo** (o en **Regenerar token** sobre un nodo existente).
4. Copia el token mostrado en pantalla (`ap_<id>_<secreto>`). **Por seguridad solo se muestra una vez**; en la base de datos se guarda únicamente su hash BCrypt.

---

## 2. Probar desde PC con el simulador (PowerShell o Python)

### En Windows con PowerShell (sin instalar nada)
```powershell
.\iot\simulador-nodo.ps1 -Token "ap_1_TU_TOKEN" -Intervalo 5 -Ciclos 3
.\iot\simulador-nodo.ps1 -Token "ap_1_TU_TOKEN" -Intervalo 5 -Ciclos 3 -Anomalia
```

### Con Python (compatible con Python 3 y 2.7)

### Envío continuo de lecturas normales (cada 10 segundos)
```powershell
python iot/simulador_nodo.py --url http://localhost:8080/api/telemetria --token ap_1_TU_TOKEN --intervalo 10
```

### Envío acotado de 3 rondas con valores anómalos (para disparar episodios de alerta)
```powershell
python iot/simulador_nodo.py --url http://localhost:8080/api/telemetria --token ap_1_TU_TOKEN --intervalo 5 --ciclos 3 --anomalia
```

### Simular varios nodos en paralelo en cada ronda
```powershell
python iot/simulador_nodo.py --token ap_1_TOKEN_A --token ap_2_TOKEN_B --intervalo 15
```

### Prueba rápida con `curl`
```powershell
curl.exe -X POST http://localhost:8080/api/telemetria `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer ap_1_TU_TOKEN" `
  -d "{\"lecturas\":[{\"tipoSensor\":\"humedad_suelo\",\"valor\":48.5},{\"tipoSensor\":\"temperatura_ambiente\",\"valor\":27.2}]}"
```

---

## 3. Conexión de sensores al ESP32

| Sensor | Variable en AgroPiura | Pin ESP32 | Alimentación | Notas |
|---|---|---:|---|---|
| Sensor capacitivo de humedad de suelo | `humedad_suelo` (%) | `GPIO 34` (ADC1) | 3.3 V / GND | Calibrar `ADC_SUELO_SECO` y `ADC_SUELO_MOJADO` |
| DHT22 (AM2302) | `temperatura_ambiente` (°C) y `humedad_ambiente` (%) | `GPIO 4` | 3.3 V / GND | Añadir resistencia pull-up de 10 kΩ si el módulo no la trae |
| Módulo sensor de pH analógico | `ph_suelo` (pH) | `GPIO 35` (ADC1) | 3.3 V / GND | Calibrar con soluciones tampón pH 4.0 y pH 7.0 |

### Pasos en Arduino IDE
1. Instala el soporte de placas **ESP32 by Espressif Systems** y la librería **DHT sensor library** (Adafruit).
2. Abre `iot/esp32_agropiura/esp32_agropiura.ino`.
3. Edita las constantes `WIFI_SSID`, `WIFI_PASSWORD`, `API_URL` (usando la IP LAN del servidor, no `localhost`) y `NODO_TOKEN`.
4. Compila y sube al ESP32; abre el Monitor Serie a `115200` baudios para verificar las respuestas `HTTP 200`.
