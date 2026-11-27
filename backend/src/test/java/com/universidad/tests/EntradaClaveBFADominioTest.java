package com.universidad.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import org.junit.jupiter.api.Test;

class EntradaClaveBFADominioTest {

    @Test
    void debeConservarLosIdentificadoresDeUnaEntradaPersistida() {
        EntradaClaveBFA entrada = new EntradaClaveBFA(40L, 10L, 20L, 30L);

        assertEquals(40L, entrada.getIdEntradaClave());
        assertEquals(10L, entrada.getIdClave());
        assertEquals(20L, entrada.getIdPregunta());
        assertEquals(30L, entrada.getIdOpcionCorrecta());
    }

    @Test
    void debePermitirUnaEntradaSinIdAntesDePersistir() {
        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);

        assertNull(entrada.getIdEntradaClave());
        assertEquals(10L, entrada.getIdClave());
        assertEquals(20L, entrada.getIdPregunta());
        assertEquals(30L, entrada.getIdOpcionCorrecta());
    }

    @Test
    void debeRechazarIdEntradaNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new EntradaClaveBFA(0L, 10L, 20L, 30L));
        assertThrows(IllegalArgumentException.class, () -> new EntradaClaveBFA(-1L, 10L, 20L, 30L));
    }

    @Test
    void debeRechazarClaveAusente() {
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, null, 20L, 30L));
    }

    @Test
    void debeRechazarIdClaveNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 0L, 20L, 30L));
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, -10L, 20L, 30L));
    }

    @Test
    void debeRechazarPreguntaAusente() {
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, null, 30L));
    }

    @Test
    void debeRechazarIdPreguntaNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, 0L, 30L));
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, -20L, 30L));
    }

    @Test
    void debeRechazarOpcionCorrectaAusente() {
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, 20L, null));
    }

    @Test
    void debeRechazarIdOpcionCorrectaNoPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, 20L, 0L));
        assertThrows(
                IllegalArgumentException.class, () -> new EntradaClaveBFA(null, 10L, 20L, -30L));
    }

    @Test
    void debeConservarLaAsociacionConSuClaveSinCambiarLosDatos() {
        EntradaClaveBFA primera = new EntradaClaveBFA(1L, 10L, 20L, 30L);
        EntradaClaveBFA segunda = new EntradaClaveBFA(2L, 11L, 20L, 31L);

        assertEquals(10L, primera.getIdClave());
        assertEquals(30L, primera.getIdOpcionCorrecta());
        assertEquals(11L, segunda.getIdClave());
        assertEquals(31L, segunda.getIdOpcionCorrecta());
    }
}
