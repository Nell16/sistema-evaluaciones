package com.universidad.tests.exception;

import java.time.Instant;

public record ErrorApiResponse(
    Instant timestamp,
    int status,
    String codigo,
    String mensaje,
    String ruta) {}
