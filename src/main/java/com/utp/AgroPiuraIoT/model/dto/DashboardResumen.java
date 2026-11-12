package com.utp.AgroPiuraIoT.model.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumen(
        long cultivosActivos,
        long alertasActivas,
        BigDecimal humedadPromedio,
        BigDecimal temperaturaPromedio,
        String ultimaSincronizacion,
        List<CultivoEstado> cultivos,
        long dispositivosActivos) {

    public DashboardResumen(
            long cultivosActivos,
            long alertasActivas,
            BigDecimal humedadPromedio,
            BigDecimal temperaturaPromedio,
            String ultimaSincronizacion,
            List<CultivoEstado> cultivos) {
        this(cultivosActivos, alertasActivas, humedadPromedio, temperaturaPromedio, ultimaSincronizacion, cultivos, 0L);
    }

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
