package com.universidad.tests.domain.model;

import java.time.Instant;

public class Resultado {

    private final Long idResultado;
    private final Long idEvaluacion;
    private final EstadoResultado estado;
    private final Long referenciaRevisionVigente;
    private final Instant actualizadoEn;

    public Resultado(
        Long idResultado,
        Long idEvaluacion,
        EstadoResultado estado,
        Long referenciaRevisionVigente,
        Instant actualizadoEn) {
        this.idResultado = idResultado;
        this.idEvaluacion = idEvaluacion;
        this.estado = estado;
        this.referenciaRevisionVigente = referenciaRevisionVigente;
        this.actualizadoEn = actualizadoEn;
    }

    public boolean estaDisponible() {
        return EstadoResultado.DISPONIBLE.equals(estado) && referenciaRevisionVigente != null;
    }

    public Long getIdResultado() {
        return idResultado;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public EstadoResultado getEstado() {
        return estado;
    }

    public Long getReferenciaRevisionVigente() {
        return referenciaRevisionVigente;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
