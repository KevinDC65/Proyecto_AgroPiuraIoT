/*
 * AgroPiura IoT - Cliente de telemetría para ESP32 (A2)
 *
 * Envía lecturas periódicas de humedad de suelo, temperatura ambiente,
 * humedad ambiente y pH a POST /api/telemetria autenticándose con el token
 * Bearer generado en la aplicación web.
 *
 * Librerías requeridas en Arduino IDE:
 *   - WiFi.h y HTTPClient.h (incluidas en el paquete ESP32)
 *   - DHT sensor library (Adafruit)
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <DHT.h>

// ===== CONFIGURACIÓN (completar antes de cargar al ESP32) =====
const char* WIFI_SSID     = "TU_RED_WIFI";
const char* WIFI_PASSWORD = "TU_CLAVE_WIFI";
const char* API_URL       = "http://192.168.1.100:8080/api/telemetria";
const char* NODO_TOKEN    = "ap_1_REEMPLAZA_CON_TU_TOKEN";

// Intervalo entre envíos en milisegundos (por defecto 5 minutos = 300000 ms)
const unsigned long INTERVALO_MS = 300000UL;

// ===== PINES DE SENSORES =====
#define PIN_DHT           4    // DHT22 (temperatura y humedad ambiente)
#define TIPO_DHT          DHT22
#define PIN_HUMEDAD_SUELO 34   // Sensor capacitivo de humedad de suelo (ADC)
#define PIN_PH_SUELO      35   // Módulo sensor de pH analógico (ADC)

// Calibración básica del sensor capacitivo de suelo (ajustar con suelo seco/mojado)
const int ADC_SUELO_SECO   = 3200;
const int ADC_SUELO_MOJADO = 1300;

DHT dht(PIN_DHT, TIPO_DHT);
unsigned long ultimoEnvio = 0;

void conectarWiFi() {
  if (WiFi.status() == WL_CONNECTED) return;
  Serial.print("Conectando a WiFi ");
  Serial.print(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  int intentos = 0;
  while (WiFi.status() != WL_CONNECTED && intentos < 30) {
    delay(500);
    Serial.print(".");
    intentos++;
  }
  Serial.println();
  if (WiFi.status() == WL_CONNECTED) {
    Serial.print("WiFi conectado. IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("No se pudo conectar a WiFi; reintentará en el próximo ciclo.");
  }
}

float leerHumedadSuelo() {
  int raw = analogRead(PIN_HUMEDAD_SUELO);
  float pct = 100.0f * (float)(ADC_SUELO_SECO - raw) / (float)(ADC_SUELO_SECO - ADC_SUELO_MOJADO);
  return constrain(pct, 0.0f, 100.0f);
}

float leerPhSuelo() {
  int raw = analogRead(PIN_PH_SUELO);
  float voltaje = (raw / 4095.0f) * 3.3f;
  // Curva lineal de referencia (calibrar con soluciones tampón pH 4 y pH 7)
  float ph = 7.0f + ((1.65f - voltaje) / 0.18f);
  return constrain(ph, 0.0f, 14.0f);
}

void enviarLecturas() {
  conectarWiFi();
  if (WiFi.status() != WL_CONNECTED) return;

  float tempAmb = dht.readTemperature();
  float humAmb  = dht.readHumidity();
  float humSuelo = leerHumedadSuelo();
  float phSuelo  = leerPhSuelo();

  String json = "{\"lecturas\":[";
  json += "{\"tipoSensor\":\"humedad_suelo\",\"valor\":" + String(humSuelo, 2) + "}";
  json += ",{\"tipoSensor\":\"ph_suelo\",\"valor\":" + String(phSuelo, 2) + "}";
  if (!isnan(tempAmb)) {
    json += ",{\"tipoSensor\":\"temperatura_ambiente\",\"valor\":" + String(tempAmb, 2) + "}";
  }
  if (!isnan(humAmb)) {
    json += ",{\"tipoSensor\":\"humedad_ambiente\",\"valor\":" + String(humAmb, 2) + "}";
  }
  json += "]}";

  HTTPClient http;
  http.begin(API_URL);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("Authorization", String("Bearer ") + NODO_TOKEN);

  int codigo = http.POST(json);
  String respuesta = http.getString();
  http.end();

  Serial.print("HTTP ");
  Serial.print(codigo);
  Serial.print(" -> ");
  Serial.println(respuesta);
}

void setup() {
  Serial.begin(115200);
  analogReadResolution(12);
  dht.begin();
  conectarWiFi();
  enviarLecturas();
  ultimoEnvio = millis();
}

void loop() {
  if (millis() - ultimoEnvio >= INTERVALO_MS) {
    ultimoEnvio = millis();
    enviarLecturas();
  }
}
