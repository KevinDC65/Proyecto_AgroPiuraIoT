package com.utp.AgroPiuraIoT;

import com.utp.AgroPiuraIoT.model.RangoFisico;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A1 · Rangos físicos posibles por tipo de sensor (prueba unitaria, sin BD). */
class RangoFisicoTest {

    @Test
    void aceptaValoresPosiblesIncluidosLosLimites() {
        assertTrue(RangoFisico.esPosible("humedad_suelo", new BigDecimal("0")));
        assertTrue(RangoFisico.esPosible("humedad_suelo", new BigDecimal("100")));
        assertTrue(RangoFisico.esPosible("humedad_ambiente", new BigDecimal("65.5")));
        assertTrue(RangoFisico.esPosible("ph_suelo", new BigDecimal("7.2")));
        assertTrue(RangoFisico.esPosible("temperatura_ambiente", new BigDecimal("-10")));
        assertTrue(RangoFisico.esPosible("temperatura_ambiente", new BigDecimal("60")));
    }

    @Test
    void rechazaValoresImposibles() {
        assertFalse(RangoFisico.esPosible("humedad_suelo", new BigDecimal("-1")));
        assertFalse(RangoFisico.esPosible("humedad_ambiente", new BigDecimal("100.1")));
        assertFalse(RangoFisico.esPosible("ph_suelo", new BigDecimal("20")));
        assertFalse(RangoFisico.esPosible("temperatura_ambiente", new BigDecimal("-50")));
        assertFalse(RangoFisico.esPosible("temperatura_ambiente", new BigDecimal("85")));
    }

    @Test
    void reconoceElTipoSinImportarMayusculasNiEspacios() {
        assertEquals(RangoFisico.PH_SUELO, RangoFisico.de(" PH_Suelo ").orElseThrow());
    }

    @Test
    void tipoSinRangoConocidoSeAcepta() {
        assertTrue(RangoFisico.de("radiacion_solar").isEmpty());
        assertTrue(RangoFisico.esPosible("radiacion_solar", new BigDecimal("1200")));
        assertTrue(RangoFisico.de(null).isEmpty());
    }

    @Test
    void valorNuloNoEsPosible() {
        assertFalse(RangoFisico.HUMEDAD_SUELO.admite(null));
    }

    @Test
    void describeElRangoConSuUnidad() {
        assertEquals("-10 a 60 °C", RangoFisico.TEMPERATURA_AMBIENTE.describir());
        assertEquals("0 a 14", RangoFisico.PH_SUELO.describir());
    }
}
