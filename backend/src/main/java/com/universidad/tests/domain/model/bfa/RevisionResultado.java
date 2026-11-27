package com.universidad.tests.domain.model.bfa;

import java.time.Instant;
import java.util.Objects;

/**
 * Revisión de un resultado (la «calificación» del contrato). Una revisión nueva no cambia la
 * vigente; solo E12 cambia cuál es la vigente.
 */
public record RevisionResultado(
        Long idRevision,
        Long idResultado,
        int numeroRevision,
        String motivo,
        String versionReglas,
        Long idClave,
        Long idBaremo,
        String idSolicitud,
        Long idCuentaAutor,
        Instant creadaEn) {

    public RevisionResultado {
        Objects.requireNonNull(idResultado, "idResultado es obligatorio");
        Objects.requireNonNull(motivo, "motivo es obligatorio");
        if (numeroRevision < 1) {
            throw new IllegalArgumentException("numeroRevision debe ser >= 1");
        }
    }
}
