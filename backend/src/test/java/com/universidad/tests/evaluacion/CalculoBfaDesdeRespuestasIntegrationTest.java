package com.universidad.tests.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Clave y respuestas ficticias sobre las tablas de claves que ya crea Flyway. */
@SpringBootTest
@AutoConfigureMockMvc(
        print = org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
@Transactional
class CalculoBfaDesdeRespuestasIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired CuentaRepository cuentas;
    @Autowired HashContrasenasPort hash;
    @Autowired AutenticacionService auth;
    long evaluacion, version, clave;
    String token;

    @BeforeEach
    void preparar() {
        var cuenta =
                cuentas.guardar(
                        new Cuenta(
                                null,
                                "calculo-" + UUID.randomUUID() + "@ejemplo.test",
                                hash.crear("ficticia-calculo-bfa".toCharArray()),
                                "Persona",
                                "Ficticia",
                                EstadoCuenta.ACTIVA,
                                Set.of(Rol.SUJETO)));
        token = auth.abrirSesion(cuenta.getCorreo(), "ficticia-calculo-bfa".toCharArray()).token();
        long test =
                jdbc.queryForObject(
                        "insert into test(codigo,nombre) values ('BFA','Test ficticio') returning id_test",
                        Long.class);
        version =
                jdbc.queryForObject(
                        "insert into version_formulario(id_test,codigo_version,fuente_ref) values (?,'FICTICIA-1','FICTICIA') returning id_version_formulario",
                        Long.class,
                        test);
        clave =
                jdbc.queryForObject(
                        "insert into version_clave_bfa(id_version_formulario,version,estado,referencia_fuente,evidencia_aprobacion) values (?,'FICTICIA-1','APROBADA','FICTICIA','FICTICIA') returning id_clave",
                        Long.class,
                        version);
        long sujeto =
                jdbc.queryForObject(
                        "insert into sujeto(nombres,apellidos) values ('Persona','Ficticia') returning id_sujeto",
                        Long.class);
        long periodo =
                jdbc.queryForObject(
                        "insert into periodo(codigo,nombre) values (?,'Ficticio') returning id_periodo",
                        Long.class,
                        "P_" + UUID.randomUUID());
        evaluacion =
                jdbc.queryForObject(
                        "insert into evaluacion(id_sujeto,id_periodo,id_test,id_version_formulario,estado) values (?,?,?,?,'EN_CURSO') returning id_evaluacion",
                        Long.class,
                        sujeto,
                        periodo,
                        test,
                        version);
        jdbc.update(
                "insert into cuenta_sujeto_asignado(id_cuenta,id_sujeto) values (?,?)",
                cuenta.getIdCuenta(),
                sujeto);
        for (int bloque = 0; bloque < 2; bloque++) {
            String codigo = bloque == 0 ? "VOC1" : "VOC2";
            long sp =
                    jdbc.queryForObject(
                            "insert into subprueba(id_test,codigo,nombre) values (?,?, 'Bloque ficticio') returning id_subprueba",
                            Long.class,
                            test,
                            codigo);
            long config =
                    jdbc.queryForObject(
                            "insert into configuracion_subprueba(id_version_formulario,id_subprueba,orden_aplicacion,tipo_respuesta,instrucciones_ref,tiempo_limite_segundos,practica_ref) values (?,?,?,'SELECCION','FICTICIA',300,'PRACTICA FICTICIA SEPARADA') returning id_configuracion_subprueba",
                            Long.class,
                            version,
                            sp,
                            bloque + 1);
            jdbc.update(
                    "insert into evaluacion_subprueba(id_evaluacion,id_configuracion_subprueba,codigo,nombre,orden,duracion_segundos,estado) values (?,?,?,'Bloque ficticio',?,300,'PENDIENTE')",
                    evaluacion,
                    config,
                    codigo,
                    bloque + 1);
            for (int i = 1; i <= 3; i++) {
                String numero = String.valueOf(i);
                long pregunta =
                        jdbc.queryForObject(
                                "insert into pregunta(id_configuracion_subprueba,numero_visible,orden_presentacion,enunciado) values (?,?,?,'Contenido ficticio') returning id_pregunta",
                                Long.class,
                                config,
                                numero,
                                i + 1);
                long correcta = 0, elegida = 0;
                for (int j = 0; j < 5; j++) {
                    long opcion =
                            jdbc.queryForObject(
                                    "insert into opcion_respuesta(id_pregunta,codigo,orden_presentacion,texto) values (?,?,?,'Opcion ficticia') returning id_opcion",
                                    Long.class,
                                    pregunta,
                                    String.valueOf((char) ('A' + j)),
                                    j + 1);
                    if (j == 0) correcta = opcion;
                    if ((i == 2 && j == 1) || (i != 2 && j == 0)) elegida = opcion;
                }
                jdbc.update(
                        "insert into entrada_clave_bfa(id_clave,id_pregunta,id_opcion_correcta) values (?,?,?)",
                        clave,
                        pregunta,
                        correcta);
                if (i != 3)
                    jdbc.update(
                            "insert into respuesta_evaluacion(id_evaluacion,subprueba_codigo,id_pregunta,id_opcion,orden_entrada) values (?,?,?,?,1)",
                            evaluacion,
                            codigo,
                            pregunta,
                            elegida);
            }
        }
        jdbc.update(
                "update version_formulario set estado='PUBLICADA',publicada_en=statement_timestamp(),evidencia_validacion='FICTICIA' where id_version_formulario=?",
                version);
        jdbc.update(
                "update evaluacion_subprueba set estado='CERRADA',fecha_inicio=statement_timestamp()-interval '5 minutes',fecha_fin_limite=statement_timestamp(),fecha_cierre=statement_timestamp() where id_evaluacion=?",
                evaluacion);
    }

    String ruta() {
        return "/api/v1/evaluaciones/" + evaluacion + "/finalizar";
    }

    @Test
    void e8CalculaDesdeCatalogoClaveAprobadaConPracticaSeparadaYRepiteSinRecalcular()
            throws Exception {
        String primero =
                mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(
                        jdbc.queryForObject(
                                "select id_clave_calculo from resultado where id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isEqualTo(clave);
        assertThat(pd("VOC1")).isEqualTo(1);
        assertThat(pd("VOC2")).isEqualTo(1);
        assertThat(pd("VOCT")).isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from resultado_factor f join resultado r using(id_resultado) where r.id_evaluacion=? and f.codigo='CV' and estado_calculo='NO_CALCULABLE' and puntuacion_directa is null",
                                Long.class,
                                evaluacion))
                .isEqualTo(1L);
        String repetido =
                mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(repetido).isEqualTo(primero);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from resultado where id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isEqualTo(1L);
    }

    @Test
    void claveBorradorNoGeneraPuntuaciones() throws Exception {
        jdbc.update(
                "update version_clave_bfa set estado='BORRADOR',evidencia_aprobacion=null where id_clave=?",
                clave);
        mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(pd("VOC1")).isNull();
        assertThat(pd("VOC2")).isNull();
    }

    @Test
    void claveAprobadaIncompletaNoSeUsaParaCalcular() throws Exception {
        jdbc.update(
                "delete from entrada_clave_bfa where id_clave=? and id_pregunta in (select id_pregunta from pregunta where numero_visible='1')",
                clave);
        mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
    }

    Integer pd(String codigo) {
        return jdbc.queryForObject(
                "select f.puntuacion_directa from resultado_factor f join resultado r using(id_resultado) where r.id_evaluacion=? and f.codigo=?",
                Integer.class,
                evaluacion,
                codigo);
    }

    @Test
    void respuestaDeOtraConfiguracionNoSeCalificaComoPropia() throws Exception {
        Long pregunta =
                jdbc.queryForObject(
                        "select p.id_pregunta from pregunta p join configuracion_subprueba c using(id_configuracion_subprueba) join subprueba s using(id_subprueba) where c.id_version_formulario=? and s.codigo='VOC2' and p.numero_visible='1'",
                        Long.class,
                        version);
        Long opcion =
                jdbc.queryForObject(
                        "select id_opcion_correcta from entrada_clave_bfa where id_clave=? and id_pregunta=?",
                        Long.class,
                        clave,
                        pregunta);
        jdbc.update(
                "insert into respuesta_evaluacion(id_evaluacion,subprueba_codigo,id_pregunta,id_opcion,orden_entrada) values (?,'VOC1',?,?,1)",
                evaluacion,
                pregunta,
                opcion);
        mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError());
    }
}
