package com.utp.AgroPiuraIoT.model.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CultivoForm(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de cultivo es obligatorio")
        @Size(max = 100, message = "El tipo no puede superar 100 caracteres")
        String tipoCultivo,

        @Size(max = 200, message = "La ubicación no puede superar 200 caracteres")
        String ubicacion,

        @DecimalMin(value = "0", message = "El área debe ser positiva")
        BigDecimal areaHa,

        @DecimalMin(value = "-90", message = "Latitud inválida")
        @DecimalMax(value = "90", message = "Latitud inválida")
        BigDecimal latitud,

        @DecimalMin(value = "-180", message = "Longitud inválida")
        @DecimalMax(value = "180", message = "Longitud inválida")
        BigDecimal longitud) {
}
