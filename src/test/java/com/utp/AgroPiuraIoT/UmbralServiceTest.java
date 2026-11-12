package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
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
import com.utp.AgroPiuraIoT.repository.AlertaRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.NotificacionUsuarioRepository;
import com.utp.AgroPiuraIoT.repository.RolRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.repository.UsuarioRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class UmbralServiceTest {

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private TipoSensorRepository tipos;

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

    private Integer crearCultivo() {
        Cultivo creado = cultivos.guardar(new CultivoForm("Limón - Parcela B", "limon",
                "San Lorenzo", new BigDecimal("2.50"), null, null));
        return creado.getIdCultivo();
    }

    private UmbralForm formulario(Integer idTipoSensor, String min, String max) {
        return new UmbralForm(idTipoSensor, new BigDecimal(min), new BigDecimal(max));
    }

    @Test
    void guardaYListaPorCultivo() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        Umbral guardado = umbrales.guardar(idCultivo, formulario(idHumedad, "20", "80"));

        assertNotNull(guardado.getIdUmbral());
        List<Umbral> lista = umbrales.listar(idCultivo);
        assertEquals(1, lista.size());
        assertEquals(idHumedad, lista.get(0).getTipoSensor().getIdTipoSensor());
        assertEquals(new BigDecimal("20"), lista.get(0).getValorMinimo());
        assertEquals(new BigDecimal("80"), lista.get(0).getValorMaximo());
    }

    @Test
    void guardaDuplicadoActualizaEnLugarDeInsertar() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        umbrales.guardar(idCultivo, formulario(idHumedad, "20", "80"));
        umbrales.guardar(idCultivo, formulario(idHumedad, "25", "85"));

        List<Umbral> lista = umbrales.listar(idCultivo);
        assertEquals(1, lista.size());
        assertEquals(new BigDecimal("25"), lista.get(0).getValorMinimo());
        assertEquals(new BigDecimal("85"), lista.get(0).getValorMaximo());
    }

    @Test
    void actualizaUmbralExitosamente() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();
        Umbral guardado = umbrales.guardar(idCultivo, formulario(idHumedad, "20", "80"));

        Umbral actualizado = umbrales.actualizar(idCultivo, guardado.getIdUmbral(),
                formulario(idHumedad, "30", "70"));
        assertEquals(new BigDecimal("30"), actualizado.getValorMinimo());
        assertEquals(new BigDecimal("70"), actualizado.getValorMaximo());
    }

    @Test
    void actualizaUmbralConConflictoDeTipoSensorLanzaExcepcion() {
        Integer idCultivo = crearCultivo();
        List<TipoSensor> todosTipos = tipos.findAll();
        Integer tipo1 = todosTipos.get(0).getIdTipoSensor();
        Integer tipo2 = todosTipos.get(1).getIdTipoSensor();

        umbrales.guardar(idCultivo, formulario(tipo1, "20", "80"));
        Umbral umbral2 = umbrales.guardar(idCultivo, formulario(tipo2, "15", "40"));

        // Intentar cambiar umbral2 para que use el tipo1 (ya ocupado en el cultivo)
        assertThrows(IllegalArgumentException.class,
                () -> umbrales.actualizar(idCultivo, umbral2.getIdUmbral(), formulario(tipo1, "22", "82")));
    }

    @Test
    void rechazaRangoInvalidoMinMayorMax() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, formulario(idHumedad, "80", "20")));
    }

    @Test
    void rechazaValoresNegativos() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, formulario(idHumedad, "-5", "50")));
        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, formulario(idHumedad, "5", "-50")));
    }

    @Test
    void rechazaValoresNulos() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class, () -> umbrales.guardar(idCultivo, null));
        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, new UmbralForm(null, new BigDecimal("10"), new BigDecimal("20"))));
        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, new UmbralForm(idHumedad, null, new BigDecimal("20"))));
        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, new UmbralForm(idHumedad, new BigDecimal("10"), null)));
    }

    @Test
    void rechazaCultivoInexistente() {
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(99999, formulario(idHumedad, "20", "80")));
    }

    @Test
    void rechazaTipoSensorInexistente() {
        Integer idCultivo = crearCultivo();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, formulario(999999, "20", "80")));
    }

    @Test
    void eliminaUmbralSinAlertas() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();
        Umbral guardado = umbrales.guardar(idCultivo, formulario(idHumedad, "20", "80"));

        umbrales.eliminar(idCultivo, guardado.getIdUmbral());

        assertTrue(umbrales.listar(idCultivo).isEmpty());
    }

    @Test
    void eliminarUmbralConCascadaDeAlertas() {
        Integer idCultivo = crearCultivo();
        Cultivo cultivo = cultivos.buscar(idCultivo);
        DispositivoIoT dispositivo = cultivos.registrarDispositivo(idCultivo);
        TipoSensor tipoSensor = tipos.findAll().get(0);

        Umbral umbral = umbrales.guardar(idCultivo, formulario(tipoSensor.getIdTipoSensor(), "20", "80"));

        Sensor sensor = new Sensor();
        sensor.setDispositivo(dispositivo);
        sensor.setTipoSensor(tipoSensor);
        sensor = sensoresRepo.save(sensor);

        Lectura lectura = new Lectura();
        lectura.setSensor(sensor);
        lectura.setValor(new BigDecimal("95.0"));
        lectura = lecturasRepo.save(lectura);

        Alerta alerta = new Alerta();
        alerta.setLectura(lectura);
        alerta.setUmbral(umbral);
        alerta.setMensaje("Lectura crítica");
        alerta = alertasRepo.save(alerta);

        Usuario usuario = new Usuario();
        usuario.setNombre("Carlos Umbral");
        usuario.setCorreo("carlos.umbral@agropiura.com");
        usuario.setContrasena("password123");
        usuario = usuariosRepo.save(usuario);

        NotificacionUsuario notificacion = new NotificacionUsuario();
        notificacion.setAlerta(alerta);
        notificacion.setUsuario(usuario);
        notificacion = notificacionesRepo.save(notificacion);

        // Eliminar el umbral debe cascada-eliminar alertas y notificaciones asociadas
        umbrales.eliminar(idCultivo, umbral.getIdUmbral());

        assertTrue(umbrales.listar(idCultivo).isEmpty());
        assertTrue(alertasRepo.findByUmbralCultivoIdCultivoOrderByFechaAlertaDesc(idCultivo).isEmpty());
        assertTrue(notificacionesRepo.findByAlertaIdAlerta(alerta.getIdAlerta()).isEmpty());
    }

    @Test
    void rechazaBuscarOEliminarUmbralDeOtroCultivo() {
        Integer cultivo1 = crearCultivo();
        Cultivo c2 = cultivos.guardar(new CultivoForm("Palto Parcela C", "palto", "Chira", new BigDecimal("1.5"), null, null));
        Integer cultivo2 = c2.getIdCultivo();

        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();
        Umbral u1 = umbrales.guardar(cultivo1, formulario(idHumedad, "20", "80"));

        assertThrows(IllegalArgumentException.class, () -> umbrales.buscar(cultivo2, u1.getIdUmbral()));
        assertThrows(IllegalArgumentException.class, () -> umbrales.eliminar(cultivo2, u1.getIdUmbral()));
    }
}
