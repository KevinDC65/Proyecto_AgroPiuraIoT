package com.utp.AgroPiuraIoT.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LecturaItemRequest {

    @JsonProperty("idDispositivo")
    @JsonAlias({"id_dispositivo", "dispositivo_id", "deviceId"})
    private Integer idDispositivo;

    @JsonProperty("idSensor")
    @JsonAlias({"id_sensor", "sensor_id", "sensorId"})
    private Integer idSensor;

    @JsonProperty("tipoSensor")
    @JsonAlias({"tipo_sensor", "tipo", "variable"})
    private String tipoSensor;

    @JsonProperty("idTipoSensor")
    @JsonAlias({"id_tipo_sensor"})
    private Integer idTipoSensor;

    @JsonProperty("valor")
    @JsonAlias({"value"})
    private BigDecimal valor;

    @JsonProperty("fechaHora")
    @JsonAlias({"fecha_hora", "timestamp"})
    private LocalDateTime fechaHora;

    public LecturaItemRequest() {
    }

    public LecturaItemRequest(Integer idSensor, BigDecimal valor) {
        this.idSensor = idSensor;
        this.valor = valor;
    }

    public LecturaItemRequest(String tipoSensor, BigDecimal valor) {
        this.tipoSensor = tipoSensor;
        this.valor = valor;
    }

    public LecturaItemRequest(Integer idDispositivo, String tipoSensor, BigDecimal valor) {
        this.idDispositivo = idDispositivo;
        this.tipoSensor = tipoSensor;
        this.valor = valor;
    }

    public LecturaItemRequest(Integer idDispositivo, Integer idSensor, BigDecimal valor) {
        this.idDispositivo = idDispositivo;
        this.idSensor = idSensor;
        this.valor = valor;
    }

    public Integer getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(Integer idDispositivo) {
        this.idDispositivo = idDispositivo;
    }

    public Integer getIdSensor() {
        return idSensor;
    }

    public void setIdSensor(Integer idSensor) {
        this.idSensor = idSensor;
    }

    public String getTipoSensor() {
        return tipoSensor;
    }

    public void setTipoSensor(String tipoSensor) {
        this.tipoSensor = tipoSensor;
    }

    public Integer getIdTipoSensor() {
        return idTipoSensor;
    }

    public void setIdTipoSensor(Integer idTipoSensor) {
        this.idTipoSensor = idTipoSensor;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }
}
