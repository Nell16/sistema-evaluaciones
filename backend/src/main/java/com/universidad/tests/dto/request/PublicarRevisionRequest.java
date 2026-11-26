package com.universidad.tests.dto.request;

import java.util.List;

public record PublicarRevisionRequest(
        Long calificacionId,
        String motivo,
        Integer revisionVigenteEsperada,
        List<String> audiencia) {}
