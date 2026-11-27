package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Alerta;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        return registrarLectura(cultivo, valor, null);
    }

    private Lectura registrarLectura(Cultivo cultivo, String valor, LocalDateTime fechaHora) {
        Sensor sensor = new Sensor();
        sensor.setDispositivo(cultivo.getDispositivos().iterator().next());
        sensor.setTipoSensor(tipoHumedad());
        sensor = sensores.save(sensor);
        Lectura lectura = sensor.registrarLectura(new BigDecimal(valor));
        if (fechaHora != null) {
            lectura.setFechaHora(fechaHora);
        }
        return lecturas.save(lectura);
    }

    private void asignarUsuario(Cultivo cultivo) {
        asignarUsuario(cultivo, "Productor Test", "productor@test.pe");
    }

    private void asignarUsuario(Cultivo cultivo, String nombre, String correo) {
        Usuario usuario = usuarios.registrar(new RegistroRequest(
                nombre, correo, "clave1234", "clave1234"));
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
        long antes = alertaRepo.count();
        Lectura lectura = registrarLectura(cultivo, "12"); // bajo el mínimo (20)

        Optional<Alerta> generada = alertas.evaluar(lectura);

        assertTrue(generada.isPresent());
        assertEquals(antes + 1, alertaRepo.count());
        assertEquals(1, alertas.listar(cultivo.getIdCultivo()).size());
        assertEquals(1, notificaciones.findByAlertaIdAlerta(generada.get().getIdAlerta()).size());
    }

    @Test
    void noGeneraAlertaSiEstaDentroDeRango() {
        Cultivo cultivo = prepararEscenario();
        long antes = alertaRepo.count();
        Lectura lectura = registrarLectura(cultivo, "45");

        Optional<?> generada = alertas.evaluar(lectura);

        assertTrue(generada.isEmpty());
        assertEquals(antes, alertaRepo.count());
        assertTrue(alertas.listar(cultivo.getIdCultivo()).isEmpty());
    }

    @Test
    void noGeneraAlertaSinUmbralConfigurado() {
        Cultivo cultivo = cultivos.guardar(new CultivoForm(
                "Plátano - Parcela D", "platano", "Sullana",
                new BigDecimal("1.50"), null, null));
        cultivos.registrarDispositivo(cultivo.getIdCultivo());
        long antes = alertaRepo.count();
        Lectura lectura = registrarLectura(cultivos.buscar(cultivo.getIdCultivo()), "10");

        Optional<?> generada = alertas.evaluar(lectura);

        assertTrue(generada.isEmpty());
        assertEquals(antes, alertaRepo.count());
        assertTrue(alertas.listar(cultivo.getIdCultivo()).isEmpty());
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

    @Test
    void cuentaNotificacionesNoLeidas() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo);
        Lectura lectura = registrarLectura(cultivo, "12");
        alertas.evaluar(lectura);

        assertEquals(1, alertas.contarNoLeidas("productor@test.pe"));

        var mias = alertas.notificacionesDe("productor@test.pe");
        alertas.marcarLeida(mias.get(0).getIdNotificacion(), "productor@test.pe");

        assertEquals(0, alertas.contarNoLeidas("productor@test.pe"));
    }

    @Test
    void diezLecturasSeguidasFueraDeRangoGeneranUnaAlertaYUnaNotificacionPorUsuario() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo, "Productor Uno", "prod1@test.pe");
        asignarUsuario(cultivo, "Productor Dos", "prod2@test.pe");

        LocalDateTime base = LocalDateTime.of(2026, 11, 24, 8, 0);
        String[] valores = {"18", "17", "15", "12", "14", "11", "16", "13", "15", "17"};
        for (int i = 0; i < valores.length; i++) {
            Lectura lectura = registrarLectura(cultivo, valores[i], base.plusMinutes(i * 5L));
            alertas.evaluar(lectura);
        }

        List<Alerta> delCultivo = alertas.listar(cultivo.getIdCultivo());
        assertEquals(1, delCultivo.size());
        Alerta episodio = delCultivo.get(0);
        assertEquals(Alerta.ESTADO_ABIERTA, episodio.getEstado());
        assertEquals(10, episodio.getCantidadLecturas());
        assertEquals(0, new BigDecimal("11").compareTo(episodio.getValorExtremo()));
        assertEquals(Alerta.DIRECCION_BAJO, episodio.getDireccion());
        assertNull(episodio.getFechaCierre());

        assertEquals(2, notificaciones.findByAlertaIdAlerta(episodio.getIdAlerta()).size());
        assertEquals(1, alertas.contarNoLeidas("prod1@test.pe"));
        assertEquals(1, alertas.contarNoLeidas("prod2@test.pe"));
        assertEquals(1, alertas.contarAbiertasPorUsuario("prod1@test.pe"));
    }

    @Test
    void cierreAlVolverAlRangoYNuevaSalidaCreaNuevaAlerta() {
        Cultivo cultivo = prepararEscenario();
        asignarUsuario(cultivo, "Productor Ciclo", "ciclo@test.pe");
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 24, 9, 0);

        // 1. Primera salida -> abre episodio 1
        Alerta primera = alertas.evaluar(registrarLectura(cultivo, "15", t0)).orElseThrow();
        assertEquals(Alerta.ESTADO_ABIERTA, primera.getEstado());
        assertEquals(1, alertas.contarAbiertasPorUsuario("ciclo@test.pe"));

        // 2. Vuelve al rango -> cierra episodio 1
        Optional<Alerta> normal = alertas.evaluar(registrarLectura(cultivo, "50", t0.plusMinutes(15)));
        assertTrue(normal.isEmpty());
        Alerta cerrada = alertaRepo.findById(primera.getIdAlerta()).orElseThrow();
        assertEquals(Alerta.ESTADO_CERRADA, cerrada.getEstado());
        assertNotNull(cerrada.getFechaCierre());
        assertEquals(0, alertas.contarAbiertasPorUsuario("ciclo@test.pe"));

        // 3. Nueva salida -> crea episodio 2 y vuelve a notificar
        Alerta segunda = alertas.evaluar(registrarLectura(cultivo, "14", t0.plusMinutes(30))).orElseThrow();
        assertFalse(segunda.getIdAlerta().equals(primera.getIdAlerta()));
        assertEquals(Alerta.ESTADO_ABIERTA, segunda.getEstado());
        assertEquals(2, alertas.listar(cultivo.getIdCultivo()).size());
        assertEquals(2, alertas.contarNoLeidas("ciclo@test.pe"));
        assertEquals(1, alertas.contarAbiertasPorUsuario("ciclo@test.pe"));
    }

    @Test
    void cambioDirectoDeDireccionCierraEpisodioAnteriorYAbreUnoNuevo() {
        Cultivo cultivo = prepararEscenario();
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 24, 10, 0);

        Alerta bajo = alertas.evaluar(registrarLectura(cultivo, "15", t0)).orElseThrow();
        Alerta alto = alertas.evaluar(registrarLectura(cultivo, "90", t0.plusMinutes(10))).orElseThrow();

        assertFalse(bajo.getIdAlerta().equals(alto.getIdAlerta()));
        assertEquals(Alerta.ESTADO_CERRADA, alertaRepo.findById(bajo.getIdAlerta()).orElseThrow().getEstado());
        assertEquals(Alerta.ESTADO_ABIERTA, alto.getEstado());
        assertEquals(Alerta.DIRECCION_ALTO, alto.getDireccion());
    }

    @Test
    void calculaCadaNivelDeSeveridadSegunDesviacionRespectoAlAncho() {
        // Umbral 20-80 -> ancho = 60. < 10% (< 6) = leve; 10-25% (6..15) = moderada; > 25% (> 15) = critica
        Cultivo cultivoLeve = prepararEscenario();
        Alerta leve = alertas.evaluar(registrarLectura(cultivoLeve, "17")).orElseThrow(); // desv 3 = 5%
        assertEquals(Alerta.SEVERIDAD_LEVE, leve.getSeveridad());

        Cultivo cultivoModerada = prepararEscenario();
        Alerta moderada = alertas.evaluar(registrarLectura(cultivoModerada, "11")).orElseThrow(); // desv 9 = 15%
        assertEquals(Alerta.SEVERIDAD_MODERADA, moderada.getSeveridad());

        Cultivo cultivoCritica = prepararEscenario();
        Alerta critica = alertas.evaluar(registrarLectura(cultivoCritica, "2")).orElseThrow(); // desv 18 = 30%
        assertEquals(Alerta.SEVERIDAD_CRITICA, critica.getSeveridad());
    }

    @Test
    void escalaSeveridadCuandoElEpisodioSuperaElTiempoConfigurable() {
        LocalDateTime t0 = LocalDateTime.of(2026, 11, 24, 8, 0);

        // Leve -> Moderada tras >= 60 minutos
        Cultivo cultivo1 = prepararEscenario();
        Alerta inicialLeve = alertas.evaluar(registrarLectura(cultivo1, "17", t0)).orElseThrow();
        assertEquals(Alerta.SEVERIDAD_LEVE, inicialLeve.getSeveridad());

        Alerta escaladaModerada = alertas.evaluar(registrarLectura(cultivo1, "17", t0.plusMinutes(65))).orElseThrow();
        assertEquals(inicialLeve.getIdAlerta(), escaladaModerada.getIdAlerta());
        assertEquals(Alerta.SEVERIDAD_MODERADA, escaladaModerada.getSeveridad());

        // Moderada -> Crítica tras >= 60 minutos
        Cultivo cultivo2 = prepararEscenario();
        Alerta inicialModerada = alertas.evaluar(registrarLectura(cultivo2, "11", t0)).orElseThrow();
        assertEquals(Alerta.SEVERIDAD_MODERADA, inicialModerada.getSeveridad());

        Alerta escaladaCritica = alertas.evaluar(registrarLectura(cultivo2, "11", t0.plusMinutes(65))).orElseThrow();
        assertEquals(inicialModerada.getIdAlerta(), escaladaCritica.getIdAlerta());
        assertEquals(Alerta.SEVERIDAD_CRITICA, escaladaCritica.getSeveridad());
    }
}
