package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.application.service.ConversionPercentilBFAService;
import com.universidad.tests.application.service.ConversionPercentilBFAService.EstadoNormativo;
import com.universidad.tests.application.service.ConversionPercentilBFAService.RangoCandidato;
import com.universidad.tests.application.service.ConversionPercentilBFAService.ResultadoConversion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ConversionPercentilBFAServiceTest {

    private final ConversionPercentilBFAService servicio = new ConversionPercentilBFAService();

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    void convierteLosExtremosInclusivosYElInteriorDelRango(int puntuacion) {
        List<RangoCandidato> rangos =
                List.of(
                        new RangoCandidato(1L, "FACTOR_A", 20, 3, 5),
                        new RangoCandidato(2L, "FACTOR_B", 80, 3, 5));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", puntuacion, rangos);

        assertEquals(new ResultadoConversion(20, 1L, EstadoNormativo.CONVERTIDO, null), resultado);
    }

    @Test
    void convierteCeroCuandoExisteUnRangoPuntualParaCero() {
        List<RangoCandidato> rangos = List.of(new RangoCandidato(3L, "FACTOR_A", 10, 0, 0));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", 0, rangos);

        assertEquals(new ResultadoConversion(10, 3L, EstadoNormativo.CONVERTIDO, null), resultado);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4, 8})
    void dejaElPercentilNuloEnHuecosYFueraDeLosRangos(int puntuacion) {
        List<RangoCandidato> rangos =
                List.of(
                        new RangoCandidato(4L, "FACTOR_A", 20, 1, 3),
                        new RangoCandidato(5L, "FACTOR_A", 40, null, null),
                        new RangoCandidato(6L, "FACTOR_A", 60, 5, 7));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", puntuacion, rangos);

        assertEquals(EstadoNormativo.SIN_COINCIDENCIA, resultado.estadoNormativo());
        assertNull(resultado.percentil());
        assertNull(resultado.idRango());
        assertEquals("Ningún rango del factor contiene la puntuación.", resultado.motivo());
    }

    @Test
    void ignoraLosRangosDeOtrosFactores() {
        List<RangoCandidato> rangos = List.of(new RangoCandidato(7L, "FACTOR_B", 50, 1, 5));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", 3, rangos);

        assertEquals(EstadoNormativo.SIN_COINCIDENCIA, resultado.estadoNormativo());
        assertNull(resultado.percentil());
        assertNull(resultado.idRango());
    }

    @Test
    void dejaElPercentilNuloCuandoDosRangosCompartenElExtremo() {
        List<RangoCandidato> rangos =
                List.of(
                        new RangoCandidato(8L, "FACTOR_A", 20, 1, 5),
                        new RangoCandidato(9L, "FACTOR_A", 40, 5, 8));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", 5, rangos);

        assertEquals(EstadoNormativo.SIN_COINCIDENCIA, resultado.estadoNormativo());
        assertNull(resultado.percentil());
        assertNull(resultado.idRango());
        assertEquals("La puntuación coincide con más de un rango del factor.", resultado.motivo());
    }

    @Test
    void devuelveNoAplicaCuandoFaltaLaPuntuacionDirecta() {
        List<RangoCandidato> rangos = List.of(new RangoCandidato(10L, "FACTOR_A", 10, 0, 0));

        ResultadoConversion resultado = servicio.convertir("FACTOR_A", null, rangos);

        assertEquals(
                new ResultadoConversion(
                        null,
                        null,
                        EstadoNormativo.NO_APLICA,
                        "El factor no tiene puntuación directa."),
                resultado);
    }

    @Test
    void rechazaRangosConUnSoloLimiteOLimitesInvertidos() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RangoCandidato(11L, "FACTOR_A", 20, 3, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RangoCandidato(12L, "FACTOR_A", 20, 5, 3));
    }
}
