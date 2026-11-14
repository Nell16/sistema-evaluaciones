package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.EntradaClaveBFAJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntradaClaveBFAJpaRepository
        extends JpaRepository<EntradaClaveBFAJpaEntity, Long> {

    List<EntradaClaveBFAJpaEntity> findByClave_IdClave(Long idClave);

    Optional<EntradaClaveBFAJpaEntity> findByClave_IdClaveAndPregunta_IdPregunta(
            Long idClave, Long idPregunta);

    boolean existsByClave_IdClaveAndPregunta_IdPregunta(Long idClave, Long idPregunta);

    long countByClave_IdClave(Long idClave);
}
