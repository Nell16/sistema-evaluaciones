package com.universidad.tests.config.gen;

import com.universidad.tests.application.usecase.ConsultarResultadoEvaluacionUseCase;
import com.universidad.tests.domain.port.AutorizacionResultadoPort;
import com.universidad.tests.domain.repository.AuditoriaRepository;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResultadoConfiguration {

    @Bean
    public ConsultarResultadoEvaluacionUseCase consultarResultadoEvaluacionUseCase(
        EvaluacionRepository evaluacionRepository,
        ResultadoRepository resultadoRepository,
        AuditoriaRepository auditoriaRepository,
        AutorizacionResultadoPort autorizacionResultadoPort) {
        return new ConsultarResultadoEvaluacionUseCase(
            evaluacionRepository,
            resultadoRepository,
            auditoriaRepository,
            autorizacionResultadoPort);
    }
}
