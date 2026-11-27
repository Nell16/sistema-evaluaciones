package com.universidad.tests.application.service;

import com.universidad.tests.domain.port.resultados.AuditoriaResultadoPort;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaResultadoService implements AuditoriaResultadoPort {
    private final AutorizacionService auth;
    private final RegistroAuditoria registro;

    public AuditoriaResultadoService(AutorizacionService auth, RegistroAuditoria registro) {
        this.auth = auth;
        this.registro = registro;
    }

    @Override
    public void registrarConsulta(Long evaluacion, Long resultado, Long revision) {
        long cuenta = auth.exigirSesion().idCuenta();
        if (revision == null) {
            registro.registrar(cuenta, "CONSULTAR_RESULTADO", "EVALUACION", evaluacion);
        } else {
            registro.registrar(
                    cuenta, "CONSULTAR_RESULTADO", "REVISION_RESULTADO", revision, evaluacion);
        }
    }
}
