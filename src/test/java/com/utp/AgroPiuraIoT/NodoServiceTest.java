package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.AlertaNodo;
import com.utp.AgroPiuraIoT.model.entity.Auditoria;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.AlertaNodoRepository;
import com.utp.AgroPiuraIoT.repository.AuditoriaRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.NodoService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NodoServiceTest {

    private static final String ADMIN = "admin.nodos@test.pe";
    private static final String COLABORADOR = "colab.nodos@test.pe";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NodoService nodoService;

    @Autowired
    private CultivoService cultivoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AccesoService accesoService;

    @Autowired
    private DispositivoIoTRepository dispositivoRepository;

    @Autowired
    private AlertaNodoRepository alertaNodoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private Cultivo cultivo;

    @BeforeEach
    void preparar() {
        usuarioService.registrar(new RegistroRequest("Admin Nodos", ADMIN, "clave1234", "clave1234"));
        usuarioService.registrar(new RegistroRequest("Colab Nodos", COLABORADOR, "clave1234", "clave1234"));
        cultivo = cultivoService.crear(new CultivoForm(
                "Limón - Parcela IoT", "limon", "Tambogrande",
                new BigDecimal("4.00"), null, null), ADMIN);
        accesoService.asignar(cultivo.getIdCultivo(), COLABORADOR, AccesoService.ROL_COLABORADOR);
    }

    @Test
    void calculaEstadoEnLineaSinConexionEInactivo() {
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 24, 18, 0);
        DispositivoIoT nodo = new DispositivoIoT();

        // Sin reportes previos -> SIN_CONEXION
        assertEquals(DispositivoIoT.CONEXION_SIN_CONEXION, nodoService.estadoConexionDe(nodo, ahora));

        // Reportó hace 10 minutos (< 30) -> EN_LINEA
        nodo.setUltimaConexion(ahora.minusMinutes(10));
        assertEquals(DispositivoIoT.CONEXION_EN_LINEA, nodoService.estadoConexionDe(nodo, ahora));
        assertTrue(nodoService.estaEnLinea(nodo, ahora));

        // Reportó hace 45 minutos (> 30) -> SIN_CONEXION
        nodo.setUltimaConexion(ahora.minusMinutes(45));
        assertEquals(DispositivoIoT.CONEXION_SIN_CONEXION, nodoService.estadoConexionDe(nodo, ahora));
        assertFalse(nodoService.estaEnLinea(nodo, ahora));

        // Desactivado administrativamente -> INACTIVO
        nodo.marcarInactivo();
        nodo.setUltimaConexion(ahora.minusMinutes(1));
        assertEquals(DispositivoIoT.CONEXION_INACTIVO, nodoService.estadoConexionDe(nodo, ahora));
    }

    @Test
    void detectaNodoSinConexionAbreAlertaYCierraAlReconectar() {
        var creado = nodoService.registrarConToken(cultivo.getIdCultivo(), ADMIN);
        DispositivoIoT nodo = creado.dispositivo();
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 24, 20, 0);

        // Última conexión hace 8 horas (> 6 horas configuradas por defecto)
        nodo.setUltimaConexion(ahora.minusHours(8));
        dispositivoRepository.save(nodo);

        int primeraRonda = nodoService.verificarNodosSinConexion(ahora);
        assertEquals(1, primeraRonda);

        // Una segunda ejecución no duplica la alerta abierta
        int segundaRonda = nodoService.verificarNodosSinConexion(ahora.plusMinutes(5));
        assertEquals(0, segundaRonda);

        List<AlertaNodo> alertas = alertaNodoRepository.buscarPorUsuario(ADMIN);
        assertEquals(1, alertas.size());
        assertEquals(AlertaNodo.TIPO_NODO_SIN_CONEXION, alertas.get(0).getTipo());
        assertEquals(AlertaNodo.ESTADO_ABIERTA, alertas.get(0).getEstado());

        // Al reconectar el nodo, se cierra automáticamente
        nodoService.registrarConexion(nodo, ahora.plusMinutes(10));
        AlertaNodo cerrada = alertaNodoRepository.findById(alertas.get(0).getIdAlertaNodo()).orElseThrow();
        assertEquals(AlertaNodo.ESTADO_CERRADA, cerrada.getEstado());
        assertNotNull(cerrada.getFechaCierre());
    }

    @Test
    void gestionDeNodoRegistraAuditoriaSinExponerToken() {
        var creado = nodoService.registrarConToken(cultivo.getIdCultivo(), ADMIN);
        Integer idNodo = creado.dispositivo().getIdDispositivo();

        nodoService.cambiarEstado(cultivo.getIdCultivo(), idNodo, false, ADMIN);
        nodoService.cambiarEstado(cultivo.getIdCultivo(), idNodo, true, ADMIN);
        String nuevoToken = nodoService.regenerarToken(cultivo.getIdCultivo(), idNodo, ADMIN);

        List<Auditoria> eventos = auditoriaRepository
                .findByEntidadAndIdEntidadOrderByFechaDesc(NodoService.ENTIDAD_DISPOSITIVO, idNodo);

        assertTrue(eventos.stream().anyMatch(e -> NodoService.ACCION_NODO_REGISTRADO.equals(e.getAccion())));
        assertTrue(eventos.stream().anyMatch(e -> NodoService.ACCION_NODO_DESACTIVADO.equals(e.getAccion())));
        assertTrue(eventos.stream().anyMatch(e -> NodoService.ACCION_NODO_ACTIVADO.equals(e.getAccion())));
        assertTrue(eventos.stream().anyMatch(e -> NodoService.ACCION_NODO_TOKEN_REGENERADO.equals(e.getAccion())));
        // Nunca debe aparecer el token en claro en la auditoría
        assertTrue(eventos.stream().noneMatch(e -> e.getDetalle() != null
                && (e.getDetalle().contains(creado.tokenPlano()) || e.getDetalle().contains(nuevoToken))));
    }

    @Test
    void colaboradorNoVeAccionesDeNodoYRecibe403SiLasIntenta() throws Exception {
        var creado = nodoService.registrarConToken(cultivo.getIdCultivo(), ADMIN);
        Integer idNodo = creado.dispositivo().getIdDispositivo();

        // Colaborador ve el detalle del cultivo pero NO ve acciones de gestión del nodo
        mockMvc.perform(get("/cultivos/{id}", cultivo.getIdCultivo())
                        .with(user(COLABORADOR)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sin conexión")))
                .andExpect(content().string(not(containsString("Regenerar token"))))
                .andExpect(content().string(not(containsString("Registrar nodo nuevo"))));

        // Colaborador recibe 403 si intenta las acciones POST directamente
        mockMvc.perform(post("/cultivos/{id}/dispositivos/{idD}/estado", cultivo.getIdCultivo(), idNodo)
                        .with(user(COLABORADOR))
                        .with(csrf())
                        .param("activar", "false"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/cultivos/{id}/dispositivos/{idD}/token", cultivo.getIdCultivo(), idNodo)
                        .with(user(COLABORADOR))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // Administrador sí puede regenerar token y recibe flash con tokenNodo
        mockMvc.perform(post("/cultivos/{id}/dispositivos/{idD}/token", cultivo.getIdCultivo(), idNodo)
                        .with(user(ADMIN))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo()))
                .andExpect(flash().attributeExists("tokenNodo", "tokenNodoId"));
    }
}
