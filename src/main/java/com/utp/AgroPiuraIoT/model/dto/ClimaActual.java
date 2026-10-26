package com.utp.AgroPiuraIoT.model.dto;

/** Clima actual de una ubicación, listo para mostrar en la interfaz. */
public record ClimaActual(
        String ciudad,
        double temperaturaCelsius,
        double humedadPorcentaje,
        String condicion) {
}
