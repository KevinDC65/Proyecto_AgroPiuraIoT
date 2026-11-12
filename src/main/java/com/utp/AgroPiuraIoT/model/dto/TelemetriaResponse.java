package com.utp.AgroPiuraIoT.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetriaResponse {

    private String status;
    private int lecturasProcesadas;
    private int alertasGeneradas;
    private String mensaje;

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
}
