package com.universidad.tests.controller;

import com.universidad.tests.application.service.CargarBaremoBFAService;
import com.universidad.tests.application.service.CargarBaremoBFAService.DatosBaremo;
import com.universidad.tests.application.service.CargarBaremoBFAService.DatosRango;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaremoBFAController {
    private final CargarBaremoBFAService servicio;

    public BaremoBFAController(CargarBaremoBFAService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/api/v1/baremos")
    public ResponseEntity<BaremoResponse> cargar(@Valid @RequestBody BaremoRequest solicitud) {
        List<DatosRango> rangos =
                solicitud.rangos().stream()
                        .map(
                                r ->
                                        new DatosRango(
                                                r.idFactor(),
                                                r.percentil(),
                                                r.limiteInferior(),
                                                r.limiteSuperior(),
                                                r.estadoCelda(),
                                                r.valorOriginal(),
                                                r.localizadorFuente()))
                        .toList();
        Long id =
                servicio.cargarDatos(
                        new DatosBaremo(
                                solicitud.nombre(),
                                solicitud.version(),
                                solicitud.anioNorma(),
                                solicitud.poblacionReferencia(),
                                solicitud.referenciaFuente(),
                                rangos));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/baremos/" + id))
                .body(new BaremoResponse(id));
    }

    public record BaremoRequest(
            @NotBlank String nombre,
            @NotBlank String version,
            @NotNull @Positive Integer anioNorma,
            String poblacionReferencia,
            String referenciaFuente,
            @NotEmpty List<@NotNull @Valid RangoRequest> rangos) {}

    public record RangoRequest(
            @NotBlank String idFactor,
            @NotNull @Min(1) @Max(99) Integer percentil,
            Integer limiteInferior,
            Integer limiteSuperior,
            @NotBlank String estadoCelda,
            String valorOriginal,
            String localizadorFuente) {}

    public record BaremoResponse(Long idBaremo) {}
}
