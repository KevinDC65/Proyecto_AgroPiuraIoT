package com.utp.AgroPiuraIoT.model.dto;

/** Punto de serie listo para graficar una lectura del histórico. */
public record LecturaPunto(
        String fechaHora,
        String variable,
        double valor,
        String unidad,
        boolean fueraDeRango) {
}
