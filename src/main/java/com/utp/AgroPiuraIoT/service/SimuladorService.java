package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simula una ronda de telemetría: una lectura por cada sensor de los nodos del
 * cultivo, evaluada por el motor de alertas como si llegara del ESP32.
 */
@Service
public class SimuladorService {

    private static final Logger log = LoggerFactory.getLogger(SimuladorService.class);

    /** Probabilidad (%) de generar un valor fuera de rango para ejercitar alertas. */
    private static final int PROBABILIDAD_ANOMALIA = 20;

    /** Rangos plausibles por variable cuando el cultivo aún no tiene umbral. */
    private static final Map<String, double[]> RANGOS_POR_DEFECTO = Map.of(
            "humedad_suelo", new double[] {20, 80},
            "temperatura_ambiente", new double[] {18, 34},
            "humedad_ambiente", new double[] {40, 90},
            "ph_suelo", new double[] {5.5, 7.5});

    private final CultivoRepository cultivos;
    private final SensorRepository sensores;
    private final LecturaRepository lecturas;
    private final UmbralRepository umbrales;
    private final AlertasService alertas;

    public SimuladorService(CultivoRepository cultivos,
                            SensorRepository sensores,
                            LecturaRepository lecturas,
                            UmbralRepository umbrales,
                            AlertasService alertas) {
        this.cultivos = cultivos;
        this.sensores = sensores;
        this.lecturas = lecturas;
        this.umbrales = umbrales;
        this.alertas = alertas;
    }

    /** @return número de lecturas generadas (0 si el cultivo no tiene nodos con sensores) */
    @Transactional
    public int simular(Integer idCultivo) {
        Cultivo cultivo = cultivos.findById(idCultivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cultivo", idCultivo));

        List<Umbral> reglas = umbrales.findByCultivoIdCultivo(idCultivo);
        int total = 0;

        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            if (!dispositivo.estaActivo()) {
                continue;
            }
            for (Sensor sensor : sensores.findByDispositivoIdDispositivo(dispositivo.getIdDispositivo())) {
                Lectura lectura = lecturas.save(sensor.registrarLectura(generarValor(sensor, reglas)));
                alertas.evaluar(lectura);
                total++;
            }
        }
        log.info("Simulación del cultivo {}: {} lecturas", idCultivo, total);
        return total;
    }

    private BigDecimal generarValor(Sensor sensor, List<Umbral> reglas) {
        Umbral regla = reglas.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor()
                        .equals(sensor.getTipoSensor().getIdTipoSensor()))
                .findFirst()
                .orElse(null);

        double min;
        double max;
        if (regla != null && regla.getValorMinimo() != null && regla.getValorMaximo() != null) {
            min = regla.getValorMinimo().doubleValue();
            max = regla.getValorMaximo().doubleValue();
        } else {
            double[] rango = RANGOS_POR_DEFECTO.getOrDefault(
                    sensor.getTipoSensor().getNombreTipo(), new double[] {0, 100});
            min = rango[0];
            max = rango[1];
        }

        ThreadLocalRandom azar = ThreadLocalRandom.current();
        double valor;
        if (regla != null && azar.nextInt(100) < PROBABILIDAD_ANOMALIA) {
            double delta = Math.max(1.0, (max - min) * 0.2);
            valor = azar.nextBoolean() ? min - delta : max + delta;
        } else {
            valor = min + (max - min) * azar.nextDouble();
        }
        // Las variables medidas no son negativas (humedad, pH, temperatura en Piura).
        return BigDecimal.valueOf(Math.max(0, valor)).setScale(2, RoundingMode.HALF_UP);
    }
}
