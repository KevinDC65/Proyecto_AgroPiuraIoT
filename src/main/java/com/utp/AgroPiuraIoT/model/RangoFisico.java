package com.utp.AgroPiuraIoT.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Límites físicos posibles por variable de sensor (compartido entre A1 telemetría y B1 umbrales).
 * Permite rechazar lecturas imposibles por falla de sensor y umbrales fuera de rango físico.
 */
public record RangoFisico(BigDecimal minimo, BigDecimal maximo, String unidad) {

    private static final Map<String, RangoFisico> POR_VARIABLE = Map.of(
            "humedad_suelo", new RangoFisico(new BigDecimal("0"), new BigDecimal("100"), "%"),
            "humedad_ambiente", new RangoFisico(new BigDecimal("0"), new BigDecimal("100"), "%"),
            "ph_suelo", new RangoFisico(new BigDecimal("0"), new BigDecimal("14"), "pH"),
            "temperatura_ambiente", new RangoFisico(new BigDecimal("-10"), new BigDecimal("60"), "°C")
    );

    public static Optional<RangoFisico> para(String nombreTipo) {
        if (nombreTipo == null || nombreTipo.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(POR_VARIABLE.get(nombreTipo.trim().toLowerCase()));
    }

    public static boolean esValido(String nombreTipo, BigDecimal valor) {
        if (valor == null) {
            return false;
        }
        return para(nombreTipo).map(r -> r.contiene(valor)).orElse(true);
    }

    public boolean contiene(BigDecimal valor) {
        return valor != null
                && valor.compareTo(minimo) >= 0
                && valor.compareTo(maximo) <= 0;
    }

    public BigDecimal ancho() {
        return maximo.subtract(minimo);
    }

    public String describir() {
        return minimo.stripTrailingZeros().toPlainString()
                + " a "
                + maximo.stripTrailingZeros().toPlainString()
                + " " + unidad;
    }
}
