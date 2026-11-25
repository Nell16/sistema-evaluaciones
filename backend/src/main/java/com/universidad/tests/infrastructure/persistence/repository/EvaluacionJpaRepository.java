package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.EvaluacionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionJpaRepository extends JpaRepository<EvaluacionJpaEntity, Long> {}
