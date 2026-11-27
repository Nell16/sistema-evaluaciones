package com.universidad.tests.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.domain.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PublicacionBfaIntegrationTest extends DatosPublicacionBfaIntegration {
    @Test
    void flujoRealE8E11E10E12E9ConservaRevisionYConversionExactas() throws Exception {
        long revision = recalcular("primera");
        revisar(revision);
        publicar(revision, "null").andExpect(status().isOk());
        mvc.perform(get(base() + "/resultados").header("Authorization", "Bearer " + profesional))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calificacionId").value(revision))
                .andExpect(jsonPath("$.idClave").value(clave))
                .andExpect(jsonPath("$.idBaremo").value(baremo))
                .andExpect(
                        jsonPath("$.factores[?(@.codigo=='VOC1')].percentil")
                                .value(org.hamcrest.Matchers.contains(50)))
                .andExpect(
                        jsonPath("$.factores[?(@.codigo=='VOCT')].puntuacionDirecta")
                                .value(org.hamcrest.Matchers.contains(2)))
                .andExpect(
                        jsonPath("$.factores[?(@.codigo=='FV')].puntuacionDirecta")
                                .value(
                                        org.hamcrest.Matchers.contains(
                                                org.hamcrest.Matchers.nullValue())));
        publicar(revision, "null").andExpect(status().isOk());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and accion='PUBLICAR_REVISION'",
                                Long.class,
                                revision))
                .isEqualTo(1L);
        mvc.perform(get(base() + "/resultados").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void recalcIdempotenteNoDuplicaSnapshotNiAuditoria() throws Exception {
        long primera = recalcular("repetida");
        long segunda = recalcular("repetida");
        assertThat(segunda).isEqualTo(primera);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from revision_resultado where id_revision_resultado=?",
                                Long.class,
                                primera))
                .isEqualTo(1L);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and accion='RECALCULAR_RESULTADO'",
                                Long.class,
                                primera))
                .isEqualTo(1L);
    }

    @Test
    void palabrasPendientesImpidenPublicar() throws Exception {
        long revision = recalcular("pendiente");
        publicar(revision, "null")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISION_NO_PUBLICABLE"));
        assertThat(
                        jdbc.queryForObject(
                                "select estado_revision from revision_resultado where id_revision_resultado=?",
                                String.class,
                                revision))
                .isEqualTo("CALCULADA");
    }

    @Test
    void revisionViejaNoSePuedeModificarDespuesDePublicarOtra() throws Exception {
        long primera = recalcular("primera");
        revisar(primera);
        publicar(primera, "null").andExpect(status().isOk());
        long segunda = recalcular("segunda");
        revisar(segunda);
        publicar(segunda, "null")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("REVISION_VIGENTE_CAMBIO"));
        publicar(segunda, "1").andExpect(status().isOk());
        mvc.perform(
                        put(base()
                                        + "/calificaciones/"
                                        + primera
                                        + "/palabras/"
                                        + palabra
                                        + "/revision")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"decision\":\"INVALIDA\",\"motivo\":\"ficticia\"}"))
                .andExpect(status().isConflict());
        assertThat(
                        jdbc.queryForObject(
                                "select estado_revision from revision_resultado where id_revision_resultado=?",
                                String.class,
                                primera))
                .isEqualTo("VALIDADA");
    }

    @Test
    void sinAlcanceNoCreaNiPublicaRevisiones() throws Exception {
        jdbc.update("delete from cuenta_sujeto_asignado where id_cuenta=?", cuentaProfesional);
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud("ajena")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reglaNoAprobadaNoProduceRevision() throws Exception {
        jdbc.update(
                "update regla_calificacion set estado='BORRADOR' where id_version_formulario=?",
                version);
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud("sin-regla")))
                .andExpect(status().isBadRequest());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from revision_resultado r join resultado c using(id_resultado) where c.id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isZero();
    }

    @Test
    void implementacionDesconocidaNoSePresentaComoCalculoVerificado() throws Exception {
        jdbc.update(
                "update regla_calificacion set referencia_implementacion='otra-implementacion' where id_version_formulario=?",
                version);
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud("otra-regla")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identificadorDeSolicitudDemasiadoLargoEs400() throws Exception {
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud("x".repeat(81))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void primeraRevisionSinVersionExplicitaUsaLaUnicaReglaAprobada() throws Exception {
        String cuerpo = solicitud("automatica").replace("\"versionReglas\":\"R-FICTICIA\",", "");
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isCreated());
        assertThat(
                        jdbc.queryForObject(
                                "select r.id_baremo from revision_resultado r join resultado c using(id_resultado) where c.id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isEqualTo(baremo);
    }

    @Test
    void variasReglasAprobadasExigenSeleccionExplicita() throws Exception {
        jdbc.update(
                "insert into regla_calificacion(id_version_formulario,version,estado,referencia_especificacion,evidencia_aprobacion,referencia_implementacion) values (?,'OTRA','APROBADA','ficticia','ficticia','bfa-verbal-v1')",
                version);
        String cuerpo = solicitud("ambigua").replace("\"versionReglas\":\"R-FICTICIA\",", "");
        mvc.perform(
                        post(base() + "/recalculos")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest());
    }
}
