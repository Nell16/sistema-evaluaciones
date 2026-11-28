package com.universidad.tests.domain.model;

import java.util.Objects;

public record ResultadoFactor(
        Long idResultadoFactor,
        String codigo,
        Integer puntuacionDirecta,
        String estadoCalculo,
        Integer percentil,
        Long idRango,
        String estadoNormativo,
        String motivo) {

    public ResultadoFactor {
        Objects.requireNonNull(codigo, "codigo es obligatorio");
        Objects.requireNonNull(estadoCalculo, "estadoCalculo es obligatorio");
        Objects.requireNonNull(estadoNormativo, "estadoNormativo es obligatorio");
    }

    public static ResultadoFactor calculado(
            String codigo, int puntuacionDirecta, Integer percentil, Long idRango) {
        String normativo = percentil != null ? "CONVERTIDO" : "SIN_BAREMO_VALIDADO";
        return new ResultadoFactor(
                null, codigo, puntuacionDirecta, "CALCULADO", percentil, idRango, normativo, null);
    }

    public static ResultadoFactor noCalculable(String codigo, String motivo) {
        return new ResultadoFactor(
                null, codigo, null, "NO_CALCULABLE", null, null, "NO_APLICA", motivo);
    }
}
