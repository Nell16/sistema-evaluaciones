package com.universidad.tests.application.service;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ConversionPercentilBFAService {

    public ResultadoConversion convertir(
            String codigoFactor, Integer puntuacionDirecta, List<RangoCandidato> rangos) {
        Objects.requireNonNull(codigoFactor, "El código del factor es obligatorio");
        Objects.requireNonNull(rangos, "Los rangos son obligatorios");

        if (puntuacionDirecta == null) {
            return new ResultadoConversion(
                    null,
                    null,
                    EstadoNormativo.NO_APLICA,
                    "El factor no tiene puntuación directa.");
        }

        RangoCandidato coincidencia = null;
        for (RangoCandidato rango : rangos) {
            if (!rango.codigoFactor().equals(codigoFactor) || !rango.contiene(puntuacionDirecta)) {
                continue;
            }

            if (coincidencia != null) {
                return new ResultadoConversion(
                        null,
                        null,
                        EstadoNormativo.SIN_COINCIDENCIA,
                        "La puntuación coincide con más de un rango del factor.");
            }

            coincidencia = rango;
        }

        if (coincidencia == null) {
            return new ResultadoConversion(
                    null,
                    null,
                    EstadoNormativo.SIN_COINCIDENCIA,
                    "Ningún rango del factor contiene la puntuación.");
        }

        return new ResultadoConversion(
                coincidencia.percentil(), coincidencia.idRango(), EstadoNormativo.CONVERTIDO, null);
    }

    public enum EstadoNormativo {
        CONVERTIDO,
        SIN_COINCIDENCIA,
        SIN_BAREMO_VALIDADO,
        NO_APLICA
    }

    public record ResultadoConversion(
            Integer percentil, Long idRango, EstadoNormativo estadoNormativo, String motivo) {}

    public record RangoCandidato(
            long idRango,
            String codigoFactor,
            int percentil,
            Integer limiteInferior,
            Integer limiteSuperior) {

        public RangoCandidato {
            Objects.requireNonNull(codigoFactor, "El código del factor es obligatorio");

            if (percentil < 1 || percentil > 99) {
                throw new IllegalArgumentException("El percentil debe estar entre 1 y 99");
            }

            if ((limiteInferior == null) != (limiteSuperior == null)) {
                throw new IllegalArgumentException("Se requieren ambos límites o ninguno");
            }

            if (limiteInferior != null && limiteInferior > limiteSuperior) {
                throw new IllegalArgumentException("El límite inferior supera al superior");
            }
        }

        private boolean contiene(int puntuacion) {
            // Una celda SIN_DATO tiene ambos límites nulos.
            return limiteInferior != null
                    && puntuacion >= limiteInferior
                    && puntuacion <= limiteSuperior;
        }
    }
}
