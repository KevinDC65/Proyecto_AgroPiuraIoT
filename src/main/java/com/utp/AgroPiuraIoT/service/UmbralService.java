package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UmbralService {

    private final UmbralRepository umbrales;
    private final CultivoRepository cultivos;
    private final TipoSensorRepository tipos;

    public UmbralService(UmbralRepository umbrales, CultivoRepository cultivos,
                         TipoSensorRepository tipos) {
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.tipos = tipos;
    }

    @Transactional(readOnly = true)
    public List<Umbral> listar(Integer idCultivo) {
        return umbrales.findByCultivoIdCultivo(idCultivo);
    }

    /**
     * Guarda el umbral de una variable para el cultivo. Si ya existe un umbral
     * para el mismo tipo de sensor en el cultivo, lo actualiza en lugar de duplicarlo.
     */
    @Transactional
    public Umbral guardar(Integer idCultivo, UmbralForm form) {
        validar(form);
        Cultivo cultivo = buscarCultivo(idCultivo);
        TipoSensor tipoSensor = tipos.findById(form.idTipoSensor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de sensor no encontrado: " + form.idTipoSensor()));

        Umbral umbral = umbrales.findByCultivoIdCultivo(idCultivo).stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor().equals(form.idTipoSensor()))
                .findFirst()
                .orElseGet(Umbral::new);
        umbral.setCultivo(cultivo);
        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(form.valorMinimo());
        umbral.setValorMaximo(form.valorMaximo());
        return umbrales.save(umbral);
    }

    @Transactional
    public void eliminar(Integer idCultivo, Integer idUmbral) {
        Umbral umbral = umbrales.findById(idUmbral)
                .orElseThrow(() -> new IllegalArgumentException("Umbral no encontrado: " + idUmbral));
        if (!umbral.getCultivo().getIdCultivo().equals(idCultivo)) {
            throw new IllegalArgumentException("El umbral no pertenece al cultivo indicado");
        }
        umbrales.delete(umbral);
    }

    private void validar(UmbralForm form) {
        if (form.valorMinimo().compareTo(form.valorMaximo()) > 0) {
            throw new IllegalArgumentException(
                    "El valor mínimo no puede ser mayor que el valor máximo");
        }
    }

    private Cultivo buscarCultivo(Integer idCultivo) {
        return cultivos.findById(idCultivo)
                .orElseThrow(() -> new IllegalArgumentException("Cultivo no encontrado: " + idCultivo));
    }
}
