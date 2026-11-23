package com.universidad.tests.dto.response;

import java.time.Instant;

// Formato comun de error (Documento general 5.2.2)
public record ErrorResponse(
        Instant timestamp, int status, String codigo, String mensaje, String ruta) {}
