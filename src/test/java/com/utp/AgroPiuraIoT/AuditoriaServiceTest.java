package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Auditoria;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.service.AuditoriaService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AuditoriaServiceTest {

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void registraEventoConUsuarioYDatosCompletos() {
        Usuario usuario = usuarioService.registrar(new RegistroRequest(
                "Lucía Audit", "lucia.audit@agropiura.pe", "clave1234", "clave1234"));

        Auditoria guardada = auditoriaService.registrar(
                usuario, "umbral_actualizado", "Umbral", 42, "Humedad 30-75 %");

        assertNotNull(guardada.getId());
        assertNotNull(guardada.getFecha());
        assertEquals(usuario.getIdUsuario(), guardada.getUsuario().getIdUsuario());
        assertEquals("UMBRAL_ACTUALIZADO", guardada.getAccion());
        assertEquals("umbral", guardada.getEntidad());
        assertEquals(42, guardada.getIdEntidad());
        assertEquals("Humedad 30-75 %", guardada.getDetalle());

        List<Auditoria> porEntidad = auditoriaService.listarPorEntidad("umbral", 42);
        assertTrue(porEntidad.stream().anyMatch(a -> a.getId().equals(guardada.getId())));
    }

    @Test
    void permiteUsuarioNuloYTruncaDetalleExtenso() {
        String detalleLargo = "x".repeat(650);

        Auditoria guardada = auditoriaService.registrar(
                null,
                AuditoriaService.ACCION_LOGIN_FALLIDO,
                AuditoriaService.ENTIDAD_USUARIO,
                null,
                detalleLargo);

        assertNotNull(guardada.getId());
        assertNull(guardada.getUsuario());
        assertEquals(500, guardada.getDetalle().length());
    }

    @Test
    void registraPorCorreoResolviendoElUsuario() {
        Usuario usuario = usuarioService.registrar(new RegistroRequest(
                "Pedro Audit", "pedro.audit@agropiura.pe", "clave1234", "clave1234"));

        Auditoria guardada = auditoriaService.registrarPorCorreo(
                "PEDRO.AUDIT@agropiura.pe", "CULTIVO_ELIMINADO", "cultivo", 15, "Parcela sur");

        assertNotNull(guardada.getUsuario());
        assertEquals(usuario.getIdUsuario(), guardada.getUsuario().getIdUsuario());
    }

    @Test
    void rechazaAccionOEntidadVacia() {
        assertThrows(IllegalArgumentException.class, () ->
                auditoriaService.registrar(null, "  ", "usuario", 1, "detalle"));
        assertThrows(IllegalArgumentException.class, () ->
                auditoriaService.registrar(null, "LOGIN_FALLIDO", "", 1, "detalle"));
    }
}
