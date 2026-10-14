package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "dispositivo_iot")
public class DispositivoIoT {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Integer idDispositivo;

    @Column(nullable = false, length = 50)
    private String estado = "activo";

    @Column(name = "fecha_instalacion")
    private LocalDate fechaInstalacion;

    @PrePersist
    void prePersist() {
        if (fechaInstalacion == null) {
            fechaInstalacion = LocalDate.now();
        }
        if (estado == null) {
            estado = "activo";
        }
    }

    public boolean estaActivo() {
        return "activo".equalsIgnoreCase(estado);
    }

    public void marcarInactivo() {
        this.estado = "inactivo";
    }

    public Integer getIdDispositivo() {
        return idDispositivo;
    }

    public void setIdDispositivo(Integer idDispositivo) {
        this.idDispositivo = idDispositivo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDate getFechaInstalacion() {
        return fechaInstalacion;
    }

    public void setFechaInstalacion(LocalDate fechaInstalacion) {
        this.fechaInstalacion = fechaInstalacion;
    }
}
