package com.universidad.tests.domain.model.bfa;

import java.time.Instant;
import java.util.Objects;

/** Decisión sobre una palabra de FV dentro de una revisión (única por revisión y respuesta). */
public record RevisionPalabra(
        Long idRevisionPalabra,
        Long idRevision,
        Long idRespuesta,
        DecisionPalabra decision,
        String motivo,
        Long idCuenta,
        Instant fechaDecision) {

    public static final String ORIGEN_DECISION = "REVISION_AUTORIZADA";

    public RevisionPalabra {
        Objects.requireNonNull(idRevision, "idRevision es obligatorio");
        Objects.requireNonNull(idRespuesta, "idRespuesta es obligatoria");
        Objects.requireNonNull(decision, "decision es obligatoria");
    }
}
