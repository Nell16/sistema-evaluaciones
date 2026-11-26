package com.universidad.tests.controller;

import com.universidad.tests.application.usecase.CrearPeriodoUseCase;
import com.universidad.tests.application.usecase.ListarPeriodosUseCase;
import com.universidad.tests.application.usecase.vista.PeriodoVista;
import com.universidad.tests.dto.request.PeriodoRequest;
import com.universidad.tests.dto.response.PeriodoResponse;
import com.universidad.tests.mapper.EvaluacionMapper;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** E20 (crear) y E21 (listar) Períodos (CU-GEN-20). */
@RestController
@RequestMapping("/api/v1/periodos")
public class PeriodoController {

    private final CrearPeriodoUseCase crearPeriodo;
    private final ListarPeriodosUseCase listarPeriodos;
    private final EvaluacionMapper mapper;

    public PeriodoController(
            CrearPeriodoUseCase crearPeriodo,
            ListarPeriodosUseCase listarPeriodos,
            EvaluacionMapper mapper) {
        this.crearPeriodo = crearPeriodo;
        this.listarPeriodos = listarPeriodos;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<PeriodoResponse> crear(@Valid @RequestBody PeriodoRequest request) {
        PeriodoVista creado =
                crearPeriodo.crear(
                        request.codigo(), request.denominacion(), request.inicio(), request.fin());
        PeriodoResponse response = mapper.aResponse(creado);
        return ResponseEntity.created(URI.create("/api/v1/periodos/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<PeriodoResponse> listar(
            @RequestParam(name = "disponible", defaultValue = "false") boolean disponible) {
        return listarPeriodos.listar(disponible).stream().map(mapper::aResponse).toList();
    }
}
