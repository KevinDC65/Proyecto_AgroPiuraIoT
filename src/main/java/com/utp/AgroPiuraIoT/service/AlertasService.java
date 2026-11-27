package com.utp.AgroPiuraIoT.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.RangoFisico;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de alertas por episodio y severidad (RF-05, B1): al llegar una lectura, la
 * compara contra el umbral configurado para la variable del sensor:
 * <ul>
 *   <li>Si está fuera de rango y ya existe un episodio abierto para el mismo umbral y
 *       dirección (por debajo/por encima), actualiza el episodio (valor extremo, cantidad
 *       de lecturas y severidad) sin duplicar alertas ni notificaciones.</li>
 *   <li>Si vuelve al rango permitido, cierra automáticamente los episodios abiertos.</li>
 *   <li>Calcula la severidad ({@code leve}, {@code moderada}, {@code critica}) según la
 *       desviación respecto al ancho del rango y escala de nivel si el episodio supera la
 *       duración configurada.</li>
 * </ul>
 * <p>
 * Los umbrales por dispositivo se resuelven con un cache de <strong>Guava</strong>
 * (expira a los 5 minutos) que se invalida al cambiar umbrales o asignaciones de nodos.
 */
@Service
public class AlertasService {

    private static final Logger log = LoggerFactory.getLogger(AlertasService.class);
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final AlertaRepository alertas;
    private final NotificacionUsuarioRepository notificaciones;
    private final UsuarioCultivoRepository accesos;

    private final BigDecimal porcentajeModerada;
    private final BigDecimal porcentajeCritica;
    private final long minutosEscalamiento;

    /** Cache de umbrales por id de dispositivo: consulta cara que apenas cambia. */
    private final LoadingCache<Integer, List<Umbral>> umbralesPorDispositivo;

    public AlertasService(
            AlertaRepository alertas,
            UmbralRepository umbrales,
            CultivoRepository cultivos,
            NotificacionUsuarioRepository notificaciones,
            UsuarioCultivoRepository accesos,
            @Value("${agro.alertas.severidad.porcentaje-moderada:10}") BigDecimal porcentajeModerada,
            @Value("${agro.alertas.severidad.porcentaje-critica:25}") BigDecimal porcentajeCritica,
            @Value("${agro.alertas.severidad.minutos-escalamiento:60}") long minutosEscalamiento) {
        this.alertas = alertas;
        this.notificaciones = notificaciones;
        this.accesos = accesos;
        this.porcentajeModerada = porcentajeModerada;
        this.porcentajeCritica = porcentajeCritica;
        this.minutosEscalamiento = minutosEscalamiento;
        this.umbralesPorDispositivo = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(5))
                .build(CacheLoader.from(idDispositivo ->
                        cultivos.findByDispositivosIdDispositivo(idDispositivo).stream()
                                .flatMap(c -> umbrales.findByCultivoIdCultivo(c.getIdCultivo()).stream())
                                .toList()));
    }

    /**
     * Punto único de evaluación de lecturas (simulador y API de telemetría).
     *
     * @return el episodio abierto o actualizado si la lectura está fuera de rango,
     *         o vacío si no hay umbral o la lectura volvió dentro de rango.
     */
    @Transactional
    public Optional<Alerta> evaluar(Lectura lectura) {
        if (lectura == null || lectura.getSensor() == null
                || lectura.getSensor().getDispositivo() == null
                || lectura.getSensor().getTipoSensor() == null
                || lectura.getValor() == null) {
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

        Optional<Umbral> encontrado = umbralesDispositivo.stream()
                .filter(u -> u.getTipoSensor().getIdTipoSensor().equals(idTipoSensor))
                .findFirst();

        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        Umbral umbral = encontrado.get();
        LocalDateTime fechaLectura = lectura.getFechaHora() != null
                ? lectura.getFechaHora()
                : LocalDateTime.now();
        List<Alerta> abiertas = alertas.findByUmbralIdUmbralAndEstado(
                umbral.getIdUmbral(), Alerta.ESTADO_ABIERTA);

        if (!lectura.estaFueraDeRango(umbral)) {
            cerrarEpisodios(abiertas, fechaLectura);
            return Optional.empty();
        }

        String direccion = determinarDireccion(lectura.getValor(), umbral);

        // Si cambió directamente a la dirección opuesta, cerrar el episodio previo.
        Alerta episodioMismaDireccion = null;
        for (Alerta abierta : abiertas) {
            if (direccion.equalsIgnoreCase(abierta.getDireccion())) {
                episodioMismaDireccion = abierta;
            } else {
                abierta.cerrar(fechaLectura);
                alertas.save(abierta);
                log.info("Episodio {} cerrado por cambio de dirección en umbral {}",
                        abierta.getIdAlerta(), umbral.getIdUmbral());
            }
        }

        if (episodioMismaDireccion != null) {
            BigDecimal extremoProyectado = calcularExtremo(
                    episodioMismaDireccion.getValorExtremo(), lectura.getValor(), direccion);
            String severidad = calcularSeveridad(
                    extremoProyectado, umbral, direccion,
                    episodioMismaDireccion.getFechaAlerta(), fechaLectura);
            episodioMismaDireccion.registrarLecturaEnEpisodio(lectura.getValor(), severidad, umbral);
            Alerta actualizada = alertas.save(episodioMismaDireccion);
            log.debug("Episodio {} actualizado: lecturas={}, extremo={}, severidad={}",
                    actualizada.getIdAlerta(), actualizada.getCantidadLecturas(),
                    actualizada.getValorExtremo(), actualizada.getSeveridad());
            return Optional.of(actualizada);
        }

        String severidadInicial = calcularSeveridad(
                lectura.getValor(), umbral, direccion, fechaLectura, fechaLectura);
        Alerta nueva = alertas.save(Alerta.abrirEpisodio(lectura, umbral, direccion, severidadInicial));
        notificar(accesos.findByCultivoIdCultivo(umbral.getCultivo().getIdCultivo()), nueva);
        log.warn("Episodio {} abierto (severidad={}, direccion={}) para lectura {} (valor {})",
                nueva.getIdAlerta(), nueva.getSeveridad(), direccion,
                lectura.getIdLectura(), lectura.getValor());
        return Optional.of(nueva);
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

    /** Cuenta los episodios actualmente abiertos en los cultivos del usuario. */
    @Transactional(readOnly = true)
    public long contarAbiertasPorUsuario(String correo) {
        return alertas.contarAbiertasPorUsuario(correo);
    }

    @Transactional
    public void marcarAtendida(Integer idAlerta) {
        Alerta alerta = alertas.findById(idAlerta)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta", idAlerta));
        if (alerta.isAtendida()) {
            return; // idempotente
        }
        alerta.marcarAtendida();
        alertas.save(alerta);
        log.info("Alerta {} marcada como atendida", idAlerta);
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

    private void cerrarEpisodios(List<Alerta> abiertas, LocalDateTime fechaCierre) {
        for (Alerta abierta : abiertas) {
            abierta.cerrar(fechaCierre);
            alertas.save(abierta);
            log.info("Episodio {} cerrado automáticamente al volver al rango ({} lecturas)",
                    abierta.getIdAlerta(), abierta.getCantidadLecturas());
        }
    }

    private String determinarDireccion(BigDecimal valor, Umbral umbral) {
        if (umbral.getValorMinimo() != null && valor.compareTo(umbral.getValorMinimo()) < 0) {
            return Alerta.DIRECCION_BAJO;
        }
        return Alerta.DIRECCION_ALTO;
    }

    private BigDecimal calcularExtremo(BigDecimal extremoActual, BigDecimal nuevoValor, String direccion) {
        if (extremoActual == null) {
            return nuevoValor;
        }
        if (Alerta.DIRECCION_BAJO.equals(direccion)) {
            return nuevoValor.compareTo(extremoActual) < 0 ? nuevoValor : extremoActual;
        }
        return nuevoValor.compareTo(extremoActual) > 0 ? nuevoValor : extremoActual;
    }

    /**
     * Calcula la severidad según el porcentaje de desviación respecto al ancho del rango
     * y sube un nivel si el episodio lleva abierto al menos {@code minutosEscalamiento}.
     */
    String calcularSeveridad(BigDecimal valor, Umbral umbral, String direccion,
                             LocalDateTime inicioEpisodio, LocalDateTime fechaActual) {
        BigDecimal desviacion = Alerta.DIRECCION_BAJO.equals(direccion) && umbral.getValorMinimo() != null
                ? umbral.getValorMinimo().subtract(valor)
                : (umbral.getValorMaximo() != null ? valor.subtract(umbral.getValorMaximo()) : BigDecimal.ZERO);
        if (desviacion.signum() < 0) {
            desviacion = BigDecimal.ZERO;
        }

        BigDecimal ancho = calcularAnchoRango(umbral);
        BigDecimal porcentaje = desviacion.multiply(CIEN).divide(ancho, 4, RoundingMode.HALF_UP);

        String severidad;
        if (porcentaje.compareTo(porcentajeCritica) > 0) {
            severidad = Alerta.SEVERIDAD_CRITICA;
        } else if (porcentaje.compareTo(porcentajeModerada) >= 0) {
            severidad = Alerta.SEVERIDAD_MODERADA;
        } else {
            severidad = Alerta.SEVERIDAD_LEVE;
        }

        if (minutosEscalamiento > 0 && inicioEpisodio != null && fechaActual != null) {
            long minutos = Duration.between(inicioEpisodio, fechaActual).toMinutes();
            if (minutos >= minutosEscalamiento) {
                severidad = Alerta.subirSeveridad(severidad);
            }
        }
        return severidad;
    }

    private BigDecimal calcularAnchoRango(Umbral umbral) {
        if (umbral.getValorMinimo() != null && umbral.getValorMaximo() != null) {
            BigDecimal diff = umbral.getValorMaximo().subtract(umbral.getValorMinimo());
            if (diff.signum() > 0) {
                return diff;
            }
        }
        if (umbral.getTipoSensor() != null) {
            Optional<RangoFisico> fisico = RangoFisico.de(umbral.getTipoSensor().getNombreTipo());
            if (fisico.isPresent()) {
                BigDecimal ancho = fisico.get().getMaximo().subtract(fisico.get().getMinimo());
                if (ancho.signum() > 0) {
                    return ancho;
                }
            }
        }
        return BigDecimal.ONE;
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
