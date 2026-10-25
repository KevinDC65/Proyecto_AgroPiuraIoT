package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de alertas (RF-05): al llegar una lectura, la compara contra el umbral
 * configurado para la variable del sensor y genera la alerta con notificaciones
 * para cada usuario con acceso al cultivo.
 */
@Service
public class AlertasService {

    private static final Logger log = LoggerFactory.getLogger(AlertasService.class);

    private final AlertaRepository alertas;
    private final UmbralRepository umbrales;
    private final CultivoRepository cultivos;
    private final NotificacionUsuarioRepository notificaciones;
    private final UsuarioCultivoRepository accesos;

    public AlertasService(AlertaRepository alertas, UmbralRepository umbrales,
                          CultivoRepository cultivos, NotificacionUsuarioRepository notificaciones,
                          UsuarioCultivoRepository accesos) {
        this.alertas = alertas;
        this.umbrales = umbrales;
        this.cultivos = cultivos;
        this.notificaciones = notificaciones;
        this.accesos = accesos;
    }

    /**
     * Evalúa la lectura contra el umbral del cultivo al que pertenece el sensor.
     * Si está fuera de rango, crea la alerta y notifica a los usuarios del cultivo.
     */
    @Transactional
    public Optional<Alerta> evaluar(Lectura lectura) {
        if (lectura == null || lectura.getSensor() == null) {
            return Optional.empty();
        }
        Integer idDispositivo = lectura.getSensor().getDispositivo().getIdDispositivo();
        Integer idTipoSensor = lectura.getSensor().getTipoSensor().getIdTipoSensor();

        Optional<Umbral> umbral = cultivos.findByDispositivosIdDispositivo(idDispositivo).stream()
                .flatMap(cultivo -> umbrales.findByCultivoIdCultivo(cultivo.getIdCultivo()).stream())
                .filter(u -> u.getTipoSensor().getIdTipoSensor().equals(idTipoSensor))
                .findFirst();

        if (umbral.isEmpty() || !lectura.estaFueraDeRango(umbral.get())) {
            return Optional.empty();
        }

        Alerta alerta = alertas.save(Alerta.generar(lectura, umbral.get()));
        notificar(accesos.findByCultivoIdCultivo(
                umbral.get().getCultivo().getIdCultivo()), alerta);
        log.warn("Alerta {} generada para lectura {} (valor {} fuera del rango)",
                alerta.getIdAlerta(), lectura.getIdLectura(), lectura.getValor());
        return Optional.of(alerta);
    }

    @Transactional(readOnly = true)
    public List<Alerta> listar(Integer idCultivo) {
        return alertas.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(idCultivo);
    }

    private void notificar(List<com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo> usuariosDelCultivo,
                           Alerta alerta) {
        for (com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo acceso : usuariosDelCultivo) {
            NotificacionUsuario notificacion = new NotificacionUsuario();
            notificacion.setAlerta(alerta);
            notificacion.setUsuario(acceso.getUsuario());
            notificaciones.save(notificacion);
        }
    }
}
