package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.VersionClaveBFAJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VersionClaveBFAJpaRepository
        extends JpaRepository<VersionClaveBFAJpaEntity, Long> {

    List<VersionClaveBFAJpaEntity> findByVersionFormulario_IdVersionFormularioOrderByVersionAsc(
            Long idVersionFormulario);

    Optional<VersionClaveBFAJpaEntity> findByVersionFormulario_IdVersionFormularioAndVersion(
            Long idVersionFormulario, String version);

    boolean existsByVersionFormulario_IdVersionFormularioAndVersion(
            Long idVersionFormulario, String version);
}
