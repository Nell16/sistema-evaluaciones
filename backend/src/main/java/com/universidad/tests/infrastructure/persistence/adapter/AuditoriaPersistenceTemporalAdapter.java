package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.repository.AuditoriaRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditoriaPersistenceTemporalAdapter implements AuditoriaRepository {

    @Override
    public void registrarConsultaResultado(
        Long idEvaluacion, Long idResultado, String actorRef, String detalle) {
        // Pendiente de integracion con tabla evento_auditoria de GEN-04-BE
    }
}
