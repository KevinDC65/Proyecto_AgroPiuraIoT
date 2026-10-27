package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.LecturaPunto;
import com.utp.AgroPiuraIoT.model.dto.ResumenVariable;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.util.excel.HistoricoExcelExporter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico de lecturas por cultivo y rango de fechas, con exportación a Excel
 * (RF-08).
 */
@Service
public class HistoricoService {

    private final LecturaRepository lecturas;
    private final UmbralRepository umbrales;

    public HistoricoService(LecturaRepository lecturas, UmbralRepository umbrales,
            CultivoRepository cultivos) {
        this.lecturas = lecturas;
        this.umbrales = umbrales;
    }

    /**
     * Devuelve las lecturas del cultivo dentro del rango indicado. Si no se pasa
     * rango, incluye todo el histórico registrado.
     */
    @Transactional(readOnly = true)
    public List<Lectura> consultar(Integer idCultivo, LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null) {
            return lecturas.buscarPorCultivoYRango(
                    idCultivo,
                    desde.atStartOfDay(),
                    hasta.atTime(LocalTime.MAX));
        }
        return lecturas.buscarPorCultivo(idCultivo);
    }

    @Transactional(readOnly = true)
    public List<Umbral> umbralesDelCultivo(Integer idCultivo) {
        return umbrales.findByCultivoIdCultivo(idCultivo);
    }

    @Transactional(readOnly = true)
    public byte[] exportarExcel(Integer idCultivo, LocalDate desde, LocalDate hasta)
            throws IOException {
        List<Lectura> datos = consultar(idCultivo, desde, hasta);
        return HistoricoExcelExporter.exportar(datos, umbralesDelCultivo(idCultivo));
    }

    /** Puntos listos para el gráfico del dashboard histórico. */
    @Transactional(readOnly = true)
    public List<LecturaPunto> puntos(Integer idCultivo, LocalDate desde, LocalDate hasta) {
        List<Umbral> umbralesCultivo = umbralesDelCultivo(idCultivo);
        return consultar(idCultivo, desde, hasta).stream()
                .map(l -> toPunto(l, umbralesCultivo))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResumenVariable> resumen(Integer idCultivo, LocalDate desde, LocalDate hasta) {
        List<Lectura> datos = consultar(idCultivo, desde, hasta);
        List<Umbral> umbralesCultivo = umbralesDelCultivo(idCultivo);

        return datos.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getSensor().getTipoSensor().getIdTipoSensor()))
                .values().stream()
                .map(lecturasTipo -> {
                    TipoSensor tipo = lecturasTipo.get(0).getSensor().getTipoSensor();

                    List<BigDecimal> valores = lecturasTipo.stream()
                            .map(Lectura::getValor)
                            .filter(Objects::nonNull)
                            .toList();

                    BigDecimal minimo = valores.stream()
                            .min(Comparator.naturalOrder())
                            .orElse(null);

                    BigDecimal maximo = valores.stream()
                            .max(Comparator.naturalOrder())
                            .orElse(null);

                    BigDecimal promedio = valores.isEmpty()
                            ? null
                            : valores.stream()
                                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                                    .divide(BigDecimal.valueOf(valores.size()),
                                            2, RoundingMode.HALF_UP);

                    Umbral umbral = umbralDe(tipo, umbralesCultivo);
                    long fuera = lecturasTipo.stream()
                            .filter(l -> l.estaFueraDeRango(umbral))
                            .count();

                    return new ResumenVariable(
                            tipo.getNombreTipo(),
                            tipo.getUnidadMedida(),
                            minimo,
                            maximo,
                            promedio,
                            valores.size(),
                            fuera);
                })
                .sorted(Comparator.comparing(ResumenVariable::variable))
                .toList();
    }

    private static Umbral umbralDe(TipoSensor tipo, List<Umbral> umbralesCultivo) {
        return umbralesCultivo.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor()
                        .equals(tipo.getIdTipoSensor()))
                .findFirst()
                .orElse(null);
    }

    private static LecturaPunto toPunto(Lectura l, List<Umbral> umbralesCultivo) {
        TipoSensor tipo = l.getSensor().getTipoSensor();
        Umbral umbral = umbralDe(tipo, umbralesCultivo);

        return new LecturaPunto(
                HistoricoExcelExporter.FORMATO.format(l.getFechaHora()),
                tipo.obtenerNombre(),
                l.getValor().doubleValue(),
                tipo.getUnidadMedida(),
                l.estaFueraDeRango(umbral));
    }

}
