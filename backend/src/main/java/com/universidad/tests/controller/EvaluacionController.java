package com.universidad.tests.controller;

import com.universidad.tests.application.service.ConsultaEvaluacionService;
import com.universidad.tests.application.service.FinalizarEvaluacionService;
import com.universidad.tests.application.service.MotorSubpruebasService;
import com.universidad.tests.dto.request.RegistrarRespuestasRequest;
import com.universidad.tests.dto.response.AvanceSubpruebaResponse;
import com.universidad.tests.dto.response.EvaluacionResponse;
import com.universidad.tests.dto.response.PreguntasSubpruebaResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import com.universidad.tests.dto.response.SubpruebaActivaResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final ConsultaEvaluacionService consultaService;

    private final MotorSubpruebasService motor;

    private final FinalizarEvaluacionService finalizarService;

    public EvaluacionController(
            ConsultaEvaluacionService consultaService,
            MotorSubpruebasService motor,
            FinalizarEvaluacionService finalizarService) {
        this.motor = motor;
        this.consultaService = consultaService;
        this.finalizarService = finalizarService;
    }

    // E4
    @GetMapping("/{evaluacionId}")
    public EvaluacionResponse consultarEvaluacion(@PathVariable Long evaluacionId) {
        return consultaService.consultar(evaluacionId);
    }

    // E8
    @PostMapping("/{evaluacionId}/finalizar")
    public EvaluacionResponse finalizarEvaluacion(@PathVariable Long evaluacionId) {
        return finalizarService.finalizar(evaluacionId);
    }

    // E9
    @GetMapping("/{evaluacionId}/resultados")
    public ResultadoResponse consultarResultados(@PathVariable Long evaluacionId) {
        return finalizarService.consultarResultados(evaluacionId);
    }

    @PostMapping("/{evaluacionId}/subpruebas/{codigo}/iniciar")
    public SubpruebaActivaResponse iniciar(
            @PathVariable Long evaluacionId, @PathVariable String codigo) {
        return motor.iniciar(evaluacionId, codigo);
    }

    @GetMapping("/{evaluacionId}/subpruebas/{codigo}/preguntas")
    public PreguntasSubpruebaResponse preguntas(
            @PathVariable Long evaluacionId, @PathVariable String codigo) {
        return motor.consultarPreguntas(evaluacionId, codigo);
    }

    @PutMapping("/{evaluacionId}/subpruebas/{codigo}/respuestas")
    public AvanceSubpruebaResponse respuestas(
            @PathVariable Long evaluacionId,
            @PathVariable String codigo,
            @RequestBody RegistrarRespuestasRequest solicitud) {
        return motor.registrarRespuestas(evaluacionId, codigo, solicitud);
    }
}
