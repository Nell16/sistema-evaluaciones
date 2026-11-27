package com.universidad.tests.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class CalculoBfaDesdeRespuestasIntegrationTest extends DatosCalculoBfaIntegration {
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
