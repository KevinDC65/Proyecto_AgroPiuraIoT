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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resumen del dashboard y del mapa (RF-04), limitado a los cultivos del usuario. */
@Service
public class DashboardService {

    static final String HUMEDAD_SUELO = "humedad_suelo";
    static final String TEMPERATURA_AMBIENTE = "temperatura_ambiente";

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
    public DashboardResumen resumen(String correo) {
        List<Cultivo> visibles = cultivos.buscarPorUsuario(correo);
        List<CultivoEstado> estados = new ArrayList<>();
        List<BigDecimal> humedades = new ArrayList<>();
        List<BigDecimal> temperaturas = new ArrayList<>();
        LocalDateTime ultimaFecha = null;

        for (Cultivo cultivo : visibles) {
            UltimasLecturas ultimas = ultimasLecturas(cultivo);
            if (ultimas.humedad() != null) {
                humedades.add(ultimas.humedad().getValor());
            }
            if (ultimas.temperatura() != null) {
                temperaturas.add(ultimas.temperatura().getValor());
            }
            String estado = "SIN_DATOS";
            if (ultimas.humedad() != null || ultimas.temperatura() != null) {
                boolean conAlertas = alertas
                        .countByUmbralCultivoIdCultivoAndAtendidaFalse(cultivo.getIdCultivo()) > 0;
                estado = conAlertas ? "ALERTA" : "NORMAL";
            }
            estados.add(new CultivoEstado(
                    cultivo.getIdCultivo(),
                    cultivo.getNombre(),
                    ultimas.humedad() == null ? null : ultimas.humedad().getValor(),
                    ultimas.temperatura() == null ? null : ultimas.temperatura().getValor(),
                    estado,
                    cultivo.getLatitud(),
                    cultivo.getLongitud()));
            if (ultimas.fecha() != null && (ultimaFecha == null || ultimas.fecha().isAfter(ultimaFecha))) {
                ultimaFecha = ultimas.fecha();
            }
        }

        return new DashboardResumen(visibles.size(), alertas.contarActivasPorUsuario(correo),
                promedio(humedades), promedio(temperaturas),
                textoSincronizacion(ultimaFecha), estados);
    }

    /**
     * Recorre una sola vez los sensores del cultivo pidiendo solo su última
     * lectura (LIMIT 1), en vez de cargar el histórico completo de cada sensor.
     */
    private UltimasLecturas ultimasLecturas(Cultivo cultivo) {
        Lectura humedad = null;
        Lectura temperatura = null;
        LocalDateTime fecha = null;
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            for (Sensor sensor : sensores.findByDispositivoIdDispositivo(dispositivo.getIdDispositivo())) {
                Lectura ultima = lecturas.findFirstBySensorIdSensorOrderByFechaHoraDesc(sensor.getIdSensor())
                        .filter(l -> l.getValor() != null && l.getFechaHora() != null)
                        .orElse(null);
                if (ultima == null) {
                    continue;
                }
                if (fecha == null || ultima.getFechaHora().isAfter(fecha)) {
                    fecha = ultima.getFechaHora();
                }
                String tipo = sensor.getTipoSensor() == null ? "" : sensor.getTipoSensor().getNombreTipo();
                if (HUMEDAD_SUELO.equalsIgnoreCase(tipo) && masReciente(ultima, humedad)) {
                    humedad = ultima;
                } else if (TEMPERATURA_AMBIENTE.equalsIgnoreCase(tipo) && masReciente(ultima, temperatura)) {
                    temperatura = ultima;
                }
            }
        }
        return new UltimasLecturas(humedad, temperatura, fecha);
    }

    private static boolean masReciente(Lectura candidata, Lectura actual) {
        return actual == null || candidata.getFechaHora().isAfter(actual.getFechaHora());
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

    private BigDecimal promedio(List<BigDecimal> valores) {
        if (valores.isEmpty()) {
            return null;
        }
        BigDecimal suma = valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return suma.divide(BigDecimal.valueOf(valores.size()), 2, RoundingMode.HALF_UP);
    }

    private record UltimasLecturas(Lectura humedad, Lectura temperatura, LocalDateTime fecha) {
    }
}
