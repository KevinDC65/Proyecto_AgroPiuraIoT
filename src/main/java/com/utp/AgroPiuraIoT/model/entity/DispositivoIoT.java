package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** Hash BCrypt del token del nodo; el token en texto plano nunca se guarda. */
    @Column(name = "token_hash", length = 100)
    private String tokenHash;

    /** Última vez que el nodo envió lecturas a la API. */
    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    @PrePersist
    protected void prePersist() {
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

    public boolean tieneToken() {
        return tokenHash != null && !tokenHash.isBlank();
    }

    public void registrarConexion(LocalDateTime momento) {
        this.ultimaConexion = momento;
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

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getUltimaConexion() {
        return ultimaConexion;
    }

    public void setUltimaConexion(LocalDateTime ultimaConexion) {
        this.ultimaConexion = ultimaConexion;
    }
}
