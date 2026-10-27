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

    @Column(nullable = false)
    private boolean atendida = false;   

    @PrePersist
    protected void prePersist() {
        if (fechaAlerta == null) {
            fechaAlerta = LocalDateTime.now();
        }
    }

    public static Alerta generar(Lectura lectura, Umbral umbral) {
        if (lectura == null || umbral == null) {
            throw new IllegalArgumentException("Lectura y umbral son obligatorios para generar la alerta");
        }
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

        public void marcarAtendida() {
        this.atendida = true;
    }

    public boolean isAtendida() {
        return atendida;
    }

    public void setAtendida(boolean atendida) {
        this.atendida = atendida;
    }
}
