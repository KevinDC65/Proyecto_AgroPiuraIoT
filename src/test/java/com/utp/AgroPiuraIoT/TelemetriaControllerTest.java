package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TelemetriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CultivoService cultivoService;

    @Autowired
    private UmbralService umbralService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private DispositivoIoTRepository dispositivoRepository;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private LecturaRepository lecturaRepository;

    @Autowired
    private AlertaRepository alertaRepository;

    @Autowired
    private NotificacionUsuarioRepository notificacionRepository;

    @Autowired
    private UsuarioCultivoRepository usuarioCultivoRepository;

    @Autowired
    private TipoSensorRepository tipoSensorRepository;

    @Autowired
    private RolRepository rolRepository;

    private Cultivo cultivo;
    private DispositivoIoT dispositivo;
    private Sensor sensorHumedad;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        cultivo = cultivoService.guardar(new CultivoForm(
                "Mango - Parcela Norte", "mango", "Chulucanas",
                new BigDecimal("8.50"), null, null));

        TipoSensor tipoHumedad = tipoSensorRepository.findAll().stream()
                .filter(t -> t.getNombreTipo().equalsIgnoreCase("humedad_suelo"))
                .findFirst()
                .orElseThrow();

        umbralService.guardar(cultivo.getIdCultivo(),
                new UmbralForm(tipoHumedad.getIdTipoSensor(), new BigDecimal("20.00"), new BigDecimal("75.00")));

        dispositivo = cultivoService.registrarDispositivo(cultivo.getIdCultivo());

        sensorHumedad = new Sensor();
        sensorHumedad.setDispositivo(dispositivo);
        sensorHumedad.setTipoSensor(tipoHumedad);
        sensorHumedad = sensorRepository.save(sensorHumedad);

        usuario = usuarioService.registrar(new RegistroRequest(
                "Ingeniero Agrónomo", "agronomo@piura.pe", "clave1234", "clave1234"));

        Rol rol = rolRepository.findAll().get(0);
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(usuario);
        acceso.setCultivo(cultivo);
        acceso.setRol(rol);
        usuarioCultivoRepository.save(acceso);
    }

    @Test
    void postLecturaDentroDeRango_guardaLecturaYNoDisparaAlerta() throws Exception {
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": %d,
                    "valor": 45.50
                }
                """, dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(0)));

        assertEquals(1, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
        assertEquals(0, alertaRepository.count());
        assertEquals(0, notificacionRepository.count());
    }

    @Test
    void postLecturaCriticaBajoMinimo_generaAlertaYNotificacion() throws Exception {
        // Umbral es 20 - 75. Enviamos 14.20 (por debajo de 20)
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": %d,
                    "valor": 14.20
                }
                """, dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)));

        assertEquals(1, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
        assertEquals(1, alertaRepository.count());
        var alertas = alertaRepository.findAll();
        assertEquals(1, alertas.size());
        assertTrue(alertas.get(0).getMensaje().contains("14.20"));

        var notificaciones = notificacionRepository.findByAlertaIdAlerta(alertas.get(0).getIdAlerta());
        assertEquals(1, notificaciones.size());
        assertEquals(usuario.getIdUsuario(), notificaciones.get(0).getUsuario().getIdUsuario());
        assertFalse(notificaciones.get(0).isLeido());
    }

    @Test
    void postLecturaCriticaSobreMaximo_generaAlertaYNotificacion() throws Exception {
        // Umbral es 20 - 75. Enviamos 88.00 (por encima de 75)
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": %d,
                    "valor": 88.00
                }
                """, dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)));

        assertEquals(1, alertaRepository.count());
        assertEquals(1, notificacionRepository.count());
    }

    @Test
    void postLecturaPorDispositivoYTipoSensor_resuelveSensorYGuarda() throws Exception {
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "tipoSensor": "humedad_suelo",
                    "valor": 55.00
                }
                """, dispositivo.getIdDispositivo());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(0)));

        assertEquals(1, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
    }

    @Test
    void postLecturaLoteMultiple_procesaTodasYGeneraAlertasCorrectas() throws Exception {
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "lecturas": [
                        { "idSensor": %d, "valor": 50.00 },
                        { "idSensor": %d, "valor": 10.00 }
                    ]
                }
                """, dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(2)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)));

        assertEquals(2, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
        assertEquals(1, alertaRepository.count());
    }

    @Test
    void postDispositivoInexistente_retorna404() throws Exception {
        String json = """
                {
                    "idDispositivo": 99999,
                    "tipoSensor": "humedad_suelo",
                    "valor": 40.00
                }
                """;

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("Dispositivo no encontrado")));
    }

    @Test
    void postSensorInexistente_retorna404() throws Exception {
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": 99999,
                    "valor": 40.00
                }
                """, dispositivo.getIdDispositivo());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("Sensor no encontrado")));
    }

    @Test
    void postSensorTipoNoConfiguradoEnDispositivo_retorna404() throws Exception {
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "tipoSensor": "ph_suelo",
                    "valor": 7.00
                }
                """, dispositivo.getIdDispositivo());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("No se encontró sensor de tipo 'ph_suelo'")));
    }

    @Test
    void postSensorNoPerteneceAlDispositivo_retorna400() throws Exception {
        DispositivoIoT otroDispositivo = cultivoService.registrarDispositivo(cultivo.getIdCultivo());

        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": %d,
                    "valor": 40.00
                }
                """, otroDispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("no pertenece al dispositivo")));
    }

    @Test
    void postPayloadVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void postSinDispositivoNiSensor_retorna400() throws Exception {
        String json = "{\"valor\": 45.00}";

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("Debe especificar idDispositivo o idSensor")));
    }

    @Test
    void postValorNulo_retorna400() throws Exception {
        String json = String.format("{\"idDispositivo\": %d, \"idSensor\": %d, \"valor\": null}",
                dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }
}
