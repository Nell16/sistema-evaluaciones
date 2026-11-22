package com.universidad.tests.dto.response;

public record FactorResultadoResponse(
    String codigo,
    Integer puntuacionDirecta,
    String estadoCalculo,
    Integer percentil,
    Long idRango,
    String estadoNormativo,
    String motivo) {}
