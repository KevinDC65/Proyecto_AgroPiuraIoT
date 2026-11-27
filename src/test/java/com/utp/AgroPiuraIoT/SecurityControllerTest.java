package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Auditoria;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.AuditoriaRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.service.AuditoriaService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

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
    void apiDeTelemetriaExigeTokenDeNodoResponde401EnJson() throws Exception {
        // Sin token: 401 en JSON (sin redirección al login).
        mockMvc.perform(post("/api/telemetria")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        // Con sesión web pero sin token Bearer de nodo: también 401 (aislada de la sesión web).
        mockMvc.perform(post("/api/telemetria")
                        .with(user("admin@agropiura.com"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
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
                        .param("contrasena", "clave1234")
                        .param("confirmacion", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registrado"));

        // Intento de registro duplicado
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Usuario Prueba 2")
                        .param("correo", "prueba_sec@agropiura.pe")
                        .param("contrasena", "clave1234")
                        .param("confirmacion", "clave1234"))
                .andExpect(status().isOk()); // Retorna al formulario con error
    }

    @Test
    void contrasenaDebilRechazadaEnFormularioDeRegistro() throws Exception {
        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Clave Débil")
                        .param("correo", "debil1@agropiura.pe")
                        .param("contrasena", "12345678")
                        .param("confirmacion", "12345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("registro", "contrasena"));

        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Clave Sin Números")
                        .param("correo", "debil2@agropiura.pe")
                        .param("contrasena", "sololetras")
                        .param("confirmacion", "sololetras"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeHasFieldErrors("registro", "contrasena"));
    }

    @Test
    void usuarioInactivoNoEntraYMensajeNoRevelaEstado() throws Exception {
        Usuario usuario = usuarioService.registrar(new RegistroRequest(
                "Cuenta Inactiva", "inactivo.login@agropiura.pe", "clave1234", "clave1234"));
        usuario.setEstado("inactivo");
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "inactivo.login@agropiura.pe")
                        .param("password", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No se pudo iniciar sesión")))
                .andExpect(content().string(not(containsString("inactiva"))))
                .andExpect(content().string(not(containsString("bloqueada"))));
    }

    @Test
    void bloqueoTrasCincoFallosDesbloqueoPorTiempoYAuditoria() throws Exception {
        Usuario usuario = usuarioService.registrar(new RegistroRequest(
                "Usuario Bloqueo", "bloqueo.login@agropiura.pe", "clave1234", "clave1234"));

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "bloqueo.login@agropiura.pe")
                            .param("password", "clave_erronea"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        Usuario bloqueado = usuarioRepository.findByCorreo("bloqueo.login@agropiura.pe").orElseThrow();
        assertEquals(5, bloqueado.getIntentosFallidos());
        assertNotNull(bloqueado.getBloqueadoHasta());
        assertTrue(bloqueado.estaBloqueado(LocalDateTime.now()));

        // Incluso con la clave correcta no entra mientras está bloqueado
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "bloqueo.login@agropiura.pe")
                        .param("password", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        // Simular expiración del bloqueo por tiempo
        bloqueado.setBloqueadoHasta(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.save(bloqueado);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "bloqueo.login@agropiura.pe")
                        .param("password", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Usuario recuperado = usuarioRepository.findByCorreo("bloqueo.login@agropiura.pe").orElseThrow();
        assertEquals(0, recuperado.getIntentosFallidos());
        assertNull(recuperado.getBloqueadoHasta());

        List<Auditoria> eventos = auditoriaRepository
                .findByUsuarioIdUsuarioOrderByFechaDesc(usuario.getIdUsuario());
        assertTrue(eventos.stream().anyMatch(e -> AuditoriaService.ACCION_LOGIN_FALLIDO.equals(e.getAccion())));
        assertTrue(eventos.stream().anyMatch(e -> AuditoriaService.ACCION_CUENTA_BLOQUEADA.equals(e.getAccion())));
        assertTrue(eventos.stream().anyMatch(e -> AuditoriaService.ACCION_LOGIN_EXITOSO.equals(e.getAccion())));
    }

    @Test
    void ingresoCorrectoReiniciaContadorDeIntentosFallidos() throws Exception {
        usuarioService.registrar(new RegistroRequest(
                "Usuario Reinicio", "reinicio.login@agropiura.pe", "clave1234", "clave1234"));

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "reinicio.login@agropiura.pe")
                            .param("password", "erronea123"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        Usuario parcial = usuarioRepository.findByCorreo("reinicio.login@agropiura.pe").orElseThrow();
        assertEquals(3, parcial.getIntentosFallidos());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "reinicio.login@agropiura.pe")
                        .param("password", "clave1234"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        Usuario reiniciado = usuarioRepository.findByCorreo("reinicio.login@agropiura.pe").orElseThrow();
        assertEquals(0, reiniciado.getIntentosFallidos());
        assertNull(reiniciado.getBloqueadoHasta());
    }

    @Test
    void cuentasDemoExistentesSiguenPudiendoEntrar() throws Exception {
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "colaborador@agropiura.com")
                        .param("password", "AgroColab2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "usuario@agropiura.com")
                        .param("password", "AgroUser2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }
}
