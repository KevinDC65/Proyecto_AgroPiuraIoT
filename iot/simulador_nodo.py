#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
AgroPiura IoT - Simulador de uno o varios nodos ESP32 (A2).

Envía lecturas reales a POST /api/telemetria autenticándose con el token Bearer
del nodo. Compatible con Python 3 y Python 2.7 sin dependencias externas.

Ejemplos:
  python iot/simulador_nodo.py --token ap_1_xxxx
  python iot/simulador_nodo.py --token ap_1_xxxx --intervalo 10 --ciclos 5
  python iot/simulador_nodo.py --token ap_1_xxxx --anomalia --ciclos 3
"""

from __future__ import print_function
import argparse
from datetime import datetime
import json
import random
import sys
import time

try:
    import urllib.request as urlreq
    import urllib.error as urlerr
except ImportError:
    import urllib2 as urlreq
    import urllib2 as urlerr


def generar_lote(forzar_anomalia):
    if forzar_anomalia:
        humedad_suelo = round(random.uniform(8.0, 18.0), 2)
        temperatura = round(random.uniform(37.5, 43.0), 2)
        humedad_amb = round(random.uniform(20.0, 35.0), 2)
        ph_suelo = round(random.uniform(4.2, 4.9), 2)
    else:
        humedad_suelo = round(random.uniform(42.0, 68.0), 2)
        temperatura = round(random.uniform(22.0, 31.0), 2)
        humedad_amb = round(random.uniform(50.0, 75.0), 2)
        ph_suelo = round(random.uniform(6.0, 7.2), 2)

    return {
        "lecturas": [
            {"tipoSensor": "humedad_suelo", "valor": humedad_suelo},
            {"tipoSensor": "temperatura_ambiente", "valor": temperatura},
            {"tipoSensor": "humedad_ambiente", "valor": humedad_amb},
            {"tipoSensor": "ph_suelo", "valor": ph_suelo},
        ]
    }


def enviar_telemetria(url, token, payload):
    cuerpo = json.dumps(payload).encode("utf-8")
    req = urlreq.Request(
        url,
        data=cuerpo,
        headers={
            "Content-Type": "application/json",
            "Authorization": "Bearer " + token,
        },
    )
    try:
        resp = urlreq.urlopen(req, timeout=10)
        codigo = getattr(resp, "status", None) or resp.getcode()
        texto = resp.read().decode("utf-8", "replace")
        resp.close()
        return codigo, texto
    except urlerr.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")
    except urlerr.URLError as e:
        return 0, "Error de conexión: " + str(e.reason)


def main():
    parser = argparse.ArgumentParser(description="Simulador de nodos IoT para AgroPiura IoT")
    parser.add_argument(
        "--url",
        default="http://localhost:8080/api/telemetria",
        help="URL del endpoint de telemetría (default: http://localhost:8080/api/telemetria)",
    )
    parser.add_argument(
        "--token",
        action="append",
        required=True,
        help="Token Bearer del nodo (puedes repetir --token para simular varios nodos)",
    )
    parser.add_argument(
        "--intervalo",
        type=float,
        default=10.0,
        help="Segundos entre envíos (default: 10)",
    )
    parser.add_argument(
        "--ciclos",
        type=int,
        default=0,
        help="Cantidad de rondas de envío (0 = infinito hasta Ctrl+C)",
    )
    parser.add_argument(
        "--anomalia",
        action="store_true",
        help="Envía valores fuera de rango típico para ejercitar alertas por episodio",
    )
    args = parser.parse_args()

    ronda = 0
    try:
        while args.ciclos == 0 or ronda < args.ciclos:
            ronda += 1
            marca = datetime.now().strftime("%H:%M:%S")
            for idx, token in enumerate(args.token, 1):
                payload = generar_lote(args.anomalia)
                codigo, cuerpo = enviar_telemetria(args.url, token, payload)
                partes = token.split("_")
                etiqueta = partes[1] if len(partes) >= 3 and partes[0] == "ap" else "#%d" % idx
                print("[%s] Ronda %d - Nodo %s -> HTTP %d: %s" % (marca, ronda, etiqueta, codigo, cuerpo))
            if args.ciclos == 0 or ronda < args.ciclos:
                time.sleep(max(0.5, args.intervalo))
    except KeyboardInterrupt:
        print("\nSimulador detenido por el usuario.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
