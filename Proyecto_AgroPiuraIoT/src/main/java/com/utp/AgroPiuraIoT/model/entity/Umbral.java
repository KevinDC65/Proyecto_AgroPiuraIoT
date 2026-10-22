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
@Table(name = "umbrales")
public class Umbral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_umbral")
    private Integer idUmbral;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_cultivo")
    private Cultivo cultivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_sensor")
    private TipoSensor tipoSensor;

    @Column(name = "valor_minimo", precision = 10, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 10, scale = 2)
    private BigDecimal valorMaximo;

    public boolean contiene(BigDecimal valor) {
        if (valor == null) {
            return false;
        }
        boolean sobreMinimo = valorMinimo == null || valor.compareTo(valorMinimo) >= 0;
        boolean bajoMaximo = valorMaximo == null || valor.compareTo(valorMaximo) <= 0;
        return sobreMinimo && bajoMaximo;
    }

    public Integer getIdUmbral() {
        return idUmbral;
    }

    public void setIdUmbral(Integer idUmbral) {
        this.idUmbral = idUmbral;
    }

    public Cultivo getCultivo() {
        return cultivo;
    }

    public void setCultivo(Cultivo cultivo) {
        this.cultivo = cultivo;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    public void setTipoSensor(TipoSensor tipoSensor) {
        this.tipoSensor = tipoSensor;
    }

    public BigDecimal getValorMinimo() {
        return valorMinimo;
    }

    public void setValorMinimo(BigDecimal valorMinimo) {
        this.valorMinimo = valorMinimo;
    }

    public BigDecimal getValorMaximo() {
        return valorMaximo;
    }

    public void setValorMaximo(BigDecimal valorMaximo) {
        this.valorMaximo = valorMaximo;
    }
}
