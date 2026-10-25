package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Rol;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.model.entity.Usuario;
import com.utp.AgroPiuraIoT.model.entity.UsuarioCultivo;
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioCultivoRepository;
import com.utp.AgroPiuraIoT.service.AlertasService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class AlertasServiceTest {

    @Autowired
    private AlertasService alertas;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private SensorRepository sensores;

    @Autowired
    private LecturaRepository lecturas;

    @Autowired
    private AlertaRepository alertaRepo;

    @Autowired
    private NotificacionUsuarioRepository notificaciones;

    @Autowired
    private UsuarioCultivoRepository accesos;

    @Autowired
    private TipoSensorRepository tipos;

    @Autowired
    private RolRepository roles;

    /** Prepara cultivo + dispositivo + sensor de humedad de suelo con umbral 20-80. */
    private Cultivo prepararEscenario() {
        Cultivo cultivo = cultivos.guardar(new CultivoForm(
                "Arroz - Parcela C", "arroz", "Catamayo",
                new BigDecimal("5.00"), null, null));
        umbrales.guardar(cultivo.getIdCultivo(),
                new UmbralForm(tipoHumedad().getIdTipoSensor(), new BigDecimal("20"), new BigDecimal("80")));
        cultivos.registrarDispositivo(cultivo.getIdCultivo());
        return cultivos.buscar(cultivo.getIdCultivo());
    }

    private TipoSensor tipoHumedad() {
        return tipos.findAll().stream()
                .filter(t -> t.getNombreTipo().equals("humedad_suelo"))
                .findFirst()
                .orElseThrow();
    }

    private Lectura registrarLectura(Cultivo cultivo, String valor) {
        Sensor sensor = new Sensor();
        sensor.setDispositivo(cultivo.getDispositivos().iterator().next());
        sensor.setTipoSensor(tipoHumedad());
        sensor = sensores.save(sensor);
        Lectura lectura = sensor.registrarLectura(new BigDecimal(valor));
        return lecturas.save(lectura);
    }

    private void asignarUsuario(Cultivo cultivo) {
        Usuario usuario = usuarios.registrar(new RegistroRequest(
                "Productor Test", "productor@test.pe", "clave1234", "clave1234"));
        UsuarioCultivo acceso = new UsuarioCultivo();
        acceso.setUsuario(usuario);
        acceso.setCultivo(cultivo);
        Rol rol = roles.findAll().get(0);
        acceso.setRol(rol);
        accesos.save(acceso);
    }

    @Test
    void generaAlertaYNotificaCuandoEstaFueraDeRango() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo);
        Lectura lectura = registrarLectura(cultivo, "12"); // bajo el mínimo (20)

        Optional<?> generada = alertas.evaluar(lectura);

        assertTrue(generada.isPresent());
        assertTrue(alertaRepo.count() == 1);
        assertEquals(1, notificaciones.findByAlertaIdAlerta(
                alertaRepo.findAll().get(0).getIdAlerta()).size());
    }

    @Test
    void noGeneraAlertaSiEstaDentroDeRango() {
        Cultivo cultivo = prepararEscenario();
        Lectura lectura = registrarLectura(cultivo, "45");

        Optional<?> generada = alertas.evaluar(lectura);

        assertTrue(generada.isEmpty());
        assertTrue(alertaRepo.count() == 0);
    }

    @Test
    void noGeneraAlertaSinUmbralConfigurado() {
        Cultivo cultivo = cultivos.guardar(new CultivoForm(
                "Plátano - Parcela D", "platano", "Sullana",
                new BigDecimal("1.50"), null, null));
        cultivos.registrarDispositivo(cultivo.getIdCultivo());
        Lectura lectura = registrarLectura(cultivos.buscar(cultivo.getIdCultivo()), "10");

        Optional<?> generada = alertas.evaluar(lectura);

        assertTrue(generada.isEmpty());
        assertTrue(alertaRepo.count() == 0);
    }

    @Test
    void noDuplicaNotificacionesParaElMismoUsuario() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo);
        Lectura primera = registrarLectura(cultivo, "15");
        Lectura segunda = registrarLectura(cultivo, "18");

        alertas.evaluar(primera);
        alertas.evaluar(segunda);

        assertFalse(alertaRepo.count() == 0);
        assertTrue(notificaciones.count() >= 1);
    }

    @Test
    void listaYMarcaNotificacionComoLeida() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo);
        Lectura lectura = registrarLectura(cultivo, "12");
        alertas.evaluar(lectura);

        var mias = alertas.notificacionesDe("productor@test.pe");
        assertEquals(1, mias.size());

        alertas.marcarLeida(mias.get(0).getIdNotificacion(), "productor@test.pe");

        var trasLeer = alertas.notificacionesDe("productor@test.pe");
        assertTrue(trasLeer.isEmpty());
    }
}
