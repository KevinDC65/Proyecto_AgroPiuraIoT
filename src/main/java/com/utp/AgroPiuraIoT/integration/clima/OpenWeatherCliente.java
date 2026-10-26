package com.utp.AgroPiuraIoT.integration.clima;

import com.utp.AgroPiuraIoT.model.dto.ClimaActual;
import com.utp.AgroPiuraIoT.port.ClienteClima;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Implementación del puerto de clima contra OpenWeatherMap (RF-06).
 * Si no hay API key configurada, responde vacío sin hacer llamadas de red:
 * la aplicación funciona sin integración, degradando con elegancia.
 * <p>
 * El mapeo JSON usa records con nombres de propiedad idénticos a la respuesta
 * del proveedor (main.temp, main.humidity, weather[].description), sin
 * dependencia directa de Jackson.
 */
@Component
public class OpenWeatherCliente implements ClienteClima {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherCliente.class);

    private final RestClient clienteWeb;
    private final String apiKey;

    public OpenWeatherCliente(
            @Value("${clima.openweather.api-key:}") String apiKey,
            @Value("${clima.openweather.url:https://api.openweathermap.org/data/2.5}")
            String baseUrl) {
        this.apiKey = apiKey;
        this.clienteWeb = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Optional<ClimaActual> obtener(String ubicacion) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("Clima desactivado: no hay API key de OpenWeatherMap configurada");
            return Optional.empty();
        }
        try {
            OpenWeatherRespuesta respuesta = clienteWeb.get()
                    .uri(uri -> uri.queryParam("q", ubicacion)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .queryParam("lang", "es")
                            .build())
                    .retrieve()
                    .body(OpenWeatherRespuesta.class);
            if (respuesta == null || respuesta.main() == null) {
                return Optional.empty();
            }
            String condicion = respuesta.weather() != null && !respuesta.weather().isEmpty()
                    ? respuesta.weather().get(0).description()
                    : "sin datos";
            return Optional.of(new ClimaActual(
                    ubicacion,
                    respuesta.main().temp() == null ? Double.NaN : respuesta.main().temp(),
                    respuesta.main().humidity() == null ? Double.NaN : respuesta.main().humidity(),
                    condicion));
        } catch (RestClientException e) {
            log.warn("OpenWeatherMap no respondió para {}: {}", ubicacion, e.getMessage());
            return Optional.empty();
        }
    }

    /** Estructura mínima de la respuesta de OpenWeatherMap. */
    record OpenWeatherRespuesta(Main main, List<Tiempo> weather) {
    }

    record Main(Double temp, Double humidity) {
    }

    record Tiempo(String description) {
    }
}
