package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.NotificacionUsuario;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UmbralRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class CultivoServiceTest {

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private DispositivoIoTRepository dispositivosRepo;

    @Autowired
    private UmbralRepository umbralesRepo;

    @Autowired
    private TipoSensorRepository tiposRepo;

    @Autowired
    private SensorRepository sensoresRepo;

    @Autowired
    private LecturaRepository lecturasRepo;

    @Autowired
    private AlertaRepository alertasRepo;

    @Autowired
    private NotificacionUsuarioRepository notificacionesRepo;

    @Autowired
    private UsuarioRepository usuariosRepo;

    @Autowired
    private RolRepository rolesRepo;

    @Autowired
    private UsuarioCultivoRepository accesosRepo;

    private CultivoForm formulario() {
        return new CultivoForm("Mango - Parcela A", "mango", "San Lorenzo",
                new BigDecimal("3.20"), new BigDecimal("-4.920000"), new BigDecimal("-80.250000"));
    }

    @Test
    void guardaYLista() {
        Cultivo creado = cultivos.guardar(formulario());
        assertNotNull(creado.getIdCultivo());
        assertFalse(cultivos.listar().isEmpty());
    }

    @Test
    void actualizaExitosamente() {
        Cultivo creado = cultivos.guardar(formulario());
        cultivos.actualizar(creado.getIdCultivo(),
                new CultivoForm("Mango - Parcela A2", "mango", "Tambogrande",
                        new BigDecimal("4.50"), null, null));
        Cultivo actualizado = cultivos.buscar(creado.getIdCultivo());
        assertEquals("Mango - Parcela A2", actualizado.getNombre());
        assertEquals("Tambogrande", actualizado.getUbicacion());
        assertEquals(new BigDecimal("4.50"), actualizado.getAreaHa());
    }

    @Test
    void buscarExistenteYNoExistente() {
        Cultivo creado = cultivos.guardar(formulario());
        assertNotNull(cultivos.buscar(creado.getIdCultivo()));

        assertThrows(IllegalArgumentException.class, () -> cultivos.buscar(999999));
    }

    @Test
    void registraYAsignaDispositivo() {
        Cultivo creado = cultivos.guardar(formulario());
        DispositivoIoT nodo = cultivos.registrarDispositivo(creado.getIdCultivo());
        assertNotNull(nodo.getIdDispositivo());
        assertEquals("activo", nodo.getEstado());
        assertEquals(1, cultivos.buscar(creado.getIdCultivo()).getDispositivos().size());
    }

    @Test
    void asignarYDesasignarDispositivo() {
        Cultivo creado = cultivos.guardar(formulario());
        DispositivoIoT nuevo = new DispositivoIoT();
        nuevo = dispositivosRepo.save(nuevo);

        cultivos.asignarDispositivo(creado.getIdCultivo(), nuevo.getIdDispositivo());
        assertEquals(1, cultivos.buscar(creado.getIdCultivo()).getDispositivos().size());

        cultivos.desasignarDispositivo(creado.getIdCultivo(), nuevo.getIdDispositivo());
        assertTrue(cultivos.buscar(creado.getIdCultivo()).getDispositivos().isEmpty());
    }

    @Test
    void asignarDispositivoInexistenteLanzaExcepcion() {
        Cultivo creado = cultivos.guardar(formulario());
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.asignarDispositivo(creado.getIdCultivo(), 999999));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.desasignarDispositivo(creado.getIdCultivo(), 999999));
    }

    @Test
    void validacionesAlGuardarYActualizar() {
        assertThrows(IllegalArgumentException.class, () -> cultivos.guardar(null));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.guardar(new CultivoForm("", "mango", "Valle", new BigDecimal("1.0"), null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.guardar(new CultivoForm("Palto", "   ", "Valle", new BigDecimal("1.0"), null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.guardar(new CultivoForm("Palto", "palto", "Valle", new BigDecimal("-2.0"), null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.guardar(new CultivoForm("Palto", "palto", "Valle", new BigDecimal("2.0"),
                        new BigDecimal("-95.0"), null)));
        assertThrows(IllegalArgumentException.class,
                () -> cultivos.guardar(new CultivoForm("Palto", "palto", "Valle", new BigDecimal("2.0"),
                        null, new BigDecimal("190.0"))));
    }

    @Test
    void eliminarConCascadaDeDependencias() {
        // 1. Crear cultivo con dispositivo
        Cultivo cultivo = cultivos.guardar(formulario());
        Integer idCultivo = cultivo.getIdCultivo();
        DispositivoIoT dispositivo = cultivos.registrarDispositivo(idCultivo);
        Integer idDispositivo = dispositivo.getIdDispositivo();

        // 2. Crear umbral para el cultivo
        TipoSensor tipoSensor = tiposRepo.findAll().get(0);
        Umbral umbral = new Umbral();
        umbral.setCultivo(cultivo);
        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(new BigDecimal("10.0"));
        umbral.setValorMaximo(new BigDecimal("50.0"));
        umbral = umbralesRepo.save(umbral);

        // 3. Crear sensor, lectura y alerta asociada al umbral
        Sensor sensor = new Sensor();
        sensor.setDispositivo(dispositivo);
        sensor.setTipoSensor(tipoSensor);
        sensor = sensoresRepo.save(sensor);

        Lectura lectura = new Lectura();
        lectura.setSensor(sensor);
        lectura.setValor(new BigDecimal("5.0"));
        lectura = lecturasRepo.save(lectura);

        Alerta alerta = new Alerta();
        alerta.setLectura(lectura);
        alerta.setUmbral(umbral);
        alerta.setMensaje("Lectura por debajo del mínimo");
        alerta = alertasRepo.save(alerta);

        // 4. Crear usuario, asignación de acceso y notificación de alerta
        Usuario usuario = new Usuario();
        usuario.setNombre("Juan Test");
        usuario.setCorreo("juan.test.cultivo@agropiura.com");
        usuario.setContrasena("password123");
        usuario = usuariosRepo.save(usuario);

        Rol rol = rolesRepo.findAll().get(0);
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setCultivo(cultivo);
        acceso.setUsuario(usuario);
        acceso.setRol(rol);
        acceso = accesosRepo.save(acceso);

        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setAlerta(alerta);
        notificacion.setUsuario(usuario);
        notificacion = notificacionesRepo.save(notificacion);

        // 5. Ejecutar eliminación en cascada
        cultivos.eliminar(idCultivo);

        // 6. Verificar que el cultivo y todas sus dependencias fueron eliminados
        assertThrows(IllegalArgumentException.class, () -> cultivos.buscar(idCultivo));
        assertTrue(umbralesRepo.findByCultivoIdCultivo(idCultivo).isEmpty());
        assertTrue(alertasRepo.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(idCultivo).isEmpty());
        assertTrue(accesosRepo.findByCultivoIdCultivo(idCultivo).isEmpty());
        assertTrue(notificacionesRepo.findByAlertaIdAlerta(alerta.getIdAlerta()).isEmpty());

        // 7. El dispositivo IoT físico persiste en la base de datos (solo desvinculado)
        assertTrue(dispositivosRepo.findById(idDispositivo).isPresent());
    }
}
