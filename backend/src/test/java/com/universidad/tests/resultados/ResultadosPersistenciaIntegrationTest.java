package com.universidad.tests.resultados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Sesiones reales, PostgreSQL y propuesta DDL aislada; no instala migraciones en el VPS. */
@SpringBootTest
@AutoConfigureMockMvc(
        print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@Transactional
class ResultadosPersistenciaIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CuentaRepository cuentas;
    @Autowired HashContrasenasPort hash;
    @Autowired AutenticacionService autenticacion;
    long sujeto, evaluacion, version, resultado;
    Cuenta profesional;
    String token;

    @BeforeEach
    void preparar() {
        Path ddl = Path.of("../docs/modelo-datos/propuestas/comun_revisiones_resultado.sql");
        if (!Files.exists(ddl))
            ddl = Path.of("docs/modelo-datos/propuestas/comun_revisiones_resultado.sql");
        var recurso = new FileSystemResource(ddl);
        jdbc.execute(
                (ConnectionCallback<Void>)
                        c -> {
                            ScriptUtils.executeSqlScript(c, recurso);
                            return null;
                        });
        profesional = cuenta(Rol.PSICOLOGO);
        token = sesion(profesional);
        long test =
                jdbc.queryForObject(
                        "insert into test(codigo,nombre) values (?,'Test ficticio de resultados') returning id_test",
                        Long.class,
                        "LOCAL_" + UUID.randomUUID());
        version =
                jdbc.queryForObject(
                        "insert into version_formulario(id_test,codigo_version,fuente_ref) values (?,'FICTICIA-1','FICTICIA') returning id_version_formulario",
                        Long.class,
                        test);
        sujeto =
                jdbc.queryForObject(
                        "insert into sujeto(nombres,apellidos) values ('Persona','Ficticia') returning id_sujeto",
                        Long.class);
        long periodo =
                jdbc.queryForObject(
                        "insert into periodo(codigo,nombre) values (?,'Periodo ficticio') returning id_periodo",
                        Long.class,
                        "L_" + UUID.randomUUID());
        evaluacion =
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
        asignar(profesional);
    }

    Cuenta cuenta(Rol rol) {
        return cuentas.guardar(
                new Cuenta(
                        null,
                        "e9-" + UUID.randomUUID() + "@ejemplo.test",
                        hash.crear("clave-ficticia-e9".toCharArray()),
                        "Persona",
                        "Ficticia",
                        EstadoCuenta.ACTIVA,
                        Set.of(rol)));
    }

    String sesion(Cuenta c) {
        return autenticacion.abrirSesion(c.getCorreo(), "clave-ficticia-e9".toCharArray()).token();
    }

    void asignar(Cuenta c) {
        jdbc.update(
                "insert into cuenta_sujeto_asignado(id_cuenta,id_sujeto) values (?,?)",
                c.getIdCuenta(),
                sujeto);
    }

    String ruta() {
        return "/api/v1/evaluaciones/" + evaluacion + "/resultados";
    }

    long publicar(int numero, String audiencia, int directa) {
        long regla =
                jdbc.queryForObject(
                        "insert into regla_calificacion(id_version_formulario,version,estado,referencia_especificacion,evidencia_aprobacion) values (?,?, 'APROBADA','FICTICIA','FICTICIA') returning id_regla_calificacion",
                        Long.class,
                        version,
                        "R-" + numero);
        long rev =
                jdbc.queryForObject(
                        "insert into revision_resultado(id_resultado,id_regla_calificacion,numero_revision,estado_revision,id_cuenta,fecha_revision,motivo,audiencia) values (?,?,?,'VALIDADA',?,statement_timestamp(),'FICTICIA',string_to_array(?,',')) returning id_revision_resultado",
                        Long.class,
                        resultado,
                        regla,
                        numero,
                        profesional.getIdCuenta(),
                        audiencia);
        long escala =
                jdbc.queryForObject(
                        "insert into escala(id_test,codigo,nombre) select id_test,?,'Factor ficticio' from version_formulario where id_version_formulario=? returning id_escala",
                        Long.class,
                        "F-" + numero,
                        version);
        jdbc.update(
                "insert into resultado_escala(id_revision_resultado,id_escala,orden,puntaje_bruto,estado_calculo,estado_normativo) values (?,?,1,?,'CALCULADO','NO_APLICA')",
                rev,
                escala,
                directa);
        jdbc.update(
                "update resultado set referencia_revision_vigente=?,estado='DISPONIBLE' where id_resultado=?",
                rev,
                resultado);
        return rev;
    }

    @Test
    void entregaReferenciaExactaAunqueExistaRevisionMayorYConservaCero() throws Exception {
        long vigente = publicar(2, "PSICOLOGO", 0);
        publicar(3, "PSICOLOGO", 99);
        jdbc.update(
                "update resultado set referencia_revision_vigente=? where id_resultado=?",
                vigente,
                resultado);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionId").value(vigente))
                .andExpect(jsonPath("$.revision").value(2))
                .andExpect(jsonPath("$.versionReglas").value("R-2"))
                .andExpect(jsonPath("$.factores[0].puntuacionDirecta").value(0));
    }

    @Test
    void auditoriaDistingueDosRevisionesDeLaMismaEvaluacion() throws Exception {
        long primera = publicar(2, "PSICOLOGO", 12);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        long segunda = publicar(3, "PSICOLOGO", 13);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(
                        jdbc.queryForList(
                                "select entidad_id from evento_auditoria where id_evaluacion=? and accion='CONSULTAR_RESULTADO' and entidad_tipo='REVISION_RESULTADO' order by id_evento_auditoria",
                                Long.class,
                                evaluacion))
                .containsExactly(primera, segunda);
    }

    @Test
    void pendienteNoExponeFactoresInternos() throws Exception {
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revision").isEmpty())
                .andExpect(jsonPath("$.factores").isEmpty());
    }

    @Test
    void rolAsignadoFueraDeAudienciaNoLeePuntajes() throws Exception {
        publicar(2, "COORDINADOR", 12);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void sujetoNoLeePuntajesAunqueSeIncluyaEnAudiencia() throws Exception {
        publicar(2, "SUJETO,PSICOLOGO", 12);
        var c = cuenta(Rol.SUJETO);
        asignar(c);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + sesion(c)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinAsignacionNoLeeResultados() throws Exception {
        publicar(2, "PSICOLOGO", 12);
        var c = cuenta(Rol.PSICOLOGO);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + sesion(c)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesion401EIdentificadorInvalido400() throws Exception {
        mvc.perform(get(ruta())).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/v1/evaluaciones/no-valido/resultados")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revisionSinValidarNoSeEntrega() throws Exception {
        long rev = publicar(2, "PSICOLOGO", 12);
        jdbc.update(
                "update revision_resultado set estado_revision='CALCULADA' where id_revision_resultado=?",
                rev);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void evaluacionSinFinalizar409YNoRegistraConsulta() throws Exception {
        jdbc.update(
                "update evaluacion set estado='EN_CURSO',fecha_fin=null where id_evaluacion=?",
                evaluacion);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("EVALUACION_NO_FINALIZADA"));
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and accion='CONSULTAR_RESULTADO'",
                                Long.class,
                                evaluacion))
                .isZero();
    }

    @Test
    void pendienteNoExponePuntajesYAperturaQuedaAuditada() throws Exception {
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoResultado").value("PENDIENTE_VALIDACION"))
                .andExpect(jsonPath("$.revision").isEmpty())
                .andExpect(jsonPath("$.factores").isEmpty());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and id_cuenta=? and accion='CONSULTAR_RESULTADO'",
                                Long.class,
                                evaluacion,
                                profesional.getIdCuenta()))
                .isEqualTo(1L);
    }

    @Test
    void publicadoSinEstadoDisponibleNoEntregaPuntajes() throws Exception {
        publicar(2, "PSICOLOGO", 12);
        jdbc.update(
                "update resultado set estado='PENDIENTE_VALIDACION' where id_resultado=?",
                resultado);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void conversionBfaSinBaremoValidadoNoSeEntrega() throws Exception {
        identificarInstrumento("BFA");
        convertir(publicar(2, "PSICOLOGO", 12), 999);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.factores").doesNotExist());
    }

    @Test
    void conversionWeillNoUsaBaremoBfaNiExponePercentilSinVerificar() throws Exception {
        identificarInstrumento("WEILL");
        convertir(publicar(2, "PSICOLOGO", 12), 999);
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factores[0].puntuacionDirecta").value(12))
                .andExpect(jsonPath("$.factores[0].estadoNormativo").value("CONVERTIDO"))
                .andExpect(jsonPath("$.factores[0].percentil").isEmpty())
                .andExpect(jsonPath("$.factores[0].idRango").isEmpty());
    }

    private void identificarInstrumento(String codigo) {
        jdbc.update(
                "update test set codigo=? where id_test=(select id_test from version_formulario where id_version_formulario=?)",
                codigo,
                version);
    }

    private void convertir(long revision, long rango) {
        Long factor =
                jdbc.queryForObject(
                        "update resultado_escala set estado_normativo='CONVERTIDO' where id_revision_resultado=? returning id_resultado_escala",
                        Long.class,
                        revision);
        jdbc.update(
                "insert into conversion_resultado_escala(id_resultado_escala,tipo_salida,puntaje_convertido,id_rango) values (?,'PERCENTIL',50,?)",
                factor,
                rango);
    }

    @Test
    void auditoriaFallidaImpideEntregarResultados() throws Exception {
        publicar(2, "PSICOLOGO", 12);
        jdbc.execute(
                "create function fallo_lectura_local() returns trigger language plpgsql as $$ begin if NEW.accion='CONSULTAR_RESULTADO' then raise exception 'Fallo ficticio local'; end if; return NEW; end $$");
        jdbc.execute(
                "create trigger fallo_lectura_local before insert on evento_auditoria for each row execute function fallo_lectura_local()");
        mvc.perform(get(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.factores").doesNotExist());
    }
}
