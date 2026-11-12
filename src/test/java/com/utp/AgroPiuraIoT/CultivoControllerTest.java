package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CultivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private DispositivoIoTRepository dispositivosRepo;

    private Cultivo crearCultivoDemo() {
        return cultivos.guardar(new CultivoForm(
                "Mango Kent Parcela 1", "mango", "Sullana",
                new BigDecimal("5.0"), new BigDecimal("-4.90"), new BigDecimal("-80.68")));
    }

    @Test
    void listaMuestraCultivos() throws Exception {
        crearCultivoDemo();

        mockMvc.perform(get("/cultivos")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/lista"))
                .andExpect(model().attributeExists("cultivos"));
    }

    @Test
    void nuevoMuestraFormularioVacio() throws Exception {
        mockMvc.perform(get("/cultivos/nuevo")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void crearExitosoRedirigeADetalleConMensajeFlash() throws Exception {
        mockMvc.perform(post("/cultivos")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("nombre", "Uva Red Globe")
                        .param("tipoCultivo", "uva")
                        .param("ubicacion", "Piura")
                        .param("areaHa", "4.20")
                        .param("latitud", "-5.19")
                        .param("longitud", "-80.63"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensaje", "Cultivo registrado exitosamente"));
    }

    @Test
    void crearConErroresRetornaFormularioConValidaciones() throws Exception {
        mockMvc.perform(post("/cultivos")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("nombre", "")
                        .param("tipoCultivo", "")
                        .param("areaHa", "-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("form", "nombre", "tipoCultivo", "areaHa"));
    }

    @Test
    void detalleMuestraDatosDelCultivo() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(get("/cultivos/" + cultivo.getIdCultivo())
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/detalle"))
                .andExpect(model().attributeExists("cultivo"));
    }

    @Test
    void editarMuestraFormularioConDatosCargados() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(get("/cultivos/" + cultivo.getIdCultivo() + "/editar")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().attribute("cultivoId", cultivo.getIdCultivo()))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    void actualizarExitosoRedirigeADetalleConFlash() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/editar")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("nombre", "Mango Kent Actualizado")
                        .param("tipoCultivo", "mango")
                        .param("ubicacion", "Chulucanas")
                        .param("areaHa", "6.50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo()))
                .andExpect(flash().attribute("mensaje", "Cultivo actualizado exitosamente"));
    }

    @Test
    void actualizarConErroresRetornaFormulario() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/editar")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("nombre", "")
                        .param("tipoCultivo", "mango"))
                .andExpect(status().isOk())
                .andExpect(view().name("cultivos/form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("form", "nombre"));
    }

    @Test
    void eliminarRedirigeAListaConFlash() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/eliminar")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos"))
                .andExpect(flash().attribute("mensaje", "Cultivo eliminado exitosamente"));
    }

    @Test
    void registrarYAsignarDispositivoRedirigeADetalle() throws Exception {
        Cultivo cultivo = crearCultivoDemo();

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/dispositivos/nuevo")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo()))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    void asignarYDesasignarDispositivoRedirigeADetalle() throws Exception {
        Cultivo cultivo = crearCultivoDemo();
        DispositivoIoT dispositivo = dispositivosRepo.save(new DispositivoIoT());

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/dispositivos")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf())
                        .param("idDispositivo", dispositivo.getIdDispositivo().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo()))
                .andExpect(flash().attribute("mensaje", "Dispositivo #" + dispositivo.getIdDispositivo() + " asignado exitosamente"));

        mockMvc.perform(post("/cultivos/" + cultivo.getIdCultivo() + "/dispositivos/" + dispositivo.getIdDispositivo() + "/desasignar")
                        .with(user("admin@agropiura.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cultivos/" + cultivo.getIdCultivo()))
                .andExpect(flash().attribute("mensaje", "Dispositivo #" + dispositivo.getIdDispositivo() + " desasignado exitosamente"));
    }

    @Test
    void exportarExcelGeneraArchivoDescargable() throws Exception {
        crearCultivoDemo();

        mockMvc.perform(get("/cultivos/exportar.xlsx")
                        .with(user("admin@agropiura.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("cultivos.xlsx")));
    }
}
