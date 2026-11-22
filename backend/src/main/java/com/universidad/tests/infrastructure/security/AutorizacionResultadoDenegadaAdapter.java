package com.universidad.tests.infrastructure.security;

import com.universidad.tests.domain.exception.DominioNegocioException;
import com.universidad.tests.domain.port.AutorizacionResultadoPort;
import com.universidad.tests.domain.port.ContextoActorResultado;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!seguridad-integrada")
public class AutorizacionResultadoDenegadaAdapter implements AutorizacionResultadoPort {

    @Override
    public ContextoActorResultado validarAccesoConsultaResultado(
        Long idEvaluacion, Long idSujetoPropietario) {
        throw new DominioNegocioException(
            403,
            "SIN_PERMISO",
            "Operacion no autorizada: el mecanismo de autenticacion y permisos aun no esta configurado");
    }
}
