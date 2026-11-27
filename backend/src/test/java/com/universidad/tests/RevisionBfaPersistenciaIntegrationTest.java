package com.universidad.tests;

import static org.assertj.core.api.Assertions.*;

import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.model.ResultadoFactor;
import com.universidad.tests.domain.model.bfa.RevisionResultado;
import com.universidad.tests.domain.port.PublicacionResultadoPort;
import com.universidad.tests.domain.repository.bfa.RevisionResultadoRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RevisionBfaPersistenciaIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired RevisionResultadoRepository revisiones;
    @Autowired PublicacionResultadoPort publicacion;
    long resultado, cuenta, version;

    @BeforeEach
    void preparar() {
        long test =
                jdbc.queryForObject(
                        "insert into test(codigo,nombre) values ('BFA','ficticio') returning id_test",
                        Long.class);
        version =
                jdbc.queryForObject(
                        "insert into version_formulario(id_test,codigo_version,fuente_ref) values (?,'1','ficticia') returning id_version_formulario",
                        Long.class,
                        test);
        long sujeto =
                jdbc.queryForObject(
                        "insert into sujeto(nombres,apellidos) values ('persona','ficticia') returning id_sujeto",
                        Long.class);
        long periodo =
                jdbc.queryForObject(
                        "insert into periodo(codigo,nombre) values (?,'ficticio') returning id_periodo",
                        Long.class,
                        "REV_" + UUID.randomUUID());
        long evaluacion =
                jdbc.queryForObject(
                        "insert into evaluacion(id_sujeto,id_periodo,id_test,id_version_formulario,estado,fecha_fin) values (?,?,?,?,'FINALIZADA',statement_timestamp()) returning id_evaluacion",
                        Long.class,
                        sujeto,
                        periodo,
                        test,
                        version);
        resultado =
                jdbc.queryForObject(
                        "insert into resultado(id_evaluacion,estado) values (?,'PENDIENTE_VALIDACION') returning id_resultado",
                        Long.class,
                        evaluacion);
        cuenta =
                jdbc.queryForObject(
                        "insert into cuenta(correo,resumen_contrasena,nombres,apellidos,estado) values (?,'ficticio','persona','ficticia','ACTIVA') returning id_cuenta",
                        Long.class,
                        UUID.randomUUID() + "@ejemplo.test");
        jdbc.update(
                "insert into regla_calificacion(id_version_formulario,version,estado,referencia_especificacion,evidencia_aprobacion,referencia_implementacion) values (?,'R','APROBADA','ficticia','ficticia','bfa-verbal-v1')",
                version);
    }

    RevisionResultado nueva(String solicitud) {
        return revisiones.guardarNueva(
                new RevisionResultado(
                        null,
                        resultado,
                        1,
                        "ficticia",
                        "R",
                        null,
                        null,
                        solicitud,
                        cuenta,
                        Instant.now()));
    }

    @Test
    void publicaSnapshotDeCeroConCuentaAudienciaYValidacion() {
        var rev = nueva("uno");
        revisiones.guardarFactores(
                rev.idRevision(), List.of(ResultadoFactor.calculado("VOC1", 0, null, null)));
        publicacion.publicar(resultado, rev.idRevision(), List.of("PSICOLOGO"), "ficticia", cuenta);
        assertThat(revisiones.estaValidada(rev.idRevision())).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select id_cuenta from revision_resultado where id_revision_resultado=?",
                                Long.class,
                                rev.idRevision()))
                .isEqualTo(cuenta);
        assertThat(
                        jdbc.queryForObject(
                                "select puntaje_bruto from resultado_escala where id_revision_resultado=?",
                                Integer.class,
                                rev.idRevision()))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select referencia_revision_vigente from resultado where id_resultado=?",
                                Long.class,
                                resultado))
                .isEqualTo(rev.idRevision());
    }

    @Test
    void cabeceraSinSnapshotNoSePublica() {
        var rev = nueva("incompleta");
        assertThatThrownBy(
                        () ->
                                publicacion.publicar(
                                        resultado,
                                        rev.idRevision(),
                                        List.of("PSICOLOGO"),
                                        "ficticia",
                                        cuenta))
                .isInstanceOf(RevisionBfaException.class);
        assertThat(
                        jdbc.queryForObject(
                                "select referencia_revision_vigente from resultado where id_resultado=?",
                                Long.class,
                                resultado))
                .isNull();
    }

    @Test
    void reglaRetiradaNoCreaUnaRevisionNueva() {
        jdbc.update(
                "update regla_calificacion set estado='RETIRADA' where id_version_formulario=?",
                version);
        assertThatThrownBy(() -> nueva("retirada")).isInstanceOf(RevisionBfaException.class);
    }

    @Test
    void publicarOtraRevisionNoModificaElSnapshotHistorico() {
        var uno = nueva("uno");
        revisiones.guardarFactores(
                uno.idRevision(), List.of(ResultadoFactor.calculado("VOC1", 0, null, null)));
        publicacion.publicar(resultado, uno.idRevision(), List.of("PSICOLOGO"), "ficticia", cuenta);
        var dos = nueva("dos");
        revisiones.guardarFactores(
                dos.idRevision(), List.of(ResultadoFactor.calculado("VOC1", 2, null, null)));
        publicacion.publicar(resultado, dos.idRevision(), List.of("PSICOLOGO"), "ficticia", cuenta);
        assertThatThrownBy(
                        () ->
                                publicacion.publicar(
                                        resultado,
                                        uno.idRevision(),
                                        List.of("PSICOLOGO"),
                                        "otra",
                                        cuenta))
                .isInstanceOf(RevisionBfaException.class);
        assertThat(
                        jdbc.queryForObject(
                                "select puntaje_bruto from resultado_escala where id_revision_resultado=?",
                                Integer.class,
                                uno.idRevision()))
                .isZero();
    }
}
