package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_sensor")
public class TipoSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_sensor")
    private Integer idTipoSensor;

    @Column(name = "nombre_tipo", nullable = false, length = 100)
    private String nombreTipo;

    @Column(name = "unidad_medida", nullable = false, length = 50)
    private String unidadMedida;

    public String obtenerNombre() {
        return nombreTipo + " (" + unidadMedida + ")";
    }

    public Integer getIdTipoSensor() {
        return idTipoSensor;
    }

    public void setIdTipoSensor(Integer idTipoSensor) {
        this.idTipoSensor = idTipoSensor;
    }

    public String getNombreTipo() {
        return nombreTipo;
    }

    public void setNombreTipo(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public String getUnidadMedida() {
        return unidadMedida;
    }

    public void setUnidadMedida(String unidadMedida) {
        this.unidadMedida = unidadMedida;
    }
}
