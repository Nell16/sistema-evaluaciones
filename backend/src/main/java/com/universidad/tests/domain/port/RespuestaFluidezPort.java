package com.universidad.tests.domain.port;

public interface RespuestaFluidezPort {

    /** Verdadero si la respuesta es una palabra de Fluidez Verbal de esa evaluación. */
    boolean esPalabraFluidezDe(Long idEvaluacion, Long idRespuesta);
}
