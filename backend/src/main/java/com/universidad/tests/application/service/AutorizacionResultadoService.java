package com.universidad.tests.application.service;

import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.resultados.EvaluacionConsultada;
import com.universidad.tests.domain.port.AlcanceSujetoPort;
import com.universidad.tests.domain.port.resultados.AudienciaResultadoPort;
import com.universidad.tests.domain.port.resultados.AutorizacionResultadoPort;
import org.springframework.stereotype.Service;

/** E9: sesion real, alcance explicito y audiencia de la revision publicada. */
@Service
public class AutorizacionResultadoService implements AutorizacionResultadoPort {
    private final AutorizacionService auth;
    private final AlcanceSujetoPort alcance;
    private final AudienciaResultadoPort audiencia;

    public AutorizacionResultadoService(
            AutorizacionService auth, AlcanceSujetoPort alcance, AudienciaResultadoPort audiencia) {
        this.auth = auth;
        this.alcance = alcance;
        this.audiencia = audiencia;
    }

    public void exigirPermisoConsultarResultados() {
        auth.exigirSesion();
    }

    public void exigirAlcanceSobre(EvaluacionConsultada evaluacion) {
        var sesion = auth.exigirSesion();
        if (!alcance.estaAsignado(sesion.idCuenta(), evaluacion.sujetoId())
                || !audiencia.permite(evaluacion.evaluacionId(), sesion.rolActivo())) {
            throw new SinPermisoException();
        }
    }
}
