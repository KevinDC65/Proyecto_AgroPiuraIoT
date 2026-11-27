package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.RangoFisico;
import com.utp.AgroPiuraIoT.model.dto.LecturaItemRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaResponse;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingesta de telemetría IoT (A1/A2): valida que el payload corresponda al nodo
 * autenticado, descarta lecturas físicamente imposibles ({@link RangoFisico})
 * reportándolas en {@code rechazos}, evalúa cada lectura válida mediante
 * {@link AlertasService#evaluar(Lectura)}, y actualiza {@code ultima_conexion}
 * cerrando alertas abiertas de desconexión.
 */
@Service
public class TelemetriaService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaService.class);

    private final DispositivoIoTRepository dispositivos;
    private final SensorRepository sensores;
    private final LecturaRepository lecturas;
    private final AlertasService alertas;
    private final NodoService nodoService;

    public TelemetriaService(DispositivoIoTRepository dispositivos,
                             SensorRepository sensores,
                             LecturaRepository lecturas,
                             AlertasService alertas,
                             NodoService nodoService) {
        this.dispositivos = dispositivos;
        this.sensores = sensores;
        this.lecturas = lecturas;
        this.alertas = alertas;
        this.nodoService = nodoService;
    }

    /** Procesamiento directo a nivel de servicio (A1 y pruebas internas). */
    @Transactional
    public TelemetriaResponse procesar(TelemetriaRequest request) {
        return procesar(request, null);
    }

    /** Procesamiento autenticado por token Bearer de nodo (A2). */
    @Transactional
    public TelemetriaResponse procesar(TelemetriaRequest request, Integer idDispositivoAutenticado) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de telemetría no puede ser nula");
        }

        if (idDispositivoAutenticado != null) {
            if (request.getIdDispositivo() != null
                    && !request.getIdDispositivo().equals(idDispositivoAutenticado)) {
                throw new IllegalArgumentException(String.format(
                        "El idDispositivo %d no corresponde al nodo autenticado (%d)",
                        request.getIdDispositivo(), idDispositivoAutenticado));
            }
            request.setIdDispositivo(idDispositivoAutenticado);
        }

        List<LecturaItemRequest> items = request.obtenerItems();
        if (items.isEmpty()) {
            throw new IllegalArgumentException("El payload no contiene ninguna lectura válida");
        }

        int lecturasProcesadas = 0;
        int alertasGeneradas = 0;
        List<String> rechazos = new ArrayList<>();
        Map<Integer, DispositivoIoT> nodosQueReportaron = new LinkedHashMap<>();

        for (LecturaItemRequest item : items) {
            if (idDispositivoAutenticado != null) {
                if (item.getIdDispositivo() != null
                        && !item.getIdDispositivo().equals(idDispositivoAutenticado)) {
                    throw new IllegalArgumentException(String.format(
                            "El dispositivo %d no corresponde al nodo autenticado (%d)",
                            item.getIdDispositivo(), idDispositivoAutenticado));
                }
                item.setIdDispositivo(idDispositivoAutenticado);
            }

            if (item.getValor() == null) {
                throw new IllegalArgumentException("El valor de la lectura no puede ser nulo");
            }

            Sensor sensor = resolverSensor(item);
            DispositivoIoT nodo = sensor.getDispositivo();
            nodosQueReportaron.putIfAbsent(nodo.getIdDispositivo(), nodo);

            String tipo = sensor.getTipoSensor().getNombreTipo();
            if (!RangoFisico.esPosible(tipo, item.getValor())) {
                String motivo = String.format("%s = %s fuera del rango físico (%s): posible falla de sensor",
                        tipo, item.getValor().toPlainString(),
                        RangoFisico.de(tipo).map(RangoFisico::describir).orElse("?"));
                log.warn("Lectura rechazada del nodo {}, sensor {}: {}",
                        nodo.getIdDispositivo(), sensor.getIdSensor(), motivo);
                rechazos.add(motivo);
                continue;
            }

            Lectura lectura = sensor.registrarLectura(item.getValor());
            if (item.getFechaHora() != null) {
                lectura.setFechaHora(item.getFechaHora());
            }
            lectura = lecturas.save(lectura);
            lecturasProcesadas++;

            Optional<Alerta> alerta = alertas.evaluar(lectura);
            if (alerta.isPresent()) {
                alertasGeneradas++;
            }
        }

        // El nodo se comunicó aunque alguna lectura se haya rechazado: cuenta como conexión.
        LocalDateTime ahora = LocalDateTime.now();
        for (DispositivoIoT nodo : nodosQueReportaron.values()) {
            nodoService.registrarConexion(nodo, ahora);
        }

        log.info("Telemetría procesada: {} lecturas guardadas, {} rechazadas, {} alertas generadas",
                lecturasProcesadas, rechazos.size(), alertasGeneradas);
        if (!rechazos.isEmpty()) {
            return TelemetriaResponse.conRechazos(lecturasProcesadas, alertasGeneradas, rechazos);
        }
        return TelemetriaResponse.ok(lecturasProcesadas, alertasGeneradas);
    }

    private Sensor resolverSensor(LecturaItemRequest item) {
        if (item.getIdSensor() != null) {
            Sensor sensor = sensores.findById(item.getIdSensor())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Sensor no encontrado: " + item.getIdSensor()));

            if (item.getIdDispositivo() != null
                    && !sensor.getDispositivo().getIdDispositivo().equals(item.getIdDispositivo())) {
                throw new IllegalArgumentException(String.format(
                        "El sensor %d no pertenece al dispositivo %d",
                        item.getIdSensor(), item.getIdDispositivo()));
            }
            return sensor;
        }

        if (item.getIdDispositivo() == null) {
            throw new IllegalArgumentException("Debe especificar idDispositivo o idSensor");
        }

        DispositivoIoT dispositivo = dispositivos.findById(item.getIdDispositivo())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Dispositivo no encontrado: " + item.getIdDispositivo()));

        if (item.getIdTipoSensor() != null) {
            return sensores.findByDispositivoIdDispositivoAndTipoSensorIdTipoSensor(
                            dispositivo.getIdDispositivo(), item.getIdTipoSensor())
                    .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                            "No se encontró sensor con tipo ID %d para el dispositivo %d",
                            item.getIdTipoSensor(), item.getIdDispositivo())));
        }

        if (item.getTipoSensor() != null && !item.getTipoSensor().isBlank()) {
            String tipoBuscado = item.getTipoSensor().trim();
            return sensores.findByDispositivoIdDispositivoAndTipoSensorNombreTipoIgnoreCase(
                            dispositivo.getIdDispositivo(), tipoBuscado)
                    .orElseThrow(() -> new RecursoNoEncontradoException(String.format(
                            "No se encontró sensor de tipo '%s' para el dispositivo %d",
                            tipoBuscado, item.getIdDispositivo())));
        }

        throw new IllegalArgumentException(String.format(
                "Debe especificar tipoSensor o idTipoSensor para el dispositivo %d",
                item.getIdDispositivo()));
    }
}
