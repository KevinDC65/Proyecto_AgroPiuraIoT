package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.LecturaItemRequest;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaResponse;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.TelemetriaService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A3 · Lecturas repetidas: si un nodo reenvía el mismo lote (mismo sensor y
 * misma fecha y hora), la lectura no se duplica ni vuelve a generar alertas.
 */
@SpringBootTest
@Transactional
class LecturasDuplicadasTest {

    private static final String CORREO = "duplicados.a3@test.pe";
    private static final LocalDateTime MEDICION = LocalDateTime.of(2026, 11, 26, 8, 30);

    @Autowired
    private TelemetriaService telemetria;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private TipoSensorRepository tipos;

    @Autowired
    private SensorRepository sensores;

    @Autowired
    private LecturaRepository lecturas;

    private Integer idCultivo;
    private Integer idNodo;

    @BeforeEach
    void prepararNodoConUmbral() {
        usuarios.registrar(new RegistroRequest("Duplicados A3", CORREO, "clave1234", "clave1234"));
        Cultivo cultivo = cultivos.crear(new CultivoForm(
                "Mango - Lote A3", "mango", "Tambogrande, Piura",
                new BigDecimal("2.00"), null, null), CORREO);
        idCultivo = cultivo.getIdCultivo();
        Integer idTemperatura = tipos.findAll().stream()
                .filter(t -> t.getNombreTipo().equals("temperatura_ambiente"))
                .findFirst().orElseThrow().getIdTipoSensor();
        umbrales.guardar(idCultivo, new UmbralForm(idTemperatura, new BigDecimal("16"), new BigDecimal("32")));
        idNodo = cultivos.registrarNodo(idCultivo).nodo().getIdDispositivo();
    }

    @Test
    void reenviarElMismoLoteNoDuplicaLecturasNiAlertas() {
        TelemetriaRequest lote = loteConFecha("temperatura_ambiente", "36");

        TelemetriaResponse primero = telemetria.procesar(lote);
        TelemetriaResponse reenvio = telemetria.procesar(loteConFecha("temperatura_ambiente", "36"));

        assertEquals(1, primero.getLecturasProcesadas());
        assertEquals(1, primero.getAlertasGeneradas());
        assertEquals(0, primero.getLecturasDuplicadas());

        assertEquals("ok", reenvio.getStatus(), "Un reenvío no es un error: el nodo no debe reintentar");
        assertEquals(0, reenvio.getLecturasProcesadas());
        assertEquals(0, reenvio.getAlertasGeneradas(), "No se repite la alerta");
        assertEquals(1, reenvio.getLecturasDuplicadas());
        assertTrue(reenvio.getMensaje().contains("ya registrada"));
        assertEquals(1, lecturasDelCultivo());
    }

    @Test
    void lecturaRepetidaDentroDelMismoLoteSeGuardaUnaVez() {
        TelemetriaResponse respuesta = telemetria.procesar(new TelemetriaRequest(idNodo, List.of(
                conFecha("humedad_suelo", "40"),
                conFecha("humedad_suelo", "40"),
                conFecha("temperatura_ambiente", "25"))));

        assertEquals(2, respuesta.getLecturasProcesadas());
        assertEquals(1, respuesta.getLecturasDuplicadas());
        assertEquals(2, lecturasDelCultivo());
    }

    @Test
    void mismaFechaEnOtroSensorNoEsDuplicado() {
        telemetria.procesar(loteConFecha("humedad_suelo", "40"));
        TelemetriaResponse otroSensor = telemetria.procesar(loteConFecha("humedad_ambiente", "70"));

        assertEquals(1, otroSensor.getLecturasProcesadas());
        assertEquals(0, otroSensor.getLecturasDuplicadas());
        assertEquals(2, lecturasDelCultivo());
    }

    @Test
    void sinFechaHoraNuncaSeConsideraDuplicado() {
        TelemetriaRequest sinFecha = new TelemetriaRequest(idNodo,
                List.of(new LecturaItemRequest("humedad_suelo", new BigDecimal("40"))));

        telemetria.procesar(sinFecha);
        TelemetriaResponse segundo = telemetria.procesar(new TelemetriaRequest(idNodo,
                List.of(new LecturaItemRequest("humedad_suelo", new BigDecimal("40")))));

        assertEquals(1, segundo.getLecturasProcesadas(), "La fecha la pone el servidor: son dos mediciones");
        assertEquals(2, lecturasDelCultivo());
    }

    @Test
    void laBaseDeDatosImpideDuplicadosAunqueSeSalteElServicio() {
        Sensor sensor = sensores.findByDispositivoIdDispositivoAndTipoSensorNombreTipoIgnoreCase(
                idNodo, "ph_suelo").orElseThrow();
        lecturas.saveAndFlush(lectura(sensor, "6.5"));

        assertThrows(DataIntegrityViolationException.class,
                () -> lecturas.saveAndFlush(lectura(sensor, "6.8")));
    }

    // ------------------------------------------------------------------

    private TelemetriaRequest loteConFecha(String tipo, String valor) {
        return new TelemetriaRequest(idNodo, List.of(conFecha(tipo, valor)));
    }

    private static LecturaItemRequest conFecha(String tipo, String valor) {
        LecturaItemRequest item = new LecturaItemRequest(tipo, new BigDecimal(valor));
        item.setFechaHora(MEDICION);
        return item;
    }

    private static Lectura lectura(Sensor sensor, String valor) {
        Lectura lectura = sensor.registrarLectura(new BigDecimal(valor));
        lectura.setFechaHora(MEDICION);
        return lectura;
    }

    private int lecturasDelCultivo() {
        return lecturas.buscarPorCultivo(idCultivo).size();
    }
}
