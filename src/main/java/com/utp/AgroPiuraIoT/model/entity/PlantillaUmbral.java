package com.utp.AgroPiuraIoT.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;

/**
 * Valores de referencia por tipo de cultivo y variable, con la fuente
 * agronómica que los respalda. Se siembran al arrancar desde
 * {@code plantillas-umbral.csv} y se copian a {@link Umbral} cuando el
 * administrador decide aplicar la plantilla al crear un cultivo.
 */
@Entity
@Table(name = "plantilla_umbral",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_plantilla_umbral_cultivo_tipo",
                columnNames = {"tipo_cultivo", "id_tipo_sensor"}))
public class PlantillaUmbral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla_umbral")
    private Integer idPlantillaUmbral;

    @Column(name = "tipo_cultivo", nullable = false, length = 100)
    private String tipoCultivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_sensor")
    private TipoSensor tipoSensor;

    @Column(name = "valor_minimo", precision = 10, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 10, scale = 2)
    private BigDecimal valorMaximo;

    @Column(nullable = false, length = 500)
    private String fuente;

    @Column(name = "url_fuente", length = 500)
    private String urlFuente;

    @Column(length = 500)
    private String notas;

    public Umbral aUmbral(Cultivo cultivo) {
        Umbral umbral = new Umbral();
        umbral.setCultivo(cultivo);
        umbral.setTipoSensor(tipoSensor);
        umbral.setValorMinimo(valorMinimo);
        umbral.setValorMaximo(valorMaximo);
        return umbral;
    }

    public Integer getIdPlantillaUmbral() {
        return idPlantillaUmbral;
    }

    public void setIdPlantillaUmbral(Integer idPlantillaUmbral) {
        this.idPlantillaUmbral = idPlantillaUmbral;
    }

    public String getTipoCultivo() {
        return tipoCultivo;
    }

    public void setTipoCultivo(String tipoCultivo) {
        this.tipoCultivo = tipoCultivo;
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

    public String getFuente() {
        return fuente;
    }

    public void setFuente(String fuente) {
        this.fuente = fuente;
    }

    public String getUrlFuente() {
        return urlFuente;
    }

    public void setUrlFuente(String urlFuente) {
        this.urlFuente = urlFuente;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }
}
