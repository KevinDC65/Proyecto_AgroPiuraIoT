package com.utp.AgroPiuraIoT.model.dto;

/**
 * Resumen del pronóstico de las próximas 24 horas para la recomendación de
 * riego. Si el proveedor no está configurado, el puerto devuelve vacío y la
 * recomendación funciona exactamente igual.
 */
public record PronosticoLluvia(
        boolean seEsperaLluvia,
        double probabilidad,
        String detalle) {
}
