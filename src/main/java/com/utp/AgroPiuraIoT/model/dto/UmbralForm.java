package com.utp.AgroPiuraIoT.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UmbralForm(
        @NotNull(message = "El tipo de sensor es obligatorio")
        Integer idTipoSensor,

        @NotNull(message = "El valor mínimo es obligatorio")
        @DecimalMin(value = "0", message = "El valor mínimo no puede ser negativo")
        @DecimalMax(value = "1000", message = "El valor mínimo no puede superar 1000")
        BigDecimal valorMinimo,

        @NotNull(message = "El valor máximo es obligatorio")
        @DecimalMin(value = "0", message = "El valor máximo no puede ser negativo")
        @DecimalMax(value = "1000", message = "El valor máximo no puede superar 1000")
        BigDecimal valorMaximo) {
}
