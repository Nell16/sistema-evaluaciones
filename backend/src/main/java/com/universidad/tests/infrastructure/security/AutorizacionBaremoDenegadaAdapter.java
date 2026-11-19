package com.universidad.tests.infrastructure.security;

import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!seguridad-integrada")
public class AutorizacionBaremoDenegadaAdapter implements AutorizacionBaremoPort {
    @Override
    public void exigirPermisoCargarBaremo() {
        throw new SecurityException("El permiso CONFIGURAR_BFA aun no esta configurado");
    }
}
