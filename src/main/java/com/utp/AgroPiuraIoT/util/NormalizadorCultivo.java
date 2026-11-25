package com.utp.AgroPiuraIoT.util;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza el tipo de cultivo escrito por el usuario para relacionarlo con
 * las plantillas de umbrales. La plantilla guarda claves canónicas sin tildes
 * (mango, limon, banano, arroz, papa) y aquí se aceptan las variantes usuales
 * del formulario: "Limón", "limonero", "Plátano", "banana", etc.
 */
public final class NormalizadorCultivo {

    private NormalizadorCultivo() {
    }

    public static String normalizar(String tipoCultivo) {
        if (tipoCultivo == null || tipoCultivo.isBlank()) {
            return "";
        }
        String texto = Normalizer.normalize(tipoCultivo.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        return switch (texto) {
            case "mango", "mango kent", "mango haden", "mango criollo" -> "mango";
            case "limon", "limon sutil", "limonero", "limonero sutil" -> "limon";
            case "banano", "banana", "platano", "platano seda", "musa" -> "banano";
            case "arroz", "arrozales" -> "arroz";
            case "papa", "patata" -> "papa";
            default -> texto;
        };
    }
}
