package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.security.AppUserDetailsService;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AppUserDetailsServiceTest {

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CultivoRepository cultivoRepository;

    @Autowired
    private AccesoService accesoService;

    @Test
    void cargaUsuarioRegistradoConRolUser() {
        usuarioService.registrar(new RegistroRequest("Carlos", "carlos@agropiura.pe", "password123", "password123"));
        UserDetails userDetails = userDetailsService.loadUserByUsername("carlos@agropiura.pe");

        assertNotNull(userDetails);
        assertEquals("carlos@agropiura.pe", userDetails.getUsername());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertFalse(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertFalse(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COLABORADOR")));
    }

    @Test
    void cargaUsuarioConRolColaborador() {
        Usuario usuario = usuarioService.registrar(
                new RegistroRequest("Marcos", "marcos@agropiura.pe", "password123", "password123"));
        Cultivo cultivo = new Cultivo();
        cultivo.setNombre("Parcela Mangos");
        cultivo.setTipoCultivo("mango");
        cultivo.setUbicacion("Chulucanas");
        cultivo.setAreaHa(new BigDecimal("3.00"));
        cultivo = cultivoRepository.save(cultivo);

        accesoService.asignar(cultivo.getIdCultivo(), usuario.getCorreo(), AccesoService.ROL_COLABORADOR);

        UserDetails userDetails = userDetailsService.loadUserByUsername("marcos@agropiura.pe");
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COLABORADOR")));
    }

    @Test
    void cargaUsuarioConRolAdmin() {
        // admin@agropiura.com ya está sembrado en DataSeeder con rol administrador
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@agropiura.com");

        assertNotNull(userDetails);
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void usuarioInactivoNoEstaHabilitado() {
        Usuario usuario = usuarioService.registrar(
                new RegistroRequest("Inactivo", "inactivo@agropiura.pe", "password123", "password123"));
        usuario.setEstado("inactivo");
        usuarioRepository.save(usuario);

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactivo@agropiura.pe");
        assertFalse(userDetails.isEnabled());
    }

    @Test
    void fallaSiUsuarioNoExiste() {
        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("noexiste@agropiura.pe"));
    }

    @Test
    void fallaSiCorreoVacio() {
        assertThrows(UsernameNotFoundException.class, () ->
                userDetailsService.loadUserByUsername("   "));
    }
}
