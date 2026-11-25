package com.universidad.tests.domain.model;

import java.time.Duration;
import java.time.Instant;

// Subprueba dentro de una evaluacion: PENDIENTE -> ACTIVA -> CERRADA
public record EvaluacionSubprueba(
        Long idEvaluacionSubprueba,
        Long idEvaluacion,
        Long idConfiguracionSubprueba,
        String codigo,
        String nombre,
        int orden,
        int duracionSegundos,
        EstadoSubprueba estado,
        String instrucciones,
        boolean permitirRetroceso,
        Instant fechaInicio,
        Instant fechaFinLimite,
        Instant fechaCierre,
        String letraInicial,
        int maximoEntradas) {

    // El tiempo restante lo calcula siempre el servidor; solo existe mientras esta ACTIVA
    public Long segundosRestantes(Instant ahora) {
        if (estado != EstadoSubprueba.ACTIVA || fechaFinLimite == null) {
            return null;
        }
        return Math.max(0, Duration.between(ahora, fechaFinLimite).getSeconds());
    }
}
