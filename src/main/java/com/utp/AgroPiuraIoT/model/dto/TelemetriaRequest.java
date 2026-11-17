package com.utp.AgroPiuraIoT.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TelemetriaRequest {

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

    @JsonProperty("temperatura")
    @JsonAlias({"temperature", "temp"})
    private BigDecimal temperatura;

    @JsonProperty("humedad")
    @JsonAlias({"humidity"})
    private BigDecimal humedad;

    @JsonProperty("humedadSuelo")
    @JsonAlias({"humedad_suelo", "soil_humidity", "soil_moisture"})
    private BigDecimal humedadSuelo;

    @JsonProperty("lecturas")
    @JsonAlias({"readings", "items"})
    private List<LecturaItemRequest> lecturas;

    public TelemetriaRequest() {
    }

    public TelemetriaRequest(Integer idDispositivo, Integer idSensor, BigDecimal valor) {
        this.idDispositivo = idDispositivo;
        this.idSensor = idSensor;
        this.valor = valor;
    }

    public TelemetriaRequest(Integer idDispositivo, String tipoSensor, BigDecimal valor) {
        this.idDispositivo = idDispositivo;
        this.tipoSensor = tipoSensor;
        this.valor = valor;
    }

    public TelemetriaRequest(Integer idDispositivo, List<LecturaItemRequest> lecturas) {
        this.idDispositivo = idDispositivo;
        this.lecturas = lecturas;
    }

    /**
     * Normaliza los datos recibidos (lectura única, atajos o lista) en una lista
     * uniforme de {@link LecturaItemRequest}.
     */
    public List<LecturaItemRequest> obtenerItems() {
        List<LecturaItemRequest> items = new ArrayList<>();

        if (lecturas != null && !lecturas.isEmpty()) {
            for (LecturaItemRequest item : lecturas) {
                if (item.getIdDispositivo() == null) {
                    item.setIdDispositivo(this.idDispositivo);
                }
                if (item.getFechaHora() == null) {
                    item.setFechaHora(this.fechaHora);
                }
                items.add(item);
            }
        } else if (valor != null) {
            LecturaItemRequest item = new LecturaItemRequest();
            item.setIdDispositivo(this.idDispositivo);
            item.setIdSensor(this.idSensor);
            item.setTipoSensor(this.tipoSensor);
            item.setIdTipoSensor(this.idTipoSensor);
            item.setValor(this.valor);
            item.setFechaHora(this.fechaHora);
            items.add(item);
        } else {
            if (this.temperatura != null) {
                LecturaItemRequest item = new LecturaItemRequest();
                item.setIdDispositivo(this.idDispositivo);
                item.setTipoSensor("temperatura_ambiente");
                item.setValor(this.temperatura);
                item.setFechaHora(this.fechaHora);
                items.add(item);
            }
            if (this.humedad != null) {
                LecturaItemRequest item = new LecturaItemRequest();
                item.setIdDispositivo(this.idDispositivo);
                item.setTipoSensor("humedad_ambiente");
                item.setValor(this.humedad);
                item.setFechaHora(this.fechaHora);
                items.add(item);
            }
            if (this.humedadSuelo != null) {
                LecturaItemRequest item = new LecturaItemRequest();
                item.setIdDispositivo(this.idDispositivo);
                item.setTipoSensor("humedad_suelo");
                item.setValor(this.humedadSuelo);
                item.setFechaHora(this.fechaHora);
                items.add(item);
            }
        }

        return items;
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

    public BigDecimal getTemperatura() {
        return temperatura;
    }

    public void setTemperatura(BigDecimal temperatura) {
        this.temperatura = temperatura;
    }

    public BigDecimal getHumedad() {
        return humedad;
    }

    public void setHumedad(BigDecimal humedad) {
        this.humedad = humedad;
    }

    public BigDecimal getHumedadSuelo() {
        return humedadSuelo;
    }

    public void setHumedadSuelo(BigDecimal humedadSuelo) {
        this.humedadSuelo = humedadSuelo;
    }

    public List<LecturaItemRequest> getLecturas() {
        return lecturas;
    }

    public void setLecturas(List<LecturaItemRequest> lecturas) {
        this.lecturas = lecturas;
    }
}
