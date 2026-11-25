# 13 · Reglas agronómicas

Reglas con las que AgroPiura IoT decide cuándo una lectura es un problema para el cultivo,
qué tan grave es y qué se le recomienda al agricultor. Este documento es el resultado del
issue **B3 · Validación agronómica** (#12) y se apoya en lo construido en B1 (#2,
episodios y severidad) y B2 (#7, plantillas y recomendaciones).

> **Estado (25/11/2026):** las secciones 1 y 5 describen lo que ya existe o está en la rama
> de B2. Las secciones 2, 3 y 4 describen la regla **acordada** que B1 debe implementar; se
> marcan con *Pendiente B1* hasta que esté en `main`. Al integrarse, actualizar este
> documento con los valores finales.

---

## 1. Umbrales por cultivo (plantillas de B2)

Al crear un cultivo se puede aplicar la plantilla de su tipo; el administrador la ajusta
después en **Umbrales**. Los valores viven en `src/main/resources/plantillas-umbral.csv`
(rama `feat/B2-plantillas-recomendaciones`), se siembran en la tabla `plantilla_umbral` y
cada fila guarda su fuente.

| Cultivo | Temperatura (°C) | Humedad ambiente (%) | pH de suelo | Fuente |
|---|:---:|:---:|:---:|---|
| Mango | 16 – 32 | 65 – 100 ¹ | 5,5 – 7,5 | MIDAGRI, Ficha Técnica N° 08 (2019) |
| Limón | 23 – 30 | 65 – 70 | 6,0 – 7,0 | MIDAGRI, Ficha Técnica N° 14 (2019) |
| Banano / plátano | 25 – 30 | 80 – 100 ¹ | 5,0 – 7,0 | MIDAGRI, Ficha Técnica N° 16 (2015); pH: FAO Ecocrop, *Musa* sp. (ficha 2483) |
| Arroz | 20 – 35 | 65 – 100 ¹ | 5,5 – 7,0 | MIDAGRI, Ficha Técnica N° 04 (2019); pH: FAO Ecocrop, *Oryza sativa* (ficha 1574) |
| Papa | 14 – 25 | 60 – 80 | 5,5 – 6,5 | MIDAGRI, Ficha Técnica N° 01 (2019) |

¹ La fuente solo da un mínimo (por ejemplo "> 65 %"); se usa 100 % como máximo porque es el
límite físico del sensor.

**Notas de criterio:**
- **Temperatura:** las fichas dan óptimos por etapa fenológica; la plantilla usa la
  *envolvente* (del menor mínimo al mayor máximo). Es un rango deliberadamente amplio: evita
  alertas en una etapa normal, a cambio de no detectar desvíos propios de una sola etapa.
- **Humedad de suelo:** no tiene plantilla porque el valor correcto depende de la textura
  del suelo (capacidad de campo y punto de marchitez). El administrador la configura a mano.
  En la validación se usó **30 – 70 %** como referencia de suelo franco.
- **Fuentes por verificar:** los valores y enlaces vienen de B2. Antes de la entrega,
  revisar cada uno contra el PDF original (columna `url_fuente` del CSV) y anotar aquí la
  página o tabla:

| Fuente | Enlace | Revisado (página / tabla) |
|---|---|---|
| MIDAGRI Ficha N° 08 · Mango | <https://repositorio.midagri.gob.pe/handle/20.500.13036/453> | ☐ |
| MIDAGRI Ficha N° 14 · Limonero | <https://repositorio.midagri.gob.pe/handle/20.500.13036/610> | ☐ |
| MIDAGRI Ficha N° 16 · Plátano | <https://www.midagri.gob.pe/portal/download/pdf/ais-2015/ficha16-platano.pdf> | ☐ |
| MIDAGRI Ficha N° 04 · Arroz | <https://repositorio.midagri.gob.pe/handle/20.500.13036/230> | ☐ |
| MIDAGRI Ficha N° 01 · Papa | <https://repositorio.midagri.gob.pe/handle/20.500.13036/228> | ☐ |
| FAO Ecocrop · *Musa* sp. | <https://ecocrop.apps.fao.org/ecocrop/srv/en/dataSheet?id=2483> | ☐ |
| FAO Ecocrop · *Oryza sativa* | <https://ecocrop.apps.fao.org/ecocrop/srv/en/dataSheet?id=1574> | ☐ |

## 2. Lecturas imposibles (sensor dañado) · *Pendiente B1*

Antes de comparar con el umbral del cultivo, la lectura se compara con el **rango físico**
del sensor (issue A1). Fuera de ese rango no es un problema del cultivo sino del sensor:

| Variable | Rango físico aceptado |
|---|:---:|
| Humedad de suelo | 0 – 100 % |
| Humedad ambiente | 0 – 100 % |
| pH de suelo | 0 – 14 |
| Temperatura ambiente | −10 – 60 °C (rango del sensor del nodo de referencia; confirmar con A1) |

Regla: la lectura imposible **no genera alerta agronómica** ni notifica al agricultor como
si su cultivo tuviera un problema; se registra como falla del sensor. **Hoy no se cumple:**
un pH de 20 genera una alerta normal (escenario 4, sección 6).

## 3. Severidad · *Pendiente B1*

La severidad mide qué tan lejos del rango está la lectura, como porcentaje del ancho del
rango del umbral:

```
desvío = (mínimo − valor) / (máximo − mínimo)   si está bajo el mínimo
desvío = (valor − máximo) / (máximo − mínimo)   si está sobre el máximo
```

| Severidad | Desvío | Ejemplo (humedad de suelo 30 – 70 %, ancho 40) |
|---|:---:|---|
| Leve | hasta 10 % | 26 – 29 % o 71 – 74 % |
| Moderada | más de 10 % y hasta 30 % | 18 – 25 % o 75 – 82 % |
| Crítica | más de 30 % | menos de 18 % o más de 82 % |

Porcentajes propuestos por B3 tras revisar los escenarios: con 10 / 30 una temperatura de
33 °C en mango (rango 16 – 32, desvío 6 %) es **leve** y 38 °C (desvío 38 %) es **crítica**,
lo que coincide con el criterio agronómico. Si B1 implementa otros valores, registrar aquí
los finales y el motivo.

## 4. Episodios · *Pendiente B1*

Un **episodio** agrupa lecturas consecutivas fuera de rango de la misma variable en el
mismo nodo, para no enviar una alerta por cada lectura:

- **Se abre** con la primera lectura fuera de rango (una sola notificación por usuario).
- **Se actualiza** con cada lectura fuera de rango siguiente: la severidad del episodio es
  la mayor alcanzada.
- **Se cierra** con la primera lectura que vuelve al rango, guardando la fecha de cierre.
- Una lectura **atrasada** (con fecha anterior al cierre) se guarda pero no reabre el
  episodio (regla de A3).

**Hoy** el motor genera una alerta por cada lectura fuera de rango (escenario 3: la ola de
calor produce 4 alertas en lugar de 1 episodio).

## 5. Recomendaciones (B2)

Cada alerta muestra una recomendación según la **variable** y la **dirección** (bajo el
mínimo / sobre el máximo). El catálogo vive en `src/main/resources/recomendaciones.properties`
(B2), no en el código. Contenido orientativo esperado:

| Variable | Bajo el mínimo | Sobre el máximo |
|---|---|---|
| Humedad de suelo | Regar; revisar que el sistema de riego funcione | Suspender el riego; revisar drenaje y encharcamiento |
| Temperatura | Proteger de frío (riego ligero, cobertura) | Regar en horas frescas; sombreo o mulch; evitar labores al mediodía |
| Humedad ambiente | Aumentar frecuencia de riego; vigilar estrés hídrico | Vigilar hongos; mejorar ventilación (poda) |
| pH de suelo | Análisis de suelo; evaluar encalado | Análisis de suelo; evaluar enmienda acidificante o materia orgánica |

Si OpenWeatherMap pronostica lluvia (probabilidad ≥ 50 %), la recomendación de riego lo
advierte. Sin clave de API la recomendación se muestra igual, sin el aviso.

Las recomendaciones son orientativas: no reemplazan la visita de un técnico agrónomo.

## 6. Validación con escenarios

Prueba automática: `src/test/java/com/utp/AgroPiuraIoT/ValidacionAgronomicaTest.java`.
Cultivo de mango (plantilla de la sección 1) con humedad de suelo 30 – 70 %, una lectura
por hora desde el 02/11/2026 06:00.

```powershell
.\mvnw.cmd test -Dtest=ValidacionAgronomicaTest
```

| # | Escenario | Lecturas | Esperado | Resultado hoy |
|---|---|---|---|---|
| 1 | Sequía | Humedad de suelo 45 → 38 → 32 → 28 → 22 → 18 % | Sin alerta hasta 32 %; alerta desde 28 %; notificación al administrador; recomendación de regar; severidad leve → moderada | Alertas y notificación: se cumple. Severidad y recomendación: pendiente B1 / B2 |
| 2 | Exceso de riego | Humedad de suelo 60 → 68 → 72 → 85 → 95 → 99 % | Alerta desde 72 %; recomendación de suspender riego; severidad hasta crítica | Alertas: se cumple. Severidad y recomendación: pendiente B1 / B2 |
| 3 | Ola de calor | Temperatura 28 → 31 → 33 → 36 → 38 → 34 → 30 → 27 °C (y humedad del aire 45 %) | Un episodio abierto en 33 °C y cerrado en 30 °C; severidad máxima crítica (38 °C); alerta aparte por aire seco | Alertas fuera de rango y nada al volver: se cumple. **4 alertas en lugar de 1 episodio** (pendiente B1) |
| 4 | Sensor dañado | pH 20,00 y temperatura −50 °C | Sin alerta agronómica; registrado como falla de sensor | **No se cumple:** genera alertas normales. Prueba desactivada hasta B1 + rangos físicos de A1 |
| 5 | Nodo caído | 3 lecturas y luego silencio | Nodo "sin comunicación" y aviso al administrador | **No existe aún:** depende de A2. Prueba desactivada |

Al integrarse B1, B2 y A2: activar las pruebas marcadas con `@Disabled`, completarlas con
las APIs reales, volver a ejecutar y actualizar la columna "Resultado hoy" y el comentario
del issue #12.
