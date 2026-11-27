package com.universidad.tests.domain.port;

import com.universidad.tests.domain.model.ResultadoFactor;
import java.util.List;

public interface PreparacionRevisionBfaPort {
    String IMPLEMENTACION = "bfa-verbal-v1";

    Calculo preparar(
            Long evaluacion, Long version, String reglas, Long clavePedida, Long baremoPedido);

    record Calculo(Long idClave, Long idBaremo, List<ResultadoFactor> factores) {
        public Calculo {
            factores = List.copyOf(factores);
        }
    }
}
