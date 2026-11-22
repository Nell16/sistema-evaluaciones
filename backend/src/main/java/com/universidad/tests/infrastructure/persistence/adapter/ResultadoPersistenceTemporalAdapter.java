package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.RevisionResultado;
import com.universidad.tests.domain.repository.ResultadoRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ResultadoPersistenceTemporalAdapter implements ResultadoRepository {

    @Override
    public Optional<Resultado> buscarPorEvaluacionId(Long idEvaluacion) {
        return Optional.empty();
    }

    @Override
    public Optional<RevisionResultado> buscarRevisionVigentePorId(Long idCalificacion) {
        return Optional.empty();
    }
}
