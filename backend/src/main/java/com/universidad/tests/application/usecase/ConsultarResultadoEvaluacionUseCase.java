package com.universidad.tests.application.usecase;

import com.universidad.tests.domain.exception.DominioNegocioException;
import com.universidad.tests.domain.model.EstadoResultado;
import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.model.FactorResultado;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.RevisionResultado;
import com.universidad.tests.domain.port.AutorizacionResultadoPort;
import com.universidad.tests.domain.port.ContextoActorResultado;
import com.universidad.tests.domain.repository.AuditoriaRepository;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import java.util.List;

public class ConsultarResultadoEvaluacionUseCase {

    private final EvaluacionRepository evaluacionRepository;
    private final ResultadoRepository resultadoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final AutorizacionResultadoPort autorizacionPort;

    public ConsultarResultadoEvaluacionUseCase(
        EvaluacionRepository evaluacionRepository,
        ResultadoRepository resultadoRepository,
        AuditoriaRepository auditoriaRepository,
        AutorizacionResultadoPort autorizacionPort) {
        this.evaluacionRepository = evaluacionRepository;
        this.resultadoRepository = resultadoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.autorizacionPort = autorizacionPort;
    }

    public ResultadoConsultado ejecutar(Long evaluacionId) {
        Evaluacion evaluacion =
            evaluacionRepository
                .buscarPorId(evaluacionId)
                .orElseThrow(
                    () ->
                        new DominioNegocioException(
                            404,
                            "EVALUACION_NO_ENCONTRADA",
                            "No existe la evaluacion con ID " + evaluacionId + "."));

        ContextoActorResultado actor =
            autorizacionPort.validarAccesoConsultaResultado(
                evaluacion.getIdEvaluacion(), evaluacion.getIdSujeto());

        if (!evaluacion.estaFinalizada()) {
            throw new DominioNegocioException(
                409,
                "EVALUACION_NO_FINALIZADA",
                "La evaluacion aun se encuentra en curso y no tiene resultados disponibles.");
        }

        Resultado resultado = resultadoRepository.buscarPorEvaluacionId(evaluacionId).orElse(null);

        if (resultado == null
            || !resultado.estaDisponible()
            || resultado.getReferenciaRevisionVigente() == null) {

            if (!actor.esProfesionalAutorizado()) {
                throw new DominioNegocioException(
                    403,
                    "RESULTADO_NO_PUBLICADO",
                    "Los resultados de esta evaluacion aun no han sido publicados.");
            }

            EstadoResultado estadoActual =
                resultado != null ? resultado.getEstado() : EstadoResultado.PENDIENTE_VALIDACION;
            Long resultadoId = resultado != null ? resultado.getIdResultado() : null;

            if (resultadoId != null) {
                auditoriaRepository.registrarConsultaResultado(
                    evaluacionId,
                    resultadoId,
                    actor.actorRef(),
                    "Consulta profesional de resultado sin revision publicada");
            }

            return new ResultadoConsultado(
                evaluacion.getIdEvaluacion(),
                resultadoId,
                evaluacion.getEstado().name(),
                estadoActual.name(),
                evaluacion.getIdVersionFormulario(),
                evaluacion.getModalidad(),
                evaluacion.getAlcance(),
                null,
                "SIN_REVISION_PUBLICADA");
        }

        RevisionResultado revisionVigente =
            resultadoRepository
                .buscarRevisionVigentePorId(resultado.getReferenciaRevisionVigente())
                .orElseThrow(
                    () ->
                        new DominioNegocioException(
                            500,
                            "ERROR_INTERNO",
                            "Inconsistencia en la referencia de revision vigente."));

        auditoriaRepository.registrarConsultaResultado(
            evaluacionId,
            resultado.getIdResultado(),
            actor.actorRef(),
            "Consulta autorizada de revision vigente " + revisionVigente.getCalificacionId());

        return new ResultadoConsultado(
            evaluacion.getIdEvaluacion(),
            resultado.getIdResultado(),
            evaluacion.getEstado().name(),
            EstadoResultado.DISPONIBLE.name(),
            evaluacion.getIdVersionFormulario(),
            evaluacion.getModalidad(),
            evaluacion.getAlcance(),
            revisionVigente,
            null);
    }

    public record ResultadoConsultado(
        Long evaluacionId,
        Long resultadoId,
        String estadoEvaluacion,
        String estadoResultado,
        Long versionFormularioId,
        String modalidad,
        String alcance,
        RevisionResultado revisionVigente,
        String motivo) {

        public List<FactorResultado> factores() {
            return revisionVigente != null ? revisionVigente.getFactores() : List.of();
        }
    }
}
