package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class EvaluacionPersistenceTemporalAdapter implements EvaluacionRepository {

    @Override
    public Optional<Evaluacion> buscarPorId(Long idEvaluacion) {
        return Optional.empty();
    }
}
