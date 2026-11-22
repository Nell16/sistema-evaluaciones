package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.Evaluacion;
import java.util.Optional;

public interface EvaluacionRepository {
    Optional<Evaluacion> buscarPorId(Long idEvaluacion);
}
