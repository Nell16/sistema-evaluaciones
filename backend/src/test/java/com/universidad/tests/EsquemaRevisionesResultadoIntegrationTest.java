package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** Comprueba el esquema instalado exclusivamente por Flyway sobre PostgreSQL. */
@SpringBootTest
@Transactional
class EsquemaRevisionesResultadoIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    long test, version, sujeto, periodo, resultado;

    @BeforeEach
    void preparar() {
        test =
                jdbc.queryForObject(
                        "insert into test(codigo,nombre) values (?, 'ficticio') returning id_test",
                        Long.class,
                        "ESQ_" + UUID.randomUUID());
        version =
                jdbc.queryForObject(
                        "insert into version_formulario(id_test,codigo_version,fuente_ref) values (?, '1', 'ficticia') returning id_version_formulario",
                        Long.class,
                        test);
        sujeto =
                jdbc.queryForObject(
                        "insert into sujeto(nombres,apellidos) values ('persona','ficticia') returning id_sujeto",
                        Long.class);
        periodo =
                jdbc.queryForObject(
                        "insert into periodo(codigo,nombre) values (?, 'ficticio') returning id_periodo",
                        Long.class,
                        "ESQ_" + UUID.randomUUID());
        resultado = contenedor();
    }

    long contenedor() {
        long evaluacion =
                jdbc.queryForObject(
                        "insert into evaluacion(id_sujeto,id_periodo,id_test,id_version_formulario,estado,fecha_fin) values (?,?,?,?,'FINALIZADA',statement_timestamp()) returning id_evaluacion",
                        Long.class,
                        sujeto,
                        periodo,
                        test,
                        version);
        return jdbc.queryForObject(
                "insert into resultado(id_evaluacion,estado) values (?, 'PENDIENTE_VALIDACION') returning id_resultado",
                Long.class,
                evaluacion);
    }

    long revision() {
        return jdbc.queryForObject(
                "insert into revision_resultado(id_resultado,numero_revision,estado_revision) values (?,1,'PENDIENTE') returning id_revision_resultado",
                Long.class,
                resultado);
    }

    long factor() {
        long escala =
                jdbc.queryForObject(
                        "insert into escala(id_test,codigo,nombre) values (?,'VOC1','ficticia') returning id_escala",
                        Long.class,
                        test);
        return jdbc.queryForObject(
                "insert into resultado_escala(id_revision_resultado,id_escala,orden,puntaje_bruto,estado_calculo,estado_normativo) values (?,?,1,0,'CALCULADO','SIN_BAREMO_VALIDADO') returning id_resultado_escala",
                Long.class,
                revision(),
                escala);
    }

    @Test
    void conservaCeroSinInventarUnaConversion() {
        long factor = factor();
        assertThat(
                        jdbc.queryForObject(
                                "select puntaje_bruto from resultado_escala where id_resultado_escala=?",
                                Integer.class,
                                factor))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from conversion_resultado_escala where id_resultado_escala=?",
                                Long.class,
                                factor))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from flyway_schema_history where version='202611251630' and success",
                                Long.class))
                .isEqualTo(1L);
    }

    @Test
    void referenciaVigenteDebePertenecerAlMismoResultado() {
        long rev = revision();
        long otro = contenedor();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update resultado set referencia_revision_vigente=?,estado='DISPONIBLE' where id_resultado=?",
                                        rev,
                                        otro))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void noDuplicaNumeroDeRevisionPorResultado() {
        revision();
        assertThatThrownBy(this::revision).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void validacionExigeCuentaFechaYMotivo() {
        long rev = revision();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update revision_resultado set estado_revision='VALIDADA' where id_revision_resultado=?",
                                        rev))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void claveDeCalculoNoPuedeSerHuerfana() {
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update resultado set id_clave_calculo=-1 where id_resultado=?",
                                        resultado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void revisionNoPuedeReferenciarUnBaremoInexistente() {
        long rev = revision();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update revision_resultado set id_baremo=-1 where id_revision_resultado=?",
                                        rev))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void percentilExigeUnValorAunqueElRangoExista() {
        long factor = factor();
        long baremo =
                jdbc.queryForObject(
                        "insert into baremo_bfa(nombre,version,anio_norma) values ('ficticio','1',2026) returning id_baremo",
                        Long.class);
        long rango =
                jdbc.queryForObject(
                        "insert into rango_baremo_bfa(id_baremo,id_factor,percentil,limite_inferior,limite_superior,estado_celda) values (?,'VOC1',50,0,1,'RANGO') returning id_rango",
                        Long.class,
                        baremo);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into conversion_resultado_escala(id_resultado_escala,tipo_salida,puntaje_convertido,id_rango) values (?,'PERCENTIL',null,?)",
                                        factor,
                                        rango))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void conversionNoPuedeReferenciarUnRangoInexistente() {
        long factor = factor();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into conversion_resultado_escala(id_resultado_escala,tipo_salida,puntaje_convertido,id_rango) values (?,'PERCENTIL',50,-1)",
                                        factor))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void solicitudDeRecalculoEsUnicaDentroDelResultado() {
        long rev = revision();
        jdbc.update(
                "update revision_resultado set id_solicitud='ficticia' where id_revision_resultado=?",
                rev);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "insert into revision_resultado(id_resultado,numero_revision,estado_revision,id_solicitud) values (?,2,'PENDIENTE','ficticia')",
                                        resultado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void autoriaDeRevisionNoPuedeApuntarAUnaCuentaInexistente() {
        long rev = revision();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update revision_resultado set id_cuenta_autor=-1 where id_revision_resultado=?",
                                        rev))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    long reglaUtilizada() {
        long regla =
                jdbc.queryForObject(
                        "insert into regla_calificacion(id_version_formulario,version,estado,referencia_especificacion,evidencia_aprobacion) values (?,'R-FICTICIA','APROBADA','ficticia','ficticia') returning id_regla_calificacion",
                        Long.class,
                        version);
        jdbc.update(
                "insert into revision_resultado(id_resultado,id_regla_calificacion,numero_revision,estado_revision) values (?,?,1,'CALCULADA')",
                resultado,
                regla);
        return regla;
    }

    @Test
    void reglaUtilizadaNoPuedeCambiarSuVersionHistorica() {
        long regla = reglaUtilizada();
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update regla_calificacion set version='otra' where id_regla_calificacion=?",
                                        regla))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void retirarReglaUtilizadaConservaSuIdentidad() {
        long regla = reglaUtilizada();
        jdbc.update(
                "update regla_calificacion set estado='RETIRADA' where id_regla_calificacion=?",
                regla);
        assertThat(
                        jdbc.queryForObject(
                                "select version from regla_calificacion where id_regla_calificacion=?",
                                String.class,
                                regla))
                .isEqualTo("R-FICTICIA");
    }
}
