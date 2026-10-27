package com.utp.AgroPiuraIoT.model.dto;

import java.math.BigDecimal;

public record ResumenVariable(
        String variable,
        String unidad,
        BigDecimal minimo,
        BigDecimal maximo,
        BigDecimal promedio,
        long total,
        long fueraDeRango) {
}