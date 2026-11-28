package com.universidad.tests.application.port;

import com.universidad.tests.application.service.ConversionPercentilBFAService.RangoCandidato;
import java.util.List;

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
}
