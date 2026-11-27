package com.universidad.tests.application.usecase.bfa;

import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.RESPUESTA_NO_FV;
import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.REVISION_COMPLETADA;
import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.REVISION_NO_ENCONTRADA;
import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.REVISION_NO_PUBLICABLE;
import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.REVISION_VIGENTE_CAMBIO;
import static com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo.SOLICITUD_INVALIDA;
import static com.universidad.tests.domain.model.MatrizPermisos.PUBLICAR_RESULTADOS;
import static com.universidad.tests.domain.model.MatrizPermisos.RECALCULAR_BFA;
import static com.universidad.tests.domain.model.MatrizPermisos.REVISAR_FLUIDEZ;

import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.RegistroAuditoria;
import com.universidad.tests.domain.exception.EvaluacionNoEncontradaException;
import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.bfa.DecisionPalabra;
import com.universidad.tests.domain.model.bfa.RevisionPalabra;
import com.universidad.tests.domain.model.bfa.RevisionResultado;
import com.universidad.tests.domain.port.PublicacionResultadoPort;
import com.universidad.tests.domain.port.RespuestaFluidezPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.domain.repository.bfa.RevisionPalabraRepository;
import com.universidad.tests.domain.repository.bfa.RevisionResultadoRepository;
import com.universidad.tests.dto.request.PublicarRevisionRequest;
import com.universidad.tests.dto.request.RecalculoRequest;
import com.universidad.tests.dto.request.RevisionPalabraRequest;
import com.universidad.tests.dto.response.PublicacionResponse;
import com.universidad.tests.dto.response.RecalculoResponse;
import com.universidad.tests.dto.response.RevisionPalabraResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BFA-BE-31: E10 revisar palabra de Fluidez Verbal, E11 recalcular (nueva revisión que conserva el
 * historial) y E12 publicar la revisión vigente.
 */
@Service
public class RevisionResultadoBfaService {

    static final Set<String> AUDIENCIAS =
            Set.of("SUJETO", "ACADEMICO", "COORDINADOR", "PSICOLOGO", "DECANO");

    private final AutorizacionService auth;
    private final EvaluacionRepository evaluaciones;
    private final ResultadoRepository resultados;
    private final RevisionResultadoRepository revisiones;
    private final RevisionPalabraRepository palabras;
    private final RespuestaFluidezPort respuestasFluidez;
    private final PublicacionResultadoPort publicacion;
    private final RegistroAuditoria auditoria;

    public RevisionResultadoBfaService(
            AutorizacionService auth,
            EvaluacionRepository evaluaciones,
            ResultadoRepository resultados,
            RevisionResultadoRepository revisiones,
            RevisionPalabraRepository palabras,
            RespuestaFluidezPort respuestasFluidez,
            PublicacionResultadoPort publicacion,
            RegistroAuditoria auditoria) {
        this.auth = auth;
        this.evaluaciones = evaluaciones;
        this.resultados = resultados;
        this.revisiones = revisiones;
        this.palabras = palabras;
        this.respuestasFluidez = respuestasFluidez;
        this.publicacion = publicacion;
        this.auditoria = auditoria;
    }

    /** E10. Repetir la misma decisión no duplica la pareja revisión-respuesta. */
    @Transactional
    public RevisionPalabraResponse revisarPalabra(
            Long evaluacionId,
            Long calificacionId,
            Long respuestaId,
            RevisionPalabraRequest solicitud) {
        var sesion = auth.exigirPermiso(REVISAR_FLUIDEZ);
        DecisionPalabra decision = decision(solicitud);
        exigirTexto(solicitud.motivo(), "motivo");

        Resultado resultado = resultadoFinalizado(evaluacionId);
        RevisionResultado revision = revisionDe(resultado, calificacionId);
        if (calificacionId.equals(resultado.referenciaRevisionVigente())) {
            throw new RevisionBfaException(
                    REVISION_COMPLETADA,
                    "La revisión ya se completó; solicite un recálculo (E11).");
        }
        if (!respuestasFluidez.esPalabraFluidezDe(evaluacionId, respuestaId)) {
            throw new RevisionBfaException(
                    RESPUESTA_NO_FV,
                    "La respuesta no es una palabra de Fluidez Verbal de esta evaluación.");
        }

        var guardada =
                palabras.guardar(
                        new RevisionPalabra(
                                null,
                                revision.idRevision(),
                                respuestaId,
                                decision,
                                solicitud.motivo().trim(),
                                sesion.idCuenta(),
                                ahora()));
        auditoria.registrar(
                sesion.idCuenta(), "REVISAR_PALABRA", "REVISION_RESULTADO", revision.idRevision());
        return new RevisionPalabraResponse(
                guardada.idRevisionPalabra(),
                revision.idRevision(),
                respuestaId,
                guardada.decision().name(),
                RevisionPalabra.ORIGEN_DECISION,
                guardada.fechaDecision());
    }

    /**
     * E11. Crea una revisión nueva; la vigente no cambia. Reenviar la misma {@code idSolicitud}
     * devuelve la misma revisión.
     */
    @Transactional
    public RecalculoResponse recalcular(Long evaluacionId, RecalculoRequest solicitud) {
        var sesion = auth.exigirPermiso(RECALCULAR_BFA);
        if (solicitud == null) {
            throw new RevisionBfaException(SOLICITUD_INVALIDA, "Falta el cuerpo de la solicitud.");
        }
        exigirTexto(solicitud.motivo(), "motivo");

        Resultado resultado = resultadoFinalizado(evaluacionId);
        if (solicitud.idSolicitud() != null && !solicitud.idSolicitud().isBlank()) {
            var existente =
                    revisiones.buscarPorSolicitud(
                            resultado.idResultado(), solicitud.idSolicitud().trim());
            if (existente.isPresent()) {
                return respuesta(resultado, existente.get());
            }
        }

        // Si no se indican versiones, se parte de las de la última revisión.
        var previa = revisiones.buscarUltima(resultado.idResultado());
        var nueva =
                revisiones.guardarNueva(
                        new RevisionResultado(
                                null,
                                resultado.idResultado(),
                                previa.map(r -> r.numeroRevision() + 1).orElse(1),
                                solicitud.motivo().trim(),
                                elegir(solicitud.versionReglas(), previa, r -> r.versionReglas()),
                                elegir(solicitud.idClave(), previa, r -> r.idClave()),
                                elegir(solicitud.idBaremo(), previa, r -> r.idBaremo()),
                                blancoANulo(solicitud.idSolicitud()),
                                sesion.idCuenta(),
                                ahora()));
        auditoria.registrar(
                sesion.idCuenta(),
                "RECALCULAR_RESULTADO",
                "REVISION_RESULTADO",
                nueva.idRevision());
        return respuesta(resultado, nueva);
    }

    /** E12. Publica una revisión completa y validada; repetirla no duplica el cambio. */
    @Transactional
    public PublicacionResponse publicar(Long evaluacionId, PublicarRevisionRequest solicitud) {
        var sesion = auth.exigirPermiso(PUBLICAR_RESULTADOS);
        if (solicitud == null || solicitud.calificacionId() == null) {
            throw new RevisionBfaException(SOLICITUD_INVALIDA, "Falta calificacionId.");
        }
        exigirTexto(solicitud.motivo(), "motivo");
        List<String> audiencia = audiencia(solicitud.audiencia());

        Resultado resultado = resultadoFinalizado(evaluacionId);
        RevisionResultado candidata = revisionDe(resultado, solicitud.calificacionId());

        if (candidata.idRevision().equals(resultado.referenciaRevisionVigente())) {
            return new PublicacionResponse(
                    resultado.idResultado(),
                    candidata.idRevision(),
                    candidata.numeroRevision(),
                    Resultado.DISPONIBLE);
        }

        Integer vigenteActual =
                resultado.referenciaRevisionVigente() == null
                        ? null
                        : revisiones
                                .buscarPorId(resultado.referenciaRevisionVigente())
                                .map(RevisionResultado::numeroRevision)
                                .orElse(null);
        if (!java.util.Objects.equals(vigenteActual, solicitud.revisionVigenteEsperada())) {
            throw new RevisionBfaException(
                    REVISION_VIGENTE_CAMBIO,
                    "La revisión vigente cambió; consulte el resultado y vuelva a intentar.");
        }
        if (palabras.contarPendientes(candidata.idRevision()) > 0) {
            throw new RevisionBfaException(
                    REVISION_NO_PUBLICABLE,
                    "La revisión tiene palabras pendientes de decisión y no se puede publicar.");
        }

        publicacion.publicar(
                resultado.idResultado(),
                candidata.idRevision(),
                audiencia,
                solicitud.motivo().trim());
        auditoria.registrar(
                sesion.idCuenta(),
                "PUBLICAR_REVISION",
                "REVISION_RESULTADO",
                candidata.idRevision());
        return new PublicacionResponse(
                resultado.idResultado(),
                candidata.idRevision(),
                candidata.numeroRevision(),
                Resultado.DISPONIBLE);
    }

    private Resultado resultadoFinalizado(Long evaluacionId) {
        var evaluacion =
                evaluaciones
                        .buscarPorId(evaluacionId)
                        .orElseThrow(() -> new EvaluacionNoEncontradaException(evaluacionId));
        if (evaluacion.estado() != EstadoEvaluacion.FINALIZADA) {
            throw new EvaluacionNoFinalizadaException(evaluacionId);
        }
        return resultados
                .buscarPorEvaluacion(evaluacionId)
                .orElseThrow(() -> new EvaluacionNoEncontradaException(evaluacionId));
    }

    private RevisionResultado revisionDe(Resultado resultado, Long calificacionId) {
        return revisiones
                .buscarPorId(calificacionId)
                .filter(r -> r.idResultado().equals(resultado.idResultado()))
                .orElseThrow(
                        () ->
                                new RevisionBfaException(
                                        REVISION_NO_ENCONTRADA,
                                        "La revisión " + calificacionId + " no existe."));
    }

    private static RecalculoResponse respuesta(Resultado resultado, RevisionResultado revision) {
        return new RecalculoResponse(
                resultado.idResultado(),
                revision.idRevision(),
                revision.numeroRevision(),
                Resultado.PENDIENTE_VALIDACION);
    }

    private static DecisionPalabra decision(RevisionPalabraRequest solicitud) {
        if (solicitud == null || solicitud.decision() == null) {
            throw new RevisionBfaException(SOLICITUD_INVALIDA, "Falta la decisión.");
        }
        try {
            return DecisionPalabra.valueOf(solicitud.decision().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RevisionBfaException(
                    SOLICITUD_INVALIDA, "La decisión debe ser VALIDA, INVALIDA o PENDIENTE.");
        }
    }

    private static List<String> audiencia(List<String> pedida) {
        if (pedida == null || pedida.isEmpty()) {
            throw new RevisionBfaException(SOLICITUD_INVALIDA, "Falta la audiencia.");
        }
        var normalizada =
                pedida.stream()
                        .map(a -> a == null ? "" : a.trim().toUpperCase())
                        .distinct()
                        .toList();
        if (!AUDIENCIAS.containsAll(normalizada)) {
            throw new RevisionBfaException(
                    SOLICITUD_INVALIDA,
                    "La audiencia solo admite SUJETO, ACADEMICO, COORDINADOR, PSICOLOGO o DECANO.");
        }
        return normalizada;
    }

    private static void exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new RevisionBfaException(SOLICITUD_INVALIDA, "Falta " + campo + ".");
        }
    }

    private static String blancoANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static <T> T elegir(
            T pedido,
            java.util.Optional<RevisionResultado> previa,
            java.util.function.Function<RevisionResultado, T> deLaPrevia) {
        if (pedido instanceof String s && s.isBlank()) {
            pedido = null;
        }
        return pedido != null ? pedido : previa.map(deLaPrevia).orElse(null);
    }

    private static Instant ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
