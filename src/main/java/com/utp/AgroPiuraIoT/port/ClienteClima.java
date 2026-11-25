package com.utp.AgroPiuraIoT.port;

import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import com.utp.AgroPiuraIoT.model.dto.PronosticoLluvia;
import java.util.Optional;

/**
 * Puerto de consulta climática (principio de inversión de dependencias, SOLID).
 * El nivel de negocio define la necesidad; la integración con OpenWeatherMap
 * la implementa sin acoplarse al resto del dominio.
 */
public interface ClienteClima {

    /** Devuelve el clima actual para una ubicación textual, o vacío si no hay datos. */
    Optional<ClimaActual> obtener(String ubicacion);

    /**
     * Pronóstico de lluvia de las próximas 24 horas. Por defecto no hay
     * pronóstico: los clientes que no lo implementen (o los falsos de prueba)
     * conservan el comportamiento anterior y la recomendación sigue funcionando.
     */
    default Optional<PronosticoLluvia> pronostico(String ubicacion) {
        return Optional.empty();
    }
}
