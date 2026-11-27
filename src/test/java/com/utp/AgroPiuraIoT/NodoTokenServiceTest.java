package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.RegistroRequest;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.DispositivoIoT;
import com.utp.AgroPiuraIoT.repository.DispositivoIoTRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.CultivoService.NodoRegistrado;
import com.utp.AgroPiuraIoT.service.NodoTokenService;
import com.utp.AgroPiuraIoT.service.UsuarioService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A1 · Token por nodo: generación, almacenamiento solo como hash y verificación. */
@SpringBootTest
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class NodoTokenServiceTest {

    private static final String CORREO = "admin.a1@test.pe";

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private NodoTokenService tokens;

    @Autowired
    private UsuarioService usuarios;

    @Autowired
    private DispositivoIoTRepository dispositivos;

    private Integer idCultivo;

    @BeforeEach
    void prepararCultivo() {
        usuarios.registrar(new RegistroRequest("Admin A1", CORREO, "clave1234", "clave1234"));
        Cultivo cultivo = cultivos.crear(new CultivoForm(
                "Limón - Lote A1", "limón", "Chulucanas, Piura",
                new BigDecimal("1.20"), null, null), CORREO);
        idCultivo = cultivo.getIdCultivo();
    }

    @Test
    void registrarNodoGeneraTokenYGuardaSoloSuHash() {
        NodoRegistrado registro = cultivos.registrarNodo(idCultivo);
        String token = registro.token();
        DispositivoIoT nodo = dispositivos.findById(registro.nodo().getIdDispositivo()).orElseThrow();

        assertEquals(43, token.length(), "256 bits en Base64 URL sin relleno");
        assertTrue(nodo.tieneToken());
        assertNotEquals(token, nodo.getTokenHash(), "El token no se guarda en texto plano");
        assertFalse(nodo.getTokenHash().contains(token));
        assertTrue(nodo.getTokenHash().startsWith("$2"), "Se guarda como hash BCrypt");
        assertTrue(tokens.verificar(nodo.getIdDispositivo(), token));
    }

    @Test
    void tokenIncorrectoNoValida() {
        NodoRegistrado registro = cultivos.registrarNodo(idCultivo);
        NodoRegistrado otro = cultivos.registrarNodo(idCultivo);
        Integer id = registro.nodo().getIdDispositivo();

        assertFalse(tokens.verificar(id, "token-inventado"));
        assertFalse(tokens.verificar(id, otro.token()), "El token de otro nodo no sirve");
        assertFalse(tokens.verificar(Integer.MAX_VALUE, registro.token()), "Nodo inexistente");
        assertFalse(tokens.verificar(id, null));
        assertFalse(tokens.verificar(id, "   "));
        assertFalse(tokens.verificar(null, registro.token()));
    }

    @Test
    void cadaNodoRecibeUnTokenDistinto() {
        assertNotEquals(cultivos.registrarNodo(idCultivo).token(), cultivos.registrarNodo(idCultivo).token());
    }

    @Test
    void nodoSinTokenNoValida() {
        DispositivoIoT antiguo = dispositivos.save(new DispositivoIoT());

        assertFalse(antiguo.tieneToken());
        assertFalse(tokens.verificar(antiguo.getIdDispositivo(), "cualquier-token"));
    }

    @Test
    void generarOtroTokenInvalidaElAnterior() {
        NodoRegistrado registro = cultivos.registrarNodo(idCultivo);
        String nuevo = tokens.asignarToken(registro.nodo());

        assertFalse(tokens.verificar(registro.nodo().getIdDispositivo(), registro.token()));
        assertTrue(tokens.verificar(registro.nodo().getIdDispositivo(), nuevo));
    }

    @Test
    void elTokenNuncaApareceEnLosLogs(CapturedOutput salida) {
        NodoRegistrado registro = cultivos.registrarNodo(idCultivo);
        tokens.verificar(registro.nodo().getIdDispositivo(), registro.token());
        tokens.verificar(registro.nodo().getIdDispositivo(), "token-inventado");

        assertFalse(salida.getAll().contains(registro.token()));
        assertFalse(salida.getAll().contains("token-inventado"));
        assertTrue(salida.getAll().contains("Token rechazado para el nodo " + registro.nodo().getIdDispositivo()));
    }
}
