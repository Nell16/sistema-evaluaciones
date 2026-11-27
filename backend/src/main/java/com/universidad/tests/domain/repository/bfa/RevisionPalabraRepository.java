package com.universidad.tests.domain.repository.bfa;

import com.universidad.tests.domain.model.bfa.RevisionPalabra;

public interface RevisionPalabraRepository {

    /** Crea o reemplaza la decisión de la pareja (revisión, respuesta); nunca duplica. */
    RevisionPalabra guardar(RevisionPalabra decision);

    long contarPendientes(Long idRevision);
}
