package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.universidad.tests.application.usecase.ConsultarResultadoEvaluacionUseCase;
import com.universidad.tests.domain.exception.DominioNegocioException;
import com.universidad.tests.domain.model.*;
import com.universidad.tests.domain.port.AutorizacionResultadoPort;
import com.universidad.tests.domain.port.ContextoActorResultado;
import com.universidad.tests.domain.repository.AuditoriaRepository;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarResultadoEvaluacionUseCaseTest {

    @Mock private EvaluacionRepository evaluacionRepository;
    @Mock private ResultadoRepository resultadoRepository;
    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private AutorizacionResultadoPort autorizacionPort;

    @InjectMocks private ConsultarResultadoEvaluacionUseCase useCase;

    private Evaluacion evaluacionFinalizada() {
        return new Evaluacion(
            15L,
            25L,
            10L,
            3L,
            EstadoEvaluacion.FINALIZADA,
            Instant.parse("2026-10-31T15:00:00Z"),
            Instant.parse("2026-10-31T15:06:00Z"),
            "INDIVIDUAL",
            "PARCIAL");
    }

    @Test
    void debeRechazarSiEvaluacionNoExiste() {
        when(evaluacionRepository.buscarPorId(99L)).thenReturn(Optional.empty());

        DominioNegocioException ex =
            assertThrows(DominioNegocioException.class, () -> useCase.ejecutar(99L));

        assertEquals(404, ex.getStatusHttp());
        assertEquals("EVALUACION_NO_ENCONTRADA", ex.getCodigo());
        verifyNoInteractions(autorizacionPort, resultadoRepository, auditoriaRepository);
    }

    @Test
    void debeRechazarSiEvaluacionEstaEnCurso() {
        Evaluacion enCurso =
            new Evaluacion(
                15L,
                25L,
                10L,
                3L,
                EstadoEvaluacion.EN_CURSO,
                Instant.now(),
                null,
                "INDIVIDUAL",
                "COMPLETA");

        when(evaluacionRepository.buscarPorId(15L)).thenReturn(Optional.of(enCurso));
        when(autorizacionPort.validarAccesoConsultaResultado(15L, 25L))
            .thenReturn(new ContextoActorResultado("prof-1", true));

        DominioNegocioException ex =
            assertThrows(DominioNegocioException.class, () -> useCase.ejecutar(15L));

        assertEquals(409, ex.getStatusHttp());
        assertEquals("EVALUACION_NO_FINALIZADA", ex.getCodigo());
        verifyNoInteractions(resultadoRepository, auditoriaRepository);
    }

    @Test
    void debeRechazarAlSujetoSiResultadoNoEstaPublicado() {
        Evaluacion evaluacion = evaluacionFinalizada();
        Resultado pendiente =
            new Resultado(70L, 15L, EstadoResultado.PENDIENTE_VALIDACION, null, Instant.now());

        when(evaluacionRepository.buscarPorId(15L)).thenReturn(Optional.of(evaluacion));
        when(autorizacionPort.validarAccesoConsultaResultado(15L, 25L))
            .thenReturn(new ContextoActorResultado("sujeto-25", false));
        when(resultadoRepository.buscarPorEvaluacionId(15L)).thenReturn(Optional.of(pendiente));

        DominioNegocioException ex =
            assertThrows(DominioNegocioException.class, () -> useCase.ejecutar(15L));

        assertEquals(403, ex.getStatusHttp());
        assertEquals("RESULTADO_NO_PUBLICADO", ex.getCodigo());
        verifyNoInteractions(auditoriaRepository);
    }

    @Test
    void debeDevolverPendienteSinMetricasCandidatasParaProfesional() {
        Evaluacion evaluacion = evaluacionFinalizada();
        Resultado pendiente =
            new Resultado(70L, 15L, EstadoResultado.PENDIENTE_VALIDACION, null, Instant.now());

        when(evaluacionRepository.buscarPorId(15L)).thenReturn(Optional.of(evaluacion));
        when(autorizacionPort.validarAccesoConsultaResultado(15L, 25L))
            .thenReturn(new ContextoActorResultado("prof-1", true));
        when(resultadoRepository.buscarPorEvaluacionId(15L)).thenReturn(Optional.of(pendiente));

        var respuesta = useCase.ejecutar(15L);

        assertEquals("PENDIENTE_VALIDACION", respuesta.estadoResultado());
        assertEquals("SIN_REVISION_PUBLICADA", respuesta.motivo());
        assertNull(respuesta.revisionVigente());
        assertTrue(respuesta.factores().isEmpty());
        verify(resultadoRepository, never()).buscarRevisionVigentePorId(any());
        verify(auditoriaRepository)
            .registrarConsultaResultado(eq(15L), eq(70L), eq("prof-1"), anyString());
    }

    @Test
    void debeConsultarExclusivamenteReferenciaRevisionVigenteYAuditar() {
        Evaluacion evaluacion = evaluacionFinalizada();
        Resultado disponible =
            new Resultado(70L, 15L, EstadoResultado.DISPONIBLE, 701L, Instant.now());
        RevisionResultado vigente =
            new RevisionResultado(
                701L,
                2,
                4L,
                "REGLAS-1",
                null,
                null,
                null,
                null,
                List.of(
                    new FactorResultado(
                        "FV",
                        2,
                        "CALCULADO",
                        null,
                        null,
                        "SIN_BAREMO_VALIDADO",
                        "Sin baremo validado")));

        when(evaluacionRepository.buscarPorId(15L)).thenReturn(Optional.of(evaluacion));
        when(autorizacionPort.validarAccesoConsultaResultado(15L, 25L))
            .thenReturn(new ContextoActorResultado("sujeto-25", false));
        when(resultadoRepository.buscarPorEvaluacionId(15L)).thenReturn(Optional.of(disponible));
        when(resultadoRepository.buscarRevisionVigentePorId(701L))
            .thenReturn(Optional.of(vigente));

        var respuesta = useCase.ejecutar(15L);

        assertEquals("DISPONIBLE", respuesta.estadoResultado());
        assertEquals(701L, respuesta.revisionVigente().getCalificacionId());
        assertEquals(1, respuesta.factores().size());
        verify(resultadoRepository).buscarRevisionVigentePorId(701L);
        verify(auditoriaRepository)
            .registrarConsultaResultado(eq(15L), eq(70L), eq("sujeto-25"), anyString());
    }
}
