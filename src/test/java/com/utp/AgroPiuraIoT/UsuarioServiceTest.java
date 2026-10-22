package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UsuarioServiceTest {

    @Autowired
    private UsuarioService usuarios;

    @Test
    void registraConClaveCifrada() {
        Usuario creado = usuarios.registrar(
                new RegistroRequest("Kevin", "kevin@agropiura.pe", "secreta123", "secreta123"));
        assertTrue(creado.getIdUsuario() != null);
        assertNotEquals("secreta123", creado.getContrasena());
        assertTrue(creado.getContrasena().startsWith("$2a$")
                || creado.getContrasena().startsWith("$2b$"));
        assertEquals("activo", creado.getEstado());
    }

    @Test
    void rechazaCorreoDuplicado() {
        usuarios.registrar(new RegistroRequest("Ana", "ana@agropiura.pe", "secreta123", "secreta123"));
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(
                new RegistroRequest("Ana 2", "ana@agropiura.pe", "otraclave1", "otraclave1")));
    }

    @Test
    void rechazaClavesDistintas() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(
                new RegistroRequest("Luis", "luis@agropiura.pe", "secreta123", "distinta456")));
    }
}
