package com.universidad.tests.application.usecase.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.dto.request.PublicarRevisionRequest;
import com.universidad.tests.dto.request.RecalculoRequest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RevisionResultadoBfaRegresionTest extends DatosRevisionBfaService {
    @Test
    void e10RepetirLaDecisionNoDuplicaLaPareja() {
        servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("INVALIDA"));
        servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA"));
        assertEquals(1, memoria.palabras.size());
        assertEquals("VALIDA", memoria.palabras.values().iterator().next().decision().name());
    }

    @Test
    void e10RespuestaQueNoEsFluidezSeRechaza() {
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () -> servicio.revisarPalabra(EVALUACION, 1L, 999L, decision("VALIDA")));
        assertEquals(Codigo.RESPUESTA_NO_FV, e.codigo());
    }

    @Test
    void e10SobreLaRevisionVigenteSolicitaRecalculo() {
        memoria.resultado = Memoria.conVigente(memoria.resultado, 1L);
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () ->
                                servicio.revisarPalabra(
                                        EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA")));
        assertEquals(Codigo.REVISION_COMPLETADA, e.codigo());
    }

    @Test
    void e10RevisionDeOtroResultadoNoExiste() {
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () ->
                                servicio.revisarPalabra(
                                        EVALUACION, 77L, RESPUESTA_FV, decision("VALIDA")));
        assertEquals(Codigo.REVISION_NO_ENCONTRADA, e.codigo());
    }

    @Test
    void e10DecisionInvalidaSeRechaza() {
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () ->
                                servicio.revisarPalabra(
                                        EVALUACION, 1L, RESPUESTA_FV, decision("TAL VEZ")));
        assertEquals(Codigo.SOLICITUD_INVALIDA, e.codigo());
    }

    @Test
    void e10SinPermisoSeDeniega() {
        entrar(Set.of());
        assertThrows(
                SinPermisoException.class,
                () -> servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA")));
    }

    @Test
    void e10EvaluacionNoFinalizadaSeRechaza() {
        estadoEvaluacion = EstadoEvaluacion.EN_CURSO;
        assertThrows(
                EvaluacionNoFinalizadaException.class,
                () -> servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA")));
    }

    @Test
    void e11ConservaLasVersionesDeLaRevisionAnteriorSiNoSeIndican() {
        servicio.recalcular(EVALUACION, new RecalculoRequest("Corrección", null, null, null, null));
        var nueva = memoria.revisiones.get(2L);
        assertEquals("DEMO-1", nueva.versionReglas());
        assertEquals(Long.valueOf(7L), nueva.idClave());
        assertEquals(Long.valueOf(8L), nueva.idBaremo());
    }

    @Test
    void e11SinMotivoSeRechaza() {
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () ->
                                servicio.recalcular(
                                        EVALUACION,
                                        new RecalculoRequest(" ", null, null, null, null)));
        assertEquals(Codigo.SOLICITUD_INVALIDA, e.codigo());
    }

    @Test
    void e12RepetirLaPublicacionNoDuplicaElCambio() {
        servicio.publicar(EVALUACION, publicar(1L, null));
        servicio.publicar(EVALUACION, publicar(1L, null));
        assertEquals(1, eventos.size());
        assertEquals(1, memoria.publicaciones);
    }

    @Test
    void e12SiLaVigenteCambioSeRechaza() {
        servicio.publicar(EVALUACION, publicar(1L, null));
        servicio.recalcular(EVALUACION, new RecalculoRequest("Otra", null, null, null, null));
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () -> servicio.publicar(EVALUACION, publicar(2L, null)));
        assertEquals(Codigo.REVISION_VIGENTE_CAMBIO, e.codigo());
    }

    @Test
    void e12ReemplazaLaVigenteSiSeEsperabaLaCorrecta() {
        servicio.publicar(EVALUACION, publicar(1L, null));
        servicio.recalcular(EVALUACION, new RecalculoRequest("Otra", null, null, null, null));
        var r = servicio.publicar(EVALUACION, publicar(2L, 1));
        assertEquals(2, r.revision());
        assertEquals(Long.valueOf(2L), memoria.resultado.referenciaRevisionVigente());
    }

    @Test
    void e12AudienciaFueraDelCatalogoSeRechaza() {
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () ->
                                servicio.publicar(
                                        EVALUACION,
                                        new PublicarRevisionRequest(
                                                1L, "Validado", null, List.of("VISITANTE"))));
        assertEquals(Codigo.SOLICITUD_INVALIDA, e.codigo());
    }

    @Test
    void e12ElCoordinadorPuedePublicarPeroSinPermisoNo() {
        entrar(Set.of("PUBLICAR_RESULTADOS"));
        assertTrue(servicio.publicar(EVALUACION, publicar(1L, null)).revision() == 1);
        memoria.resultado = Memoria.resultadoInicial();
        entrar(Set.of("REVISAR_FLUIDEZ"));
        assertThrows(
                SinPermisoException.class, () -> servicio.publicar(EVALUACION, publicar(1L, null)));
    }
}
