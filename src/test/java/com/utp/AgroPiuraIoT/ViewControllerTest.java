package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.service.CultivoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CultivoService cultivos;

    private Cultivo registrarCultivoDemo() {
        return cultivos.guardar(new CultivoForm(
                "Mango Haden Fundo San José",
                "mango",
                "Chulucanas",
                new BigDecimal("6.5"),
                new BigDecimal("-5.09"),
                new BigDecimal("-80.16")));
    }

    @Test
    @DisplayName("La portada pública renderiza la plantilla index correctamente")
    void portadaPublicaRenderizaIndex() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("La vista de login renderiza la plantilla login")
    void loginRenderizaVista() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Login con parámetros de error, logout y registro renderiza correctamente")
    void loginConParametrosDeEstado() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(get("/login").param("logout", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(get("/login").param("registrado", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("El formulario de registro renderiza la plantilla registro con el modelo registro")
    void registroRenderizaFormulario() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("registro"));
    }

    @Test
    @DisplayName("El dashboard protegido renderiza la plantilla dashboard y sus modelos")
    void dashboardRenderizaConModelos() throws Exception {
        registrarCultivoDemo();

        mockMvc.perform(get("/dashboard")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("resumen"))
                .andExpect(model().attributeExists("notificacionesPendientes"));
    }

    @Test
    @DisplayName("La lista de cultivos renderiza cultivos/lista")
    void listaCultivosRenderiza() throws Exception {
        registrarCultivoDemo();

        mockMvc.perform(get("/cultivos")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/lista"))
                .andExpect(model().attributeExists("cultivos"));
    }

    @Test
    @DisplayName("El formulario de nuevo cultivo renderiza cultivos/form")
    void nuevoCultivoRenderizaForm() throws Exception {
        mockMvc.perform(get("/cultivos/nuevo")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("El detalle del cultivo renderiza cultivos/detalle")
    void detalleCultivoRenderiza() throws Exception {
        Cultivo c = registrarCultivoDemo();

        mockMvc.perform(get("/cultivos/" + c.getIdCultivo())
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/detalle"))
                .andExpect(model().attributeExists("cultivo"));
    }

    @Test
    @DisplayName("La edición de cultivo renderiza cultivos/form con el modelo form y cultivoId")
    void editarCultivoRenderizaForm() throws Exception {
        Cultivo c = registrarCultivoDemo();

        mockMvc.perform(get("/cultivos/" + c.getIdCultivo() + "/editar")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().attributeExists("form", "cultivoId"));
    }

    @Test
    @DisplayName("La vista de umbrales renderiza umbrales/lista con variables y formulario")
    void umbralesListaRenderiza() throws Exception {
        Cultivo c = registrarCultivoDemo();

        mockMvc.perform(get("/cultivos/" + c.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().attributeExists("cultivo", "umbrales", "tiposSensor", "form"));
    }

    @Test
    @DisplayName("La vista de alertas renderiza alertas/lista con notificaciones y alertas")
    void alertasListaRenderiza() throws Exception {
        mockMvc.perform(get("/alertas")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("alertas/lista"))
                .andExpect(model().attributeExists("alertas", "notificaciones", "notificacionesPendientes"));
    }

    @Test
    @DisplayName("La vista de mapa renderiza mapa/lista con el resumen geoespacial")
    void mapaListaRenderiza() throws Exception {
        registrarCultivoDemo();

        mockMvc.perform(get("/mapa")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("mapa/lista"))
                .andExpect(model().attributeExists("resumen"));
    }

    @Test
    @DisplayName("La vista de histórico renderiza historico/lista")
    void historicoListaRenderiza() throws Exception {
        Cultivo c = registrarCultivoDemo();

        mockMvc.perform(get("/cultivos/" + c.getIdCultivo() + "/historico")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("historico/lista"))
                .andExpect(model().attributeExists("cultivo", "puntos", "resumen"));
    }

    @Test
    @DisplayName("La vista de accesos renderiza accesos/lista")
    void accesosListaRenderiza() throws Exception {
        Cultivo c = registrarCultivoDemo();

        mockMvc.perform(get("/cultivos/" + c.getIdCultivo() + "/accesos")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("accesos/lista"))
                .andExpect(model().attributeExists("cultivo", "accesos"));
    }
}
