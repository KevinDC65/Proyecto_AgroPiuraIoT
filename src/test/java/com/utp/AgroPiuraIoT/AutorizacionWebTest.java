package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Autorización por cultivo (RF-07) de extremo a extremo: filtro de seguridad,
 * {@code @PreAuthorize}, controladores y plantillas Thymeleaf reales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutorizacionWebTest {

    private static final String ADMIN = "admin.web@test.pe";
    private static final String COLABORADOR = "colab.web@test.pe";
    private static final String AJENO = "ajeno.web@test.pe";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private AccesoService accesos;

    @Autowired
    private TipoSensorRepository tipos;

    private Integer idCultivo;

    @BeforeEach
    void preparar() {
        for (String correo : List.of(ADMIN, COLABORADOR, AJENO)) {
            usuarios.registrar(new RegistroRequest("Usuario", correo, "secreta123", "secreta123"));
        }
        Cultivo cultivo = cultivos.crear(new CultivoForm("Mango - Web", "mango", "Piura",
                new BigDecimal("1.50"), null, null), ADMIN);
        idCultivo = cultivo.getIdCultivo();
        accesos.asignar(idCultivo, COLABORADOR, AccesoService.ROL_COLABORADOR);
    }

    @Test
    void anonimoEsRedirigidoAlLogin() throws Exception {
        mvc.perform(get("/cultivos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void cadaUsuarioSoloVeSusCultivos() throws Exception {
        mvc.perform(get("/cultivos").with(user(COLABORADOR)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cultivos", hasSize(1)));
        mvc.perform(get("/cultivos").with(user(AJENO)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("cultivos", hasSize(0)));
    }

    @Test
    void usuarioAjenoNoPuedeVerElCultivo() throws Exception {
        mvc.perform(get("/cultivos/{id}", idCultivo).with(user(AJENO)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/cultivos/{id}/historico", idCultivo).with(user(AJENO)))
                .andExpect(status().isForbidden());
    }

    @Test
    void cultivoInexistenteNoRevelaSiExiste() throws Exception {
        mvc.perform(get("/cultivos/{id}", 999999).with(user(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void colaboradorVePeroNoAdministra() throws Exception {
        mvc.perform(get("/cultivos/{id}", idCultivo).with(user(COLABORADOR)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("puedeAdministrar", false));
        mvc.perform(post("/cultivos/{id}/eliminar", idCultivo).with(user(COLABORADOR)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/cultivos/{id}/accesos", idCultivo).with(user(COLABORADOR)).with(csrf())
                        .param("correo", AJENO).param("rol", "administrador"))
                .andExpect(status().isForbidden());
    }

    @Test
    void colaboradorPuedeSimularLecturas() throws Exception {
        cultivos.registrarDispositivo(idCultivo);
        mvc.perform(post("/cultivos/{id}/simular", idCultivo).with(user(COLABORADOR)).with(csrf()))
                .andExpect(redirectedUrl("/cultivos/" + idCultivo));
    }

    @Test
    void administradorEliminaSuCultivo() throws Exception {
        mvc.perform(get("/cultivos/{id}", idCultivo).with(user(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("puedeAdministrar", true));
        mvc.perform(post("/cultivos/{id}/eliminar", idCultivo).with(user(ADMIN)).with(csrf()))
                .andExpect(redirectedUrl("/cultivos"));
    }

    @Test
    void postSinCsrfEsRechazado() throws Exception {
        mvc.perform(post("/cultivos/{id}/eliminar", idCultivo).with(user(ADMIN)))
                .andExpect(status().isForbidden());
    }

    @Test
    void umbralConRangoInvertidoMuestraErrorEnElFormulario() throws Exception {
        Integer idTipo = tipos.findAll().get(0).getIdTipoSensor();
        mvc.perform(post("/cultivos/{id}/umbrales", idCultivo).with(user(ADMIN)).with(csrf())
                        .param("idTipoSensor", idTipo.toString())
                        .param("valorMinimo", "80")
                        .param("valorMaximo", "20"))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void umbralDeOtroCultivoDevuelve404() throws Exception {
        mvc.perform(get("/cultivos/{id}/umbrales", idCultivo).param("editar", "999999").with(user(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoConRangoInvertidoMuestraMensajeYConservaElFiltro() throws Exception {
        LocalDate hoy = LocalDate.now();
        mvc.perform(get("/cultivos/{id}/historico", idCultivo).with(user(COLABORADOR))
                        .param("desde", hoy.toString())
                        .param("hasta", hoy.minusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("desde", hoy));
    }

    @Test
    void dashboardMapaYAlertasRenderizan() throws Exception {
        for (String ruta : List.of("/dashboard", "/mapa", "/alertas")) {
            mvc.perform(get(ruta).with(user(COLABORADOR))).andExpect(status().isOk());
        }
    }

    @Test
    void crearCultivoDejaAlCreadorComoAdministrador() throws Exception {
        mvc.perform(post("/cultivos").with(user(AJENO)).with(csrf())
                        .param("nombre", "Limón - Nuevo")
                        .param("tipoCultivo", "limon")
                        .param("ubicacion", "Sullana"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/cultivos").with(user(AJENO)))
                .andExpect(model().attribute("cultivos", hasSize(1)));
    }
}
