package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import com.universidad.tests.infrastructure.persistence.entity.BaremoBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.RangoBaremoBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.BaremoBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.RangoBaremoBFAJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class BaremoBFAPersistenceAdapter implements BaremoBFARepository {
    private final BaremoBFAJpaRepository baremos;
    private final RangoBaremoBFAJpaRepository rangos;

    public BaremoBFAPersistenceAdapter(
            BaremoBFAJpaRepository baremos, RangoBaremoBFAJpaRepository rangos) {
        this.baremos = baremos;
        this.rangos = rangos;
    }

    @Override
    public boolean existePorNombreYVersion(String nombre, String version) {
        return baremos.existsByNombreAndVersion(nombre, version);
    }

    @Override
    @Transactional
    public BaremoBFA guardar(BaremoBFA baremo) {
        BaremoBFAJpaEntity guardado =
                baremos.saveAndFlush(
                        new BaremoBFAJpaEntity(
                                baremo.nombre(),
                                baremo.version(),
                                baremo.anioNorma(),
                                baremo.poblacionReferencia(),
                                baremo.referenciaFuente()));
        rangos.saveAllAndFlush(
                baremo.rangos().stream()
                        .map(r -> new RangoBaremoBFAJpaEntity(guardado, r))
                        .toList());
        return new BaremoBFA(
                guardado.getIdBaremo(),
                baremo.nombre(),
                baremo.version(),
                baremo.anioNorma(),
                baremo.poblacionReferencia(),
                baremo.referenciaFuente(),
                baremo.rangos());
    }
}
