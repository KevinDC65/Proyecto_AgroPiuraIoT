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
import com.utp.AgroPiuraIoT.service.NodoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
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
    private NodoService nodoService;

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
    private String tokenPlano;
    private Sensor sensorHumedad;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = usuarioService.registrar(new RegistroRequest(
                "Ingeniero Agrónomo", "agronomo@piura.pe", "clave1234", "clave1234"));

        cultivo = cultivoService.crear(new CultivoForm(
                "Mango - Parcela Norte", "mango", "Chulucanas",
                new BigDecimal("8.50"), null, null), usuario.getCorreo());

        TipoSensor tipoHumedad = tipoSensorRepository.findAll().stream()
                .filter(t -> t.getNombreTipo().equalsIgnoreCase("humedad_suelo"))
                .findFirst()
                .orElseThrow();

        umbralService.guardar(cultivo.getIdCultivo(),
                new UmbralForm(tipoHumedad.getIdTipoSensor(), new BigDecimal("20.00"), new BigDecimal("75.00")));

        var nodoCreado = nodoService.registrarConToken(cultivo.getIdCultivo(), usuario.getCorreo());
        dispositivo = nodoCreado.dispositivo();
        tokenPlano = nodoCreado.tokenPlano();

        sensorHumedad = sensorRepository
                .findByDispositivoIdDispositivoAndTipoSensorIdTipoSensor(
                        dispositivo.getIdDispositivo(), tipoHumedad.getIdTipoSensor())
                .orElseThrow();
    }

    @Test
    void sinTokenOConTokenInvalidoResponde401EnJson() throws Exception {
        String json = "{\"tipoSensor\": \"humedad_suelo\", \"valor\": 45.00}";

        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")));

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer ap_" + dispositivo.getIdDispositivo() + "_invalido")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void nodoInactivoRechazadoCon403EnJson() throws Exception {
        nodoService.cambiarEstado(cultivo.getIdCultivo(), dispositivo.getIdDispositivo(),
                false, usuario.getCorreo());

        String json = "{\"tipoSensor\": \"humedad_suelo\", \"valor\": 45.00}";

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("inactivo")));
    }

    @Test
    void tokenRegeneradoInvalidaElAnterior() throws Exception {
        String nuevoToken = nodoService.regenerarToken(
                cultivo.getIdCultivo(), dispositivo.getIdDispositivo(), usuario.getCorreo());
        String json = "{\"tipoSensor\": \"humedad_suelo\", \"valor\": 50.00}";

        // Token anterior queda invalidado
        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());

        // Nuevo token funciona
        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + nuevoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)));
    }

    @Test
    void postLecturaDentroDeRango_guardaActualizaUltimaConexionYNoDisparaAlerta() throws Exception {
        long alertasAntes = alertaRepository.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(cultivo.getIdCultivo()).size();
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "idSensor": %d,
                    "valor": 45.50
                }
                """, dispositivo.getIdDispositivo(), sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(0)));

        assertEquals(1, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
        assertEquals(alertasAntes, alertaRepository.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(cultivo.getIdCultivo()).size());

        DispositivoIoT actualizado = dispositivoRepository.findById(dispositivo.getIdDispositivo()).orElseThrow();
        assertNotNull(actualizado.getUltimaConexion());
    }

    @Test
    void postLecturaFueraDeRangoBajoMinimo_generaAlertaYNotificacion() throws Exception {
        String json = String.format("""
                {
                    "idSensor": %d,
                    "valor": 14.20
                }
                """, sensorHumedad.getIdSensor());

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)));

        var alertas = alertaRepository.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(cultivo.getIdCultivo());
        assertEquals(1, alertas.size());
        assertTrue(alertas.get(0).getMensaje().contains("14.20"));

        var notificaciones = notificacionRepository.findByAlertaIdAlerta(alertas.get(0).getIdAlerta());
        assertEquals(1, notificaciones.size());
        assertEquals(usuario.getIdUsuario(), notificaciones.get(0).getUsuario().getIdUsuario());
        assertFalse(notificaciones.get(0).isLeido());
    }

    @Test
    void postLecturaSobreMaximo_generaAlertaYNotificacion() throws Exception {
        String json = """
                {
                    "tipoSensor": "humedad_suelo",
                    "valor": 88.00
                }
                """;

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(1)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)));

        assertEquals(1, alertaRepository.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(cultivo.getIdCultivo()).size());
    }

    @Test
    void postLoteMixto_procesaValidasYRechazaInvalidasSinFallarElLote() throws Exception {
        String json = """
                {
                    "lecturas": [
                        { "tipoSensor": "humedad_suelo", "valor": 12.00 },
                        { "tipoSensor": "humedad_suelo", "valor": 150.00 },
                        { "tipoSensor": "ph_suelo", "valor": 6.50 },
                        { "tipoSensor": "ph_suelo", "valor": 22.00 },
                        { "tipoSensor": "sensor_inexistente", "valor": 10.00 }
                    ]
                }
                """;

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.lecturasProcesadas", is(2)))
                .andExpect(jsonPath("$.lecturasRechazadas", is(3)))
                .andExpect(jsonPath("$.alertasGeneradas", is(1)))
                .andExpect(jsonPath("$.errores", hasSize(3)));

        assertEquals(1, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
        assertEquals(1, alertaRepository.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(cultivo.getIdCultivo()).size());
    }

    @Test
    void postLecturaUnicaFueraDeRangoFisico_retorna400() throws Exception {
        String json = """
                {
                    "tipoSensor": "humedad_suelo",
                    "valor": 150.00
                }
                """;

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("rango físico")));

        assertEquals(0, lecturaRepository.findBySensorIdSensorOrderByFechaHoraDesc(sensorHumedad.getIdSensor()).size());
    }

    @Test
    void postConIdDispositivoDeOtroNodo_retorna400() throws Exception {
        DispositivoIoT otro = cultivoService.registrarDispositivo(cultivo.getIdCultivo());
        String json = String.format("""
                {
                    "idDispositivo": %d,
                    "tipoSensor": "humedad_suelo",
                    "valor": 40.00
                }
                """, otro.getIdDispositivo());

        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.mensaje", containsString("no corresponde al nodo autenticado")));
    }

    @Test
    void postSensorInexistenteYTipoDesconocido_retorna404() throws Exception {
        String jsonSensor = """
                {
                    "idSensor": 999999,
                    "valor": 40.00
                }
                """;
        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSensor))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));

        String jsonTipo = """
                {
                    "tipoSensor": "salinidad_inexistente",
                    "valor": 7.00
                }
                """;
        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonTipo))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void postPayloadVacio_retorna400() throws Exception {
        mockMvc.perform(post("/api/telemetria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenPlano)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    void apiSinCsrfNoRompeLoginNiCsrfEnRutasWeb() throws Exception {
        // Anónimo a ruta web -> sigue redirigiendo a /login
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // POST web sin CSRF -> sigue respondiendo 403
        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/eliminar")
                        .with(user(usuario.getCorreo()).roles("USER", "ADMIN")))
                .andExpect(status().isForbidden());
    }
}
