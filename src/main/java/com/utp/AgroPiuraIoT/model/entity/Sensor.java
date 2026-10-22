package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "sensores")
public class Sensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sensor")
    private Integer idSensor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_dispositivo")
    private DispositivoIoT dispositivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_sensor")
    private TipoSensor tipoSensor;

    public Integer getIdSensor() {
        return idSensor;
    }

    public void setIdSensor(Integer idSensor) {
        this.idSensor = idSensor;
    }

    public DispositivoIoT getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(DispositivoIoT dispositivo) {
        this.dispositivo = dispositivo;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public void setTipoSensor(TipoSensor tipoSensor) {
        this.tipoSensor = tipoSensor;
    }

    public Lectura registrarLectura(BigDecimal valor) {
        Lectura lectura = new Lectura();
        lectura.setSensor(this);
        lectura.setValor(valor);
        return lectura;
    }
}
