package com.utp.AgroPiuraIoT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El seeder debe garantizar un administrador de demostración para poder
 * gestionar accesos (RF-07) desde el primer arranque de la aplicación.
 */
@SpringBootTest
class DataSeederTest {

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService detalles;

    @Autowired
    private PasswordEncoder claves;

    /**
     * El usuario demo admin@agropiura.com autentica con su clave conocida y
     * posee la autoridad ROLE_ADMIN (nestor de la gestión de accesos RF-07).
     */
    @Test
    void usuarioAdministradorDemoExisteYAutentica() {
        org.springframework.security.core.userdetails.UserDetails admin =
                detalles.loadUserByUsername("admin@agropiura.com");

        assertEquals("admin@agropiura.com", admin.getUsername());
        assertTrue(claves.matches("AgroAdmin2026", admin.getPassword()));
        assertTrue(admin.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
