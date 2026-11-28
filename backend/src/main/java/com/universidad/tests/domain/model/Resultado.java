package com.universidad.tests.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Resultado(
        Long idResultado,
        Long idEvaluacion,
        String estadoResultado,
        Long versionFormularioId,
        Instant creadoEn,
        Long referenciaRevisionVigente,
        Long idClaveCalculo,
        List<ResultadoFactor> factores) {

    public static final String PENDIENTE_VALIDACION = "PENDIENTE_VALIDACION";
    public static final String CALCULANDO = "CALCULANDO";
    public static final String DISPONIBLE = "DISPONIBLE";

    public Resultado {
        Objects.requireNonNull(idEvaluacion, "idEvaluacion es obligatorio");
        Objects.requireNonNull(estadoResultado, "estadoResultado es obligatorio");
        factores = factores == null ? List.of() : List.copyOf(factores);
    }

    public Resultado(
            Long idResultado,
            Long idEvaluacion,
            String estadoResultado,
            Long versionFormularioId,
            Instant creadoEn,
            Long referenciaRevisionVigente,
            List<ResultadoFactor> factores) {
        this(
                idResultado,
                idEvaluacion,
                estadoResultado,
                versionFormularioId,
                creadoEn,
                referenciaRevisionVigente,
                null,
                factores);
    }

    public static Resultado nuevo(
            Long idEvaluacion,
            Long versionFormularioId,
            Instant ahora,
            List<ResultadoFactor> factores) {
        return new Resultado(
                null,
                idEvaluacion,
                PENDIENTE_VALIDACION,
                versionFormularioId,
                ahora,
                null,
                factores);
    }
}
