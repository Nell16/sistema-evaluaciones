package com.universidad.tests.application.usecase.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.domain.model.BaremoBFA;
import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CargarBaremoDemoBfaUseCaseTest {

    /** Repositorio en memoria: guarda los baremos y asigna ids. */
    private static final class BaremosEnMemoria implements BaremoBFARepository {
        final List<BaremoBFA> datos = new ArrayList<>();

        @Override
        public boolean existePorNombreYVersion(String nombre, String version) {
            return datos.stream()
                    .anyMatch(b -> b.nombre().equals(nombre) && b.version().equals(version));
        }

        @Override
        public BaremoBFA guardar(BaremoBFA b) {
            BaremoBFA nuevo =
                    new BaremoBFA(
                            (long) datos.size() + 1,
                            b.nombre(),
                            b.version(),
                            b.anioNorma(),
                            b.poblacionReferencia(),
                            b.referenciaFuente(),
                            b.rangos());
            datos.add(nuevo);
            return nuevo;
        }
    }

    private BaremosEnMemoria baremos;
    private CargarBaremoDemoBfaUseCase cargador;

    @BeforeEach
    void setUp() {
        baremos = new BaremosEnMemoria();
        cargador = new CargarBaremoDemoBfaUseCase(baremos);
    }

    @Test
    void creaUnBaremoFicticioConUnJuegoDeRangosPorFamilia() {
        assertTrue(cargador.cargarBaremo());

        assertEquals(1, baremos.datos.size());
        BaremoBFA baremo = baremos.datos.get(0);
        assertTrue(baremo.poblacionReferencia().contains("ficticia"));
        assertEquals(
                List.of("VOC1", "VOC2", "S1", "S2", "R1", "R2", "CV", "N1", "N2", "FV"),
                baremo.rangos().stream().map(RangoBaremoBFA::idFactor).distinct().toList());
    }

    @Test
    void todosLosRangosEstanMarcadosComoFicticios() {
        cargador.cargarBaremo();

        for (RangoBaremoBFA r : baremos.datos.get(0).rangos()) {
            assertEquals("FICTICIO", r.valorOriginal());
            assertTrue(r.localizadorFuente().startsWith("demo/"));
            assertEquals(EstadoCeldaBaremo.RANGO, r.estadoCelda());
        }
    }

    @Test
    void losRangosDeCadaFamiliaCubrenDe0AlMaximoSinSolaparseNiHuecos() {
        cargador.cargarBaremo();

        Map<String, List<RangoBaremoBFA>> porFamilia =
                baremos.datos.get(0).rangos().stream()
                        .collect(Collectors.groupingBy(RangoBaremoBFA::idFactor));
        Map<String, Integer> maximos = CargarBaremoDemoBfaUseCase.puntajeMaximoPorFamilia();
        assertEquals(maximos.keySet(), porFamilia.keySet());
        porFamilia.forEach(
                (familia, rangos) -> {
                    rangos.sort(Comparator.comparingInt(RangoBaremoBFA::limiteInferior));
                    assertEquals(0, rangos.get(0).limiteInferior());
                    assertEquals(
                            maximos.get(familia), rangos.get(rangos.size() - 1).limiteSuperior());
                    for (int i = 1; i < rangos.size(); i++) {
                        assertEquals(
                                rangos.get(i - 1).limiteSuperior() + 1,
                                rangos.get(i).limiteInferior());
                        assertTrue(rangos.get(i).percentil() > rangos.get(i - 1).percentil());
                    }
                });
    }

    @Test
    void losPercentilesEstanEntre1Y99YNoSeRepitenPorFamilia() {
        cargador.cargarBaremo();

        Map<String, List<RangoBaremoBFA>> porFamilia =
                baremos.datos.get(0).rangos().stream()
                        .collect(Collectors.groupingBy(RangoBaremoBFA::idFactor));
        porFamilia.forEach(
                (familia, rangos) -> {
                    assertEquals(
                            rangos.size(),
                            rangos.stream().map(RangoBaremoBFA::percentil).distinct().count());
                    rangos.forEach(r -> assertTrue(r.percentil() >= 1 && r.percentil() <= 99));
                });
    }

    @Test
    void lasFamiliasCompuestasSumanSusSubpruebasYFvUsaSuPropiaEscala() {
        Map<String, Integer> maximos = CargarBaremoDemoBfaUseCase.puntajeMaximoPorFamilia();

        assertEquals(6, maximos.get("S1"));
        assertEquals(6, maximos.get("N1"));
        assertEquals(6, maximos.get("N2"));
        assertEquals(3, maximos.get("VOC1"));
        assertEquals(75, maximos.get("FV"));
    }

    @Test
    void cargarDosVecesNoDuplicaElBaremo() {
        assertTrue(cargador.cargarBaremo());

        assertFalse(cargador.cargarBaremo());

        assertEquals(1, baremos.datos.size());
    }
}
