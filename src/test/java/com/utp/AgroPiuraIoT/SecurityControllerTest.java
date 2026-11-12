package com.utp.AgroPiuraIoT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rutasPublicasSonAccesiblesSinAutenticacion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk());
    }

    @Test
    void rutaApiPublicaSinCsrf() throws Exception {
        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    // No debe ser redireccion a login (302) ni prohibido por CSRF (403)
                    assert sc != 302 && sc != 403;
                });
    }

    @Test
    void rutasProtegidasRedirigenALoginSinSesion() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/cultivos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/alertas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        mockMvc.perform(get("/umbrales"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void rutasProtegidasAccesiblesConSesion() throws Exception {
        mockMvc.perform(get("/dashboard")
                        .with(user("admin@agropiura.com").roles("USER", "ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cultivos")
                        .with(user("admin@agropiura.com").roles("USER", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void noAdminNoPuedeModificarAccesos() throws Exception {
        mockMvc.perform(post("/cultivos/1/accesos")
                        .with(user("colab@agropiura.pe").roles("USER", "COLABORADOR"))
                        .with(csrf())
                        .param("correo", "test@agropiura.pe")
                        .param("rol", "colaborador"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginExitosoYFallido() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin@agropiura.com")
                        .param("password", "AgroAdmin2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "admin@agropiura.com")
                        .param("password", "clave_erronea"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutRedirigeAPortada() throws Exception {
        mockMvc.perform(post("/logout")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?logout"));
    }

    @Test
    void registroFlujoCompleto() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Usuario Prueba")
                        .param("correo", "prueba_sec@agropiura.pe")
                        .param("contrasena", "12345678")
                        .param("confirmacion", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        // Intento de registro duplicado
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Usuario Prueba 2")
                        .param("correo", "prueba_sec@agropiura.pe")
                        .param("contrasena", "12345678")
                        .param("confirmacion", "12345678"))
                .andExpect(status().isOk()); // Retorna al formulario con error
    }
}
