package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.PeriodoJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodoJpaRepository extends JpaRepository<PeriodoJpaEntity, Long> {

    Optional<PeriodoJpaEntity> findByCodigo(String codigo);

    List<PeriodoJpaEntity> findAllByOrderByCodigoDesc();

    List<PeriodoJpaEntity> findByActivoTrueOrderByCodigoDesc();
}
