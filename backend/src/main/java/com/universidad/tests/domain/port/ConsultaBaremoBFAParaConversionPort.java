package com.universidad.tests.domain.port;

import java.util.List;
import java.util.Objects;

public interface ConsultaBaremoBFAParaConversionPort {

    /**
     * Devuelve una entrada por cada baremo aprobado con compatibilidad acreditada para la versión
     * del formulario y la versión de las reglas. Cada entrada incluye los rangos de todos sus
     * factores.
     */
    List<BaremoCandidato> buscarAprobadosCompatibles(
            long versionFormularioId, String versionReglas);

    record BaremoCandidato(long idBaremo, List<RangoCandidato> rangos) {

        public BaremoCandidato {
            rangos = List.copyOf(rangos);
        }
    }

    record RangoCandidato(
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

        public boolean contiene(int puntuacion) {
            return limiteInferior != null
                    && puntuacion >= limiteInferior
                    && puntuacion <= limiteSuperior;
        }
    }
}
