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

/**
 * Episodio de alerta operativa de un nodo IoT (por ejemplo {@code nodo_sin_conexion}).
 * Usa el mismo modelo de estado ({@code abierta}/{@code cerrada}) que {@link Alerta}
 * sin alterar la tabla de alertas por umbral de B1.
 */
@Entity
@Table(name = "alerta_nodo")
public class AlertaNodo {

    public static final String TIPO_NODO_SIN_CONEXION = "nodo_sin_conexion";
    public static final String ESTADO_ABIERTA = "abierta";
    public static final String ESTADO_CERRADA = "cerrada";
    public static final String SEVERIDAD_MODERADA = "moderada";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta_nodo")
    private Integer idAlertaNodo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_dispositivo")
    private DispositivoIoT dispositivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cultivo")
    private Cultivo cultivo;

    @Column(nullable = false, length = 40)
    private String tipo = TIPO_NODO_SIN_CONEXION;

    @Column(nullable = false, length = 20)
    private String estado = ESTADO_ABIERTA;

    @Column(nullable = false, length = 20)
    private String severidad = SEVERIDAD_MODERADA;

    @Column(length = 255)
    private String mensaje;

    @Column(name = "fecha_alerta", nullable = false)
    private LocalDateTime fechaAlerta;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(nullable = false)
    private boolean atendida = false;

    @PrePersist
    protected void prePersist() {
        if (fechaAlerta == null) {
            fechaAlerta = LocalDateTime.now();
        }
        if (tipo == null) {
            tipo = TIPO_NODO_SIN_CONEXION;
        }
        if (estado == null) {
            estado = ESTADO_ABIERTA;
        }
        if (severidad == null) {
            severidad = SEVERIDAD_MODERADA;
        }
    }

    public static AlertaNodo abrirSinConexion(DispositivoIoT dispositivo, Cultivo cultivo, long horasLimite) {
        AlertaNodo alerta = new AlertaNodo();
        alerta.setDispositivo(dispositivo);
        alerta.setCultivo(cultivo);
        alerta.setTipo(TIPO_NODO_SIN_CONEXION);
        alerta.setEstado(ESTADO_ABIERTA);
        alerta.setSeveridad(SEVERIDAD_MODERADA);
        alerta.setMensaje("Nodo #" + dispositivo.getIdDispositivo()
                + " sin reportar lecturas hace más de " + horasLimite + " h");
        alerta.setFechaAlerta(LocalDateTime.now());
        return alerta;
    }

    public void cerrar(LocalDateTime fecha) {
        this.estado = ESTADO_CERRADA;
        this.fechaCierre = fecha != null ? fecha : LocalDateTime.now();
    }

    public void marcarAtendida() {
        this.atendida = true;
    }

    public boolean estaAbierta() {
        return ESTADO_ABIERTA.equalsIgnoreCase(estado);
    }

    public Integer getIdAlertaNodo() {
        return idAlertaNodo;
    }

    public void setIdAlertaNodo(Integer idAlertaNodo) {
        this.idAlertaNodo = idAlertaNodo;
    }

    public DispositivoIoT getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(DispositivoIoT dispositivo) {
        this.dispositivo = dispositivo;
    }

    public Cultivo getCultivo() {
        return cultivo;
    }

    public void setCultivo(Cultivo cultivo) {
        this.cultivo = cultivo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getSeveridad() {
        return severidad;
    }

    public void setSeveridad(String severidad) {
        this.severidad = severidad;
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

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public boolean isAtendida() {
        return atendida;
    }

    public void setAtendida(boolean atendida) {
        this.atendida = atendida;
    }
}
