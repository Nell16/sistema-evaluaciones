package com.universidad.tests.domain.model;

import java.util.List;

public record BaremoBFA(
        Long idBaremo,
        String nombre,
        String version,
        int anioNorma,
        String poblacionReferencia,
        String referenciaFuente,
        List<RangoBaremoBFA> rangos) {
    public BaremoBFA {
        rangos = List.copyOf(rangos);
    }
}
