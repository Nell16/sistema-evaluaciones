package com.universidad.tests.application.evaluator.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.Subprueba;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionFormulario;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BloqueNumericoVersionadoTest {

    @Test
    void desde_catalogoCompatible_conservaLaVersionYLasPreguntas() {
        var bloque =
                BloqueNumericoVersionado.desde(
                        version(100L),
                        configuracion(100L),
                        subprueba(),
                        List.of(pregunta(101L, 20L), pregunta(102L, 20L)));

        assertEquals(100L, bloque.idVersionFormulario());
        assertEquals("N1-A", bloque.codigoSubprueba());
        assertEquals(Set.of(101L, 102L), bloque.idsPreguntas());
    }

    @Test
    void desde_configuracionDeOtraVersion_rechazaElBloque() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(200L),
                                subprueba(),
                                List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_preguntaDeOtraConfiguracion_rechazaElBloque() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(100L),
                                subprueba(),
                                List.of(pregunta(101L, 99L))));
    }

    @Test
    void desde_subpruebaDiferenteALaConfiguracion_rechazaElBloque() {
        var otra = new Subprueba(99L, 10L, "N1-A", "Ficticia", null, "N1");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(100L),
                                otra,
                                List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_subpruebaDeOtroTest_rechazaElBloque() {
        var otra = new Subprueba(30L, 99L, "N1-A", "Ficticia", null, "N1");
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(100L),
                                otra,
                                List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_configuracionDeTextoLibre_rechazaElBloque() {
        var texto =
                new ConfiguracionSubprueba(
                        20L,
                        100L,
                        30L,
                        1,
                        null,
                        TipoRespuesta.TEXTO_LIBRE,
                        "instrucciones-ficticias",
                        null,
                        null);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L), texto, subprueba(), List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_preguntasDuplicadas_noLasConvierteEnUnConteoMenor() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(100L),
                                subprueba(),
                                List.of(pregunta(101L, 20L), pregunta(101L, 20L))));
    }

    @Test
    void desde_sinPreguntas_rechazaLaPreparacion() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L), configuracion(100L), subprueba(), List.of()));
    }

    @Test
    void desde_configuracionSinIdPersistido_rechazaElBloque() {
        var sinId =
                new ConfiguracionSubprueba(
                        null,
                        100L,
                        30L,
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "instrucciones-ficticias",
                        null,
                        null);
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L), sinId, subprueba(), List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_preguntaSinIdPersistido_rechazaElBloque() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(100L),
                                configuracion(100L),
                                subprueba(),
                                List.of(pregunta(null, 20L))));
    }

    @Test
    void desde_versionSinIdPersistido_rechazaElBloque() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        BloqueNumericoVersionado.desde(
                                version(null),
                                configuracion(100L),
                                subprueba(),
                                List.of(pregunta(101L, 20L))));
    }

    @Test
    void desde_catalogoMutable_conservaUnaInstantaneaInmutable() {
        var preguntas = new ArrayList<>(List.of(pregunta(101L, 20L)));
        var subprueba = subprueba();
        var bloque =
                BloqueNumericoVersionado.desde(
                        version(100L), configuracion(100L), subprueba, preguntas);
        preguntas.clear();
        subprueba.setCodigo("N2-A");

        assertEquals("N1-A", bloque.codigoSubprueba());
        assertEquals(Set.of(101L), bloque.idsPreguntas());
        assertThrows(UnsupportedOperationException.class, () -> bloque.idsPreguntas().clear());
    }

    @Test
    void desde_edicionAplicadaQueLuegoSeRetiro_conservaLaLecturaHistorica() {
        var historica = version(100L);
        historica.publicar("evidencia-ficticia");
        historica.retirar();
        var bloque =
                BloqueNumericoVersionado.desde(
                        historica, configuracion(100L), subprueba(), List.of(pregunta(101L, 20L)));
        assertEquals(100L, bloque.idVersionFormulario());
    }

    @Test
    void calificar_contextoCompatible_entregaConteosSinTruncarNiConsolidarFactores() {
        var bloque = bloqueDeDosPreguntas();
        var resultado =
                new CalificadorNumericoBfa()
                        .calificarVersionado(
                                bloque,
                                List.of(new RespuestaSeleccionBfa(101L, 11L)),
                                Map.of(101L, 11L, 102L, 22L),
                                opciones());
        assertEquals(new ConteoBloqueBfa(1, 0, 1), resultado);
    }

    @Test
    void calificar_claveIncompleta_rechazaAntesDeGenerarPuntuacion() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CalificadorNumericoBfa()
                                .calificarVersionado(
                                        bloqueDeDosPreguntas(),
                                        List.of(),
                                        Map.of(101L, 11L),
                                        opciones()));
    }

    @Test
    void calificar_claveConPreguntaFueraDelContexto_rechazaAntesDeGenerarPuntuacion() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CalificadorNumericoBfa()
                                .calificarVersionado(
                                        bloqueDeDosPreguntas(),
                                        List.of(),
                                        Map.of(101L, 11L, 102L, 22L, 999L, 99L),
                                        opciones()));
    }

    @Test
    void calificar_otroBloqueDeSeleccion_rechazaSuCodigoNoNumerico() {
        var otra = new Subprueba(30L, 10L, "R1", "Ficticia", null, "R1");
        var bloque =
                BloqueNumericoVersionado.desde(
                        version(100L),
                        configuracion(100L),
                        otra,
                        List.of(pregunta(101L, 20L), pregunta(102L, 20L)));
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new CalificadorNumericoBfa()
                                .calificarVersionado(
                                        bloque,
                                        List.of(),
                                        Map.of(101L, 11L, 102L, 22L),
                                        opciones()));
    }

    private static BloqueNumericoVersionado bloqueDeDosPreguntas() {
        return BloqueNumericoVersionado.desde(
                version(100L),
                configuracion(100L),
                subprueba(),
                List.of(pregunta(101L, 20L), pregunta(102L, 20L)));
    }

    private static List<OpcionRespuesta> opciones() {
        return List.of(
                new OpcionRespuesta(11L, 101L, "A", 1, "Alternativa ficticia", null),
                new OpcionRespuesta(12L, 101L, "B", 2, "Alternativa ficticia", null),
                new OpcionRespuesta(21L, 102L, "A", 1, "Alternativa ficticia", null),
                new OpcionRespuesta(22L, 102L, "B", 2, "Alternativa ficticia", null));
    }

    private static VersionFormulario version(Long id) {
        return new VersionFormulario(id, 10L, "edicion-sintetica", null, "fuente-ficticia");
    }

    private static ConfiguracionSubprueba configuracion(Long version) {
        return new ConfiguracionSubprueba(
                20L,
                version,
                30L,
                1,
                null,
                TipoRespuesta.SELECCION,
                "instrucciones-ficticias",
                null,
                null);
    }

    private static Subprueba subprueba() {
        return new Subprueba(30L, 10L, "N1-A", "Bloque sintetico", null, "N1");
    }

    private static Pregunta pregunta(Long id, Long configuracion) {
        return new Pregunta(id, configuracion, "ficticia-" + id, 1, "Entrada sintetica", null);
    }
}
