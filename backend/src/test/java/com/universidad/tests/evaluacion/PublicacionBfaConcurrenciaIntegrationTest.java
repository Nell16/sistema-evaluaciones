package com.universidad.tests.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** solicitudes HTTP y transacciones independientes en un esquema exclusivo de esta clase. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicacionBfaConcurrenciaIntegrationTest extends DatosPublicacionBfaIntegration {
    private static final String ESQUEMA =
            "bfa_red_" + UUID.randomUUID().toString().replace("-", "");
    @LocalServerPort int puerto;
    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @DynamicPropertySource
    static void esquema(DynamicPropertyRegistry r) {
        r.add("spring.flyway.schemas", () -> ESQUEMA);
        r.add("spring.flyway.default-schema", () -> ESQUEMA);
        r.add("spring.datasource.hikari.schema", () -> ESQUEMA);
    }

    @AfterAll
    void limpiarEsquemaExclusivo() {
        jdbc.execute("drop schema " + ESQUEMA + " cascade");
    }

    HttpResponse<String> pedir(String metodo, String ruta, String cuerpo) throws Exception {
        return http.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + ruta))
                        .timeout(Duration.ofSeconds(30))
                        .header("Authorization", "Bearer " + profesional)
                        .header("Content-Type", "application/json")
                        .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    String publicacion(long revision, int esperada) {
        return "{\"calificacionId\":"
                + revision
                + ",\"revisionVigenteEsperada\":"
                + esperada
                + ",\"motivo\":\"ficticia\",\"audiencia\":[\"PSICOLOGO\"]}";
    }

    List<HttpResponse<String>> competir(
            Callable<HttpResponse<String>> uno, Callable<HttpResponse<String>> dos)
            throws Exception {
        var barrera = new CyclicBarrier(2);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var a =
                    pool.submit(
                            () -> {
                                barrera.await(10, java.util.concurrent.TimeUnit.SECONDS);
                                return uno.call();
                            });
            var b =
                    pool.submit(
                            () -> {
                                barrera.await(10, java.util.concurrent.TimeUnit.SECONDS);
                                return dos.call();
                            });
            return List.of(
                    a.get(45, java.util.concurrent.TimeUnit.SECONDS),
                    b.get(45, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void solicitudesConcurrentesYFalloDeAuditoriaConservanUnaRevisionPublicada() throws Exception {
        var repetidas =
                competir(
                        () -> pedir("POST", base() + "/recalculos", solicitud("concurrente")),
                        () -> pedir("POST", base() + "/recalculos", solicitud("concurrente")));
        assertThat(repetidas).extracting(HttpResponse::statusCode).containsExactly(201, 201);
        long primera = json.readTree(repetidas.get(0).body()).get("calificacionId").asLong();
        assertThat(json.readTree(repetidas.get(1).body()).get("calificacionId").asLong())
                .isEqualTo(primera);
        revisar(primera);
        publicar(primera, "null")
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isOk());
        long segunda = recalcular("segunda"), tercera = recalcular("tercera");
        revisar(segunda);
        revisar(tercera);
        var carrera =
                competir(
                        () -> pedir("PUT", base() + "/resultados/vigente", publicacion(segunda, 1)),
                        () ->
                                pedir(
                                        "PUT",
                                        base() + "/resultados/vigente",
                                        publicacion(tercera, 1)));
        assertThat(carrera)
                .extracting(HttpResponse::statusCode)
                .containsExactlyInAnyOrder(200, 409);
        long vigente =
                jdbc.queryForObject(
                        "select referencia_revision_vigente from resultado where id_evaluacion=?",
                        Long.class,
                        evaluacion);
        assertThat(vigente).isIn(segunda, tercera);
        int numero =
                jdbc.queryForObject(
                        "select numero_revision from revision_resultado where id_revision_resultado=?",
                        Integer.class,
                        vigente);
        long cuarta = recalcular("cuarta");
        revisar(cuarta);
        jdbc.execute(
                "create function fallo_publicacion_ficticio() returns trigger language plpgsql as $$ begin if NEW.accion='PUBLICAR_REVISION' then raise exception 'fallo ficticio'; end if; return NEW; end $$");
        jdbc.execute(
                "create trigger fallo_publicacion_ficticio before insert on evento_auditoria for each row execute function fallo_publicacion_ficticio()");
        var rechazada = pedir("PUT", base() + "/resultados/vigente", publicacion(cuarta, numero));
        assertThat(rechazada.statusCode()).isEqualTo(500);
        assertThat(
                        jdbc.queryForObject(
                                "select referencia_revision_vigente from resultado where id_evaluacion=?",
                                Long.class,
                                evaluacion))
                .isEqualTo(vigente);
        assertThat(
                        jdbc.queryForObject(
                                "select estado_revision from revision_resultado where id_revision_resultado=?",
                                String.class,
                                cuarta))
                .isEqualTo("CALCULADA");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and accion='PUBLICAR_REVISION'",
                                Long.class,
                                cuarta))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from evento_auditoria where entidad_id=? and accion='RECALCULAR_RESULTADO'",
                                Long.class,
                                primera))
                .isEqualTo(1L);
        var lectura = pedir("GET", base() + "/resultados", "");
        assertThat(lectura.statusCode()).isEqualTo(200);
        assertThat(json.readTree(lectura.body()).get("calificacionId").asLong()).isEqualTo(vigente);
    }
}
