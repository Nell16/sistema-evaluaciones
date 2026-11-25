package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.Evaluacion;
import java.util.Optional;

public interface EvaluacionRepository {

    // Devuelve la evaluacion con sus subpruebas ordenadas por orden de aplicacion
    Optional<Evaluacion> buscarPorId(Long idEvaluacion);

    // Inserta (id nulo) la evaluacion y sus subpruebas
    Evaluacion guardar(Evaluacion evaluacion);
}
