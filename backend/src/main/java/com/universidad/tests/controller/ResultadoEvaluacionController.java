package com.universidad.tests.controller;

import com.universidad.tests.application.usecase.ConsultarResultadoEvaluacionUseCase;
import com.universidad.tests.dto.response.ResultadoResponse;
import com.universidad.tests.mapper.ResultadoResponseMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class ResultadoEvaluacionController {

    private final ConsultarResultadoEvaluacionUseCase consultarResultadoUseCase;
    private final ResultadoResponseMapper mapper;

    public ResultadoEvaluacionController(
        ConsultarResultadoEvaluacionUseCase consultarResultadoUseCase,
        ResultadoResponseMapper mapper) {
        this.consultarResultadoUseCase = consultarResultadoUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/{evaluacionId}/resultados")
    public ResponseEntity<ResultadoResponse> consultarResultados(@PathVariable Long evaluacionId) {
        var resultado = consultarResultadoUseCase.ejecutar(evaluacionId);
        return ResponseEntity.ok(mapper.toResponse(resultado));
    }
}
