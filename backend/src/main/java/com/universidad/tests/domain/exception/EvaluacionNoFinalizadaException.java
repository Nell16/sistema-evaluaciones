package com.universidad.tests.domain.exception;

public class EvaluacionNoFinalizadaException extends RuntimeException {

    public EvaluacionNoFinalizadaException(Long idEvaluacion) {
        super("La evaluación " + idEvaluacion + " aún no ha finalizado");
    }
}
