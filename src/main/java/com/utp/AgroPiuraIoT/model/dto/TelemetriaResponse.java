package com.utp.AgroPiuraIoT.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetriaResponse {

    private String status;
    private int lecturasProcesadas;
    private int alertasGeneradas;
    private String mensaje;
    private int lecturasRechazadas;
    /** Lecturas ignoradas porque ya estaban registradas (mismo sensor y fecha). */
    private int lecturasDuplicadas;
    /** Motivo de cada lectura rechazada; se omite en el JSON si no hubo rechazos. */
    private List<String> rechazos;

    public TelemetriaResponse() {
    }

    public TelemetriaResponse(String status, int lecturasProcesadas, int alertasGeneradas, String mensaje) {
        this.status = status;
        this.lecturasProcesadas = lecturasProcesadas;
        this.alertasGeneradas = alertasGeneradas;
        this.mensaje = mensaje;
    }

    public static TelemetriaResponse ok(int lecturasProcesadas, int alertasGeneradas) {
        return new TelemetriaResponse("ok", lecturasProcesadas, alertasGeneradas, "Telemetría procesada exitosamente");
    }

    /**
     * Resultado de un lote en el que algunas lecturas se rechazaron por ser
     * físicamente imposibles: "parcial" si se guardó alguna, "rechazado" si ninguna.
     */
    public static TelemetriaResponse conRechazos(int lecturasProcesadas, int alertasGeneradas,
                                                 List<String> rechazos) {
        String status = lecturasProcesadas > 0 ? "parcial" : "rechazado";
        String mensaje = rechazos.size() + " lectura(s) rechazada(s) por valor físicamente imposible";
        TelemetriaResponse r = new TelemetriaResponse(status, lecturasProcesadas, alertasGeneradas, mensaje);
        r.setLecturasRechazadas(rechazos.size());
        r.setRechazos(List.copyOf(rechazos));
        return r;
    }

    public static TelemetriaResponse error(String mensaje) {
        return new TelemetriaResponse("error", 0, 0, mensaje);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getLecturasProcesadas() {
        return lecturasProcesadas;
    }

    public void setLecturasProcesadas(int lecturasProcesadas) {
        this.lecturasProcesadas = lecturasProcesadas;
    }

    public int getAlertasGeneradas() {
        return alertasGeneradas;
    }

    public void setAlertasGeneradas(int alertasGeneradas) {
        this.alertasGeneradas = alertasGeneradas;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public int getLecturasRechazadas() {
        return lecturasRechazadas;
    }

    public void setLecturasRechazadas(int lecturasRechazadas) {
        this.lecturasRechazadas = lecturasRechazadas;
    }

    public int getLecturasDuplicadas() {
        return lecturasDuplicadas;
    }

    public void setLecturasDuplicadas(int lecturasDuplicadas) {
        this.lecturasDuplicadas = lecturasDuplicadas;
    }

    public List<String> getRechazos() {
        return rechazos;
    }

    public void setRechazos(List<String> rechazos) {
        this.rechazos = rechazos;
    }
}
