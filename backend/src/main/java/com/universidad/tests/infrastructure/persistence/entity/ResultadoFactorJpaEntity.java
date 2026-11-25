package com.universidad.tests.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resultado_factor")
public class ResultadoFactorJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado_factor")
    private Long idResultadoFactor;

    @Column(name = "id_resultado", nullable = false)
    private Long idResultado;

    @Column(name = "codigo", nullable = false, length = 80)
    private String codigo;

    @Column(name = "puntuacion_directa")
    private Integer puntuacionDirecta;

    @Column(name = "estado_calculo", nullable = false, length = 30)
    private String estadoCalculo;

    @Column(name = "percentil")
    private Integer percentil;

    @Column(name = "id_rango")
    private Long idRango;

    @Column(name = "estado_normativo", nullable = false, length = 30)
    private String estadoNormativo;

    @Column(name = "motivo", length = 200)
    private String motivo;

    protected ResultadoFactorJpaEntity() {
        // Constructor requerido por JPA
    }

    public ResultadoFactorJpaEntity(
            Long idResultado,
            String codigo,
            Integer puntuacionDirecta,
            String estadoCalculo,
            Integer percentil,
            Long idRango,
            String estadoNormativo,
            String motivo) {
        this.idResultado = idResultado;
        this.codigo = codigo;
        this.puntuacionDirecta = puntuacionDirecta;
        this.estadoCalculo = estadoCalculo;
        this.percentil = percentil;
        this.idRango = idRango;
        this.estadoNormativo = estadoNormativo;
        this.motivo = motivo;
    }

    public Long getIdResultadoFactor() {
        return idResultadoFactor;
    }

    public Long getIdResultado() {
        return idResultado;
    }

    public String getCodigo() {
        return codigo;
    }

    public Integer getPuntuacionDirecta() {
        return puntuacionDirecta;
    }

    public String getEstadoCalculo() {
        return estadoCalculo;
    }

    public Integer getPercentil() {
        return percentil;
    }

    public Long getIdRango() {
        return idRango;
    }

    public String getEstadoNormativo() {
        return estadoNormativo;
    }

    public String getMotivo() {
        return motivo;
    }
}
