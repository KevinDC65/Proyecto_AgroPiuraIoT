package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Integer idAlerta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_lectura")
    private Lectura lectura;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_umbral")
    private Umbral umbral;

    @Column(length = 255)
    private String mensaje;

    @Column(name = "fecha_alerta")
    private LocalDateTime fechaAlerta;

    @PrePersist
    void prePersist() {
        if (fechaAlerta == null) {
            fechaAlerta = LocalDateTime.now();
        }
    }

    public static Alerta generar(Lectura lectura, Umbral umbral) {
        Alerta alerta = new Alerta();
        alerta.setLectura(lectura);
        alerta.setUmbral(umbral);
        alerta.setMensaje("Lectura " + lectura.getValor() + " fuera del rango ["
                + umbral.getValorMinimo() + " - " + umbral.getValorMaximo() + "]");
        return alerta;
    }

    public Integer getIdAlerta() {
        return idAlerta;
    }

    public void setIdAlerta(Integer idAlerta) {
        this.idAlerta = idAlerta;
    }

    public Lectura getLectura() {
        return lectura;
    }

    public void setLectura(Lectura lectura) {
        this.lectura = lectura;
    }

    public Umbral getUmbral() {
        return umbral;
    }

    public void setUmbral(Umbral umbral) {
        this.umbral = umbral;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFechaAlerta() {
        return fechaAlerta;
    }

    public void setFechaAlerta(LocalDateTime fechaAlerta) {
        this.fechaAlerta = fechaAlerta;
    }
}
