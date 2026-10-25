package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.LecturaPunto;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.util.excel.HistoricoExcelExporter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Histórico de lecturas por cultivo y rango de fechas, con exportación a Excel (RF-08). */
@Service
public class HistoricoService {

    private final LecturaRepository lecturas;
    private final UmbralRepository umbrales;
    private final CultivoRepository cultivos;

    public HistoricoService(LecturaRepository lecturas, UmbralRepository umbrales,
                            CultivoRepository cultivos) {
        this.lecturas = lecturas;
        this.umbrales = umbrales;
        this.cultivos = cultivos;
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

    private static LecturaPunto toPunto(Lectura l, List<Umbral> umbralesCultivo) {
        Umbral umbral = umbralesCultivo.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor()
                        .equals(l.getSensor().getTipoSensor().getIdTipoSensor()))
                .findFirst()
                .orElse(null);
        return new LecturaPunto(
                HistoricoExcelExporter.FORMATO.format(l.getFechaHora()),
                l.getSensor().getTipoSensor().obtenerNombre(),
                l.getValor().doubleValue(),
                l.getSensor().getTipoSensor().getUnidadMedida(),
                l.estaFueraDeRango(umbral));
    }
}
