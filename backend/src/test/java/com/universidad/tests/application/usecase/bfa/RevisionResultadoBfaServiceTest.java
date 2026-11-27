package com.universidad.tests.application.usecase.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo;
import com.universidad.tests.dto.request.RecalculoRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevisionResultadoBfaServiceTest extends DatosRevisionBfaService {
    @Test
    void e10RegistraLaDecisionYAudita() {
        var r = servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA"));
        assertEquals("VALIDA", r.decision());
        assertEquals("REVISION_AUTORIZADA", r.origenDecision());
        assertEquals(Long.valueOf(1L), r.calificacionId());
        assertEquals(List.of("REVISAR_PALABRA:REVISION_RESULTADO:1"), eventos);
    }

    @Test
    void e11CreaOtraRevisionYNoCambiaLaVigente() {
        memoria.resultado = Memoria.conVigente(memoria.resultado, 1L);
        var r =
                servicio.recalcular(
                        EVALUACION, new RecalculoRequest("Corrección", null, null, null, "s-1"));
        assertEquals(2, r.revision());
        assertEquals(Long.valueOf(1L), memoria.resultado.referenciaRevisionVigente());
        assertEquals(2, memoria.revisiones.size());
    }

    @Test
    void e11ReenviarLaMismaSolicitudDevuelveLaMismaRevision() {
        var a =
                servicio.recalcular(
                        EVALUACION, new RecalculoRequest("Corrección", "DEMO-2", 7L, 8L, "s-9"));
        var b =
                servicio.recalcular(
                        EVALUACION, new RecalculoRequest("Corrección", "DEMO-2", 7L, 8L, "s-9"));
        assertEquals(a.calificacionId(), b.calificacionId());
        assertEquals(2, memoria.revisiones.size());
    }

    @Test
    void e12PublicaCambiaElEstadoYAudita() {
        var r = servicio.publicar(EVALUACION, publicar(1L, null));
        assertEquals("DISPONIBLE", r.estadoResultado());
        assertEquals(1, r.revision());
        assertEquals(Long.valueOf(1L), memoria.resultado.referenciaRevisionVigente());
        assertEquals("DISPONIBLE", memoria.resultado.estadoResultado());
        assertEquals(List.of("PUBLICAR_REVISION:REVISION_RESULTADO:1"), eventos);
    }

    @Test
    void e12ConPalabrasPendientesNoSePublica() {
        servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("PENDIENTE"));
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () -> servicio.publicar(EVALUACION, publicar(1L, null)));
        assertEquals(Codigo.REVISION_NO_PUBLICABLE, e.codigo());
        assertFalse(memoria.publicada());
    }
}
