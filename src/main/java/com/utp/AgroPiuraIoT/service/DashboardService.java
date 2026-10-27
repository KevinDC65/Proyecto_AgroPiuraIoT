package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.DashboardResumen;
import com.utp.AgroPiuraIoT.model.dto.DashboardResumen.CultivoEstado;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private final CultivoRepository cultivos;
    private final SensorRepository sensores;
    private final LecturaRepository lecturas;
    private final AlertaRepository alertas;

    public DashboardService(CultivoRepository cultivos, SensorRepository sensores,
                            LecturaRepository lecturas, AlertaRepository alertas) {
        this.cultivos = cultivos;
        this.sensores = sensores;
        this.lecturas = lecturas;
        this.alertas = alertas;
    }

    @Transactional(readOnly = true)
    public DashboardResumen resumen() {
        List<Cultivo> todos = cultivos.findAll();
        List<CultivoEstado> estados = new ArrayList<>();
        List<BigDecimal> humedades = new ArrayList<>();
        List<BigDecimal> temperaturas = new ArrayList<>();
        LocalDateTime ultimaFecha = null;

        for (Cultivo cultivo : todos) {
            BigDecimal humedad = ultimaLectura(cultivo, "humedad_suelo");
            BigDecimal temperatura = ultimaLectura(cultivo, "temperatura_ambiente");
            if (humedad != null) {
                humedades.add(humedad);
            }
            if (temperatura != null) {
                temperaturas.add(temperatura);
            }
            String estado = "SIN_DATOS";
            if (humedad != null || temperatura != null) {
                boolean conAlertas = alertas
                    .countByUmbralCultivoIdCultivoAndAtendidaFalse(cultivo.getIdCultivo()) > 0;
                estado = conAlertas ? "ALERTA" : "NORMAL";
            }
            estados.add(new CultivoEstado(
                cultivo.getIdCultivo(),
                cultivo.getNombre(),
                humedad,
                temperatura,
                estado,
                cultivo.getLatitud(),
                cultivo.getLongitud()));
            LocalDateTime fechaCultivo = ultimaFecha(cultivo);
            if (fechaCultivo != null && (ultimaFecha == null || fechaCultivo.isAfter(ultimaFecha))) {
                ultimaFecha = fechaCultivo;
            }
        }

        return new DashboardResumen(todos.size(), alertas.countByAtendidaFalse(),
                promedio(humedades), promedio(temperaturas),
                textoSincronizacion(ultimaFecha), estados);
    }

    private LocalDateTime ultimaFecha(Cultivo cultivo) {
        LocalDateTime ultima = null;
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            for (Sensor sensor : sensores.findByDispositivoIdDispositivo(dispositivo.getIdDispositivo())) {
                List<Lectura> recientes =
                        lecturas.findBySensorIdSensorOrderByFechaHoraDesc(sensor.getIdSensor());
                if (!recientes.isEmpty() && recientes.get(0).getFechaHora() != null
                        && (ultima == null || recientes.get(0).getFechaHora().isAfter(ultima))) {
                    ultima = recientes.get(0).getFechaHora();
                }
            }
        }
        return ultima;
    }

    private String textoSincronizacion(LocalDateTime fecha) {
        if (fecha == null) {
            return "Sin datos";
        }
        long minutos = Duration.between(fecha, LocalDateTime.now()).toMinutes();
        if (minutos < 1) {
            return "hace un momento";
        }
        if (minutos < 60) {
            return "hace " + minutos + " min";
        }
        long horas = minutos / 60;
        if (horas < 24) {
            return "hace " + horas + " h";
        }
        return "hace " + (horas / 24) + " días";
    }

    private BigDecimal ultimaLectura(Cultivo cultivo, String tipoSensor) {
        BigDecimal ultima = null;
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            for (Sensor sensor : sensores.findByDispositivoIdDispositivo(dispositivo.getIdDispositivo())) {
                if (sensor.getTipoSensor() != null
                        && tipoSensor.equalsIgnoreCase(sensor.getTipoSensor().getNombreTipo())) {
                    List<Lectura> recientes =
                            lecturas.findBySensorIdSensorOrderByFechaHoraDesc(sensor.getIdSensor());
                    if (!recientes.isEmpty() && recientes.get(0).getValor() != null) {
                        ultima = recientes.get(0).getValor();
                    }
                }
            }
        }
        return ultima;
    }

    private BigDecimal promedio(List<BigDecimal> valores) {
        if (valores == null || valores.isEmpty()) {
            return null;
        }
        BigDecimal suma = valores.stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, (a, b) -> a.add(b));
        return suma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }
}
