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
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ingesta de telemetría IoT (A1/A2): valida que el payload corresponda al nodo
 * autenticado, actualiza {@code ultima_conexion}, descarta lecturas físicamente
 * imposibles ({@link RangoFisico}) soportando lotes mixtos, y evalúa cada lectura
 * válida mediante {@link AlertasService#evaluar(Lectura)}.
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

    /** Compatibilidad interna para llamadas sin token explícito. */
    @Transactional
    public TelemetriaResponse procesar(TelemetriaRequest request) {
        return procesar(request, null);
    }

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

        DispositivoIoT nodoAutenticado = null;
        if (idDispositivoAutenticado != null) {
            nodoAutenticado = dispositivos.findById(idDispositivoAutenticado)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Dispositivo no encontrado: " + idDispositivoAutenticado));
        }

        boolean esLoteMultiple = items.size() > 1;
        int lecturasProcesadas = 0;
        int lecturasRechazadas = 0;
        int alertasGeneradas = 0;
        List<String> errores = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            LecturaItemRequest item = items.get(i);
            int numeroItem = i + 1;

            if (idDispositivoAutenticado != null) {
                if (item.getIdDispositivo() != null
                        && !item.getIdDispositivo().equals(idDispositivoAutenticado)) {
                    String msg = String.format("El dispositivo %d no corresponde al nodo autenticado (%d)",
                            item.getIdDispositivo(), idDispositivoAutenticado);
                    if (!esLoteMultiple) {
                        throw new IllegalArgumentException(msg);
                    }
                    lecturasRechazadas++;
                    errores.add("Lectura #" + numeroItem + ": " + msg);
                    continue;
                }
                item.setIdDispositivo(idDispositivoAutenticado);
            }

            try {
                if (item.getValor() == null) {
                    throw new IllegalArgumentException("El valor de la lectura no puede ser nulo");
                }

                Sensor sensor = resolverSensor(item);
                validarRangoFisico(sensor, item);

                Lectura lectura = sensor.registrarLectura(item.getValor());
                if (item.getFechaHora() != null) {
                    lectura.setFechaHora(item.getFechaHora());
                }
                lectura = lecturas.save(lectura);
                lecturasProcesadas++;

                if (nodoAutenticado == null) {
                    nodoAutenticado = sensor.getDispositivo();
                }

                Optional<Alerta> alerta = alertas.evaluar(lectura);
                if (alerta.isPresent()) {
                    alertasGeneradas++;
                }
            } catch (RecursoNoEncontradoException | IllegalArgumentException ex) {
                if (!esLoteMultiple) {
                    throw ex;
                }
                lecturasRechazadas++;
                errores.add("Lectura #" + numeroItem + ": " + ex.getMessage());
            }
        }

        if (nodoAutenticado != null && lecturasProcesadas > 0) {
            nodoService.registrarConexion(nodoAutenticado, LocalDateTime.now());
        }

        if (lecturasProcesadas == 0 && !errores.isEmpty()) {
            throw new LoteInvalidoException("Ninguna lectura del lote es válida", lecturasRechazadas, errores);
        }

        if (lecturasRechazadas > 0) {
            log.warn("Telemetría procesada con observaciones: {} válidas, {} rechazadas, {} alertas",
                    lecturasProcesadas, lecturasRechazadas, alertasGeneradas);
            return TelemetriaResponse.okParcial(lecturasProcesadas, lecturasRechazadas, alertasGeneradas, errores);
        }

        log.info("Telemetría procesada: {} lecturas guardadas, {} alertas evaluadas",
                lecturasProcesadas, alertasGeneradas);
        return TelemetriaResponse.ok(lecturasProcesadas, alertasGeneradas);
    }

    private void validarRangoFisico(Sensor sensor, LecturaItemRequest item) {
        String nombreTipo = sensor.getTipoSensor().getNombreTipo();
        RangoFisico.para(nombreTipo).ifPresent(rango -> {
            if (!rango.contiene(item.getValor())) {
                log.warn("Posible falla de sensor en nodo {} ({}): valor {} fuera del rango físico ({})",
                        sensor.getDispositivo().getIdDispositivo(), nombreTipo,
                        item.getValor(), rango.describir());
                throw new IllegalArgumentException(String.format(
                        "Valor %s fuera del rango físico para %s (%s)",
                        item.getValor(), nombreTipo, rango.describir()));
            }
        });
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

    /** Excepción tipada cuando todas las lecturas de un lote múltiple resultan inválidas. */
    public static class LoteInvalidoException extends RuntimeException {
        private final int lecturasRechazadas;
        private final List<String> errores;

        public LoteInvalidoException(String message, int lecturasRechazadas, List<String> errores) {
            super(message);
            this.lecturasRechazadas = lecturasRechazadas;
            this.errores = errores;
        }

        public int getLecturasRechazadas() {
            return lecturasRechazadas;
        }

        public List<String> getErrores() {
            return errores;
        }
    }
}
