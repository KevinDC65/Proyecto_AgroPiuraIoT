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

    public static final String ESTADO_ACTIVO = "activo";
    public static final String ESTADO_INACTIVO = "inactivo";

    public static final String CONEXION_EN_LINEA = "EN_LINEA";
    public static final String CONEXION_SIN_CONEXION = "SIN_CONEXION";
    public static final String CONEXION_INACTIVO = "INACTIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Integer idDispositivo;

    @Column(nullable = false, length = 50)
    private String estado = ESTADO_ACTIVO;

    @Column(name = "fecha_instalacion")
    private LocalDate fechaInstalacion;

    @Column(name = "token_hash", length = 255)
    private String tokenHash;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    @PrePersist
    protected void prePersist() {
        if (fechaInstalacion == null) {
            fechaInstalacion = LocalDate.now();
        }
        if (estado == null) {
            estado = ESTADO_ACTIVO;
        }
    }

    public boolean estaActivo() {
        return ESTADO_ACTIVO.equalsIgnoreCase(estado);
    }

    public void marcarActivo() {
        this.estado = ESTADO_ACTIVO;
    }

    public void marcarInactivo() {
        this.estado = ESTADO_INACTIVO;
    }

    public boolean tieneToken() {
        return tokenHash != null && !tokenHash.isBlank();
    }

    public boolean estaEnLinea(LocalDateTime ahora, long minutosEnLinea) {
        if (!estaActivo() || ultimaConexion == null || ahora == null) {
            return false;
        }
        return !ultimaConexion.isBefore(ahora.minusMinutes(minutosEnLinea));
    }

    public String estadoConexion(LocalDateTime ahora, long minutosEnLinea) {
        if (!estaActivo()) {
            return CONEXION_INACTIVO;
        }
        return estaEnLinea(ahora, minutosEnLinea) ? CONEXION_EN_LINEA : CONEXION_SIN_CONEXION;
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
