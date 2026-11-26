package com.universidad.tests.domain.port;

import java.util.List;

public interface PublicacionResultadoPort {

    /**
     * Cambia en una sola operación la revisión vigente y el estado del resultado a DISPONIBLE, y
     * guarda audiencia y motivo de la publicación.
     */
    void publicar(Long idResultado, Long idRevision, List<String> audiencia, String motivo);
}
