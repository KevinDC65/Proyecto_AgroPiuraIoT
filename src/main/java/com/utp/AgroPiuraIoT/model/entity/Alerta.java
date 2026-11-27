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
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {

    public static final String ESTADO_ABIERTA = "abierta";
    public static final String ESTADO_CERRADA = "cerrada";

    public static final String SEVERIDAD_LEVE = "leve";
    public static final String SEVERIDAD_MODERADA = "moderada";
    public static final String SEVERIDAD_CRITICA = "critica";

    public static final String DIRECCION_BAJO = "bajo";
    public static final String DIRECCION_ALTO = "alto";

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

    @Column(nullable = false, length = 20)
    private String estado = ESTADO_ABIERTA;

    @Column(nullable = false, length = 20)
    private String severidad = SEVERIDAD_LEVE;

    @Column(nullable = false, length = 10)
    private String direccion = DIRECCION_BAJO;

    @Column(name = "valor_extremo", precision = 10, scale = 2)
    private BigDecimal valorExtremo;

    @Column(name = "cantidad_lecturas", nullable = false)
    private int cantidadLecturas = 1;

    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @PrePersist
    protected void prePersist() {
        if (fechaAlerta == null) {
            fechaAlerta = LocalDateTime.now();
        }
        if (estado == null) {
            estado = ESTADO_ABIERTA;
        }
        if (severidad == null) {
            severidad = SEVERIDAD_LEVE;
        }
        if (direccion == null) {
            direccion = DIRECCION_BAJO;
        }
        if (cantidadLecturas < 1) {
            cantidadLecturas = 1;
        }
    }

    public static Alerta generar(Lectura lectura, Umbral umbral) {
        String dir = umbral != null && umbral.getValorMaximo() != null
                && lectura != null && lectura.getValor() != null
                && lectura.getValor().compareTo(umbral.getValorMaximo()) > 0
                ? DIRECCION_ALTO
                : DIRECCION_BAJO;
        return abrirEpisodio(lectura, umbral, dir, SEVERIDAD_LEVE);
    }

    public static Alerta abrirEpisodio(Lectura lectura, Umbral umbral, String direccion, String severidad) {
        if (lectura == null || umbral == null) {
            throw new IllegalArgumentException("Lectura y umbral son obligatorios para generar la alerta");
        }
        Alerta alerta = new Alerta();
        alerta.setLectura(lectura);
        alerta.setUmbral(umbral);
        alerta.setFechaAlerta(lectura.getFechaHora() != null ? lectura.getFechaHora() : LocalDateTime.now());
        alerta.setEstado(ESTADO_ABIERTA);
        alerta.setDireccion(direccion);
        alerta.setSeveridad(severidad);
        alerta.setValorExtremo(lectura.getValor());
        alerta.setCantidadLecturas(1);
        alerta.setMensaje(construirMensaje(lectura.getValor(), umbral, direccion, 1));
        return alerta;
    }

    /**
     * Registra una nueva lectura fuera de rango dentro del mismo episodio abierto:
     * conserva el valor más extremo, incrementa el conteo y solo permite subir la severidad.
     */
    public void registrarLecturaEnEpisodio(BigDecimal valor, String severidadCalculada, Umbral umbralActual) {
        this.cantidadLecturas++;
        if (valor != null) {
            if (this.valorExtremo == null) {
                this.valorExtremo = valor;
            } else if (DIRECCION_BAJO.equals(this.direccion) && valor.compareTo(this.valorExtremo) < 0) {
                this.valorExtremo = valor;
            } else if (DIRECCION_ALTO.equals(this.direccion) && valor.compareTo(this.valorExtremo) > 0) {
                this.valorExtremo = valor;
            }
        }
        if (rangoSeveridad(severidadCalculada) > rangoSeveridad(this.severidad)) {
            this.severidad = severidadCalculada;
        }
        this.mensaje = construirMensaje(this.valorExtremo, umbralActual, this.direccion, this.cantidadLecturas);
    }

    public void cerrar(LocalDateTime fecha) {
        this.estado = ESTADO_CERRADA;
        this.fechaCierre = fecha != null ? fecha : LocalDateTime.now();
    }

    public boolean estaAbierta() {
        return ESTADO_ABIERTA.equalsIgnoreCase(this.estado);
    }

    public static int rangoSeveridad(String severidad) {
        if (SEVERIDAD_CRITICA.equalsIgnoreCase(severidad)) {
            return 3;
        }
        if (SEVERIDAD_MODERADA.equalsIgnoreCase(severidad)) {
            return 2;
        }
        return 1;
    }

    public static String subirSeveridad(String severidad) {
        if (SEVERIDAD_LEVE.equalsIgnoreCase(severidad)) {
            return SEVERIDAD_MODERADA;
        }
        return SEVERIDAD_CRITICA;
    }

    private static String construirMensaje(BigDecimal valorExtremo, Umbral umbral,
                                           String direccion, int lecturas) {
        String sentido = DIRECCION_ALTO.equals(direccion) ? "por encima del máximo" : "por debajo del mínimo";
        String texto = "Lectura " + valorExtremo + " " + sentido
                + " [" + umbral.getValorMinimo() + " - " + umbral.getValorMaximo() + "]";
        if (lecturas > 1) {
            texto += " (" + lecturas + " lecturas)";
        }
        return texto.length() <= 255 ? texto : texto.substring(0, 255);
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

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public BigDecimal getValorExtremo() {
        return valorExtremo;
    }

    public void setValorExtremo(BigDecimal valorExtremo) {
        this.valorExtremo = valorExtremo;
    }

    public int getCantidadLecturas() {
        return cantidadLecturas;
    }

    public void setCantidadLecturas(int cantidadLecturas) {
        this.cantidadLecturas = cantidadLecturas;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }
}
