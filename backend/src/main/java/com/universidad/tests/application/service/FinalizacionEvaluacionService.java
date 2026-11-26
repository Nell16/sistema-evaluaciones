package com.universidad.tests.application.service;

import com.universidad.tests.application.evaluator.bfa.BfaTestEvaluator;
import com.universidad.tests.domain.exception.EvaluacionNoEncontradaException;
import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.port.AlcanceSujetoPort;
import com.universidad.tests.domain.port.FinalizacionEvaluacionPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cierre transaccional/idempotente de la evaluación (E8) y consulta autorizada de resultados (E9)
 * con cálculo psicométrico BFA.
 */
@Service
public class FinalizacionEvaluacionService {

    private final AutorizacionService auth;
    private final AlcanceSujetoPort alcance;
    private final EvaluacionRepository evaluaciones;
    private final PreparacionFinalizacionService preparacion;
    private final FinalizacionEvaluacionPort cierre;
    private final RegistroAuditoria auditoria;
    private final ResultadoRepository resultadoRepository;

    public FinalizacionEvaluacionService(
            AutorizacionService auth,
            AlcanceSujetoPort alcance,
            EvaluacionRepository evaluaciones,
            PreparacionFinalizacionService preparacion,
            FinalizacionEvaluacionPort cierre,
            RegistroAuditoria auditoria,
            ResultadoRepository resultadoRepository) {
        this.auth = auth;
        this.alcance = alcance;
        this.evaluaciones = evaluaciones;
        this.preparacion = preparacion;
        this.cierre = cierre;
        this.auditoria = auditoria;
        this.resultadoRepository = resultadoRepository;
    }

    @Transactional
    public EvaluacionResponse finalizar(Long id) {
        return finalizar(id, Map.of());
    }

    @Transactional
    public EvaluacionResponse finalizar(Long id, Map<String, Integer> puntuacionesDirectas) {
        var sesion = auth.exigirPermiso("EVALUACION_FINALIZAR");
        var original =
                evaluaciones
                        .buscarPorIdParaActualizar(id)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(id));
        if (!"SUJETO".equals(sesion.rolActivo())
                || !alcance.estaAsignado(sesion.idCuenta(), original.idSujeto())) {
            throw new SinPermisoException();
        }

        preparacion.preparar(id);

        Instant ahora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        var resultado = cierre.finalizar(id, ahora);

        if (original.estado() != EstadoEvaluacion.FINALIZADA) {
            auditoria.registrar(sesion.idCuenta(), "FINALIZAR_EVALUACION", "EVALUACION", id);

            // BFA-BE-38: Consolidar y persistir factores psicométricos BFA
            consolidarYPersistirFactoresBfa(
                    resultado.resultadoId(),
                    original.idEvaluacion(),
                    original.idVersionFormulario(),
                    puntuacionesDirectas,
                    ahora);
        }

        var e =
                evaluaciones
                        .buscarPorId(id)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(id));
        return new EvaluacionResponse(
                e.idEvaluacion(),
                e.idTest(),
                e.idSujeto(),
                e.idPeriodo(),
                e.idVersionFormulario(),
                e.estado().name(),
                e.fechaInicio(),
                e.fechaFin(),
                null,
                e.subpruebas().stream()
                        .map(
                                s ->
                                        new EvaluacionResponse.SubpruebaEstado(
                                                s.codigo(), s.estado().name(), s.fechaCierre()))
                        .toList(),
                resultado.resultadoId(),
                resultado.estadoResultado());
    }

    @Transactional(readOnly = true)
    public ResultadoResponse consultarResultados(Long id) {
        var sesion = auth.exigirSesion();
        var evaluacion =
                evaluaciones
                        .buscarPorId(id)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(id));

        if (!alcance.estaAsignado(sesion.idCuenta(), evaluacion.idSujeto())) {
            throw new SinPermisoException();
        }

        if (evaluacion.estado() != EstadoEvaluacion.FINALIZADA) {
            throw new EvaluacionNoFinalizadaException(id);
        }

        var resultado =
                resultadoRepository
                        .buscarPorEvaluacion(id)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(id));

        // OpenAPI: Sin revisión publicada, calificacionId y revision son nulos, factores está vacío
        if (resultado.referenciaRevisionVigente() == null) {
            return new ResultadoResponse(
                    evaluacion.idEvaluacion(),
                    resultado.idResultado(),
                    resultado.estadoResultado(),
                    evaluacion.idVersionFormulario(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of());
        }

        // Con revisión vigente publicada
        return new ResultadoResponse(
                evaluacion.idEvaluacion(),
                resultado.idResultado(),
                resultado.estadoResultado(),
                evaluacion.idVersionFormulario(),
                resultado.referenciaRevisionVigente(),
                1,
                null,
                null,
                null,
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
                        .toList());
    }

    private void consolidarYPersistirFactoresBfa(
            Long resultadoId,
            Long evaluacionId,
            Long versionFormularioId,
            Map<String, Integer> puntuacionesDirectas,
            Instant ahora) {
        List<String> subpruebasBfa =
                List.of("VOC1", "VOC2", "CV", "S1", "S2", "R1", "R2", "N1", "N2", "FV");
        Map<String, Integer> insumos = new LinkedHashMap<>();
        for (String sp : subpruebasBfa) {
            insumos.put(sp, puntuacionesDirectas != null ? puntuacionesDirectas.get(sp) : null);
        }

        BfaTestEvaluator evaluador = new BfaTestEvaluator();
        var consolidacion =
                evaluador.consolidar(
                        evaluacionId,
                        versionFormularioId != null ? versionFormularioId : 0L,
                        insumos);

        List<ResultadoFactor> factores = new ArrayList<>();
        if (consolidacion.factores() != null) {
            for (var f : consolidacion.factores().values()) {
                if (f.estadoCalculo()
                        == com.universidad.tests.application.evaluator.FactorCalculado.EstadoCalculo
                                .CALCULADO) {
                    factores.add(
                            ResultadoFactor.calculado(
                                    f.codigo(), f.puntuacionDirecta(), f.percentil(), null));
                } else {
                    factores.add(
                            ResultadoFactor.noCalculable(
                                    f.codigo(),
                                    f.motivo() != null ? f.motivo() : "FALTAN_INSUMOS"));
                }
            }
        }

        Resultado resultadoObj =
                new Resultado(
                        resultadoId,
                        evaluacionId,
                        Resultado.PENDIENTE_VALIDACION,
                        versionFormularioId,
                        ahora,
                        null,
                        factores);

        resultadoRepository.guardar(resultadoObj);
    }
}
