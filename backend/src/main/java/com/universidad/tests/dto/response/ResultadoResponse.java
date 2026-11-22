package com.universidad.tests.dto.response;

import java.util.List;

public record ResultadoResponse(
    Long evaluacionId,
    Long resultadoId,
    String estadoEvaluacion,
    String estadoResultado,
    Long versionFormularioId,
    String modalidad,
    String alcance,
    Long calificacionId,
    Integer revision,
    Long idReglas,
    String versionReglas,
    Long idClave,
    String versionClave,
    Long idBaremo,
    String versionBaremo,
    String motivo,
    List<FactorResultadoResponse> factores) {}
