package com.universidad.tests.domain.repository.bfa;

import com.universidad.tests.domain.model.bfa.RevisionResultado;
import java.util.Optional;

public interface RevisionResultadoRepository {

    Optional<RevisionResultado> buscarPorId(Long idRevision);

    Optional<RevisionResultado> buscarPorSolicitud(Long idResultado, String idSolicitud);

    /** Última revisión creada del resultado (mayor número), no la vigente. */
    Optional<RevisionResultado> buscarUltima(Long idResultado);

    String resolverReglas(Long idResultado, String versionPedida);

    boolean estaValidada(Long idRevision);

    void guardarFactores(
            Long idRevision,
            java.util.List<com.universidad.tests.domain.model.ResultadoFactor> factores);

    /** Asigna el siguiente número de revisión del resultado y la guarda. */
    RevisionResultado guardarNueva(RevisionResultado revision);
}
