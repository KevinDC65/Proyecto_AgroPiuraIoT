package com.utp.AgroPiuraIoT.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumen(
        long cultivosActivos,
        long alertasActivas,
        BigDecimal humedadPromedio,
        BigDecimal temperaturaPromedio,
        String ultimaSincronizacion,
        List<CultivoEstado> cultivos) {

    public record CultivoEstado(
        Integer idCultivo,
        String nombre,
        BigDecimal humedad,
        BigDecimal temperatura,
        String estado,
        BigDecimal latitud,
        BigDecimal longitud) {
        }
}
