package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.exception.ReglaNegocioException;
import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.CultivoRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CultivoServiceTest {

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private SensorRepository sensores;

    @Autowired
    private TipoSensorRepository tipos;

    @Autowired
    private DispositivoIoTRepository dispositivos;

    @Autowired
    private CultivoRepository cultivoRepo;

    private CultivoForm formulario() {
        return new CultivoForm("Mango - Parcela A", "mango", "San Lorenzo",
                new java.math.BigDecimal("3.20"), null, null);
    }

    @Test
    void guardaYLista() {
        Cultivo creado = cultivos.guardar(formulario());
        assertTrue(creado.getIdCultivo() != null);
        assertFalse(cultivos.listar().isEmpty());
    }

    @Test
    void actualizaYElimina() {
        Cultivo creado = cultivos.guardar(formulario());
        cultivos.actualizar(creado.getIdCultivo(),
                new CultivoForm("Mango - Parcela A2", "mango", "Tambogrande",
                        new java.math.BigDecimal("4.50"), null, null));
        assertEquals("Tambogrande", cultivos.buscar(creado.getIdCultivo()).getUbicacion());
        cultivos.eliminar(creado.getIdCultivo());
        assertTrue(cultivos.listar().stream()
                .noneMatch(c -> c.getIdCultivo().equals(creado.getIdCultivo())));
    }

    @Test
    void detectaCultivosSinAdministrador() {
        Cultivo sinDueno = cultivos.guardar(formulario());
        usuarios.registrar(new RegistroRequest("Dueña", "duena.sinadmin@test.pe", "Clave12345", "Clave12345"));
        Cultivo conDueno = cultivos.crear(formulario(), "duena.sinadmin@test.pe");

        List<Integer> ids = cultivoRepo.findSinAdministrador().stream().map(Cultivo::getIdCultivo).toList();

        assertTrue(ids.contains(sinDueno.getIdCultivo()));
        assertFalse(ids.contains(conDueno.getIdCultivo()));
    }

    @Test
    void eliminarBorraLosNodosQueQuedanSinCultivoYConservaLosCompartidos() {
        Cultivo a = cultivos.guardar(formulario());
        Cultivo b = cultivos.guardar(formulario());
        DispositivoIoT propio = cultivos.registrarDispositivo(a.getIdCultivo());
        DispositivoIoT compartido = cultivos.registrarDispositivo(a.getIdCultivo());
        cultivos.asignarDispositivo(b.getIdCultivo(), compartido.getIdDispositivo());

        cultivos.eliminar(a.getIdCultivo());

        assertFalse(dispositivos.existsById(propio.getIdDispositivo()),
                "el nodo que solo pertenecía al cultivo eliminado no debe quedar huérfano");
        assertTrue(sensores.findByDispositivoIdDispositivo(propio.getIdDispositivo()).isEmpty());
        assertTrue(dispositivos.existsById(compartido.getIdDispositivo()),
                "un nodo asignado a otro cultivo se conserva");
        assertEquals(1, cultivos.buscar(b.getIdCultivo()).getDispositivos().size());
    }

    @Test
    void registraYAsignaDispositivo() {
        Cultivo creado = cultivos.guardar(formulario());
        DispositivoIoT nodo = cultivos.registrarDispositivo(creado.getIdCultivo());
        assertTrue(nodo.getIdDispositivo() != null);
        assertEquals("activo", nodo.getEstado());
        assertEquals(1, cultivos.buscar(creado.getIdCultivo()).getDispositivos().size());
        // El nodo nace con un sensor por tipo del catálogo: sin ellos no habría lecturas.
        assertEquals(tipos.count(), sensores.findByDispositivoIdDispositivo(nodo.getIdDispositivo()).size());
    }

    @Test
    void crearDejaAlCreadorComoAdministradorYSoloLoVeEl() {
        usuarios.registrar(new RegistroRequest("Dueño", "dueno.cultivo@test.pe", "secreta123", "secreta123"));
        usuarios.registrar(new RegistroRequest("Ajeno", "ajeno.cultivo@test.pe", "secreta123", "secreta123"));

        Cultivo creado = cultivos.crear(formulario(), "dueno.cultivo@test.pe");

        assertEquals(List.of(creado.getIdCultivo()), cultivos.listarPara("dueno.cultivo@test.pe").stream()
                .map(Cultivo::getIdCultivo).toList());
        assertTrue(cultivos.idsAdministradosPor("dueno.cultivo@test.pe").contains(creado.getIdCultivo()));
        assertTrue(cultivos.listarPara("ajeno.cultivo@test.pe").isEmpty());
    }

    @Test
    void buscarInexistenteLanzaNoEncontrado() {
        assertThrows(RecursoNoEncontradoException.class, () -> cultivos.buscar(999999));
    }

    @Test
    void asignarNodoInexistenteOYaAsignadoEsReglaDeNegocio() {
        Cultivo creado = cultivos.guardar(formulario());
        DispositivoIoT nodo = cultivos.registrarDispositivo(creado.getIdCultivo());

        assertThrows(ReglaNegocioException.class,
                () -> cultivos.asignarDispositivo(creado.getIdCultivo(), 999999));
        assertThrows(ReglaNegocioException.class,
                () -> cultivos.asignarDispositivo(creado.getIdCultivo(), nodo.getIdDispositivo()));
    }
}
