package com.utp.AgroPiuraIoT.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import java.time.Duration;
import java.time.LocalDateTime;
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
 * <p>
 * Los umbrales por dispositivo se resuelven con un cache de <strong>Guava</strong>
 * (expira a los 5 minutos): en un flujo continuo de telemetría evita repetir la
 * misma consulta de umbrales en cada lectura entrante. Se invalida al cambiar
 * umbrales o la asignación de dispositivos.
 */
@Service
public class AlertasService {

    private static final Logger log = LoggerFactory.getLogger(AlertasService.class);

    private final AlertaRepository alertas;
    private final NotificacionUsuarioRepository notificaciones;
    private final UsuarioCultivoRepository accesos;
    private final UsuarioRepository usuarios;
    private final AuditoriaService auditoria;

    /** Cache de umbrales por id de dispositivo: consulta cara que apenas cambia. */
    private final LoadingCache<Integer, List<Umbral>> umbralesPorDispositivo;

    public AlertasService(AlertaRepository alertas, UmbralRepository umbrales,
                          CultivoRepository cultivos, NotificacionUsuarioRepository notificaciones,
                          UsuarioCultivoRepository accesos, UsuarioRepository usuarios,
                          AuditoriaService auditoria) {
        this.alertas = alertas;
        this.notificaciones = notificaciones;
        this.accesos = accesos;
        this.usuarios = usuarios;
        this.auditoria = auditoria;
        this.umbralesPorDispositivo = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build(CacheLoader.from(idDispositivo ->
                        cultivos.findByDispositivosIdDispositivo(idDispositivo).stream()
                                .flatMap(c -> umbrales.findByCultivoIdCultivo(c.getIdCultivo()).stream())
                                .toList()));
    }

    /**
     * Evalúa la lectura contra el umbral del cultivo al que pertenece el sensor.
     * Si está fuera de rango, crea la alerta y notifica a los usuarios del cultivo.
     */
    @Transactional
    public Optional<Alerta> evaluar(Lectura lectura) {
        if (lectura == null || lectura.getSensor() == null
                || lectura.getSensor().getDispositivo() == null
                || lectura.getSensor().getTipoSensor() == null) {
            return Optional.empty();
        }
        Integer idDispositivo = lectura.getSensor().getDispositivo().getIdDispositivo();
        Integer idTipoSensor = lectura.getSensor().getTipoSensor().getIdTipoSensor();

        List<Umbral> umbralesDispositivo;
        try {
            umbralesDispositivo = umbralesPorDispositivo.getUnchecked(idDispositivo);
        } catch (RuntimeException e) {
            throw new IllegalStateException("No se pudieron cargar los umbrales del dispositivo "
                    + idDispositivo, e);
        }

        Optional<Umbral> umbral = umbralesDispositivo.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor().equals(idTipoSensor))
                .findFirst();

        if (umbral.isEmpty() || !lectura.estaFueraDeRango(umbral.get())) {
            return Optional.empty();
        }

        Alerta alerta = alertas.save(Alerta.generar(lectura, umbral.get()));
        notificar(accesos.findByCultivoIdCultivo(umbral.get().getCultivo().getIdCultivo()), alerta);
        log.warn("Alerta {} generada para lectura {} (valor {} fuera del rango)",
                alerta.getIdAlerta(), lectura.getIdLectura(), lectura.getValor());
        return Optional.of(alerta);
    }

    /** Invalida el cache de umbrales cuando se actualiza la configuración. */
    public void invalidarCacheUmbrales(Integer idDispositivo) {
        if (idDispositivo != null) {
            umbralesPorDispositivo.invalidate(idDispositivo);
            log.debug("Cache de umbrales invalidado para dispositivo {}", idDispositivo);
        }
    }

    @Transactional(readOnly = true)
    public List<Alerta> listar(Integer idCultivo) {
        return alertas.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(idCultivo);
    }

    /** Alertas de los cultivos a los que el usuario tiene acceso. */
    @Transactional(readOnly = true)
    public List<Alerta> listarPara(String correo) {
        return alertas.buscarPorUsuario(correo);
    }

    /**
     * Marca la alerta como atendida y guarda responsable, fecha y comentario.
     * Es idempotente: una alerta ya atendida no cambia de responsable ni de
     * fecha. La acción queda en la auditoría (por ahora, log estructurado).
     */
    @Transactional
    public void marcarAtendida(Integer idAlerta, String correoUsuario, String comentario) {
        Alerta alerta = alertas.findById(idAlerta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta", idAlerta));
        if (alerta.isAtendida()) {
            return;
        }
        Usuario usuario = usuarios.findByCorreo(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", correoUsuario));
        String limpio = comentario == null ? null : comentario.trim();
        if (limpio != null && limpio.isEmpty()) {
            limpio = null;
        }
        if (limpio != null && limpio.length() > 500) {
            throw new ReglaNegocioException(
                    "El comentario de atención no puede superar 500 caracteres");
        }
        alerta.marcarAtendida(usuario, LocalDateTime.now(), limpio);
        alertas.save(alerta);
        auditoria.registrar(usuario, "ATENDER_ALERTA", "alerta", idAlerta, limpio);
        log.info("Alerta {} atendida por {} (comentario: {})",
                idAlerta, correoUsuario, limpio == null ? "sin comentario" : limpio);
    }

    /** Notificaciones no leídas del usuario indicado (por correo). */
    @Transactional(readOnly = true)
    public List<NotificacionUsuario> notificacionesDe(String correo) {
        return notificaciones.findByUsuarioCorreoAndLeidoFalseOrderByIdNotificacionDesc(correo);
    }

    @Transactional(readOnly = true)
    public long contarNoLeidas(String correo) {
        return notificaciones.countByUsuarioCorreoAndLeidoFalse(correo);
    }

    /**
     * Marca una notificación como leída. Si no pertenece al usuario se responde
     * como inexistente, para no revelar notificaciones ajenas.
     */
    @Transactional
    public void marcarLeida(Integer idNotificacion, String correo) {
        NotificacionUsuario notificacion = notificaciones.findById(idNotificacion)
                .filter(n -> n.getUsuario().getCorreo().equals(correo))
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificación", idNotificacion));
        notificacion.marcarLeida();
        notificaciones.save(notificacion);
    }

    private void notificar(List<UsuarioCultivo> usuariosDelCultivo, Alerta alerta) {
        for (UsuarioCultivo acceso : usuariosDelCultivo) {
            NotificacionUsuario notificacion = new NotificacionUsuario();
            notificacion.setAlerta(alerta);
            notificacion.setUsuario(acceso.getUsuario());
            notificaciones.save(notificacion);
        }
    }
}
