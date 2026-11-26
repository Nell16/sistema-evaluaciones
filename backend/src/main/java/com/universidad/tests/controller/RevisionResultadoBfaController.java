package com.universidad.tests.controller;

import com.universidad.tests.application.usecase.bfa.RevisionResultadoBfaService;
import com.universidad.tests.dto.request.PublicarRevisionRequest;
import com.universidad.tests.dto.request.RecalculoRequest;
import com.universidad.tests.dto.request.RevisionPalabraRequest;
import com.universidad.tests.dto.response.PublicacionResponse;
import com.universidad.tests.dto.response.RecalculoResponse;
import com.universidad.tests.dto.response.RevisionPalabraResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** BFA-BE-31: E10, E11 y E12 del contrato. */
@RestController
@RequestMapping("/api/v1/evaluaciones/{evaluacionId}")
public class RevisionResultadoBfaController {

    private final RevisionResultadoBfaService servicio;

    public RevisionResultadoBfaController(RevisionResultadoBfaService servicio) {
        this.servicio = servicio;
    }

    // E10
    @PutMapping("/calificaciones/{calificacionId}/palabras/{respuestaId}/revision")
    public RevisionPalabraResponse revisarPalabra(
            @PathVariable Long evaluacionId,
            @PathVariable Long calificacionId,
            @PathVariable Long respuestaId,
            @RequestBody RevisionPalabraRequest solicitud) {
        return servicio.revisarPalabra(evaluacionId, calificacionId, respuestaId, solicitud);
    }

    // E11
    @PostMapping("/recalculos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecalculoResponse recalcular(
            @PathVariable Long evaluacionId, @RequestBody RecalculoRequest solicitud) {
        return servicio.recalcular(evaluacionId, solicitud);
    }

    // E12
    @PutMapping("/resultados/vigente")
    public PublicacionResponse publicar(
            @PathVariable Long evaluacionId, @RequestBody PublicarRevisionRequest solicitud) {
        return servicio.publicar(evaluacionId, solicitud);
    }
}
