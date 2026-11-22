package com.universidad.tests.domain.model;

public class FactorResultado {

    private final String codigo;
    private final Integer puntuacionDirecta;
    private final String estadoCalculo;
    private final Integer percentil;
    private final Long idRango;
    private final String estadoNormativo;
    private final String motivo;

    public FactorResultado(
        String codigo,
        Integer puntuacionDirecta,
        String estadoCalculo,
        Integer percentil,
        Long idRango,
        String estadoNormativo,
        String motivo) {
        this.codigo = codigo;
        this.puntuacionDirecta = puntuacionDirecta;
        this.estadoCalculo = estadoCalculo;
        this.percentil = percentil;
        this.idRango = idRango;
        this.estadoNormativo = estadoNormativo;
        this.motivo = motivo;
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
