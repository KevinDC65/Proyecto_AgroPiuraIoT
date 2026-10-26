package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.service.ClimaService;
import com.utp.AgroPiuraIoT.service.CultivoService;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@TestPropertySource(properties = "clima.cache.minutos=1")
class ClimaServiceTest {

    /**
     * Cliente de clima falso e inyectado como @Primary: cuenta las veces que el
     * proveedor real fue llamado (para evidenciar el cache de Guava).
     */
    @TestConfiguration
    static class ClientesFalsos {
        @Bean
        @Primary
        ClienteClimaFalso clienteClima() {
            return new ClienteClimaFalso();
        }
    }

    static class ClienteClimaFalso implements com.utp.AgroPiuraIoT.port.ClienteClima {
        final AtomicInteger llamadas = new AtomicInteger();

        @Override
        public Optional<ClimaActual> obtener(String ubicacion) {
            llamadas.incrementAndGet();
            if ("SIN-COBERTURA".equals(ubicacion)) {
                return Optional.empty();
            }
            return Optional.of(new ClimaActual(ubicacion, 31.5, 62.0, "Parcialmente nublado"));
        }
    }

    @Autowired
    private ClimaService clima;

    @Autowired
    private ClienteClimaFalso clienteFalso;

    @Autowired
    private CultivoService cultivos;

    /** El contexto y el fake se comparten entre tests: se reinicia el contador. */
    @BeforeEach
    void reiniciarContador() {
        clienteFalso.llamadas.set(0);
    }

    private Cultivo crearCultivoConUbicacion(String ubicacion) {
        Cultivo c = cultivos.guardar(new CultivoForm("Arroz - Parcela F", "arroz",
                ubicacion, new BigDecimal("1.00"), null, null));
        return c;
    }

    @Test
    void proporcionaClimaDeLaUbicacionDelCultivo() {
        Cultivo cultivo = crearCultivoConUbicacion("Piura");

        Optional<ClimaActual> actual = clima.actual(cultivo);

        assertTrue(actual.isPresent());
        assertEquals("Piura", actual.get().ciudad());
        assertEquals(31.5, actual.get().temperaturaCelsius());
        assertEquals(62.0, actual.get().humedadPorcentaje());
    }

    @Test
    void cacheaLaRespuestaDelProveedor() {
        Cultivo cultivo = crearCultivoConUbicacion("Tambogrande");

        Optional<ClimaActual> primera = clima.actual(cultivo);
        Optional<ClimaActual> segunda = clima.actual(cultivo);

        assertSame(primera.orElseThrow(), segunda.orElseThrow()); // misma instancia cacheada
        assertEquals(1, clienteFalso.llamadas.get());             // el proveedor solo recibió 1 llamada
    }

    @Test
    void sinUbicacionDevuelveOpcionalVacio() {
        Cultivo cultivo = crearCultivoConUbicacion(null);

        assertTrue(clima.actual(cultivo).isEmpty());
        assertEquals(0, clienteFalso.llamadas.get());
    }

    @Test
    void anteProveedorSinDatosDevuelveOpcionalVacio() {
        Cultivo cultivo = crearCultivoConUbicacion("SIN-COBERTURA");

        assertTrue(clima.actual(cultivo).isEmpty());
        assertEquals(1, clienteFalso.llamadas.get());
    }
}
