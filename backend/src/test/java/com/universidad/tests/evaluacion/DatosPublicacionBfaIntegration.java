package com.universidad.tests.evaluacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.tests.domain.model.*;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;

abstract class DatosPublicacionBfaIntegration extends DatosCalculoBfaIntegration {
    String profesional;
    long cuentaProfesional, palabra, baremo;
    ObjectMapper json = new ObjectMapper();

    @Override
    void prepararExtras(long test) {
        long sp =
                jdbc.queryForObject(
                        "insert into subprueba(id_test,codigo,nombre) values (?,'FV','ficticia') returning id_subprueba",
                        Long.class,
                        test);
        long config =
                jdbc.queryForObject(
                        "insert into configuracion_subprueba(id_version_formulario,id_subprueba,orden_aplicacion,tipo_respuesta,instrucciones_ref,tiempo_limite_segundos) values (?,?,3,'TEXTO_LIBRE','ficticia',300) returning id_configuracion_subprueba",
                        Long.class,
                        version,
                        sp);
        jdbc.update(
                "insert into evaluacion_subprueba(id_evaluacion,id_configuracion_subprueba,codigo,nombre,orden,duracion_segundos,estado) values (?,?,'FV','ficticia',3,300,'PENDIENTE')",
                evaluacion,
                config);
        palabra =
                jdbc.queryForObject(
                        "insert into respuesta_evaluacion(id_evaluacion,subprueba_codigo,valor_texto,orden_entrada) values (?,'FV','palabra ficticia',1) returning id_respuesta_evaluacion",
                        Long.class,
                        evaluacion);
    }

    @BeforeEach
    void prepararPublicacion() throws Exception {
        mvc.perform(post(ruta()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        var cuenta =
                cuentas.guardar(
                        new Cuenta(
                                null,
                                "publicacion-" + UUID.randomUUID() + "@ejemplo.test",
                                hash.crear("ficticia-publicacion".toCharArray()),
                                "persona",
                                "ficticia",
                                EstadoCuenta.ACTIVA,
                                Set.of(Rol.PSICOLOGO)));
        cuentaProfesional = cuenta.getIdCuenta();
        profesional =
                auth.abrirSesion(cuenta.getCorreo(), "ficticia-publicacion".toCharArray()).token();
        jdbc.update(
                "insert into cuenta_sujeto_asignado(id_cuenta,id_sujeto) select ?,id_sujeto from evaluacion where id_evaluacion=?",
                cuentaProfesional,
                evaluacion);
        jdbc.update(
                "insert into regla_calificacion(id_version_formulario,version,estado,referencia_especificacion,evidencia_aprobacion,referencia_implementacion) values (?,'R-FICTICIA','APROBADA','ficticia','ficticia','bfa-verbal-v1')",
                version);
        baremo =
                jdbc.queryForObject(
                        "insert into baremo_bfa(nombre,version,anio_norma) values (?,'1',2026) returning id_baremo",
                        Long.class,
                        "ficticio-" + UUID.randomUUID());
        jdbc.update(
                "insert into rango_baremo_bfa(id_baremo,id_factor,percentil,limite_inferior,limite_superior,estado_celda) values (?,'VOC1',50,1,1,'RANGO')",
                baremo);
        jdbc.update(
                "insert into asignacion_baremo_bfa(id_baremo,id_version_formulario,version_reglas,estado,evidencia_aprobacion) values (?,?,'R-FICTICIA','APROBADO','ficticia')",
                baremo,
                version);
    }

    String base() {
        return "/api/v1/evaluaciones/" + evaluacion;
    }

    String solicitud(String id) {
        return "{\"motivo\":\"ficticio\",\"versionReglas\":\"R-FICTICIA\",\"idSolicitud\":\""
                + id
                + "\"}";
    }

    long recalcular(String id) throws Exception {
        String r =
                mvc.perform(
                                post(base() + "/recalculos")
                                        .header("Authorization", "Bearer " + profesional)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(solicitud(id)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return json.readTree(r).get("calificacionId").asLong();
    }

    void revisar(long revision) throws Exception {
        mvc.perform(
                        put(base()
                                        + "/calificaciones/"
                                        + revision
                                        + "/palabras/"
                                        + palabra
                                        + "/revision")
                                .header("Authorization", "Bearer " + profesional)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"decision\":\"VALIDA\",\"motivo\":\"revisión ficticia\"}"))
                .andExpect(status().isOk());
    }

    org.springframework.test.web.servlet.ResultActions publicar(long revision, String esperada)
            throws Exception {
        return mvc.perform(
                put(base() + "/resultados/vigente")
                        .header("Authorization", "Bearer " + profesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"calificacionId\":"
                                        + revision
                                        + ",\"revisionVigenteEsperada\":"
                                        + esperada
                                        + ",\"motivo\":\"validación ficticia\",\"audiencia\":[\"PSICOLOGO\"]}"));
    }
}
