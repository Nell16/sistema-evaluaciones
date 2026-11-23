package com.universidad.tests.dto.response;

import java.util.List;

public record TestDetalleResponse(
        Long id,
        String codigo,
        String nombre,
        Long versionFormularioId,
        List<ConfiguracionSubpruebaResponse> subpruebas) {}
