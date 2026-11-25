package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.PlantillaUmbral;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.PlantillaUmbralRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.util.NormalizadorCultivo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plantillas de umbrales por tipo de cultivo (B2). */
@Service
public class PlantillaUmbralService {

    private static final Logger log = LoggerFactory.getLogger(PlantillaUmbralService.class);

    private final PlantillaUmbralRepository plantillas;
    private final UmbralRepository umbrales;

    public PlantillaUmbralService(PlantillaUmbralRepository plantillas,
                                  UmbralRepository umbrales) {
        this.plantillas = plantillas;
        this.umbrales = umbrales;
    }

    /** Plantillas de referencia para un tipo de cultivo, ya normalizado. */
    @Transactional(readOnly = true)
    public List<PlantillaUmbral> para(String tipoCultivo) {
        String tipo = NormalizadorCultivo.normalizar(tipoCultivo);
        if (tipo.isBlank()) {
            return List.of();
        }
        return plantillas.findByTipoCultivoOrderByTipoSensorNombreTipo(tipo);
    }

    /**
     * Copia a umbrales del cultivo las plantillas que todavía no tenga. Es
     * idempotente: si el cultivo ya tiene un umbral para esa variable, no lo
     * sobrescribe (el administrador pudo haberlo ajustado).
     *
     * @return cantidad de umbrales creados
     */
    @Transactional
    public int aplicar(Cultivo cultivo) {
        if (cultivo == null || cultivo.getIdCultivo() == null) {
            return 0;
        }
        List<PlantillaUmbral> disponibles = para(cultivo.getTipoCultivo());
        if (disponibles.isEmpty()) {
            log.info("No hay plantilla de umbrales para el tipo de cultivo '{}'",
                    cultivo.getTipoCultivo());
            return 0;
        }
        Set<Integer> yaConfigurados = umbrales.findByCultivoIdCultivo(cultivo.getIdCultivo())
                .stream()
                .map(u -> u.getTipoSensor().getIdTipoSensor())
                .collect(Collectors.toSet());
        List<Umbral> nuevos = new ArrayList<>();
        for (PlantillaUmbral plantilla : disponibles) {
            Integer idTipo = plantilla.getTipoSensor().getIdTipoSensor();
            if (yaConfigurados.contains(idTipo)) {
                continue;
            }
            nuevos.add(plantilla.aUmbral(cultivo));
        }
        if (!nuevos.isEmpty()) {
            umbrales.saveAll(nuevos);
        }
        log.info("Plantilla '{}' aplicada al cultivo {}: {} umbral(es) creado(s)",
                NormalizadorCultivo.normalizar(cultivo.getTipoCultivo()),
                cultivo.getIdCultivo(), nuevos.size());
        return nuevos.size();
    }
}
