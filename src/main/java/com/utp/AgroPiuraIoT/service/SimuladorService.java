package com.utp.AgroPiuraIoT.service;

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
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimuladorService {

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

    @Transactional
    public int simular(Integer idCultivo) {
        Cultivo cultivo = cultivos.findById(idCultivo)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Cultivo no encontrado: " + idCultivo));

        List<Umbral> reglas = umbrales.findByCultivoIdCultivo(idCultivo);
        int total = 0;

        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            for (Sensor sensor : sensores.findByDispositivoIdDispositivo(
                    dispositivo.getIdDispositivo())) {

                BigDecimal valor = generarValor(sensor, reglas);
                Lectura lectura = sensor.registrarLectura(valor);
                lecturas.save(lectura);
                alertas.evaluar(lectura);
                total++;
            }
        }
        return total;
    }

    private BigDecimal generarValor(Sensor sensor, List<Umbral> reglas) {
        Umbral regla = reglas.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor()
                        .equals(sensor.getTipoSensor().getIdTipoSensor()))
                .findFirst()
                .orElse(null);

        if (regla == null || regla.getValorMinimo() == null
                || regla.getValorMaximo() == null) {
            return BigDecimal.valueOf(50);
        }

        double min = regla.getValorMinimo().doubleValue();
        double max = regla.getValorMaximo().doubleValue();
        ThreadLocalRandom azar = ThreadLocalRandom.current();

        double valor;
        if (azar.nextInt(100) < 20) { // 20% de probabilidad de generar alerta
            double delta = Math.max(1.0, (max - min) * 0.2);
            valor = azar.nextBoolean() ? min - delta : max + delta;
        } else {
            valor = min + (max - min) * azar.nextDouble();
        }

        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP);
    }
}