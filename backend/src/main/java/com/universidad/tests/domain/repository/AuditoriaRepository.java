package com.universidad.tests.domain.repository;

public interface AuditoriaRepository {
    void registrarConsultaResultado(
        Long idEvaluacion, Long idResultado, String actorRef, String detalle);
}
