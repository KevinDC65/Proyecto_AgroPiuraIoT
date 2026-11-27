package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UmbralControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private TipoSensorRepository tipos;

    private Cultivo crearCultivoDemo() {
        // Con permisos por cultivo, el cultivo necesita administrador: se crea como lo hace la web.
        return cultivos.crear(new CultivoForm(
                "Banano Orgánico Parcela 3", "banano", "Sullana",
                new BigDecimal("8.0"), null, null), "admin@agropiura.com");
    }

    @Test
    void menuUmbralesRedirigeAlPrimerCultivo() throws Exception {
        crearCultivoDemo();
        // Redirige al primer cultivo **del usuario** (por nombre), no al primero de toda la BD.
        Cultivo primerCultivo = cultivos.listarPara("admin@agropiura.com").get(0);

        mockMvc.perform(get("/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + primerCultivo.getIdCultivo() + "/umbrales"));
    }

    @Test
    void listaUmbralesCargaCorrectamente() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(get("/cultivos/" + cultivo.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().attributeExists("cultivo", "umbrales", "tiposSensor", "form"));
    }

    @Test
    void editarCargaUmbralEnFormulario() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        TipoSensor tipo = tipos.findAll().get(0);
        Umbral u = umbrales.guardar(cultivo.getIdCultivo(),
                new UmbralForm(tipo.getIdTipoSensor(), new BigDecimal("15"), new BigDecimal("60")));

        mockMvc.perform(get("/cultivos/" + cultivo.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .param("editar", u.getIdUmbral().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().attributeExists("umbralEditado", "form"));
    }

    @Test
    void guardarExitosoRedirigeAListaConFlash() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        TipoSensor tipo = tipos.findAll().get(0);

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("idTipoSensor", tipo.getIdTipoSensor().toString())
                        .param("valorMinimo", "20.5")
                        .param("valorMaximo", "75.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo() + "/umbrales"))
                .andExpect(flash().attribute("mensaje", "Umbral guardado"));
    }

    @Test
    void guardarConMinimoMayorQueMaximoMuestraError() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        TipoSensor tipo = tipos.findAll().get(0);

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("idTipoSensor", tipo.getIdTipoSensor().toString())
                        .param("valorMinimo", "90.0")
                        .param("valorMaximo", "40.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void eliminarRedirigeConFlash() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        TipoSensor tipo = tipos.findAll().get(0);
        Umbral u = umbrales.guardar(cultivo.getIdCultivo(),
                new UmbralForm(tipo.getIdTipoSensor(), new BigDecimal("15"), new BigDecimal("60")));

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/umbrales/" + u.getIdUmbral() + "/eliminar")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo() + "/umbrales"))
                .andExpect(flash().attribute("mensaje", "Umbral eliminado"));
    }

    @Test
    void guardarUmbralFueraDeRangoFisicoMuestraErrorEnFormulario() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        TipoSensor tipoPh = tipos.findAll().stream()
                .filter(t -> t.getNombreTipo().equalsIgnoreCase("ph_suelo"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/umbrales")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("idTipoSensor", tipoPh.getIdTipoSensor().toString())
                        .param("valorMinimo", "5.0")
                        .param("valorMaximo", "20.0"))
                .andExpect(status().isOk())
                .andExpect(view().name("umbrales/lista"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasErrors("form"));
    }
}
