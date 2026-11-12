package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UmbralService {

    private final UmbralRepository umbrales;
    private final CultivoRepository cultivos;
    private final TipoSensorRepository tipos;
    private final AlertasService alertasService;
    private final AlertaRepository alertas;
    private final NotificacionUsuarioRepository notificaciones;

    public UmbralService(UmbralRepository umbrales, CultivoRepository cultivos,
                         TipoSensorRepository tipos, AlertasService alertasService,
                         AlertaRepository alertas, NotificacionUsuarioRepository notificaciones) {
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.tipos = tipos;
        this.alertasService = alertasService;
        this.alertas = alertas;
        this.notificaciones = notificaciones;
    }

    @Transactional(readOnly = true)
    public List<Umbral> listar(Integer idCultivo) {
        return umbrales.findByCultivoIdCultivo(idCultivo);
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
        Cultivo cultivo = buscarCultivo(idCultivo);
        TipoSensor tipoSensor = tipos.findById(form.idTipoSensor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de sensor no encontrado: " + form.idTipoSensor()));

        Umbral umbral = umbrales.findByCultivoIdCultivoAndTipoSensorIdTipoSensor(idCultivo, form.idTipoSensor())
                .orElseGet(Umbral::new);

        umbral.setCultivo(cultivo);
        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(form.valorMinimo());
        umbral.setValorMaximo(form.valorMaximo());
        Umbral guardado = umbrales.save(umbral);
        invalidarCache(cultivo);
        return guardado;
    }

    /**
     * Actualiza un umbral existente identificado por su id.
     * Valida que no exista colisión con otro umbral para el mismo tipo de sensor en el cultivo.
     */
    @Transactional
    public Umbral actualizar(Integer idCultivo, Integer idUmbral, UmbralForm form) {
        validar(form);
        Umbral umbral = buscar(idCultivo, idUmbral);
        TipoSensor tipoSensor = tipos.findById(form.idTipoSensor())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tipo de sensor no encontrado: " + form.idTipoSensor()));

        Optional<Umbral> existente = umbrales.findByCultivoIdCultivoAndTipoSensorIdTipoSensor(idCultivo, form.idTipoSensor());
        if (existente.isPresent() && !existente.get().getIdUmbral().equals(idUmbral)) {
            throw new IllegalArgumentException("Ya existe un umbral configurado para este tipo de sensor en el cultivo");
        }

        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(form.valorMinimo());
        umbral.setValorMaximo(form.valorMaximo());
        Umbral guardado = umbrales.save(umbral);
        invalidarCache(umbral.getCultivo());
        return guardado;
    }

    @Transactional(readOnly = true)
    public Umbral buscar(Integer idCultivo, Integer idUmbral) {
        Umbral umbral = umbrales.findById(idUmbral)
                .orElseThrow(() -> new IllegalArgumentException("Umbral no encontrado: " + idUmbral));
        if (!umbral.getCultivo().getIdCultivo().equals(idCultivo)) {
            throw new IllegalArgumentException("El umbral no pertenece al cultivo indicado");
        }
        return umbral;
    }

    @Transactional
    public void eliminar(Integer idCultivo, Integer idUmbral) {
        Umbral umbral = buscar(idCultivo, idUmbral);

        // Eliminar alertas y notificaciones asociadas al umbral para evitar violación de FK
        List<Alerta> alertasDelUmbral = alertas.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(idCultivo).stream()
                .filter(a -> a.getUmbral().getIdUmbral().equals(idUmbral))
                .toList();
        for (Alerta a : alertasDelUmbral) {
            List<NotificacionUsuario> notifs = notificaciones.findByAlertaIdAlerta(a.getIdAlerta());
            if (!notifs.isEmpty()) {
                notificaciones.deleteAll(notifs);
            }
        }
        if (!alertasDelUmbral.isEmpty()) {
            alertas.deleteAll(alertasDelUmbral);
        }

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
        if (form == null) {
            throw new IllegalArgumentException("El formulario de umbral no puede ser nulo");
        }
        if (form.idTipoSensor() == null) {
            throw new IllegalArgumentException("El tipo de sensor es obligatorio");
        }
        if (form.valorMinimo() == null || form.valorMaximo() == null) {
            throw new IllegalArgumentException("Los valores mínimo y máximo son obligatorios");
        }
        if (form.valorMinimo().compareTo(BigDecimal.ZERO) < 0 || form.valorMaximo().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Los valores de umbral no pueden ser negativos");
        }
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
