package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.BaremoBFAJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BaremoBFAJpaRepository extends JpaRepository<BaremoBFAJpaEntity, Long> {
    boolean existsByNombreAndVersion(String nombre, String version);
}
