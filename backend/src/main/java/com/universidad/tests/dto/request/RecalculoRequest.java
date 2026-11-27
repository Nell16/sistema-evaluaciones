package com.universidad.tests.dto.request;

public record RecalculoRequest(
        String motivo, String versionReglas, Long idClave, Long idBaremo, String idSolicitud) {}
