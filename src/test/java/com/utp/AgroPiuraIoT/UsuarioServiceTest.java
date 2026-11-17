package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertThrows(ReglaNegocioException.class, () -> usuarios.registrar(
                new RegistroRequest("Ana 2", "ana@agropiura.pe", "otraclave1", "otraclave1")));
    }

    @Test
    void rechazaClavesDistintas() {
        assertThrows(ReglaNegocioException.class, () -> usuarios.registrar(
                new RegistroRequest("Luis", "luis@agropiura.pe", "secreta123", "distinta456")));
    }

    @Test
    void rechazaRequestNulo() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(null));
    }

    @Test
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(
                new RegistroRequest("   ", "pedro@agropiura.pe", "secreta123", "secreta123")));
    }

    @Test
    void rechazaCorreoVacio() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(
                new RegistroRequest("Pedro", "   ", "secreta123", "secreta123")));
    }

    @Test
    void rechazaContrasenaVacia() {
        assertThrows(IllegalArgumentException.class, () -> usuarios.registrar(
                new RegistroRequest("Pedro", "pedro@agropiura.pe", "", "")));
    }

    @Test
    void buscarPorCorreo() {
        usuarios.registrar(new RegistroRequest("Rosa", "rosa@agropiura.pe", "secreta123", "secreta123"));
        Usuario encontrado = usuarios.buscarPorCorreo("ROSA@agropiura.pe");
        assertNotNull(encontrado);
        assertEquals("rosa@agropiura.pe", encontrado.getCorreo());
        assertEquals("Rosa", encontrado.getNombre());

        assertNull(usuarios.buscarPorCorreo("inexistente@agropiura.pe"));
        assertNull(usuarios.buscarPorCorreo("  "));
    }
}
