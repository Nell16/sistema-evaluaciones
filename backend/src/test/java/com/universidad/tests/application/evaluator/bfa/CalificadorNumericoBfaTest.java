package com.universidad.tests.application.evaluator.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.domain.model.OpcionRespuesta;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CalificadorNumericoBfaTest {

    private final CalificadorNumericoBfa calificador = new CalificadorNumericoBfa();

    @ParameterizedTest
    @ValueSource(strings = {"N1-A", "N1-B", "N2-A", "N2-B"})
    void calificar_cadaBloqueNumerico_conservaAciertosErroresYOmisiones(String bloque) {
        int cantidad = bloque.startsWith("N1") ? 2 : 4;
        var opciones = new ArrayList<OpcionRespuesta>();
        for (long pregunta = 1; pregunta <= 3; pregunta++) {
            for (int indice = 1; indice <= cantidad; indice++) {
                opciones.add(
                        opcion(
                                pregunta * 10 + indice,
                                pregunta,
                                String.valueOf((char) ('A' + indice - 1))));
            }
        }
        var clave = Map.of(1L, 10L + cantidad, 2L, 22L, 3L, 31L);
        var respuestas =
                List.of(
                        new RespuestaSeleccionBfa(1L, 10L + cantidad),
                        new RespuestaSeleccionBfa(2L, 21L),
                        new RespuestaSeleccionBfa(3L, null));

        assertEquals(
                new ConteoBloqueBfa(1, 1, 1),
                calificador.calificar(bloque, respuestas, clave, opciones));
    }

    @Test
    void calificar_bloqueSinRespuestas_cuentaTodasComoOmisiones() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "B"));
        assertEquals(
                new ConteoBloqueBfa(0, 0, 1),
                calificador.calificar("N1-B", List.of(), Map.of(1L, 11L), opciones));
    }

    @Test
    void calificar_respuestaDuplicada_rechazaElBloque() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                "N1-A",
                                List.of(
                                        new RespuestaSeleccionBfa(1L, null),
                                        new RespuestaSeleccionBfa(1L, 11L)),
                                Map.of(1L, 11L),
                                opciones));
    }

    @Test
    void calificar_claveVacia_noProduceUnPuntajeDefinitivoCero() {
        assertThrows(
                IllegalArgumentException.class,
                () -> calificador.calificar("N1-A", List.of(), Map.of(), List.of()));
    }

    @Test
    void calificar_preguntaAjena_rechazaElBloque() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                "N1-A",
                                List.of(new RespuestaSeleccionBfa(2L, 11L)),
                                Map.of(1L, 11L),
                                opciones));
    }

    @Test
    void calificar_opcionDeOtraPregunta_rechazaLaRespuesta() {
        var opciones =
                List.of(
                        opcion(11, 1, "A"),
                        opcion(12, 1, "B"),
                        opcion(21, 2, "A"),
                        opcion(22, 2, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                "N1-A",
                                List.of(new RespuestaSeleccionBfa(1L, 21L)),
                                Map.of(1L, 11L, 2L, 22L),
                                opciones));
    }

    @Test
    void calificar_claveConOpcionAjena_rechazaLaClave() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () -> calificador.calificar("N1-A", List.of(), Map.of(1L, 99L), opciones));
    }

    @Test
    void calificar_catalogoIncompleto_rechazaElBloque() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                "N1-A", List.of(), Map.of(1L, 11L), List.of(opcion(11, 1, "A"))));
    }

    @Test
    void calificar_codigosDuplicados_rechazaElCatalogo() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "A"), opcion(13, 1, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () -> calificador.calificar("N1-A", List.of(), Map.of(1L, 11L), opciones));
    }

    @Test
    void calificar_idOpcionDuplicado_rechazaElCatalogo() {
        var opciones = List.of(opcion(11, 1, "A"), opcion(11, 1, "B"));
        assertThrows(
                IllegalArgumentException.class,
                () -> calificador.calificar("N1-A", List.of(), Map.of(1L, 11L), opciones));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N1-A", "N1-B"})
    void calificar_bloqueBinarioConAlternativaC_rechazaLaOpcion(String bloque) {
        var opciones = List.of(opcion(11, 1, "A"), opcion(12, 1, "B"), opcion(13, 1, "C"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                bloque,
                                List.of(new RespuestaSeleccionBfa(1L, 13L)),
                                Map.of(1L, 11L),
                                opciones));
    }

    @ParameterizedTest
    @ValueSource(strings = {"N2-A", "N2-B"})
    void calificar_bloqueCuatroAlternativasConE_rechazaLaOpcion(String bloque) {
        var opciones =
                List.of(
                        opcion(11, 1, "A"),
                        opcion(12, 1, "B"),
                        opcion(13, 1, "C"),
                        opcion(14, 1, "D"),
                        opcion(15, 1, "E"));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                bloque,
                                List.of(new RespuestaSeleccionBfa(1L, 15L)),
                                Map.of(1L, 11L),
                                opciones));
    }

    @ParameterizedTest
    @ValueSource(strings = {"R1", "N1", "N2", "", "n1-a"})
    void calificar_codigoNoNumerico_rechazaElBloque(String bloque) {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        calificador.calificar(
                                bloque,
                                List.of(),
                                Map.of(1L, 11L),
                                List.of(opcion(11, 1, "A"), opcion(12, 1, "B"))));
    }

    @Test
    void calificar_sumasConAciertoErrorYOmision_entregaConteosSeparados() {
        var opciones =
                List.of(
                        opcion(11, 1, "A"),
                        opcion(12, 1, "B"),
                        opcion(21, 2, "A"),
                        opcion(22, 2, "B"),
                        opcion(31, 3, "A"),
                        opcion(32, 3, "B"));
        var respuestas =
                List.of(new RespuestaSeleccionBfa(1L, 11L), new RespuestaSeleccionBfa(2L, 21L));

        assertEquals(
                new ConteoBloqueBfa(1, 1, 1),
                calificador.calificar(
                        "N1-A", respuestas, Map.of(1L, 11L, 2L, 22L, 3L, 31L), opciones));
    }

    private static OpcionRespuesta opcion(long id, long pregunta, String codigo) {
        return new OpcionRespuesta(
                id,
                pregunta,
                codigo,
                codigo.charAt(0) - 'A' + 1,
                "Alternativa sintetica " + codigo,
                null);
    }
}
