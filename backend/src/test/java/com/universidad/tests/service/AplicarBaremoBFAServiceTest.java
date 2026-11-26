package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.universidad.tests.application.service.AplicarBaremoBFAService;
import com.universidad.tests.application.service.AplicarBaremoBFAService.ResultadoAplicacion;
import com.universidad.tests.application.service.AplicarBaremoBFAService.ResultadoRevision;
import com.universidad.tests.application.service.ConversionPercentilBFAService;
import com.universidad.tests.application.service.ConversionPercentilBFAService.EstadoNormativo;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort.BaremoCandidato;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort.RangoCandidato;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AplicarBaremoBFAServiceTest {

    private static final long VERSION_FORMULARIO = 12L;
    private static final String VERSION_REGLAS = "reglas-sinteticas-v1";
    private static final String FACTOR = "FACTOR_A";

    private final ConversionPercentilBFAService conversion = new ConversionPercentilBFAService();

    @Test
    void consultaLaEdicionYLasReglasYConservaLosIdsDeLaConversion() {
        ConsultaBaremoBFAParaConversionPort consulta =
                (versionFormularioId, versionReglas) -> {
                    assertEquals(VERSION_FORMULARIO, versionFormularioId);
                    assertEquals(VERSION_REGLAS, versionReglas);
                    return List.of(
                            new BaremoCandidato(
                                    21L, List.of(new RangoCandidato(31L, FACTOR, 40, 3, 5))));
                };

        AplicarBaremoBFAService servicio = new AplicarBaremoBFAService(consulta, conversion);

        ResultadoAplicacion resultado =
                servicio.aplicar(VERSION_FORMULARIO, VERSION_REGLAS, FACTOR, 5);

        assertEquals(21L, resultado.idBaremo());
        assertEquals(31L, resultado.conversion().idRango());
        assertEquals(40, resultado.conversion().percentil());
        assertEquals(EstadoNormativo.CONVERTIDO, resultado.conversion().estadoNormativo());
    }

    @Test
    void devuelveSinBaremoValidadoCuandoNoHayCandidatos() {
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService((formulario, reglas) -> List.of(), conversion);

        ResultadoAplicacion resultado =
                servicio.aplicar(VERSION_FORMULARIO, VERSION_REGLAS, FACTOR, 5);

        assertNull(resultado.idBaremo());
        assertNull(resultado.conversion().idRango());
        assertNull(resultado.conversion().percentil());
        assertEquals(EstadoNormativo.SIN_BAREMO_VALIDADO, resultado.conversion().estadoNormativo());
        assertNotNull(resultado.conversion().motivo());
    }

    @Test
    void dejaLaConversionPendienteDeSeleccionCuandoHayVariosBaremos() {
        List<RangoCandidato> rangos = List.of(new RangoCandidato(31L, FACTOR, 40, 3, 5));
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService(
                        (formulario, reglas) ->
                                List.of(
                                        new BaremoCandidato(21L, rangos),
                                        new BaremoCandidato(
                                                22L,
                                                List.of(
                                                        new RangoCandidato(
                                                                32L, FACTOR, 60, 3, 5)))),
                        conversion);

        ResultadoAplicacion resultado =
                servicio.aplicar(VERSION_FORMULARIO, VERSION_REGLAS, FACTOR, 5);

        assertNull(resultado.idBaremo());
        assertNull(resultado.conversion().idRango());
        assertNull(resultado.conversion().percentil());
        assertEquals(EstadoNormativo.SIN_BAREMO_VALIDADO, resultado.conversion().estadoNormativo());
        assertEquals(
                "Hay más de un baremo aprobado y compatible para este cálculo.",
                resultado.conversion().motivo());
    }

    @Test
    void conservaElBaremoCuandoElFactorNoTieneRangosAplicables() {
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService(
                        (formulario, reglas) -> List.of(new BaremoCandidato(21L, List.of())),
                        conversion);

        ResultadoAplicacion resultado =
                servicio.aplicar(VERSION_FORMULARIO, VERSION_REGLAS, FACTOR, 5);

        assertEquals(21L, resultado.idBaremo());
        assertNull(resultado.conversion().idRango());
        assertNull(resultado.conversion().percentil());
        assertEquals(EstadoNormativo.SIN_COINCIDENCIA, resultado.conversion().estadoNormativo());
    }

    @Test
    void omiteLaConsultaCuandoElFactorNoTienePuntuacionDirecta() {
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService(
                        (formulario, reglas) ->
                                fail("Se consultaron baremos para un factor sin puntuación."),
                        conversion);

        ResultadoAplicacion resultado =
                servicio.aplicar(VERSION_FORMULARIO, VERSION_REGLAS, FACTOR, null);

        assertNull(resultado.idBaremo());
        assertNull(resultado.conversion().percentil());
        assertEquals(EstadoNormativo.NO_APLICA, resultado.conversion().estadoNormativo());
    }

    @Test
    void consultaUnaSolaVezYUsaElMismoBaremoParaTodaLaRevision() {
        AtomicInteger consultas = new AtomicInteger();
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService(
                        (formulario, reglas) -> {
                            consultas.incrementAndGet();
                            return List.of(
                                    new BaremoCandidato(
                                            21L,
                                            List.of(
                                                    new RangoCandidato(31L, FACTOR, 40, 3, 5),
                                                    new RangoCandidato(
                                                            32L, "FACTOR_B", 70, 7, 9))));
                        },
                        conversion);

        Map<String, Integer> puntuaciones = new HashMap<>();
        puntuaciones.put(FACTOR, 5);
        puntuaciones.put("FACTOR_B", 8);
        puntuaciones.put("FACTOR_C", 2);
        puntuaciones.put("FACTOR_D", null);

        ResultadoRevision resultado =
                servicio.aplicarRevision(VERSION_FORMULARIO, VERSION_REGLAS, puntuaciones);

        assertEquals(1, consultas.get());
        assertEquals(21L, resultado.idBaremo());
        assertEquals(31L, resultado.factores().get(FACTOR).idRango());
        assertEquals(40, resultado.factores().get(FACTOR).percentil());
        assertEquals(32L, resultado.factores().get("FACTOR_B").idRango());
        assertEquals(70, resultado.factores().get("FACTOR_B").percentil());
        assertEquals(
                EstadoNormativo.SIN_COINCIDENCIA,
                resultado.factores().get("FACTOR_C").estadoNormativo());
        assertNull(resultado.factores().get("FACTOR_C").percentil());
        assertEquals(
                EstadoNormativo.NO_APLICA, resultado.factores().get("FACTOR_D").estadoNormativo());
    }

    @Test
    void distingueUnFactorSinPuntuacionCuandoLaRevisionNoTieneBaremoValidado() {
        AplicarBaremoBFAService servicio =
                new AplicarBaremoBFAService((formulario, reglas) -> List.of(), conversion);
        Map<String, Integer> puntuaciones = new HashMap<>();
        puntuaciones.put(FACTOR, 5);
        puntuaciones.put("FACTOR_B", null);

        ResultadoRevision resultado =
                servicio.aplicarRevision(VERSION_FORMULARIO, VERSION_REGLAS, puntuaciones);

        assertNull(resultado.idBaremo());
        assertEquals(
                EstadoNormativo.SIN_BAREMO_VALIDADO,
                resultado.factores().get(FACTOR).estadoNormativo());
        assertNull(resultado.factores().get(FACTOR).percentil());
        assertEquals(
                EstadoNormativo.NO_APLICA, resultado.factores().get("FACTOR_B").estadoNormativo());
    }
}
