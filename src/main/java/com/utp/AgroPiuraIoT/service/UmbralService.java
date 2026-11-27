package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.RangoFisico;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Umbrales por cultivo y variable (RF-03). */
@Service
public class UmbralService {

    private final UmbralRepository umbrales;
    private final CultivoRepository cultivos;
    private final TipoSensorRepository tipos;
    private final AlertasService alertasService;

    public UmbralService(UmbralRepository umbrales, CultivoRepository cultivos,
                         TipoSensorRepository tipos, AlertasService alertasService) {
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.tipos = tipos;
        this.alertasService = alertasService;
    }

    @Transactional(readOnly = true)
    public List<Umbral> listar(Integer idCultivo) {
        return umbrales.findByCultivoIdCultivo(idCultivo);
    }

    @Transactional(readOnly = true)
    public List<TipoSensor> tiposSensor() {
        return tipos.findAll();
    }

    /**
     * Guarda el umbral de una variable para el cultivo. Si ya existe un umbral
     * para el mismo tipo de sensor en el cultivo, lo actualiza en lugar de duplicarlo.
     * Invalida el cache de umbrales del motor de alertas (Guava) para que las
     * evaluaciones siguientes usen el valor recién guardado.
     */
    @Transactional
    public Umbral guardar(Integer idCultivo, UmbralForm form) {
        validar(form);
        Cultivo cultivo = cultivos.findById(idCultivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cultivo", idCultivo));
        TipoSensor tipoSensor = tipos.findById(form.idTipoSensor())
                .orElseThrow(() -> new ReglaNegocioException(
                        "Tipo de sensor no reconocido: " + form.idTipoSensor()));
        validarRangoFisico(tipoSensor, form);

        Umbral umbral = umbrales
                .findByCultivoIdCultivoAndTipoSensorIdTipoSensor(idCultivo, form.idTipoSensor())
                .orElseGet(Umbral::new);
        umbral.setCultivo(cultivo);
        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(form.valorMinimo());
        umbral.setValorMaximo(form.valorMaximo());
        Umbral guardado = umbrales.save(umbral);
        invalidarCache(cultivo);
        return guardado;
    }

    /** Umbral del cultivo; uno de otro cultivo se trata como inexistente. */
    @Transactional(readOnly = true)
    public Umbral buscar(Integer idCultivo, Integer idUmbral) {
        return umbrales.findById(idUmbral)
                .filter(u -> u.getCultivo().getIdCultivo().equals(idCultivo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Umbral", idUmbral));
    }

    @Transactional
    public void eliminar(Integer idCultivo, Integer idUmbral) {
        Umbral umbral = buscar(idCultivo, idUmbral);
        umbrales.delete(umbral);
        invalidarCache(umbral.getCultivo());
    }

    /** Deja en blanco el cache del motor de alertas para los dispositivos del cultivo. */
    private void invalidarCache(Cultivo cultivo) {
        for (DispositivoIoT dispositivo : cultivo.getDispositivos()) {
            alertasService.invalidarCacheUmbrales(dispositivo.getIdDispositivo());
        }
    }

    private void validar(UmbralForm form) {
        if (form.valorMinimo() == null || form.valorMaximo() == null || form.idTipoSensor() == null) {
            throw new ReglaNegocioException("Tipo de sensor, mínimo y máximo son obligatorios");
        }
        if (form.valorMinimo().compareTo(form.valorMaximo()) > 0) {
            throw new ReglaNegocioException("El valor mínimo no puede ser mayor que el valor máximo");
        }
    }

    private void validarRangoFisico(TipoSensor tipoSensor, UmbralForm form) {
        RangoFisico.de(tipoSensor.getNombreTipo()).ifPresent(rango -> {
            if (!rango.admite(form.valorMinimo()) || !rango.admite(form.valorMaximo())) {
                String nombreLegible = tipoSensor.getNombreTipo().replace('_', ' ');
                throw new ReglaNegocioException(
                        "Los valores para " + nombreLegible
                                + " deben estar dentro del rango físico permitido ("
                                + rango.describir() + ")");
            }
        });
    }
}
