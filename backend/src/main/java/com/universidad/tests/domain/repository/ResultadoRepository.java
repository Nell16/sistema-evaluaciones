package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.Resultado;
import java.util.Optional;

public interface ResultadoRepository {

    Resultado guardar(Resultado resultado);

    Optional<Resultado> buscarPorEvaluacion(Long idEvaluacion);
}
