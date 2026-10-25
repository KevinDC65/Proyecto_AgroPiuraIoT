package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.dto.UmbralForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.UmbralService;
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
class UmbralServiceTest {

    @Autowired
    private UmbralService umbrales;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private TipoSensorRepository tipos;

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

        assertTrue(guardado.getIdUmbral() != null);
        List<Umbral> lista = umbrales.listar(idCultivo);
        assertEquals(1, lista.size());
        assertEquals(idHumedad, lista.get(0).getTipoSensor().getIdTipoSensor());
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
    void rechazaRangoInvalido() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(idCultivo, formulario(idHumedad, "80", "20")));
    }

    @Test
    void rechazaCultivoInexistente() {
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();

        assertThrows(IllegalArgumentException.class,
                () -> umbrales.guardar(99999, formulario(idHumedad, "20", "80")));
    }

    @Test
    void elimina() {
        Integer idCultivo = crearCultivo();
        Integer idHumedad = tipos.findAll().get(0).getIdTipoSensor();
        Umbral guardado = umbrales.guardar(idCultivo, formulario(idHumedad, "20", "80"));

        umbrales.eliminar(idCultivo, guardado.getIdUmbral());

        assertTrue(umbrales.listar(idCultivo).isEmpty());
    }
}
