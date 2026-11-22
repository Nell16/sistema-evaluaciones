package com.universidad.tests.domain.model;

import java.time.Instant;

public class Evaluacion {

    private final Long idEvaluacion;
    private final Long idSujeto;
    private final Long idVersionFormulario;
    private final Long idPeriodo;
    private final EstadoEvaluacion estado;
    private final Instant iniciadaEn;
    private final Instant finalizadaEn;
    private final String modalidad;
    private final String alcance;

    public Evaluacion(
        Long idEvaluacion,
        Long idSujeto,
        Long idVersionFormulario,
        Long idPeriodo,
        EstadoEvaluacion estado,
        Instant iniciadaEn,
        Instant finalizadaEn,
        String modalidad,
        String alcance) {
        this.idEvaluacion = idEvaluacion;
        this.idSujeto = idSujeto;
        this.idVersionFormulario = idVersionFormulario;
        this.idPeriodo = idPeriodo;
        this.estado = estado;
        this.iniciadaEn = iniciadaEn;
        this.finalizadaEn = finalizadaEn;
        this.modalidad = modalidad;
        this.alcance = alcance;
    }

    public boolean estaFinalizada() {
        return EstadoEvaluacion.FINALIZADA.equals(estado);
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public Long getIdSujeto() {
        return idSujeto;
    }

    public Long getIdVersionFormulario() {
        return idVersionFormulario;
    }

    public Long getIdPeriodo() {
        return idPeriodo;
    }

    public EstadoEvaluacion getEstado() {
        return estado;
    }

    public Instant getIniciadaEn() {
        return iniciadaEn;
    }

    public Instant getFinalizadaEn() {
        return finalizadaEn;
    }

    public String getModalidad() {
        return modalidad;
    }

    public String getAlcance() {
        return alcance;
    }
}
