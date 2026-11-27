package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.ResultadoJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoJpaRepository extends JpaRepository<ResultadoJpaEntity, Long> {

    Optional<ResultadoJpaEntity> findByIdEvaluacion(Long idEvaluacion);
}
