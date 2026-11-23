package com.universidad.tests.infrastructure.persistence.repository;

import com.universidad.tests.infrastructure.persistence.entity.OpcionRespuestaJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpcionRespuestaJpaRepository
        extends JpaRepository<OpcionRespuestaJpaEntity, Long> {

    List<OpcionRespuestaJpaEntity> findByPregunta_IdPreguntaOrderByOrdenPresentacionAsc(
            Long idPregunta);

    Optional<OpcionRespuestaJpaEntity> findByPregunta_IdPreguntaAndCodigo(
            Long idPregunta, String codigo);

    boolean existsByPregunta_IdPreguntaAndCodigo(Long idPregunta, String codigo);

    @Query(
            "select count(o) from OpcionRespuestaJpaEntity o"
                    + " where o.pregunta.configuracionSubprueba.idConfiguracionSubprueba = :id"
                    + " group by o.pregunta.idPregunta")
    List<Long> contarPorPreguntaDeConfiguracion(@Param("id") Long idConfiguracionSubprueba);
}
