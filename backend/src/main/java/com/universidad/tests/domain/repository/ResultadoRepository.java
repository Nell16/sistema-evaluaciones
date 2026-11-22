package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.RevisionResultado;
import java.util.Optional;

public interface ResultadoRepository {
    Optional<Resultado> buscarPorEvaluacionId(Long idEvaluacion);

    Optional<RevisionResultado> buscarRevisionVigentePorId(Long idCalificacion);
}
