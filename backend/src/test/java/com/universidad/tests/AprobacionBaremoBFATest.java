package com.universidad.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.domain.model.AprobacionBaremoBFA;
import com.universidad.tests.domain.model.AprobacionBaremoBFA.Estado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class AprobacionBaremoBFATest {

    private static final String EVIDENCIA = "Acta sintética de validación BFA";

    @Test
    void creaElBaremoEnBorradorSinEvidencia() {
        AprobacionBaremoBFA borrador = AprobacionBaremoBFA.borrador();

        assertEquals(Estado.BORRADOR, borrador.estado());
        assertNull(borrador.evidencia());
        assertFalse(borrador.estaAprobado());
    }

    @Test
    void conservaLaEvidenciaAlAprobarYRetirarElBaremo() {
        AprobacionBaremoBFA aprobado = AprobacionBaremoBFA.borrador().aprobar(EVIDENCIA);
        AprobacionBaremoBFA retirado = aprobado.retirar();

        assertEquals(Estado.APROBADO, aprobado.estado());
        assertEquals(EVIDENCIA, aprobado.evidencia());
        assertTrue(aprobado.estaAprobado());
        assertEquals(Estado.RETIRADO, retirado.estado());
        assertEquals(EVIDENCIA, retirado.evidencia());
        assertFalse(retirado.estaAprobado());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rechazaAprobacionesSinEvidencia(String evidencia) {
        assertThrows(
                IllegalArgumentException.class,
                () -> AprobacionBaremoBFA.borrador().aprobar(evidencia));
    }

    @Test
    void rechazaTransicionesInvalidas() {
        AprobacionBaremoBFA borrador = AprobacionBaremoBFA.borrador();
        AprobacionBaremoBFA aprobado = borrador.aprobar(EVIDENCIA);
        AprobacionBaremoBFA retirado = aprobado.retirar();

        assertThrows(IllegalStateException.class, borrador::retirar);
        assertThrows(IllegalStateException.class, () -> aprobado.aprobar(EVIDENCIA));
        assertThrows(IllegalStateException.class, () -> retirado.aprobar(EVIDENCIA));
        assertThrows(IllegalStateException.class, retirado::retirar);
    }

    @Test
    void rechazaEstadosPersistidosInconsistentes() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AprobacionBaremoBFA(Estado.BORRADOR, EVIDENCIA));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AprobacionBaremoBFA(Estado.APROBADO, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AprobacionBaremoBFA(Estado.RETIRADO, null));
    }
}
