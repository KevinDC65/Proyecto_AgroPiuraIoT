package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.service.AccesoService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AccesoServiceTest {

    @Autowired
    private AccesoService accesos;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UsuarioService usuarios;

    private Integer crearCultivo(String nombre) {
        return cultivos.guardar(new CultivoForm(nombre, "limon",
                "San Lorenzo", new BigDecimal("2.50"), null, null)).getIdCultivo();
    }

    private void crearUsuario(String correo) {
        usuarios.registrar(new RegistroRequest("Colaborador Prueba", correo,
                "Secreta999", "Secreta999"));
    }

    @Test
    void asignaAccesoPorCorreoYRol() {
        Integer idCultivo = crearCultivo("Limón - Accesos A");
        crearUsuario("colab.acc1@test.com");

        accesos.asignar(idCultivo, "colab.acc1@test.com", "colaborador");

        List<UsuarioCultivo> lista = accesos.listar(idCultivo);
        assertEquals(1, lista.size());
        assertEquals("colaborador", lista.get(0).getRol().getNombreRol());
    }

    @Test
    void reasignarActualizaElRolSinDuplicar() {
        Integer idCultivo = crearCultivo("Limón - Accesos B");
        crearUsuario("colab.acc2@test.com");

        accesos.asignar(idCultivo, "colab.acc2@test.com", "colaborador");
        accesos.asignar(idCultivo, "colab.acc2@test.com", "administrador");

        List<UsuarioCultivo> lista = accesos.listar(idCultivo);
        assertEquals(1, lista.size());
        assertEquals("administrador", lista.get(0).getRol().getNombreRol());
    }

    @Test
    void quitarEliminaElAcceso() {
        Integer idCultivo = crearCultivo("Limón - Accesos C");
        crearUsuario("colab.acc3@test.com");
        accesos.asignar(idCultivo, "colab.acc3@test.com", "colaborador");

        accesos.quitar(idCultivo, "colab.acc3@test.com");

        assertTrue(accesos.listar(idCultivo).isEmpty());
    }

    @Test
    void correoInexistenteFallaConMensaje() {
        Integer idCultivo = crearCultivo("Limón - Accesos D");

        Exception e = assertThrows(IllegalArgumentException.class,
                () -> accesos.asignar(idCultivo, "nadie@test.com", "colaborador"));
        assertEquals("No existe un usuario con ese correo", e.getMessage());
    }

    @Test
    void rolInexistenteFallaConMensaje() {
        Integer idCultivo = crearCultivo("Limón - Accesos E");
        crearUsuario("colab.acc4@test.com");

        Exception e = assertThrows(IllegalArgumentException.class,
                () -> accesos.asignar(idCultivo, "colab.acc4@test.com", "visitante"));
        assertEquals("Rol no reconocido: visitante", e.getMessage());
    }
}
