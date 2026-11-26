package com.universidad.tests.application.service;

import com.universidad.tests.application.evaluator.bfa.BfaTestEvaluator;
import com.universidad.tests.domain.exception.EvaluacionNoEncontradaException;
import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.port.AlcanceSujetoPort;
import com.universidad.tests.domain.port.FinalizacionEvaluacionPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
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
    private final CalculoPuntuacionesBfaService calculoPuntuaciones;

    public FinalizacionEvaluacionService(
            AutorizacionService auth,
            AlcanceSujetoPort alcance,
            EvaluacionRepository evaluaciones,
            PreparacionFinalizacionService preparacion,
            FinalizacionEvaluacionPort cierre,
            RegistroAuditoria auditoria,
            ResultadoRepository resultadoRepository,
            CalculoPuntuacionesBfaService calculoPuntuaciones) {
        this.auth = auth;
        this.alcance = alcance;
        this.evaluaciones = evaluaciones;
        this.preparacion = preparacion;
        this.cierre = cierre;
        this.auditoria = auditoria;
        this.resultadoRepository = resultadoRepository;
        this.calculoPuntuaciones = calculoPuntuaciones;
    }

    @Transactional
    public EvaluacionResponse finalizar(Long id) {
        return finalizar(id, null);
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
            var puntuaciones =
                    puntuacionesDirectas != null
                            ? java.util.Optional.of(
                                    new CalculoPuntuacionesBfaService.Calculo(
                                            puntuacionesDirectas, null))
                            : calculoPuntuaciones.calcular(
                                    original.idEvaluacion(), original.idVersionFormulario());
            puntuaciones.ifPresent(
                    insumos ->
                            consolidarYPersistirFactoresBfa(
                                    resultado.resultadoId(),
                                    original.idEvaluacion(),
                                    original.idVersionFormulario(),
                                    insumos.puntuaciones(),
                                    ahora,
                                    insumos.idClave()));
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

    private void consolidarYPersistirFactoresBfa(
            Long resultadoId,
            Long evaluacionId,
            Long versionFormularioId,
            Map<String, Integer> puntuacionesDirectas,
            Instant ahora,
            Long idClaveCalculo) {
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
                        idClaveCalculo,
                        factores);

        resultadoRepository.guardar(resultadoObj);
    }
}
