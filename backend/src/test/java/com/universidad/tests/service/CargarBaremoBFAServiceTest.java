package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.universidad.tests.application.service.CargarBaremoBFAService;
import com.universidad.tests.domain.model.*;
import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CargarBaremoBFAServiceTest {
    @Test
    void rechazaRangosSuperpuestosAntesDePersistir() {
        BaremoBFARepository baremos = mock(BaremoBFARepository.class);
        CargarBaremoBFAService servicio =
                new CargarBaremoBFAService(baremos, mock(AutorizacionBaremoPort.class));
        BaremoBFA baremo =
                new BaremoBFA(
                        null,
                        "BFA",
                        "v1",
                        1992,
                        null,
                        null,
                        List.of(
                                new RangoBaremoBFA(
                                        "VOC1", 5, 1, 6, EstadoCeldaBaremo.RANGO, null, null),
                                new RangoBaremoBFA(
                                        "VOC1", 10, 6, 12, EstadoCeldaBaremo.RANGO, null, null)));

        assertThrows(
                CargarBaremoBFAService.BaremoInvalidoException.class,
                () -> servicio.cargar(baremo));
        verify(baremos, never()).guardar(any());
    }
}
