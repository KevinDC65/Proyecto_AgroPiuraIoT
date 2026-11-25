package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.AlertasService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * B3 · Validación agronómica: cinco escenarios con datos fijos (sequía, exceso
 * de riego, ola de calor, sensor dañado y nodo caído) sobre un cultivo de mango
 * con los umbrales de la plantilla de B2 (ver docs/13-reglas-agronomicas.md).
 * <p>
 * Cada escenario envía una serie de lecturas con hora (una por hora desde
 * {@link #INICIO}) y comprueba qué alertas produce el motor. Las pruebas
 * activas validan lo que el motor hace hoy; las desactivadas describen el
 * comportamiento esperado que depende de B1 (severidad y episodios), B2
 * (recomendaciones) y A2 (estado de nodos), y se activan al integrarlos.
 */
@SpringBootTest
@Transactional
class ValidacionAgronomicaTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 2, 6, 0);
    private static final String CORREO = "agricultor.b3@test.pe";

    @Autowired
    private AlertasService alertas;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private SensorRepository sensores;

    @Autowired
    private LecturaRepository lecturas;

    @Autowired
    private TipoSensorRepository tipos;

    @Autowired
    private NotificacionUsuarioRepository notificaciones;

    private DispositivoIoT nodo;

    /**
     * Mango en Tambogrande con la plantilla de B2 (MIDAGRI, Ficha N° 08):
     * temperatura 16-32 °C, humedad ambiente 65-100 %, pH 5,5-7,5. La humedad
     * de suelo no tiene plantilla (depende de la textura): se usa 30-70 %, el
     * valor que configuraría el administrador para un suelo franco.
     */
    @BeforeEach
    void prepararCultivoDeMango() {
        usuarios.registrar(new RegistroRequest("Agricultor B3", CORREO, "clave1234", "clave1234"));
        Cultivo cultivo = cultivos.crear(new CultivoForm(
                "Mango Kent - Lote B3", "mango", "Tambogrande, Piura",
                new BigDecimal("2.50"), null, null), CORREO);
        Integer id = cultivo.getIdCultivo();
        umbral(id, "temperatura_ambiente", "16", "32");
        umbral(id, "humedad_ambiente", "65", "100");
        umbral(id, "ph_suelo", "5.5", "7.5");
        umbral(id, "humedad_suelo", "30", "70");
        nodo = cultivos.registrarDispositivo(id);
    }

    // ------------------------------------------------------------------
    // Escenario 1 · Sequía
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Escenario 1 · Sequía: alerta solo cuando la humedad de suelo baja de 30 %")
    void sequiaGeneraAlertaCuandoLaHumedadDeSueloBajaDelMinimo() {
        List<Optional<Alerta>> resultado = enviarSerie("humedad_suelo",
                "45", "38", "32", "28", "22", "18");

        sinAlertaEn(resultado, 0, 1, 2);
        conAlertaEn(resultado, 3, 4, 5);
        Alerta primera = resultado.get(3).orElseThrow();
        assertEquals("humedad_suelo", primera.getUmbral().getTipoSensor().getNombreTipo());
        assertTrue(primera.getMensaje().contains("28"), primera.getMensaje());
        assertFalse(notificaciones.findByAlertaIdAlerta(primera.getIdAlerta()).isEmpty(),
                "El administrador del cultivo debe recibir la notificación");
    }

    // ------------------------------------------------------------------
    // Escenario 2 · Exceso de riego
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Escenario 2 · Exceso de riego: alerta cuando la humedad de suelo supera 70 %")
    void excesoDeRiegoGeneraAlertaCuandoLaHumedadDeSueloSuperaElMaximo() {
        List<Optional<Alerta>> resultado = enviarSerie("humedad_suelo",
                "60", "68", "72", "85", "95", "99");

        sinAlertaEn(resultado, 0, 1);
        conAlertaEn(resultado, 2, 3, 4, 5);
        assertTrue(resultado.get(2).orElseThrow().getMensaje().contains("72"));
    }

    // ------------------------------------------------------------------
    // Escenario 3 · Ola de calor
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Escenario 3 · Ola de calor: alerta sobre 32 °C y nada al volver al rango")
    void olaDeCalorGeneraAlertaSobreElMaximoYNadaAlVolverAlRango() {
        List<Optional<Alerta>> resultado = enviarSerie("temperatura_ambiente",
                "28", "31", "33", "36", "38", "34", "30", "27");

        sinAlertaEn(resultado, 0, 1);
        conAlertaEn(resultado, 2, 3, 4, 5);
        sinAlertaEn(resultado, 6, 7);
    }

    @Test
    @DisplayName("Escenario 3 · Ola de calor con humedad baja: cada variable alerta por separado")
    void olaDeCalorConAireSecoAlertaPorCadaVariable() {
        List<Optional<Alerta>> temperatura = enviarSerie("temperatura_ambiente", "36");
        List<Optional<Alerta>> humedad = enviarSerie("humedad_ambiente", "45");

        conAlertaEn(temperatura, 0);
        conAlertaEn(humedad, 0);
        assertEquals("humedad_ambiente",
                humedad.get(0).orElseThrow().getUmbral().getTipoSensor().getNombreTipo());
    }

    // ------------------------------------------------------------------
    // Escenario 4 · Sensor dañado
    // ------------------------------------------------------------------

    @Test
    @Disabled("Falla hoy: el motor trata un valor imposible como alerta agronómica. "
            + "Se activa cuando B1 (#2) aplique los rangos físicos de A1 (#1)")
    @DisplayName("Escenario 4 · Sensor dañado: un valor imposible no genera alerta agronómica")
    void sensorDanadoNoGeneraAlertaAgronomica() {
        // pH fuera de la escala 0-14 y temperatura imposible en Piura.
        List<Optional<Alerta>> ph = enviarSerie("ph_suelo", "20.00");
        List<Optional<Alerta>> temperatura = enviarSerie("temperatura_ambiente", "-50");

        // Esperado: la lectura se rechaza (o se marca como falla de sensor) y no
        // se avisa al agricultor que su suelo es alcalino ni que hay helada.
        sinAlertaEn(ph, 0);
        sinAlertaEn(temperatura, 0);
    }

    // ------------------------------------------------------------------
    // Escenario 5 · Nodo caído
    // ------------------------------------------------------------------

    @Test
    @Disabled("Pendiente A2 (#6): estado de nodos por última comunicación")
    @DisplayName("Escenario 5 · Nodo caído: sin lecturas por más del intervalo, el nodo queda sin comunicación")
    void nodoCaidoSeDetectaPorFaltaDeLecturas() {
        enviarSerie("temperatura_ambiente", "28", "29", "29");
        // Esperado con A2: si pasan más de N intervalos sin lecturas desde
        // INICIO + 2 h, el nodo aparece como "sin comunicación" y se avisa al
        // administrador. Ninguna lectura previa genera alerta agronómica.
        fail("Completar con la API de estado de nodos de A2");
    }

    // ------------------------------------------------------------------
    // Severidad, episodios y recomendaciones (B1 y B2)
    // ------------------------------------------------------------------

    @Test
    @Disabled("Pendiente B1 (#2): alertas por episodio")
    @DisplayName("Episodios: la ola de calor abre un solo episodio y se cierra al volver al rango")
    void olaDeCalorAbreUnSoloEpisodioYSeCierraAlVolverAlRango() {
        enviarSerie("temperatura_ambiente", "28", "31", "33", "36", "38", "34", "30", "27");
        // Esperado: 1 episodio abierto en la lectura 33 °C (INICIO + 2 h), con
        // 4 lecturas fuera de rango y cerrado en la lectura 30 °C (INICIO + 6 h).
        // Una sola notificación por usuario para todo el episodio.
        fail("Completar con la API de episodios de B1");
    }

    @Test
    @Disabled("Pendiente B1 (#2): severidad por distancia al umbral")
    @DisplayName("Severidad: crece con la distancia al umbral")
    void severidadCreceConLaDistanciaAlUmbral() {
        enviarSerie("humedad_suelo", "28", "22", "12");
        // Esperado (rango 30-70, ancho 40): 28 % (5 % del ancho) → leve,
        // 22 % (20 %) → moderada, 12 % (45 %) → crítica. Ver docs/13, sección 3.
        fail("Completar con la API de severidad de B1");
    }

    @Test
    @Disabled("Pendiente B2 (#7): recomendaciones por variable y dirección")
    @DisplayName("Recomendaciones: sequía recomienda regar y exceso de riego suspenderlo")
    void cadaAlertaTieneLaRecomendacionDeSuVariableYDireccion() {
        // Esperado: humedad_suelo.baja → regar; humedad_suelo.alta → suspender
        // riego y revisar drenaje; temperatura_ambiente.alta → riego en horas
        // frescas y sombreo; ph_suelo.bajo/alto → análisis de suelo y enmienda.
        fail("Completar con RecomendacionService de B2");
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------

    private void umbral(Integer idCultivo, String tipo, String minimo, String maximo) {
        umbrales.guardar(idCultivo, new UmbralForm(
                tipo(tipo).getIdTipoSensor(), new BigDecimal(minimo), new BigDecimal(maximo)));
    }

    private TipoSensor tipo(String nombre) {
        return tipos.findAll().stream()
                .filter(t -> t.getNombreTipo().equals(nombre))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Tipo de sensor no sembrado: " + nombre));
    }

    /** Envía una lectura por hora desde {@link #INICIO} y devuelve lo que produjo el motor. */
    private List<Optional<Alerta>> enviarSerie(String tipoSensor, String... valores) {
        Sensor sensor = sensores.findByDispositivoIdDispositivoAndTipoSensorNombreTipoIgnoreCase(
                nodo.getIdDispositivo(), tipoSensor).orElseThrow();
        List<Optional<Alerta>> resultado = new ArrayList<>();
        for (int i = 0; i < valores.length; i++) {
            Lectura lectura = sensor.registrarLectura(new BigDecimal(valores[i]));
            lectura.setFechaHora(INICIO.plusHours(i));
            resultado.add(alertas.evaluar(lecturas.save(lectura)));
        }
        return resultado;
    }

    private static void conAlertaEn(List<Optional<Alerta>> resultado, int... posiciones) {
        for (int i : posiciones) {
            assertTrue(resultado.get(i).isPresent(), "Se esperaba alerta en la lectura " + i);
        }
    }

    private static void sinAlertaEn(List<Optional<Alerta>> resultado, int... posiciones) {
        for (int i : posiciones) {
            assertTrue(resultado.get(i).isEmpty(), "No se esperaba alerta en la lectura " + i);
        }
    }
}
