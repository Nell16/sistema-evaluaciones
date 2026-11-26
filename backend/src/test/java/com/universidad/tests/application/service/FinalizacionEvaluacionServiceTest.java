package com.universidad.tests.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.EstadoSubprueba;
import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.model.EvaluacionSubprueba;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.port.AlcanceSujetoPort;
import com.universidad.tests.domain.port.FinalizacionEvaluacionPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinalizacionEvaluacionServiceTest {

    private AutorizacionService auth;
    private AlcanceSujetoPort alcance;
    private EvaluacionRepository evaluaciones;
    private PreparacionFinalizacionService preparacion;
    private FinalizacionEvaluacionPort cierre;
    private RegistroAuditoria auditoria;
    private MemoriaResultadoRepository resultadoRepository;
    private FinalizacionEvaluacionService servicio;

    private final Instant ahora = Instant.parse("2026-11-25T12:00:00Z");

    @BeforeEach
    void setUp() {
        auth = mock(AutorizacionService.class);
        alcance = mock(AlcanceSujetoPort.class);
        evaluaciones = mock(EvaluacionRepository.class);
        preparacion = mock(PreparacionFinalizacionService.class);
        cierre = mock(FinalizacionEvaluacionPort.class);
        auditoria = mock(RegistroAuditoria.class);
        resultadoRepository = new MemoriaResultadoRepository();

        when(auth.exigirPermiso(anyString()))
                .thenReturn(
                        new IdentidadSesion(
                                10L,
                                1L,
                                "SUJETO",
                                Set.of("EVALUACION_FINALIZAR", "RECUPERAR_INTENTO")));
        when(auth.exigirSesion())
                .thenReturn(
                        new IdentidadSesion(
                                10L,
                                1L,
                                "SUJETO",
                                Set.of("EVALUACION_FINALIZAR", "RECUPERAR_INTENTO")));
        when(alcance.estaAsignado(anyLong(), anyLong())).thenReturn(true);

        servicio =
                new FinalizacionEvaluacionService(
                        auth,
                        alcance,
                        evaluaciones,
                        preparacion,
                        cierre,
                        auditoria,
                        resultadoRepository,
                        mock(
                                com.universidad.tests.application.service
                                        .CalculoPuntuacionesBfaService.class));
    }

    @Test
    void finalizaEvaluacionCalculaCompuestosBfaYRegistraAuditoria() {
        Evaluacion enCurso = crearEvaluacion(1L, EstadoEvaluacion.EN_CURSO);
        Evaluacion finalizada = crearEvaluacion(1L, EstadoEvaluacion.FINALIZADA);

        when(evaluaciones.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(enCurso));
        when(evaluaciones.buscarPorId(1L)).thenReturn(Optional.of(finalizada));
        when(cierre.finalizar(eq(1L), any(Instant.class)))
                .thenReturn(new FinalizacionEvaluacionPort.Cierre(100L, "PENDIENTE_VALIDACION"));

        Map<String, Integer> pds = Map.of("VOC1", 10, "VOC2", 15);
        EvaluacionResponse response = servicio.finalizar(1L, pds);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.estado()).isEqualTo("FINALIZADA");
        assertThat(response.resultadoId()).isEqualTo(100L);
        assertThat(response.estadoResultado()).isEqualTo("PENDIENTE_VALIDACION");

        verify(auditoria).registrar(10L, "FINALIZAR_EVALUACION", "EVALUACION", 1L);

        Resultado resultadoGuardado = resultadoRepository.buscarPorEvaluacion(1L).orElseThrow();
        assertThat(resultadoGuardado.idResultado()).isEqualTo(100L);
        assertThat(resultadoGuardado.estadoResultado()).isEqualTo("PENDIENTE_VALIDACION");

        // Compuesto VOCT = 10 + 15 = 25
        ResultadoFactor voct =
                resultadoGuardado.factores().stream()
                        .filter(f -> "VOCT".equals(f.codigo()))
                        .findFirst()
                        .orElseThrow();
        assertThat(voct.puntuacionDirecta()).isEqualTo(25);
        assertThat(voct.estadoCalculo()).isEqualTo("CALCULADO");
    }

    @Test
    void devuelveCierreExistenteSinRecalcularCuandoYaEstaFinalizada() {
        Evaluacion yaFinalizada = crearEvaluacion(1L, EstadoEvaluacion.FINALIZADA);

        when(evaluaciones.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(yaFinalizada));
        when(evaluaciones.buscarPorId(1L)).thenReturn(Optional.of(yaFinalizada));
        when(cierre.finalizar(eq(1L), any(Instant.class)))
                .thenReturn(new FinalizacionEvaluacionPort.Cierre(100L, "PENDIENTE_VALIDACION"));

        EvaluacionResponse response = servicio.finalizar(1L);
        assertThat(response.estado()).isEqualTo("FINALIZADA");
        assertThat(response.resultadoId()).isEqualTo(100L);
    }

    @Test
    void rechazaFinalizacionSiRolNoEsSujeto() {
        when(auth.exigirPermiso(anyString()))
                .thenReturn(
                        new IdentidadSesion(10L, 1L, "PSICOLOGO", Set.of("EVALUACION_FINALIZAR")));
        Evaluacion enCurso = crearEvaluacion(1L, EstadoEvaluacion.EN_CURSO);
        when(evaluaciones.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(enCurso));

        assertThatThrownBy(() -> servicio.finalizar(1L)).isInstanceOf(SinPermisoException.class);
    }

    @Test
    void rechazaFinalizacionSiSujetoNoEstaAsignado() {
        when(alcance.estaAsignado(anyLong(), anyLong())).thenReturn(false);
        Evaluacion enCurso = crearEvaluacion(1L, EstadoEvaluacion.EN_CURSO);
        when(evaluaciones.buscarPorIdParaActualizar(1L)).thenReturn(Optional.of(enCurso));

        assertThatThrownBy(() -> servicio.finalizar(1L)).isInstanceOf(SinPermisoException.class);
    }

    private Evaluacion crearEvaluacion(Long id, EstadoEvaluacion estado) {
        return new Evaluacion(
                id,
                25L,
                3L,
                1L,
                10L,
                estado,
                ahora.minusSeconds(600),
                estado == EstadoEvaluacion.FINALIZADA ? ahora : null,
                List.of(
                        new EvaluacionSubprueba(
                                1L,
                                id,
                                101L,
                                "VOC1",
                                "Sinónimos",
                                1,
                                300,
                                EstadoSubprueba.CERRADA,
                                "Instrucciones",
                                true,
                                ahora.minusSeconds(600),
                                ahora.minusSeconds(300),
                                ahora.minusSeconds(300),
                                null,
                                75)));
    }

    private static class MemoriaResultadoRepository implements ResultadoRepository {
        private final Map<Long, Resultado> porEvaluacion = new HashMap<>();
        private long seq = 1L;

        @Override
        public Resultado guardar(Resultado r) {
            long id = r.idResultado() != null ? r.idResultado() : seq++;
            Resultado guardado =
                    new Resultado(
                            id,
                            r.idEvaluacion(),
                            r.estadoResultado(),
                            r.versionFormularioId(),
                            r.creadoEn(),
                            r.referenciaRevisionVigente(),
                            r.factores());
            porEvaluacion.put(r.idEvaluacion(), guardado);
            return guardado;
        }

        @Override
        public Optional<Resultado> buscarPorEvaluacion(Long idEvaluacion) {
            return Optional.ofNullable(porEvaluacion.get(idEvaluacion));
        }
    }
}
