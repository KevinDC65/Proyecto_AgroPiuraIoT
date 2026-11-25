package com.utp.AgroPiuraIoT.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

/**
 * Valores físicamente posibles que puede medir cada tipo de sensor. Una lectura
 * fuera de este rango no describe al cultivo sino a un sensor dañado o mal
 * conectado, así que se rechaza antes de evaluar umbrales (A1) y no debe generar
 * alertas agronómicas (B1). Ver docs/12-api-de-lecturas.md.
 * <p>
 * No confundir con los umbrales del cultivo: el umbral dice qué es bueno para la
 * planta; el rango físico dice qué es posible medir.
 */
public enum RangoFisico {

    HUMEDAD_SUELO("humedad_suelo", "0", "100", "%"),
    HUMEDAD_AMBIENTE("humedad_ambiente", "0", "100", "%"),
    PH_SUELO("ph_suelo", "0", "14", ""),
    TEMPERATURA_AMBIENTE("temperatura_ambiente", "-10", "60", "°C");

    private final String tipoSensor;
    private final BigDecimal minimo;
    private final BigDecimal maximo;
    private final String unidad;

    RangoFisico(String tipoSensor, String minimo, String maximo, String unidad) {
        this.tipoSensor = tipoSensor;
        this.minimo = new BigDecimal(minimo);
        this.maximo = new BigDecimal(maximo);
        this.unidad = unidad;
    }

    /** Rango del tipo de sensor ({@code nombre_tipo} de {@code tipo_sensor}), si se conoce. */
    public static Optional<RangoFisico> de(String tipoSensor) {
        if (tipoSensor == null) {
            return Optional.empty();
        }
        String buscado = tipoSensor.trim();
        return Arrays.stream(values())
                .filter(r -> r.tipoSensor.equalsIgnoreCase(buscado))
                .findFirst();
    }

    /**
     * Indica si el valor es físicamente posible para ese tipo de sensor. Un tipo
     * sin rango conocido se acepta, para no bloquear sensores nuevos.
     */
    public static boolean esPosible(String tipoSensor, BigDecimal valor) {
        return de(tipoSensor).map(r -> r.admite(valor)).orElse(true);
    }

    /** Límites inclusivos: 0 % y 100 % son lecturas válidas. */
    public boolean admite(BigDecimal valor) {
        return valor != null && valor.compareTo(minimo) >= 0 && valor.compareTo(maximo) <= 0;
    }

    /** Texto para logs y respuestas, por ejemplo "0 a 14" o "-10 a 60 °C". */
    public String describir() {
        String texto = minimo.toPlainString() + " a " + maximo.toPlainString();
        return unidad.isEmpty() ? texto : texto + " " + unidad;
    }

    public String getTipoSensor() {
        return tipoSensor;
    }

    public BigDecimal getMinimo() {
        return minimo;
    }

    public BigDecimal getMaximo() {
        return maximo;
    }

    public String getUnidad() {
        return unidad;
    }
}
