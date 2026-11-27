package com.universidad.tests.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.FinalizacionEvaluacionService;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.EstadoSubprueba;
import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.model.EvaluacionSubprueba;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.port.AlcanceSujetoPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.dto.response.EvaluacionResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import com.universidad.tests.infrastructure.persistence.entity.ConfiguracionSubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.PeriodoJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.SubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.SujetoJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FinalizarEvaluacionIntegrationTest {

    @Autowired private FinalizacionEvaluacionService finalizacionService;
    @Autowired private EvaluacionRepository evaluacionRepository;
    @Autowired private ResultadoRepository resultadoRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @MockitoBean private AutorizacionService auth;
    @MockitoBean private AlcanceSujetoPort alcance;

    private Long idTest;
    private Long idVersion;
    private Long idConfigVoc1;
    private Long idConfigVoc2;
    private Long idSujeto;
    private Long idPeriodo;

    @BeforeEach
    void prepararDatos() {
        long cuenta =
                jdbc.queryForObject(
                        "insert into cuenta(correo,resumen_contrasena,nombres,apellidos,estado) values (?,'resumen-ficticio','persona','ficticia','ACTIVA') returning id_cuenta",
                        Long.class,
                        "finalizar-" + UUID.randomUUID() + "@ejemplo.test");
        when(auth.exigirPermiso(anyString()))
                .thenReturn(
                        new IdentidadSesion(
                                cuenta,
                                1L,
                                "SUJETO",
                                Set.of("EVALUACION_FINALIZAR", "RECUPERAR_INTENTO")));
        when(auth.exigirSesion())
                .thenReturn(
                        new IdentidadSesion(
                                cuenta,
                                1L,
                                "SUJETO",
                                Set.of("EVALUACION_FINALIZAR", "RECUPERAR_INTENTO")));
        when(alcance.estaAsignado(any(), any())).thenReturn(true);

        TestJpaEntity test = new TestJpaEntity("BFA", "Batería Factorial de Aptitudes", null, true);
        entityManager.persist(test);

        // Crear versión en estado inicial BORRADOR
        VersionFormularioJpaEntity version =
                new VersionFormularioJpaEntity(test, "1.0", "DEMO-1", "FUENTE-001");
        entityManager.persist(version);

        SubpruebaJpaEntity spVoc1 = new SubpruebaJpaEntity(test, "VOC1", "Sinónimos", "V");
        SubpruebaJpaEntity spVoc2 = new SubpruebaJpaEntity(test, "VOC2", "Palabra Excluida", "V");
        entityManager.persist(spVoc1);
        entityManager.persist(spVoc2);

        ConfiguracionSubpruebaJpaEntity c1 =
                new ConfiguracionSubpruebaJpaEntity(
                        version,
                        spVoc1,
                        1,
                        300,
                        com.universidad.tests.domain.model.TipoRespuesta.SELECCION,
                        "Instrucciones VOC1",
                        null,
                        null);
        ConfiguracionSubpruebaJpaEntity c2 =
                new ConfiguracionSubpruebaJpaEntity(
                        version,
                        spVoc2,
                        2,
                        300,
                        com.universidad.tests.domain.model.TipoRespuesta.SELECCION,
                        "Instrucciones VOC2",
                        null,
                        null);
        entityManager.persist(c1);
        entityManager.persist(c2);

        // Transicionar formalmente a PUBLICADA cumpliendo el trigger
        // comun_bloquear_estado_inicial_invalido
        version.aplicarEstado(
                com.universidad.tests.domain.model.EstadoVersionFormulario.PUBLICADA,
                Instant.now(),
                "Validacion aprobada");

        SujetoJpaEntity sujeto =
                new SujetoJpaEntity("COD_" + UUID.randomUUID(), "Nelson", "Lacayo", null);
        entityManager.persist(sujeto);

        PeriodoJpaEntity periodo =
                new PeriodoJpaEntity("PER_" + UUID.randomUUID(), "Periodo 2026", null, null, true);
        entityManager.persist(periodo);

        entityManager.flush();

        idTest = test.getIdTest();
        idVersion = version.getIdVersionFormulario();
        idConfigVoc1 = c1.getIdConfiguracionSubprueba();
        idConfigVoc2 = c2.getIdConfiguracionSubprueba();
        idSujeto = sujeto.getIdSujeto();
        idPeriodo = periodo.getIdPeriodo();
    }

    @Test
    void flujoCompletoFinalizacionYPuntajesE8E9() {
        Instant inicio = Instant.now().minusSeconds(600);
        Instant cierre = Instant.now().minusSeconds(100);

        Evaluacion evaluacion =
                new Evaluacion(
                        null,
                        idSujeto,
                        idPeriodo,
                        idTest,
                        idVersion,
                        EstadoEvaluacion.EN_CURSO,
                        inicio,
                        null,
                        List.of(
                                new EvaluacionSubprueba(
                                        null,
                                        null,
                                        idConfigVoc1,
                                        "VOC1",
                                        "Sinónimos",
                                        1,
                                        300,
                                        EstadoSubprueba.CERRADA,
                                        "Instrucciones",
                                        true,
                                        inicio,
                                        inicio.plusSeconds(300),
                                        cierre,
                                        null,
                                        75),
                                new EvaluacionSubprueba(
                                        null,
                                        null,
                                        idConfigVoc2,
                                        "VOC2",
                                        "Palabra Excluida",
                                        2,
                                        300,
                                        EstadoSubprueba.CERRADA,
                                        "Instrucciones",
                                        true,
                                        inicio.plusSeconds(300),
                                        inicio.plusSeconds(600),
                                        cierre,
                                        null,
                                        75)));

        Evaluacion guardada = evaluacionRepository.guardar(evaluacion);
        Long evaluacionId = guardada.idEvaluacion();

        // Antes de finalizar, E9 debe fallar con 409 EVALUACION_NO_FINALIZADA
        assertThatThrownBy(() -> finalizacionService.consultarResultados(evaluacionId))
                .isInstanceOf(EvaluacionNoFinalizadaException.class);

        // Finalizar con puntuaciones conocidas (E8)
        Map<String, Integer> puntuacionesDirectas = Map.of("VOC1", 12, "VOC2", 18);
        EvaluacionResponse responseE8 =
                finalizacionService.finalizar(evaluacionId, puntuacionesDirectas);

        assertThat(responseE8.estado()).isEqualTo("FINALIZADA");
        assertThat(responseE8.estadoResultado()).isEqualTo("PENDIENTE_VALIDACION");
        assertThat(responseE8.resultadoId()).isNotNull();
        assertThat(responseE8.fechaFin()).isNotNull();

        // Consultar resultados (E9)
        ResultadoResponse responseE9 = finalizacionService.consultarResultados(evaluacionId);
        assertThat(responseE9.evaluacionId()).isEqualTo(evaluacionId);
        assertThat(responseE9.estadoResultado()).isEqualTo("PENDIENTE_VALIDACION");
        assertThat(responseE9.calificacionId()).isNull();
        assertThat(responseE9.revision()).isNull();
        // Antes de publicación de la revisión vigente, factores está vacío según OpenAPI
        assertThat(responseE9.factores()).isEmpty();

        // Los factores psicométricos calculados quedan persistidos en base de datos
        Resultado resultadoBd = resultadoRepository.buscarPorEvaluacion(evaluacionId).orElseThrow();
        assertThat(resultadoBd.factores()).isNotEmpty();
        Map<String, ResultadoFactor> factoresBd =
                resultadoBd.factores().stream()
                        .collect(Collectors.toMap(ResultadoFactor::codigo, f -> f));

        // Factores directos calculados
        assertThat(factoresBd.get("VOC1").puntuacionDirecta()).isEqualTo(12);
        assertThat(factoresBd.get("VOC1").estadoCalculo()).isEqualTo("CALCULADO");
        assertThat(factoresBd.get("VOC2").puntuacionDirecta()).isEqualTo(18);
        assertThat(factoresBd.get("VOC2").estadoCalculo()).isEqualTo("CALCULADO");

        // Compuesto VOCT = VOC1 + VOC2 = 30
        assertThat(factoresBd.get("VOCT").puntuacionDirecta()).isEqualTo(30);
        assertThat(factoresBd.get("VOCT").estadoCalculo()).isEqualTo("CALCULADO");

        // Factores sin calificador quedan NO_CALCULABLE
        assertThat(factoresBd.get("CV").puntuacionDirecta()).isNull();
        assertThat(factoresBd.get("CV").estadoCalculo()).isEqualTo("NO_CALCULABLE");
        assertThat(factoresBd.get("CV").motivo()).isEqualTo("FALTA_PUNTUACION_CV");

        // Idempotencia: repetir E8 devuelve el resultado existente sin recalcular
        EvaluacionResponse repeticionE8 = finalizacionService.finalizar(evaluacionId);
        assertThat(repeticionE8.resultadoId()).isEqualTo(responseE8.resultadoId());
        assertThat(repeticionE8.fechaFin()).isEqualTo(responseE8.fechaFin());
    }

    @Test
    void e8RechazaFinalizacionSinAsignacionYNoModificaEstado() {
        when(alcance.estaAsignado(any(), any())).thenReturn(false);

        Evaluacion evaluacion =
                new Evaluacion(
                        null,
                        idSujeto,
                        idPeriodo,
                        idTest,
                        idVersion,
                        EstadoEvaluacion.EN_CURSO,
                        Instant.now(),
                        null,
                        List.of());
        Evaluacion guardada = evaluacionRepository.guardar(evaluacion);

        assertThatThrownBy(() -> finalizacionService.finalizar(guardada.idEvaluacion()))
                .isInstanceOf(SinPermisoException.class);

        Evaluacion persistida =
                evaluacionRepository.buscarPorId(guardada.idEvaluacion()).orElseThrow();
        assertThat(persistida.estado()).isEqualTo(EstadoEvaluacion.EN_CURSO);
    }
}
