package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.repository.ResultadoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ResultadoActualizacionIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ResultadoRepository resultados;

    @Test
    void actualizaContenedorExistenteYReemplazaFactoresSinDuplicar() {
        long test =
                jdbc.queryForObject(
                        "insert into test(codigo,nombre) values (?, 'test ficticio') returning id_test",
                        Long.class,
                        "REG_" + UUID.randomUUID());
        long version =
                jdbc.queryForObject(
                        "insert into version_formulario(id_test,codigo_version,fuente_ref) values (?, '1', 'ficticia') returning id_version_formulario",
                        Long.class,
                        test);
        long sujeto =
                jdbc.queryForObject(
                        "insert into sujeto(nombres,apellidos) values ('persona','ficticia') returning id_sujeto",
                        Long.class);
        long periodo =
                jdbc.queryForObject(
                        "insert into periodo(codigo,nombre) values (?, 'ficticio') returning id_periodo",
                        Long.class,
                        "REG_" + UUID.randomUUID());
        long evaluacion =
                jdbc.queryForObject(
                        "insert into evaluacion(id_sujeto,id_periodo,id_test,id_version_formulario,estado,fecha_fin) values (?,?,?,?,'FINALIZADA',statement_timestamp()) returning id_evaluacion",
                        Long.class,
                        sujeto,
                        periodo,
                        test,
                        version);
        long contenedor =
                jdbc.queryForObject(
                        "insert into resultado(id_evaluacion,estado) values (?, 'PENDIENTE_VALIDACION') returning id_resultado",
                        Long.class,
                        evaluacion);
        Instant creado =
                jdbc.queryForObject(
                                "select creado_en from resultado where id_resultado=?",
                                java.sql.Timestamp.class,
                                contenedor)
                        .toInstant();

        Resultado primero =
                resultados.guardar(
                        Resultado.nuevo(
                                evaluacion,
                                version,
                                Instant.now(),
                                List.of(
                                        ResultadoFactor.calculado("VOC1", 0, null, null),
                                        ResultadoFactor.noCalculable(
                                                "CV", "sin datos ficticios"))));
        Resultado segundo =
                resultados.guardar(
                        Resultado.nuevo(
                                evaluacion,
                                version,
                                Instant.now(),
                                List.of(ResultadoFactor.calculado("VOC1", 2, null, null))));

        assertThat(primero.idResultado()).isEqualTo(contenedor);
        assertThat(primero.factores())
                .filteredOn(f -> f.codigo().equals("VOC1"))
                .extracting(ResultadoFactor::puntuacionDirecta)
                .containsExactly(0);
        assertThat(segundo.idResultado()).isEqualTo(contenedor);
        assertThat(segundo.creadoEn()).isEqualTo(creado);
        assertThat(segundo.referenciaRevisionVigente()).isNull();
        assertThat(segundo.versionFormularioId()).isEqualTo(version);
        assertThat(segundo.factores()).extracting(ResultadoFactor::codigo).containsExactly("VOC1");
        assertThat(resultados.buscarPorEvaluacion(evaluacion).orElseThrow().factores())
                .extracting(ResultadoFactor::puntuacionDirecta)
                .containsExactly(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from resultado where id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isEqualTo(1L);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from resultado_factor where id_resultado=?",
                                Long.class,
                                contenedor))
                .isEqualTo(1L);
    }
}
