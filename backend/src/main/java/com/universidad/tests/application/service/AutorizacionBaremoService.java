package com.universidad.tests.application.service;

import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import org.springframework.stereotype.Service;

@Service
public class AutorizacionBaremoService implements AutorizacionBaremoPort {

    private final AutorizacionService autorizacion;

    public AutorizacionBaremoService(AutorizacionService autorizacion) {
        this.autorizacion = autorizacion;
    }

    @Override
    public void exigirPermisoCargarBaremo() {
        autorizacion.exigirPermiso("CONFIGURAR_BFA");
    }
}
