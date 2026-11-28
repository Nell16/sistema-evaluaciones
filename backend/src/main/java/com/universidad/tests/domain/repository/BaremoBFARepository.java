package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.BaremoBFA;

public interface BaremoBFARepository {
    boolean existePorId(Long idBaremo);

    boolean existePorNombreYVersion(String nombre, String version);

    BaremoBFA guardar(BaremoBFA baremo);
}
