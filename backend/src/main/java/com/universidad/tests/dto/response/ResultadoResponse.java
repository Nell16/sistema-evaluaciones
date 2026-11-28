package com.universidad.tests.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** E9 — Consultar resultados autorizados (schema ResultadoResponse del contrato OpenAPI). */
public record ResultadoResponse(
        Long evaluacionId,
        Long resultadoId,
        String estadoResultado,
        Long versionFormularioId,
        @JsonInclude(JsonInclude.Include.ALWAYS) Long calificacionId,
        @JsonInclude(JsonInclude.Include.ALWAYS) Integer revision,
        @JsonInclude(JsonInclude.Include.ALWAYS) String versionReglas,
        @JsonInclude(JsonInclude.Include.ALWAYS) Long idClave,
        @JsonInclude(JsonInclude.Include.ALWAYS) Long idBaremo,
        List<FactorResponse> factores) {

    public record FactorResponse(
            String codigo,
            @JsonInclude(JsonInclude.Include.ALWAYS) Integer puntuacionDirecta,
            String estadoCalculo,
            @JsonInclude(JsonInclude.Include.ALWAYS) Integer percentil,
            @JsonInclude(JsonInclude.Include.ALWAYS) Long idRango,
            String estadoNormativo,
            @JsonInclude(JsonInclude.Include.ALWAYS) String motivo) {}
}
