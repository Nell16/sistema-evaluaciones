package com.universidad.tests.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.universidad.tests.domain.repository.CierreSubpruebasRepository;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FinalizarEvaluacionServiceTest {

    private MemoriaEvaluacionRepository evaluacionRepository;
    private MemoriaResultadoRepository resultadoRepository;
    private CierreSubpruebasService cierreService;
    private FinalizarEvaluacionService service;
    private final Instant ahora = Instant.parse("2026-11-25T12:00:00Z");

    @BeforeEach
    void setUp() {
        evaluacionRepository = new MemoriaEvaluacionRepository();
        resultadoRepository = new MemoriaResultadoRepository();
        Clock clock = Clock.fixed(ahora, ZoneId.of("UTC"));
        cierreService =
                new CierreSubpruebasService(
                        new MemoriaCierreRepository(evaluacionRepository), clock);
        TestEvaluatorRegistry registry = new TestEvaluatorRegistry();
        registry.registrar(new BfaTestEvaluator());

        service =
                new FinalizarEvaluacionService(
                        cierreService, evaluacionRepository, registry, resultadoRepository, clock);
    }

    @Test
    void finalizaNormalmenteCuandoTodasLasSubpruebasEstanCerradas() {
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        1L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100)),
                                crearSubprueba(
                                        2L,
                                        "VOC2",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(50))));
        evaluacionRepository.guardarDirecto(evaluacion);

        EvaluacionResponse finalizada = service.finalizar(1L);

        assertEquals("FINALIZADA", finalizada.estado());
        assertEquals("PENDIENTE_VALIDACION", finalizada.estadoResultado());
        assertNotNull(finalizada.resultadoId());
        assertEquals(ahora, finalizada.fechaFin());
        assertEquals(2, finalizada.subpruebas().size());
        assertEquals("CERRADA", finalizada.subpruebas().get(0).estado());
    }

    @Test
    void calculaCompuestosBfaYGuardaResultadoPendienteValidacion() {
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        2L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100)),
                                crearSubprueba(
                                        2L,
                                        "VOC2",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(50))));
        evaluacionRepository.guardarDirecto(evaluacion);

        Map<String, Integer> puntuacionesDirectas = Map.of("VOC1", 10, "VOC2", 15);
        EvaluacionResponse finalizada = service.finalizar(2L, puntuacionesDirectas);

        assertEquals("FINALIZADA", finalizada.estado());
        assertEquals("PENDIENTE_VALIDACION", finalizada.estadoResultado());

        ResultadoResponse resultado = service.consultarResultados(2L);
        assertEquals("PENDIENTE_VALIDACION", resultado.estadoResultado());
        assertNotNull(resultado.resultadoId());

        Map<String, ResultadoResponse.FactorResponse> porCodigo = new HashMap<>();
        for (ResultadoResponse.FactorResponse f : resultado.factores()) {
            porCodigo.put(f.codigo(), f);
        }

        // VOC1 y VOC2 calculados
        assertEquals(10, porCodigo.get("VOC1").puntuacionDirecta());
        assertEquals("CALCULADO", porCodigo.get("VOC1").estadoCalculo());
        assertEquals(15, porCodigo.get("VOC2").puntuacionDirecta());
        assertEquals("CALCULADO", porCodigo.get("VOC2").estadoCalculo());

        // Compuesto VOCT = VOC1 + VOC2 = 25
        assertEquals(25, porCodigo.get("VOCT").puntuacionDirecta());
        assertEquals("CALCULADO", porCodigo.get("VOCT").estadoCalculo());

        // Familia sin calificador queda NO_CALCULABLE (no se inventa)
        assertNull(porCodigo.get("CV").puntuacionDirecta());
        assertEquals("NO_CALCULABLE", porCodigo.get("CV").estadoCalculo());
    }

    @Test
    void devuelveCierreExistenteSinRecalcularCuandoYaEstaFinalizada() {
        Instant fechaFinPrevia = ahora.minusSeconds(3600);
        Evaluacion evaluacion =
                new Evaluacion(
                        3L,
                        25L,
                        3L,
                        1L,
                        10L,
                        EstadoEvaluacion.FINALIZADA,
                        ahora.minusSeconds(7200),
                        fechaFinPrevia,
                        List.of(
                                crearSubprueba(
                                        1L, "VOC1", EstadoSubprueba.CERRADA, fechaFinPrevia)));
        evaluacionRepository.guardarDirecto(evaluacion);

        Resultado previo =
                new Resultado(
                        888L,
                        3L,
                        "PENDIENTE_VALIDACION",
                        10L,
                        fechaFinPrevia,
                        null,
                        "Detalle previo",
                        List.of(ResultadoFactor.calculado("VOC1", 10, null, null)));
        resultadoRepository.guardar(previo);

        EvaluacionResponse resultado = service.finalizar(3L);

        assertEquals("FINALIZADA", resultado.estado());
        assertEquals(888L, resultado.resultadoId());
        assertEquals(fechaFinPrevia, resultado.fechaFin());
    }

    @Test
    void rechazaFinalizacionSiHaySubpruebasActivasSinVencer() {
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        4L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100)),
                                new EvaluacionSubprueba(
                                        20L,
                                        4L,
                                        2L,
                                        "VOC2",
                                        "VOC2",
                                        2,
                                        300,
                                        EstadoSubprueba.ACTIVA,
                                        null,
                                        true,
                                        ahora.minusSeconds(60),
                                        ahora.plusSeconds(240), // todavía no vence
                                        null,
                                        null,
                                        75)));
        evaluacionRepository.guardarDirecto(evaluacion);

        assertThrows(SubpruebasPendientesException.class, () -> service.finalizar(4L));
    }

    @Test
    void rechazaFinalizacionSiHaySubpruebasPendientes() {
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        5L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100)),
                                new EvaluacionSubprueba(
                                        21L,
                                        5L,
                                        2L,
                                        "VOC2",
                                        "VOC2",
                                        2,
                                        300,
                                        EstadoSubprueba.PENDIENTE,
                                        null,
                                        true,
                                        null,
                                        null,
                                        null,
                                        null,
                                        75)));
        evaluacionRepository.guardarDirecto(evaluacion);

        assertThrows(SubpruebasPendientesException.class, () -> service.finalizar(5L));
    }

    @Test
    void cierraDeFormaIdempotenteSubpruebaActivaCuyoFinLimiteYaVencio() {
        Instant finLimiteVencido = ahora.minusSeconds(10);
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        6L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100)),
                                new EvaluacionSubprueba(
                                        22L,
                                        6L,
                                        2L,
                                        "VOC2",
                                        "VOC2",
                                        2,
                                        300,
                                        EstadoSubprueba.ACTIVA,
                                        null,
                                        true,
                                        ahora.minusSeconds(310),
                                        finLimiteVencido, // ya venció
                                        null,
                                        null,
                                        75)));
        evaluacionRepository.guardarDirecto(evaluacion);

        EvaluacionResponse finalizada = service.finalizar(6L);

        assertEquals("FINALIZADA", finalizada.estado());
        EvaluacionResponse.SubpruebaEstado sp2 = finalizada.subpruebas().get(1);
        assertEquals("CERRADA", sp2.estado());
        assertEquals(finLimiteVencido, sp2.cerradaEn());
    }

    @Test
    void lanza404CuandoLaEvaluacionNoExiste() {
        assertThrows(EvaluacionNoEncontradaException.class, () -> service.finalizar(9999L));
    }

    @Test
    void consultarResultadosLanza409CuandoEvaluacionNoHaFinalizado() {
        Evaluacion evaluacion =
                crearEvaluacionPrueba(
                        7L,
                        EstadoEvaluacion.EN_CURSO,
                        List.of(
                                crearSubprueba(
                                        1L,
                                        "VOC1",
                                        EstadoSubprueba.CERRADA,
                                        ahora.minusSeconds(100))));
        evaluacionRepository.guardarDirecto(evaluacion);

        assertThrows(EvaluacionNoFinalizadaException.class, () -> service.consultarResultados(7L));
    }

    private static Evaluacion crearEvaluacionPrueba(
            long id, EstadoEvaluacion estado, List<EvaluacionSubprueba> subpruebas) {
        return new Evaluacion(
                id,
                25L,
                3L,
                1L,
                10L,
                estado,
                Instant.parse("2026-11-25T10:00:00Z"),
                estado == EstadoEvaluacion.FINALIZADA
                        ? Instant.parse("2026-11-25T11:00:00Z")
                        : null,
                subpruebas);
    }

    private static EvaluacionSubprueba crearSubprueba(
            long configId, String codigo, EstadoSubprueba estado, Instant cerradaEn) {
        return new EvaluacionSubprueba(
                configId + 100,
                null,
                configId,
                codigo,
                codigo,
                1,
                300,
                estado,
                null,
                true,
                cerradaEn != null ? cerradaEn.minusSeconds(200) : null,
                cerradaEn != null ? cerradaEn.plusSeconds(100) : null,
                cerradaEn,
                null,
                75);
    }

    private static class MemoriaEvaluacionRepository implements EvaluacionRepository {
        private final Map<Long, Evaluacion> mapa = new HashMap<>();

        @Override
        public Optional<Evaluacion> buscarPorId(Long id) {
            return Optional.ofNullable(mapa.get(id));
        }

        @Override
        public Optional<Evaluacion> buscarPorIdParaActualizar(Long id) {
            return buscarPorId(id);
        }

        @Override
        public void iniciarSubprueba(EvaluacionSubprueba subprueba) {}

        @Override
        public boolean existeEnCurso(Long idSujeto, Long idTest, Long idPeriodo) {
            return false;
        }

        @Override
        public Evaluacion guardar(Evaluacion evaluacion) {
            long nuevoId = mapa.size() + 1L;
            Evaluacion nueva =
                    new Evaluacion(
                            nuevoId,
                            evaluacion.idSujeto(),
                            evaluacion.idPeriodo(),
                            evaluacion.idTest(),
                            evaluacion.idVersionFormulario(),
                            evaluacion.estado(),
                            evaluacion.fechaInicio(),
                            evaluacion.fechaFin(),
                            evaluacion.subpruebas());
            mapa.put(nuevoId, nueva);
            return nueva;
        }

        public void guardarDirecto(Evaluacion evaluacion) {
            mapa.put(evaluacion.idEvaluacion(), evaluacion);
        }

        @Override
        public void finalizarEvaluacion(Long idEvaluacion, Instant fechaFin) {
            Evaluacion actual = mapa.get(idEvaluacion);
            if (actual != null) {
                mapa.put(
                        idEvaluacion,
                        new Evaluacion(
                                actual.idEvaluacion(),
                                actual.idSujeto(),
                                actual.idPeriodo(),
                                actual.idTest(),
                                actual.idVersionFormulario(),
                                EstadoEvaluacion.FINALIZADA,
                                actual.fechaInicio(),
                                fechaFin,
                                actual.subpruebas()));
            }
        }
    }

    private static class MemoriaResultadoRepository implements ResultadoRepository {
        private final Map<Long, Resultado> porEvaluacion = new HashMap<>();
        private long seq = 1;

        @Override
        public Resultado guardar(Resultado resultado) {
            long id = resultado.idResultado() != null ? resultado.idResultado() : seq++;
            Resultado guardado =
                    new Resultado(
                            id,
                            resultado.idEvaluacion(),
                            resultado.estadoResultado(),
                            resultado.versionFormularioId(),
                            resultado.fechaCalculo(),
                            resultado.referenciaRevisionVigente(),
                            resultado.detalle(),
                            resultado.factores());
            porEvaluacion.put(resultado.idEvaluacion(), guardado);
            return guardado;
        }

        @Override
        public Optional<Resultado> buscarPorEvaluacion(Long idEvaluacion) {
            return Optional.ofNullable(porEvaluacion.get(idEvaluacion));
        }
    }

    private static class MemoriaCierreRepository implements CierreSubpruebasRepository {
        private final MemoriaEvaluacionRepository evaluaciones;

        MemoriaCierreRepository(MemoriaEvaluacionRepository evaluaciones) {
            this.evaluaciones = evaluaciones;
        }

        @Override
        public List<Long> buscarEvaluacionesVencidas(Instant ahora, int limite) {
            return List.of();
        }

        @Override
        public void cerrarVencidas(Long idEvaluacion, Supplier<Instant> reloj) {
            Evaluacion e = evaluaciones.mapa.get(idEvaluacion);
            if (e == null) return;
            Instant ahora = reloj.get();
            List<EvaluacionSubprueba> actualizadas = new ArrayList<>();
            for (EvaluacionSubprueba s : e.subpruebas()) {
                if (s.estado() == EstadoSubprueba.ACTIVA
                        && s.fechaFinLimite() != null
                        && !s.fechaFinLimite().isAfter(ahora)) {
                    actualizadas.add(
                            new EvaluacionSubprueba(
                                    s.idEvaluacionSubprueba(),
                                    s.idEvaluacion(),
                                    s.idConfiguracionSubprueba(),
                                    s.codigo(),
                                    s.nombre(),
                                    s.orden(),
                                    s.duracionSegundos(),
                                    EstadoSubprueba.CERRADA,
                                    s.instrucciones(),
                                    s.permitirRetroceso(),
                                    s.fechaInicio(),
                                    s.fechaFinLimite(),
                                    s.fechaFinLimite(),
                                    s.letraInicial(),
                                    s.maximoEntradas()));
                } else {
                    actualizadas.add(s);
                }
            }
            evaluaciones.guardarDirecto(
                    new Evaluacion(
                            e.idEvaluacion(),
                            e.idSujeto(),
                            e.idPeriodo(),
                            e.idTest(),
                            e.idVersionFormulario(),
                            e.estado(),
                            e.fechaInicio(),
                            e.fechaFin(),
                            actualizadas));
        }
    }
}
