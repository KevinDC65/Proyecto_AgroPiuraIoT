package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.entity.AlertaNodo;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.AlertaNodoRepository;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estado de conexión, gestión administrativa auditada y monitoreo de desconexión
 * de nodos IoT (A2), integrado con la generación de tokens de {@link NodoTokenService} (A1).
 */
@Service
public class NodoService {

    private static final Logger log = LoggerFactory.getLogger(NodoService.class);

    public static final String ACCION_NODO_REGISTRADO = "NODO_REGISTRADO";
    public static final String ACCION_NODO_ACTIVADO = "NODO_ACTIVADO";
    public static final String ACCION_NODO_DESACTIVADO = "NODO_DESACTIVADO";
    public static final String ACCION_NODO_TOKEN_REGENERADO = "NODO_TOKEN_REGENERADO";
    public static final String ENTIDAD_DISPOSITIVO = "dispositivo";

    private final CultivoService cultivoService;
    private final CultivoRepository cultivos;
    private final DispositivoIoTRepository dispositivos;
    private final AlertaNodoRepository alertasNodo;
    private final NodoTokenService nodoTokenService;
    private final AuditoriaService auditoria;
    private final AlertasService alertas;

    private final long minutosEnLinea;
    private final long horasAlertaSinConexion;
    private final boolean monitorHabilitado;

    public NodoService(
            CultivoService cultivoService,
            CultivoRepository cultivos,
            DispositivoIoTRepository dispositivos,
            AlertaNodoRepository alertasNodo,
            NodoTokenService nodoTokenService,
            AuditoriaService auditoria,
            AlertasService alertas,
            @Value("${agro.iot.nodo.minutos-en-linea:30}") long minutosEnLinea,
            @Value("${agro.iot.nodo.horas-alerta-sin-conexion:6}") long horasAlertaSinConexion,
            @Value("${agro.iot.monitor.habilitado:true}") boolean monitorHabilitado) {
        this.cultivoService = cultivoService;
        this.cultivos = cultivos;
        this.dispositivos = dispositivos;
        this.alertasNodo = alertasNodo;
        this.nodoTokenService = nodoTokenService;
        this.auditoria = auditoria;
        this.alertas = alertas;
        this.minutosEnLinea = Math.max(1, minutosEnLinea);
        this.horasAlertaSinConexion = Math.max(1, horasAlertaSinConexion);
        this.monitorHabilitado = monitorHabilitado;
    }

    public long getMinutosEnLinea() {
        return minutosEnLinea;
    }

    public long getHorasAlertaSinConexion() {
        return horasAlertaSinConexion;
    }

    public String estadoConexionDe(DispositivoIoT dispositivo) {
        return estadoConexionDe(dispositivo, LocalDateTime.now());
    }

    public String estadoConexionDe(DispositivoIoT dispositivo, LocalDateTime ahora) {
        if (dispositivo == null) {
            return DispositivoIoT.CONEXION_INACTIVO;
        }
        return dispositivo.estadoConexion(ahora, minutosEnLinea);
    }

    public boolean estaEnLinea(DispositivoIoT dispositivo, LocalDateTime ahora) {
        return dispositivo != null && dispositivo.estaEnLinea(ahora, minutosEnLinea);
    }

    public Map<Integer, String> estadosDeCultivo(Cultivo cultivo) {
        Map<Integer, String> mapa = new LinkedHashMap<>();
        LocalDateTime ahora = LocalDateTime.now();
        for (DispositivoIoT d : cultivo.getDispositivos()) {
            mapa.put(d.getIdDispositivo(), estadoConexionDe(d, ahora));
        }
        return mapa;
    }

    /**
     * Registra un nodo nuevo para el cultivo con su token BCrypt (vía A1 {@link CultivoService#registrarNodo})
     * y registra el alta en la auditoría. El token en claro solo se devuelve aquí una vez.
     */
    @Transactional
    public NodoConToken registrarConToken(Integer idCultivo, String correoAdmin) {
        CultivoService.NodoRegistrado registro = cultivoService.registrarNodo(idCultivo);
        DispositivoIoT dispositivo = registro.nodo();

        auditoria.registrarPorCorreo(
                correoAdmin,
                ACCION_NODO_REGISTRADO,
                ENTIDAD_DISPOSITIVO,
                dispositivo.getIdDispositivo(),
                "Nodo #" + dispositivo.getIdDispositivo() + " registrado en cultivo #" + idCultivo);

        return new NodoConToken(dispositivo, registro.token());
    }

    /** Activa o desactiva un nodo del cultivo y lo registra en auditoría (C1). */
    @Transactional
    public DispositivoIoT cambiarEstado(Integer idCultivo, Integer idDispositivo,
                                        boolean activar, String correoAdmin) {
        DispositivoIoT dispositivo = buscarDispositivoDeCultivo(idCultivo, idDispositivo);
        if (activar) {
            dispositivo.marcarActivo();
        } else {
            dispositivo.marcarInactivo();
            cerrarAlertasDesconexion(idDispositivo, LocalDateTime.now());
        }
        dispositivo = dispositivos.save(dispositivo);
        alertas.invalidarCacheUmbrales(idDispositivo);

        String accion = activar ? ACCION_NODO_ACTIVADO : ACCION_NODO_DESACTIVADO;
        String detalle = "Nodo #" + idDispositivo + (activar ? " activado" : " desactivado")
                + " en cultivo #" + idCultivo;
        auditoria.registrarPorCorreo(correoAdmin, accion, ENTIDAD_DISPOSITIVO, idDispositivo, detalle);
        log.info("{}", detalle);
        return dispositivo;
    }

    /**
     * Regenera el token del nodo (invalida inmediatamente el anterior) mediante
     * {@link NodoTokenService} y registra la acción en auditoría sin exponer jamás el token.
     */
    @Transactional
    public String regenerarToken(Integer idCultivo, Integer idDispositivo, String correoAdmin) {
        DispositivoIoT dispositivo = buscarDispositivoDeCultivo(idCultivo, idDispositivo);
        String nuevoToken = nodoTokenService.asignarToken(dispositivo);

        auditoria.registrarPorCorreo(
                correoAdmin,
                ACCION_NODO_TOKEN_REGENERADO,
                ENTIDAD_DISPOSITIVO,
                idDispositivo,
                "Token regenerado para nodo #" + idDispositivo + " en cultivo #" + idCultivo);
        log.info("Token regenerado para nodo {} en cultivo {}", idDispositivo, idCultivo);
        return nuevoToken;
    }

    /** Registra la última conexión del nodo y cierra alertas de desconexión abiertas. */
    @Transactional
    public void registrarConexion(DispositivoIoT dispositivo, LocalDateTime fecha) {
        LocalDateTime instante = fecha != null ? fecha : LocalDateTime.now();
        dispositivo.registrarConexion(instante);
        dispositivos.save(dispositivo);
        cerrarAlertasDesconexion(dispositivo.getIdDispositivo(), instante);
    }

    /** Cierra los episodios abiertos de "nodo sin conexión" para el nodo indicado. */
    @Transactional
    public int cerrarAlertasDesconexion(Integer idDispositivo, LocalDateTime fechaCierre) {
        List<AlertaNodo> abiertas = alertasNodo.findByDispositivoIdDispositivoAndEstado(
                idDispositivo, AlertaNodo.ESTADO_ABIERTA);
        for (AlertaNodo alerta : abiertas) {
            alerta.cerrar(fechaCierre);
            alertasNodo.save(alerta);
        }
        return abiertas.size();
    }

    /**
     * Tarea programada: detecta nodos activos cuya última conexión superó
     * {@code horasAlertaSinConexion} y abre una alerta {@code nodo_sin_conexion}
     * sin duplicarla si ya existe una abierta.
     */
    @Scheduled(fixedDelayString = "${agro.iot.monitor.intervalo-ms:300000}")
    @Transactional
    public int monitorearDesconexionesProgramado() {
        if (!monitorHabilitado) {
            return 0;
        }
        return verificarNodosSinConexion(LocalDateTime.now());
    }

    @Transactional
    public int verificarNodosSinConexion(LocalDateTime ahora) {
        LocalDateTime limite = ahora.minusHours(horasAlertaSinConexion);
        int generadas = 0;
        for (DispositivoIoT dispositivo : dispositivos.findAll()) {
            if (!dispositivo.estaActivo() || dispositivo.getUltimaConexion() == null) {
                continue;
            }
            if (!dispositivo.getUltimaConexion().isBefore(limite)) {
                continue;
            }
            for (Cultivo cultivo : cultivos.findByDispositivosIdDispositivo(dispositivo.getIdDispositivo())) {
                boolean yaAbierta = alertasNodo.existsByDispositivoIdDispositivoAndCultivoIdCultivoAndEstado(
                        dispositivo.getIdDispositivo(), cultivo.getIdCultivo(), AlertaNodo.ESTADO_ABIERTA);
                if (!yaAbierta) {
                    alertasNodo.save(AlertaNodo.abrirSinConexion(dispositivo, cultivo, horasAlertaSinConexion));
                    generadas++;
                    log.warn("Alerta nodo_sin_conexion abierta para nodo {} en cultivo {}",
                            dispositivo.getIdDispositivo(), cultivo.getIdCultivo());
                }
            }
        }
        return generadas;
    }

    @Transactional(readOnly = true)
    public List<AlertaNodo> listarAlertasNodoPara(String correo) {
        return alertasNodo.buscarPorUsuario(correo);
    }

    @Transactional
    public void marcarAlertaNodoAtendida(Integer idAlertaNodo) {
        AlertaNodo alerta = alertasNodo.findById(idAlertaNodo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alerta de nodo", idAlertaNodo));
        if (!alerta.isAtendida()) {
            alerta.marcarAtendida();
            alertasNodo.save(alerta);
        }
    }

    private DispositivoIoT buscarDispositivoDeCultivo(Integer idCultivo, Integer idDispositivo) {
        Cultivo cultivo = cultivoService.buscar(idCultivo);
        return cultivo.getDispositivos().stream()
                .filter(d -> d.getIdDispositivo().equals(idDispositivo))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Dispositivo", idDispositivo));
    }

    public record NodoConToken(DispositivoIoT dispositivo, String tokenPlano) {
    }
}
