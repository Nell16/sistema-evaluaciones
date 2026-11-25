package com.universidad.tests.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record Evaluacion(
        Long idEvaluacion,
        Long idSujeto,
        Long idPeriodo,
        Long idTest,
        Long idVersionFormulario,
        EstadoEvaluacion estado,
        Instant fechaInicio,
        Instant fechaFin,
        List<EvaluacionSubprueba> subpruebas) {

    public Evaluacion {
        subpruebas = subpruebas == null ? List.of() : List.copyOf(subpruebas);
    }

    public Optional<EvaluacionSubprueba> subpruebaActiva() {
        return subpruebas.stream().filter(s -> s.estado() == EstadoSubprueba.ACTIVA).findFirst();
    }
}
