package com.universidad.tests.dto.response;

public record ConfiguracionSubpruebaResponse(
        String codigo,
        String nombre,
        int orden,
        String factor,
        Integer tiempoLimiteSegundos,
        String tipoRespuesta,
        Integer opciones,
        int numeroPreguntas,
        Integer maximoEntradas,
        boolean tienePractica) {}
