package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.ResultadoFactorJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoFactorJpaRepository
        extends JpaRepository<ResultadoFactorJpaEntity, Long> {

    List<ResultadoFactorJpaEntity> findByIdResultadoOrderByCodigoAsc(Long idResultado);
}
