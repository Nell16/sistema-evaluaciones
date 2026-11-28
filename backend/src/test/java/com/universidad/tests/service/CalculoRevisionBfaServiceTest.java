package com.universidad.tests.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.universidad.tests.application.service.*;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CalculoRevisionBfaServiceTest {
    CalculoPuntuacionesBfaService entradas;
    CalculoRevisionBfaService servicio;
    List<BaremoCandidato> candidatos;

    @BeforeEach
    void preparar() {
        entradas = mock(CalculoPuntuacionesBfaService.class);
        when(entradas.calcular(10L, 20L))
                .thenReturn(
                        Optional.of(
                                new CalculoPuntuacionesBfaService.Calculo(
                                        Map.of("VOC1", 0, "VOC2", 1), 7L)));
        candidatos =
                new ArrayList<>(
                        List.of(
                                new BaremoCandidato(
                                        8L, List.of(new RangoCandidato(30L, "VOC1", 50, 0, 0)))));
        servicio =
                new CalculoRevisionBfaService(
                        entradas,
                        new AplicarBaremoBFAService(
                                (version, reglas) -> List.copyOf(candidatos),
                                new ConversionPercentilBFAService()));
    }

    @Test
    void conservaCeroYTrazaLaConversionSinInventarLosFaltantes() {
        var calculo = servicio.preparar(10L, 20L, "ficticia", 7L, 8L);
        assertThat(calculo.idClave()).isEqualTo(7L);
        assertThat(calculo.idBaremo()).isEqualTo(8L);
        var vocabulario =
                calculo.factores().stream()
                        .filter(f -> f.codigo().equals("VOC1"))
                        .findFirst()
                        .orElseThrow();
        assertThat(vocabulario.puntuacionDirecta()).isZero();
        assertThat(vocabulario.percentil()).isEqualTo(50);
        assertThat(vocabulario.idRango()).isEqualTo(30L);
        assertThat(calculo.factores())
                .filteredOn(f -> f.codigo().equals("VOCT"))
                .extracting(f -> f.puntuacionDirecta())
                .containsExactly(1);
        assertThat(calculo.factores())
                .filteredOn(f -> f.codigo().equals("FV"))
                .allSatisfy(
                        f -> {
                            assertThat(f.puntuacionDirecta()).isNull();
                            assertThat(f.estadoCalculo()).isEqualTo("NO_CALCULABLE");
                        });
    }

    @Test
    void rechazaClaveDistintaDeLaUsadaPorElCalificador() {
        assertThatThrownBy(() -> servicio.preparar(10L, 20L, "ficticia", 99L, null))
                .isInstanceOf(RevisionBfaException.class);
    }

    @Test
    void rechazaBaremoPedidoSinAsignacionCompatible() {
        assertThatThrownBy(() -> servicio.preparar(10L, 20L, "ficticia", null, 99L))
                .isInstanceOf(RevisionBfaException.class);
    }

    @Test
    void faltaDeBaremoMantienePuntajeDirectoSinPercentil() {
        candidatos.clear();
        var calculo = servicio.preparar(10L, 20L, "ficticia", null, null);
        assertThat(calculo.idBaremo()).isNull();
        assertThat(calculo.factores())
                .filteredOn(f -> f.codigo().equals("VOC1"))
                .allSatisfy(
                        f -> {
                            assertThat(f.puntuacionDirecta()).isZero();
                            assertThat(f.percentil()).isNull();
                            assertThat(f.estadoNormativo()).isEqualTo("SIN_BAREMO_VALIDADO");
                        });
    }

    @Test
    void normasAmbiguasNoProducenUnPercentil() {
        candidatos.add(new BaremoCandidato(9L, List.of(new RangoCandidato(31L, "VOC1", 60, 0, 0))));
        var calculo = servicio.preparar(10L, 20L, "ficticia", null, null);
        assertThat(calculo.idBaremo()).isNull();
        assertThat(calculo.factores()).allSatisfy(f -> assertThat(f.percentil()).isNull());
    }

    @Test
    void otroInstrumentoNoSeRecalculaComoBfa() {
        when(entradas.calcular(10L, 20L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.preparar(10L, 20L, "ficticia", null, null))
                .isInstanceOf(RevisionBfaException.class);
    }
}
