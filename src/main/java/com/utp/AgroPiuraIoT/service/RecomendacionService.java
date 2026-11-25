package com.utp.AgroPiuraIoT.service;

import com.utp.AgroPiuraIoT.model.entity.Alerta;
import com.utp.AgroPiuraIoT.model.entity.Lectura;
import com.utp.AgroPiuraIoT.model.entity.Umbral;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Traduce una lectura fuera de rango en una recomendación orientativa.
 * El texto vive en {@code recomendaciones.properties}; este servicio solo
 * decide la variable y la dirección (baja/alta) y aplica el fallback genérico.
 */
@Service
public class RecomendacionService {

    public enum Direccion {
        BAJA, ALTA
    }

    private static final List<String> VARIABLES = List.of(
            "humedad_suelo", "temperatura_ambiente", "humedad_ambiente", "ph_suelo");

    private final Properties catalogo = new Properties();
    private final ClimaService clima;

    public RecomendacionService(
            @Value("classpath:recomendaciones.properties") Resource recurso,
            ClimaService clima) {
        this.clima = clima;
        try (Reader reader = new InputStreamReader(
                recurso.getInputStream(), StandardCharsets.UTF_8)) {
            catalogo.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No se pudo cargar recomendaciones.properties", e);
        }
        validarCatalogo();
    }

    /** Recomendación para una alerta concreta, con aviso de lluvia si aplica. */
    public String para(Alerta alerta) {
        if (alerta == null || alerta.getUmbral() == null || alerta.getLectura() == null) {
            return buscar("generica", Direccion.ALTA);
        }
        Umbral umbral = alerta.getUmbral();
        Lectura lectura = alerta.getLectura();
        if (umbral.getTipoSensor() == null || lectura.getValor() == null) {
            return buscar("generica", Direccion.ALTA);
        }
        String variable = umbral.getTipoSensor().getNombreTipo();
        Direccion direccion = direccion(lectura.getValor(),
                umbral.getValorMinimo(), umbral.getValorMaximo());
        String texto = buscar(variable, direccion);

        if (!alerta.isAtendida() && direccion == Direccion.BAJA
                && "humedad_suelo".equals(variable)
                && umbral.getCultivo() != null
                && clima.pronostico(umbral.getCultivo())
                        .map(p -> p.seEsperaLluvia())
                        .orElse(false)) {
            texto = texto + " " + catalogo.getProperty("humedad_suelo.baja.lluvia");
        }
        return texto;
    }

    /** Recomendación de catálogo, sin clima; útil para pruebas y otros usos. */
    public String para(String variable, Direccion direccion) {
        return buscar(variable, direccion);
    }

    public Direccion direccion(BigDecimal valor, BigDecimal minimo, BigDecimal maximo) {
        if (minimo != null && valor.compareTo(minimo) < 0) {
            return Direccion.BAJA;
        }
        if (maximo != null && valor.compareTo(maximo) > 0) {
            return Direccion.ALTA;
        }
        return Direccion.BAJA;
    }

    private String buscar(String variable, Direccion direccion) {
        String sufijo = direccion.name().toLowerCase(Locale.ROOT);
        String texto = catalogo.getProperty(variable + "." + sufijo);
        if (texto == null || texto.isBlank()) {
            texto = catalogo.getProperty("generica." + sufijo);
        }
        if (texto == null || texto.isBlank()) {
            throw new IllegalStateException(
                    "Falta la recomendación genérica para " + sufijo);
        }
        return texto;
    }

    private void validarCatalogo() {
        for (String variable : VARIABLES) {
            for (Direccion direccion : Direccion.values()) {
                exigir(variable + "." + direccion.name().toLowerCase(Locale.ROOT));
            }
        }
        exigir("generica.baja");
        exigir("generica.alta");
        exigir("humedad_suelo.baja.lluvia");
    }

    private void exigir(String clave) {
        String valor = catalogo.getProperty(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta la clave '" + clave + "' en recomendaciones.properties");
        }
    }
}
