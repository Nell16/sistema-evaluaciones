package com.universidad.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import org.junit.jupiter.api.Test;

class VersionClaveBFADominioTest {

    private VersionClaveBFA crearClave() {
        return new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
    }

    // 1. UNA NUEVA CLAVE INICIA EN BORRADOR

    @Test
    void debeCrearClaveEnBorrador() {

        VersionClaveBFA clave = crearClave();

        assertEquals(EstadoVersionClaveBFA.BORRADOR, clave.getEstado());

        assertTrue(clave.esEditable());

        assertNull(clave.getEvidenciaAprobacion());
    }

    // 2. APROBAR UNA CLAVE CON EVIDENCIA

    @Test
    void debeAprobarClaveConEvidencia() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("APROBACION-SINTETICA-001");

        assertEquals(EstadoVersionClaveBFA.APROBADA, clave.getEstado());

        assertEquals("APROBACION-SINTETICA-001", clave.getEvidenciaAprobacion());

        assertFalse(clave.esEditable());
    }

    // 3. RECHAZAR APROBACION SIN EVIDENCIA

    @Test
    void debeRechazarAprobacionSinEvidencia() {

        VersionClaveBFA clave = crearClave();

        assertThrows(IllegalArgumentException.class, () -> clave.aprobar(null));

        assertThrows(IllegalArgumentException.class, () -> clave.aprobar("   "));

        assertEquals(EstadoVersionClaveBFA.BORRADOR, clave.getEstado());

        assertNull(clave.getEvidenciaAprobacion());
    }

    // 4. RECHAZAR APROBACION DE CLAVE SIN ID

    @Test
    void debeRechazarAprobacionDeClaveSinId() {

        VersionClaveBFA clave = new VersionClaveBFA(null, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");

        assertThrows(IllegalStateException.class, () -> clave.aprobar("EVIDENCIA-SINTETICA"));

        assertEquals(EstadoVersionClaveBFA.BORRADOR, clave.getEstado());
    }

    // 5. IMPEDIR APROBAR NUEVAMENTE UNA CLAVE

    @Test
    void debeRechazarDobleAprobacion() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("APROBACION-ORIGINAL");

        assertThrows(IllegalStateException.class, () -> clave.aprobar("OTRA-APROBACION"));

        assertEquals("APROBACION-ORIGINAL", clave.getEvidenciaAprobacion());
    }

    // 6. RETIRAR UNA CLAVE APROBADA

    @Test
    void debeRetirarClaveAprobada() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("APROBACION-SINTETICA");
        clave.retirar();

        assertEquals(EstadoVersionClaveBFA.RETIRADA, clave.getEstado());

        assertFalse(clave.esEditable());

        assertEquals("APROBACION-SINTETICA", clave.getEvidenciaAprobacion());
    }

    // 7. IMPEDIR RETIRAR UNA CLAVE EN BORRADOR

    @Test
    void debeRechazarRetiroDeClaveEnBorrador() {

        VersionClaveBFA clave = crearClave();

        assertThrows(IllegalStateException.class, clave::retirar);

        assertEquals(EstadoVersionClaveBFA.BORRADOR, clave.getEstado());
    }

    // 8. IMPEDIR RETIRAR DOS VECES UNA CLAVE

    @Test
    void debeRechazarDobleRetiro() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("APROBACION-SINTETICA");
        clave.retirar();

        assertThrows(IllegalStateException.class, clave::retirar);

        assertEquals(EstadoVersionClaveBFA.RETIRADA, clave.getEstado());
    }

    // 9. IMPEDIR VOLVER A APROBAR UNA CLAVE RETIRADA

    @Test
    void debeRechazarReaprobacionDeClaveRetirada() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("APROBACION-ORIGINAL");
        clave.retirar();

        assertThrows(IllegalStateException.class, () -> clave.aprobar("NUEVA-APROBACION"));

        assertEquals(EstadoVersionClaveBFA.RETIRADA, clave.getEstado());

        assertEquals("APROBACION-ORIGINAL", clave.getEvidenciaAprobacion());
    }

    // 10. BORRADOR NO PUEDE CALIFICAR

    @Test
    void debeRechazarNuevaCalificacionConClaveEnBorrador() {

        VersionClaveBFA clave = crearClave();

        assertFalse(clave.esAptaParaNuevaCalificacion());

        IllegalStateException excepcion =
                assertThrows(IllegalStateException.class, clave::exigirAptaParaNuevaCalificacion);

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        assertEquals(EstadoVersionClaveBFA.BORRADOR, clave.getEstado());
    }

    // 11. CLAVE APROBADA PUEDE CALIFICAR

    @Test
    void debePermitirNuevaCalificacionConClaveAprobada() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("EVIDENCIA-OFICIAL-SINTETICA");

        assertTrue(clave.esAptaParaNuevaCalificacion());

        assertDoesNotThrow(clave::exigirAptaParaNuevaCalificacion);

        assertEquals(EstadoVersionClaveBFA.APROBADA, clave.getEstado());

        assertEquals("EVIDENCIA-OFICIAL-SINTETICA", clave.getEvidenciaAprobacion());
    }

    // 12. CLAVE RETIRADA NO PUEDE CALIFICAR

    @Test
    void debeRechazarNuevaCalificacionConClaveRetirada() {

        VersionClaveBFA clave = crearClave();

        clave.aprobar("EVIDENCIA-OFICIAL-SINTETICA");
        clave.retirar();

        assertFalse(clave.esAptaParaNuevaCalificacion());

        IllegalStateException excepcion =
                assertThrows(IllegalStateException.class, clave::exigirAptaParaNuevaCalificacion);

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        assertEquals(EstadoVersionClaveBFA.RETIRADA, clave.getEstado());

        // La evidencia historica permanece intacta.
        assertEquals("EVIDENCIA-OFICIAL-SINTETICA", clave.getEvidenciaAprobacion());
    }
}
