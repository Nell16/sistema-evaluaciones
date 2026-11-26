package com.universidad.tests.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.usecase.vista.PeriodoVista;
import com.universidad.tests.domain.exception.ErrorNegocioException;
import com.universidad.tests.domain.model.Periodo;
import com.universidad.tests.domain.port.AutorizacionEvaluacionPort;
import com.universidad.tests.domain.repository.PeriodoRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearPeriodoUseCaseTest {

    @Mock private AutorizacionEvaluacionPort autorizacion;
    @Mock private PeriodoRepository periodoRepository;

    @InjectMocks private CrearPeriodoUseCase crearPeriodoUseCase;

    @Test
    void deberiaCrearPeriodoFicticioCorrectamente() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin = LocalDate.of(2026, 12, 15);
        when(periodoRepository.buscarPorCodigo("2026-2")).thenReturn(Optional.empty());
        when(periodoRepository.guardar(any(Periodo.class)))
                .thenReturn(new Periodo(3L, "2026-2", "Segundo semestre 2026", inicio, fin, true));

        PeriodoVista resultado =
                crearPeriodoUseCase.crear("2026-2", "Segundo semestre 2026", inicio, fin);

        assertThat(resultado.id()).isEqualTo(3L);
        assertThat(resultado.codigo()).isEqualTo("2026-2");
        assertThat(resultado.disponible()).isTrue();
        verify(autorizacion).exigirPermisoCrearPeriodo();
    }

    @Test
    void deberiaLanzarPeriodoInvalidoCuandoInicioEsPosteriorAFin() {
        LocalDate inicio = LocalDate.of(2026, 12, 20);
        LocalDate fin = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(
                        () -> crearPeriodoUseCase.crear("2026-2", "Período inválido", inicio, fin))
                .isInstanceOf(ErrorNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "PERIODO_INVALIDO");
    }

    @Test
    void deberiaLanzarPeriodoDuplicadoCuandoCodigoYaExiste() {
        LocalDate inicio = LocalDate.of(2026, 8, 1);
        LocalDate fin = LocalDate.of(2026, 12, 15);
        Periodo existente = new Periodo(3L, "2026-2", "Existente", inicio, fin, true);
        when(periodoRepository.buscarPorCodigo("2026-2")).thenReturn(Optional.of(existente));

        assertThatThrownBy(
                        () ->
                                crearPeriodoUseCase.crear(
                                        "2026-2", "Segundo semestre 2026", inicio, fin))
                .isInstanceOf(ErrorNegocioException.class)
                .hasFieldOrPropertyWithValue("codigo", "PERIODO_DUPLICADO");
    }
}
