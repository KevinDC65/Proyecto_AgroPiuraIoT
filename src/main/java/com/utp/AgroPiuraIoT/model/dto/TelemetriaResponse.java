package com.utp.AgroPiuraIoT.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelemetriaResponse {

    private String status;
    private int lecturasProcesadas;
    private Integer lecturasRechazadas;
    private int alertasGeneradas;
    private String mensaje;
    private List<String> errores;

    public TelemetriaResponse() {
    }

    public TelemetriaResponse(String status, int lecturasProcesadas, int alertasGeneradas, String mensaje) {
        this.status = status;
        this.lecturasProcesadas = lecturasProcesadas;
        this.alertasGeneradas = alertasGeneradas;
        this.mensaje = mensaje;
    }

    public static TelemetriaResponse ok(int lecturasProcesadas, int alertasGeneradas) {
        return new TelemetriaResponse("ok", lecturasProcesadas, alertasGeneradas,
                "Telemetría procesada exitosamente");
    }

    public static TelemetriaResponse okParcial(int lecturasProcesadas, int lecturasRechazadas,
                                               int alertasGeneradas, List<String> errores) {
        TelemetriaResponse r = new TelemetriaResponse("ok", lecturasProcesadas, alertasGeneradas,
                "Telemetría procesada con observaciones");
        r.setLecturasRechazadas(lecturasRechazadas);
        r.setErrores(errores);
        return r;
    }

    public static TelemetriaResponse error(String mensaje) {
        return new TelemetriaResponse("error", 0, 0, mensaje);
    }

    public static TelemetriaResponse errorConDetalle(String mensaje, int lecturasRechazadas, List<String> errores) {
        TelemetriaResponse r = new TelemetriaResponse("error", 0, 0, mensaje);
        r.setLecturasRechazadas(lecturasRechazadas);
        r.setErrores(errores);
        return r;
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

    public Integer getLecturasRechazadas() {
        return lecturasRechazadas;
    }

    public void setLecturasRechazadas(Integer lecturasRechazadas) {
        this.lecturasRechazadas = lecturasRechazadas;
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

    public List<String> getErrores() {
        return errores;
    }

    public void setErrores(List<String> errores) {
        this.errores = errores;
    }
}
