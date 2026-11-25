package com.universidad.tests.application.service;

import com.universidad.tests.application.evaluator.ResultadoEvaluacion;
import com.universidad.tests.application.evaluator.TestEvaluator;
import com.universidad.tests.application.evaluator.TestEvaluatorRegistry;
import com.universidad.tests.application.evaluator.bfa.BfaTestEvaluator;
import com.universidad.tests.domain.exception.EvaluacionNoEncontradaException;
import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.SubpruebasPendientesException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.EstadoSubprueba;
import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.model.EvaluacionSubprueba;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de aplicación para finalizar la evaluación y coordinar el cálculo psicométrico (E8 y
 * E9).
 */
@Service
public class FinalizarEvaluacionService {

    private final CierreSubpruebasService cierre;
    private final EvaluacionRepository evaluaciones;
    private final TestEvaluatorRegistry evaluadorRegistry;
    private final ResultadoRepository resultados;
    private final Clock reloj;

    @Autowired
    public FinalizarEvaluacionService(
            CierreSubpruebasService cierre,
            EvaluacionRepository evaluaciones,
            TestEvaluatorRegistry evaluadorRegistry,
            ResultadoRepository resultados) {
        this(cierre, evaluaciones, evaluadorRegistry, resultados, Clock.systemUTC());
    }

    public FinalizarEvaluacionService(
            CierreSubpruebasService cierre,
            EvaluacionRepository evaluaciones,
            TestEvaluatorRegistry evaluadorRegistry,
            ResultadoRepository resultados,
            Clock reloj) {
        this.cierre = Objects.requireNonNull(cierre, "cierre es obligatorio");
        this.evaluaciones = Objects.requireNonNull(evaluaciones, "evaluaciones es obligatorio");
        this.evaluadorRegistry =
                Objects.requireNonNull(evaluadorRegistry, "evaluadorRegistry es obligatorio");
        this.resultados = Objects.requireNonNull(resultados, "resultados es obligatorio");
        this.reloj = Objects.requireNonNull(reloj, "reloj es obligatorio");
    }

    /** E8 — Finalizar la evaluación y consolidar resultados. */
    @Transactional
    public EvaluacionResponse finalizar(Long idEvaluacion) {
        return finalizar(idEvaluacion, Map.of());
    }

    /** E8 — Finalizar la evaluación pasando puntuaciones directas conocidas o precalculadas. */
    @Transactional
    public EvaluacionResponse finalizar(
            Long idEvaluacion, Map<String, Integer> puntuacionesDirectas) {
        cierre.cerrarVencidas(idEvaluacion);

        Evaluacion evaluacion =
                evaluaciones
                        .buscarPorId(idEvaluacion)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(idEvaluacion));

        // Idempotencia: si ya está finalizada, devuelve el cierre existente sin otro cálculo
        if (evaluacion.estado() == EstadoEvaluacion.FINALIZADA) {
            Resultado existente = resultados.buscarPorEvaluacion(idEvaluacion).orElse(null);
            Long resultadoId =
                    existente != null ? existente.idResultado() : evaluacion.idEvaluacion();
            String estadoRes =
                    existente != null
                            ? existente.estadoResultado()
                            : Resultado.PENDIENTE_VALIDACION;
            return aEvaluacionResponse(evaluacion, resultadoId, estadoRes);
        }

        // Validación de subpruebas pendientes
        if (evaluacion.subpruebas().stream().anyMatch(s -> s.estado() != EstadoSubprueba.CERRADA)) {
            throw new SubpruebasPendientesException();
        }

        // Preparar entradas para el evaluador BFA con todas las subpruebas oficiales
        Map<String, Integer> puntuaciones = new HashMap<>();
        List<String> subpruebasBfa =
                List.of("VOC1", "VOC2", "CV", "S1", "S2", "R1", "R2", "N1", "N2", "FV");
        for (String cod : subpruebasBfa) {
            puntuaciones.put(cod, null);
        }
        if (puntuacionesDirectas != null) {
            puntuaciones.putAll(puntuacionesDirectas);
        }
        for (EvaluacionSubprueba sp : evaluacion.subpruebas()) {
            if (puntuacionesDirectas == null || !puntuacionesDirectas.containsKey(sp.codigo())) {
                puntuaciones.putIfAbsent(sp.codigo(), null);
            }
        }

        TestEvaluator evaluador = evaluadorRegistry.obtener("BFA").orElseGet(BfaTestEvaluator::new);

        ResultadoEvaluacion resultadoEval =
                evaluador.consolidar(
                        idEvaluacion,
                        evaluacion.idVersionFormulario() != null
                                ? evaluacion.idVersionFormulario()
                                : 1L,
                        puntuaciones);

        Instant ahora = reloj.instant().truncatedTo(ChronoUnit.MICROS);

        List<ResultadoFactor> factores =
                resultadoEval.factores().values().stream()
                        .map(
                                f -> {
                                    if (f.puntuacionDirecta() != null) {
                                        return ResultadoFactor.calculado(
                                                f.codigo(),
                                                f.puntuacionDirecta(),
                                                f.percentil(),
                                                null);
                                    } else {
                                        return ResultadoFactor.noCalculable(f.codigo(), f.motivo());
                                    }
                                })
                        .toList();

        Resultado resultadoGuardado =
                resultados.guardar(
                        Resultado.nuevo(
                                idEvaluacion,
                                evaluacion.idVersionFormulario(),
                                ahora,
                                resultadoEval.detalle(),
                                factores));

        evaluaciones.finalizarEvaluacion(idEvaluacion, ahora);
        Evaluacion evaluacionFinalizada = evaluaciones.buscarPorId(idEvaluacion).orElse(evaluacion);

        return aEvaluacionResponse(
                evaluacionFinalizada,
                resultadoGuardado.idResultado(),
                resultadoGuardado.estadoResultado());
    }

    /** E9 — Consultar resultados autorizados. */
    @Transactional(readOnly = true)
    public ResultadoResponse consultarResultados(Long idEvaluacion) {
        Evaluacion evaluacion =
                evaluaciones
                        .buscarPorId(idEvaluacion)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(idEvaluacion));

        if (evaluacion.estado() != EstadoEvaluacion.FINALIZADA) {
            throw new EvaluacionNoFinalizadaException(idEvaluacion);
        }

        Resultado resultado =
                resultados
                        .buscarPorEvaluacion(idEvaluacion)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(idEvaluacion));

        List<ResultadoResponse.FactorResponse> factores =
                resultado.factores().stream()
                        .map(
                                f ->
                                        new ResultadoResponse.FactorResponse(
                                                f.codigo(),
                                                f.puntuacionDirecta(),
                                                f.estadoCalculo(),
                                                f.percentil(),
                                                f.idRango(),
                                                f.estadoNormativo(),
                                                f.motivo()))
                        .toList();

        return new ResultadoResponse(
                idEvaluacion,
                resultado.idResultado(),
                resultado.estadoResultado(),
                evaluacion.idVersionFormulario(),
                resultado.idResultado(),
                1,
                "1.0",
                null,
                null,
                factores);
    }

    private static EvaluacionResponse aEvaluacionResponse(
            Evaluacion evaluacion, Long resultadoId, String estadoResultado) {
        List<EvaluacionResponse.SubpruebaEstado> subpruebas =
                evaluacion.subpruebas().stream()
                        .map(
                                s ->
                                        new EvaluacionResponse.SubpruebaEstado(
                                                s.codigo(),
                                                s.estado().name(),
                                                s.fechaCierre() != null
                                                        ? s.fechaCierre()
                                                        : s.fechaFinLimite()))
                        .toList();

        return new EvaluacionResponse(
                evaluacion.idEvaluacion(),
                evaluacion.idTest(),
                evaluacion.idSujeto(),
                evaluacion.idPeriodo(),
                evaluacion.idVersionFormulario(),
                evaluacion.estado().name(),
                evaluacion.fechaInicio(),
                evaluacion.fechaFin(),
                null,
                subpruebas,
                resultadoId,
                estadoResultado);
    }
}
