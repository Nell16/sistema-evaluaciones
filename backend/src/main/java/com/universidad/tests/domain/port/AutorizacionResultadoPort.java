package com.universidad.tests.domain.port;

public interface AutorizacionResultadoPort {
    ContextoActorResultado validarAccesoConsultaResultado(
        Long idEvaluacion, Long idSujetoPropietario);
}
