package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.exception.RecursoNoEncontradoException;
import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.LecturaItemRequest;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaRequest;
import com.utp.AgroPiuraIoT.model.dto.TelemetriaResponse;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.TelemetriaService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A1 · Validación de lecturas y última conexión, a nivel de servicio. La prueba
 * HTTP con token ({@code TelemetriaControllerTest}) se reactiva en A2, cuando se
 * abra {@code /api/**}.
 */
@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class TelemetriaServiceTest {

    private static final String CORREO = "telemetria.a1@test.pe";

    @Autowired
    private TelemetriaService telemetria;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private DispositivoIoTRepository dispositivos;

    @Autowired
    private LecturaRepository lecturas;

    private Integer idCultivo;
    private Integer idNodo;

    @BeforeEach
    void prepararNodo() {
        usuarios.registrar(new RegistroRequest("Telemetría A1", CORREO, "clave1234", "clave1234"));
        Cultivo cultivo = cultivos.crear(new CultivoForm(
                "Banano - Lote A1", "banano", "Sullana, Piura",
                new BigDecimal("3.00"), null, null), CORREO);
        idCultivo = cultivo.getIdCultivo();
        idNodo = cultivos.registrarNodo(idCultivo).nodo().getIdDispositivo();
    }

    @Test
    void lecturaValidaSeGuardaYActualizaLaUltimaConexion() {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        assertNull(nodo().getUltimaConexion(), "Un nodo recién registrado aún no se ha conectado");

        TelemetriaResponse respuesta = enviar(lectura("temperatura_ambiente", "28.5"));

        assertEquals("ok", respuesta.getStatus());
        assertEquals(1, respuesta.getLecturasProcesadas());
        assertEquals(0, respuesta.getLecturasRechazadas());
        assertNull(respuesta.getRechazos());
        assertEquals(1, lecturasDelCultivo());
        assertNotNull(nodo().getUltimaConexion());
        assertTrue(nodo().getUltimaConexion().isAfter(antes));
    }

    @Test
    void lecturasEnLosLimitesFisicosSonValidas() {
        TelemetriaResponse respuesta = enviar(
                lectura("humedad_suelo", "0"),
                lectura("humedad_ambiente", "100"),
                lectura("ph_suelo", "14"));

        assertEquals("ok", respuesta.getStatus());
        assertEquals(3, respuesta.getLecturasProcesadas());
    }

    @Test
    void lecturaFisicamenteImposibleSeRechazaYQuedaEnElLog(CapturedOutput salida) {
        TelemetriaResponse respuesta = enviar(lectura("ph_suelo", "20"));

        assertEquals("rechazado", respuesta.getStatus());
        assertEquals(0, respuesta.getLecturasProcesadas());
        assertEquals(0, respuesta.getAlertasGeneradas());
        assertEquals(1, respuesta.getLecturasRechazadas());
        assertTrue(respuesta.getRechazos().get(0).contains("ph_suelo"));
        assertEquals(0, lecturasDelCultivo(), "La lectura imposible no se guarda");
        assertTrue(salida.getAll().contains("posible falla de sensor"));
        assertNotNull(nodo().getUltimaConexion(), "El nodo sí se comunicó");
    }

    @Test
    void loteMixtoGuardaLasValidasYRechazaLasImposibles() {
        TelemetriaResponse respuesta = enviar(
                lectura("temperatura_ambiente", "27"),
                lectura("humedad_suelo", "150"),
                lectura("temperatura_ambiente", "-50"));

        assertEquals("parcial", respuesta.getStatus());
        assertEquals(1, respuesta.getLecturasProcesadas());
        assertEquals(2, respuesta.getLecturasRechazadas());
        assertEquals(1, lecturasDelCultivo());
    }

    @Test
    void tipoDeSensorQueElNodoNoTieneSeRechazaSinGuardarNada() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> enviar(lectura("radiacion_solar", "800")));
        assertEquals(0, lecturasDelCultivo());
        assertNull(nodo().getUltimaConexion(), "Una solicitud inválida no cuenta como conexión");
    }

    @Test
    void nodoInexistenteSeRechaza() {
        TelemetriaRequest request = new TelemetriaRequest(Integer.MAX_VALUE,
                List.of(new LecturaItemRequest("temperatura_ambiente", new BigDecimal("25"))));

        assertThrows(RecursoNoEncontradoException.class, () -> telemetria.procesar(request));
    }

    // ------------------------------------------------------------------

    private TelemetriaResponse enviar(LecturaItemRequest... items) {
        return telemetria.procesar(new TelemetriaRequest(idNodo, List.of(items)));
    }

    private static LecturaItemRequest lectura(String tipo, String valor) {
        return new LecturaItemRequest(tipo, new BigDecimal(valor));
    }

    private DispositivoIoT nodo() {
        return dispositivos.findById(idNodo).orElseThrow();
    }

    private int lecturasDelCultivo() {
        return lecturas.buscarPorCultivo(idCultivo).size();
    }
}
