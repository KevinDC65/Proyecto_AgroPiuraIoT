package com.utp.AgroPiuraIoT.port;

import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import java.util.Optional;

/**
 * Puerto de consulta climática (principio de inversión de dependencias, SOLID).
 * El nivel de negocio define la necesidad; la integración con OpenWeatherMap
 * la implementa sin acoplarse al resto del dominio.
 */
public interface ClienteClima {

    /** Devuelve el clima actual para una ubicación textual, o vacío si no hay datos. */
    Optional<ClimaActual> obtener(String ubicacion);
}
