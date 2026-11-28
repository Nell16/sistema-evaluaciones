package com.universidad.tests.domain.model;

public record RangoBaremoBFA(
        String idFactor,
        int percentil,
        Integer limiteInferior,
        Integer limiteSuperior,
        EstadoCeldaBaremo estadoCelda,
        String valorOriginal,
        String localizadorFuente) {}
