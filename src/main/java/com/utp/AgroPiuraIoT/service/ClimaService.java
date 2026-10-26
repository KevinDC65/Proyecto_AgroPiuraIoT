package com.utp.AgroPiuraIoT.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import com.utp.AgroPiuraIoT.model.entity.Cultivo;
import com.utp.AgroPiuraIoT.port.ClienteClima;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Clima de terceros para el dashboard (RF-06, CU-07).
 * Respeta los límites del proveedor gratuito cacheando la respuesta con
 * <strong>Guava</strong>; si el proveedor falla o la ubicación no existe,
 * devuelve vacío para no interrumpir la operación del dashboard.
 */
@Service
public class ClimaService {

    private static final Logger log = LoggerFactory.getLogger(ClimaService.class);

    private final ClienteClima cliente;
    private final LoadingCache<String, Optional<ClimaActual>> climaPorUbicacion;

    public ClimaService(ClienteClima cliente,
                        @Value("${clima.cache.minutos:30}") long minutosCache) {
        this.cliente = cliente;
        this.climaPorUbicacion = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(minutosCache))
                .build(CacheLoader.from(this::consultarProveedor));
    }

    /** Clima actual para la ubicación del cultivo (vacío si el cultivo no tiene). */
    public Optional<ClimaActual> actual(Cultivo cultivo) {
        if (cultivo == null || cultivo.getUbicacion() == null
                || cultivo.getUbicacion().isBlank()) {
            return Optional.empty();
        }
        try {
            return climaPorUbicacion.get(cultivo.getUbicacion().trim());
        } catch (Exception e) {
            log.error("No se pudo obtener el clima para {}: {}",
                    cultivo.getUbicacion(), e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ClimaActual> consultarProveedor(String ubicacion) {
        return cliente.obtener(ubicacion);
    }
}
