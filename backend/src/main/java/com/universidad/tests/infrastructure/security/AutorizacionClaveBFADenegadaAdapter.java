package com.universidad.tests.infrastructure.security;

import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!seguridad-integrada")
public class AutorizacionClaveBFADenegadaAdapter implements AutorizacionClaveBFAPort {

    @Override
    public void exigirPermisoCrearClave(Long idVersionFormulario) {
        denegarAcceso();
    }

    @Override
    public void exigirPermisoAgregarEntrada(Long idClave) {
        denegarAcceso();
    }

    @Override
    public void exigirPermisoConsultarClave(Long idClave) {
        denegarAcceso();
    }

    @Override
    public void exigirPermisoAprobarClave(Long idClave) {
        denegarAcceso();
    }

    @Override
    public void exigirPermisoRetirarClave(Long idClave) {
        denegarAcceso();
    }

    private void denegarAcceso() {
        throw new SecurityException(
                "Operacion no autorizada: "
                        + "el mecanismo de autenticacion "
                        + "y permisos para claves BFA aun no esta configurado");
    }
}
