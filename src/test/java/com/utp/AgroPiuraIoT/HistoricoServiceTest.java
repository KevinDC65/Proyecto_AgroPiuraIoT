package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.dto.CultivoForm;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Sensor;
import com.utp.AgroPiuraIoT.model.entity.TipoSensor;
import com.utp.AgroPiuraIoT.repository.LecturaRepository;
import com.utp.AgroPiuraIoT.repository.SensorRepository;
import com.utp.AgroPiuraIoT.repository.TipoSensorRepository;
import com.utp.AgroPiuraIoT.service.CultivoService;
import com.utp.AgroPiuraIoT.service.HistoricoService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class HistoricoServiceTest {

    @Autowired
    private HistoricoService historico;

    @Autowired
    private CultivoService cultivos;

    @Autowired
    private SensorRepository sensores;

    @Autowired
    private LecturaRepository lecturas;

    @Autowired
    private TipoSensorRepository tipos;

    private Cultivo prepararConLecturas() {
        Cultivo cultivo = cultivos.guardar(new CultivoForm(
                "Maíz - Parcela E", "maiz", "Chulucanas",
                new BigDecimal("2.00"), null, null));
        cultivos.registrarDispositivo(cultivo.getIdCultivo());
        cultivo = cultivos.buscar(cultivo.getIdCultivo());

        TipoSensor tipoHumedad = tipos.findAll().stream()
                .filter(t -> t.getNombreTipo().equals("humedad_suelo"))
                .findFirst().orElseThrow();
        Sensor sensor = new Sensor();
        sensor.setDispositivo(cultivo.getDispositivos().iterator().next());
        sensor.setTipoSensor(tipoHumedad);
        sensor = sensores.save(sensor);

        registrar(sensor, "30", LocalDateTime.now().minusDays(3));
        registrar(sensor, "45", LocalDateTime.now().minusDays(1));
        registrar(sensor, "60", LocalDateTime.now());
        return cultivo;
    }

    private void registrar(Sensor sensor, String valor, LocalDateTime fechaHora) {
        Lectura lectura = sensor.registrarLectura(new BigDecimal(valor));
        lectura.setFechaHora(fechaHora);
        lecturas.save(lectura);
    }

    @Test
    void filtraLecturasPorRangoDeFechas() {
        Cultivo cultivo = prepararConLecturas();
        LocalDate hoy = LocalDate.now();

        List<Lectura> ultimasDos = historico.consultar(cultivo.getIdCultivo(),
                hoy.minusDays(1), hoy);

        assertEquals(2, ultimasDos.size());
    }

    @Test
    void sinFechasDevuelveTodoElHistorico() {
        Cultivo cultivo = prepararConLecturas();

        List<Lectura> todas = historico.consultar(cultivo.getIdCultivo(), null, null);

        assertEquals(3, todas.size());
    }

    @Test
    void exportaExcelDelHistorico() throws Exception {
        Cultivo cultivo = prepararConLecturas();

        byte[] archivo = historico.exportarExcel(cultivo.getIdCultivo(),
                LocalDate.now().minusDays(7), LocalDate.now());

        assertTrue(archivo.length > 0);
    }

    @Test
    void calculaResumenDelRango() {
        Cultivo cultivo = prepararConLecturas();

        var resumen = historico.resumen(cultivo.getIdCultivo(),
                LocalDate.now().minusDays(7), LocalDate.now());

        assertEquals(1, resumen.size());

        var humedad = resumen.get(0);
        assertEquals("humedad_suelo", humedad.variable());
        assertEquals(0, humedad.minimo().compareTo(new BigDecimal("30")));
        assertEquals(0, humedad.maximo().compareTo(new BigDecimal("60")));
        assertEquals(0, humedad.promedio().compareTo(new BigDecimal("45")));
        assertEquals(3L, humedad.total());
        assertEquals(0L, humedad.fueraDeRango());
    }
}
