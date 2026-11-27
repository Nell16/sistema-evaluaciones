package com.universidad.tests.dto.response;

import java.time.Instant;

public record RevisionPalabraResponse(
        Long idRevisionPalabra,
        Long calificacionId,
        Long respuestaId,
        String decision,
        String origenDecision,
        Instant fechaDecision) {}
