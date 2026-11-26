package com.universidad.tests.application.usecase.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.ContextoSesion;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.application.service.RegistroAuditoria;
import com.universidad.tests.domain.exception.EvaluacionNoFinalizadaException;
import com.universidad.tests.domain.exception.SinPermisoException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo;
import com.universidad.tests.domain.model.EstadoEvaluacion;
import com.universidad.tests.domain.model.Evaluacion;
import com.universidad.tests.domain.model.Resultado;
import com.universidad.tests.domain.model.bfa.RevisionPalabra;
import com.universidad.tests.domain.model.bfa.RevisionResultado;
import com.universidad.tests.domain.port.PublicacionResultadoPort;
import com.universidad.tests.domain.port.RespuestaFluidezPort;
import com.universidad.tests.domain.repository.EvaluacionRepository;
import com.universidad.tests.domain.repository.ResultadoRepository;
import com.universidad.tests.domain.repository.bfa.RevisionPalabraRepository;
import com.universidad.tests.domain.repository.bfa.RevisionResultadoRepository;
import com.universidad.tests.dto.request.PublicarRevisionRequest;
import com.universidad.tests.dto.request.RecalculoRequest;
import com.universidad.tests.dto.request.RevisionPalabraRequest;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Datos ficticios. Persistencia en memoria. */
class RevisionResultadoBfaServiceTest {

    private static final long EVALUACION = 10L;
    private static final long RESULTADO = 20L;
    private static final long RESPUESTA_FV = 500L;

    private Memoria memoria;
    private List<String> eventos;
    private RevisionResultadoBfaService servicio;
    private EstadoEvaluacion estadoEvaluacion;

    @BeforeEach
    void preparar() {
        memoria = new Memoria();
        eventos = new ArrayList<>();
        estadoEvaluacion = EstadoEvaluacion.FINALIZADA;
        entrar(Set.of("REVISAR_FLUIDEZ", "RECALCULAR_BFA", "PUBLICAR_RESULTADOS"));
        EvaluacionRepository evaluaciones =
                (EvaluacionRepository)
                        Proxy.newProxyInstance(
                                getClass().getClassLoader(),
                                new Class<?>[] {EvaluacionRepository.class},
                                (p, m, a) ->
                                        Optional.of(
                                                new Evaluacion(
                                                        EVALUACION,
                                                        1L,
                                                        1L,
                                                        1L,
                                                        1L,
                                                        estadoEvaluacion,
                                                        Instant.EPOCH,
                                                        Instant.EPOCH,
                                                        List.of())));
        servicio =
                new RevisionResultadoBfaService(
                        new AutorizacionService(),
                        evaluaciones,
                        memoria,
                        memoria,
                        memoria,
                        memoria,
                        memoria,
                        new RegistroAuditoria(
                                (cuenta, tipo, entidad, id) ->
                                        eventos.add(tipo + ":" + entidad + ":" + id)));
        memoria.revisiones.put(
                1L,
                new RevisionResultado(
                        1L,
                        RESULTADO,
                        1,
                        "Cálculo inicial",
                        "DEMO-1",
                        7L,
                        8L,
                        null,
                        1L,
                        Instant.EPOCH));
        memoria.siguienteId = 2L;
    }

    private void entrar(Set<String> permisos) {
        ContextoSesion.establecer(new IdentidadSesion(99L, 1L, "PSICOLOGO", permisos));
    }

    private RevisionPalabraRequest decision(String valor) {
        return new RevisionPalabraRequest(valor, "Palabra ficticia revisada");
    }

    // ---------- E10 ----------

    @Test
    void e10RegistraLaDecisionYAudita() {
        var r = servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("VALIDA"));
        assertEquals("VALIDA", r.decision());
        assertEquals("REVISION_AUTORIZADA", r.origenDecision());
        assertEquals(Long.valueOf(1L), r.calificacionId());
        assertEquals(List.of("REVISAR_PALABRA:REVISION_RESULTADO:1"), eventos);
    }

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

    // ---------- E11 ----------

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
    void e11ConservaLasVersionesDeLaRevisionAnteriorSiNoSeIndican() {
        servicio.recalcular(EVALUACION, new RecalculoRequest("Corrección", null, null, null, null));
        var nueva = memoria.revisiones.get(2L);
        assertEquals("DEMO-1", nueva.versionReglas());
        assertEquals(Long.valueOf(7L), nueva.idClave());
        assertEquals(Long.valueOf(8L), nueva.idBaremo());
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

    // ---------- E12 ----------

    private PublicarRevisionRequest publicar(Long calificacion, Integer esperada) {
        return new PublicarRevisionRequest(calificacion, "Validado", esperada, List.of("SUJETO"));
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
    void e12ConPalabrasPendientesNoSePublica() {
        servicio.revisarPalabra(EVALUACION, 1L, RESPUESTA_FV, decision("PENDIENTE"));
        var e =
                assertThrows(
                        RevisionBfaException.class,
                        () -> servicio.publicar(EVALUACION, publicar(1L, null)));
        assertEquals(Codigo.REVISION_NO_PUBLICABLE, e.codigo());
        assertFalse(memoria.publicada());
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

    // ---------- persistencia en memoria ----------

    static final class Memoria
            implements ResultadoRepository,
                    RevisionResultadoRepository,
                    RevisionPalabraRepository,
                    RespuestaFluidezPort,
                    PublicacionResultadoPort {

        Resultado resultado = resultadoInicial();
        final Map<Long, RevisionResultado> revisiones = new HashMap<>();
        final Map<String, RevisionPalabra> palabras = new HashMap<>();
        long siguienteId = 1L;
        int publicaciones;

        static Resultado resultadoInicial() {
            return new Resultado(
                    RESULTADO,
                    EVALUACION,
                    Resultado.PENDIENTE_VALIDACION,
                    1L,
                    Instant.EPOCH,
                    null,
                    List.of());
        }

        static Resultado conVigente(Resultado r, Long idRevision) {
            return new Resultado(
                    r.idResultado(),
                    r.idEvaluacion(),
                    Resultado.DISPONIBLE,
                    r.versionFormularioId(),
                    r.creadoEn(),
                    idRevision,
                    r.factores());
        }

        boolean publicada() {
            return publicaciones > 0;
        }

        @Override
        public Resultado guardar(Resultado r) {
            resultado = r;
            return r;
        }

        @Override
        public Optional<Resultado> buscarPorEvaluacion(Long idEvaluacion) {
            return Optional.of(resultado);
        }

        @Override
        public Optional<RevisionResultado> buscarPorId(Long id) {
            return Optional.ofNullable(revisiones.get(id));
        }

        @Override
        public Optional<RevisionResultado> buscarPorSolicitud(
                Long idResultado, String idSolicitud) {
            return revisiones.values().stream()
                    .filter(r -> idSolicitud.equals(r.idSolicitud()))
                    .findFirst();
        }

        @Override
        public Optional<RevisionResultado> buscarUltima(Long idResultado) {
            return revisiones.values().stream()
                    .max(java.util.Comparator.comparingInt(RevisionResultado::numeroRevision));
        }

        @Override
        public RevisionResultado guardarNueva(RevisionResultado r) {
            var guardada =
                    new RevisionResultado(
                            siguienteId++,
                            r.idResultado(),
                            r.numeroRevision(),
                            r.motivo(),
                            r.versionReglas(),
                            r.idClave(),
                            r.idBaremo(),
                            r.idSolicitud(),
                            r.idCuentaAutor(),
                            r.creadaEn());
            revisiones.put(guardada.idRevision(), guardada);
            return guardada;
        }

        @Override
        public RevisionPalabra guardar(RevisionPalabra d) {
            var clave = d.idRevision() + ":" + d.idRespuesta();
            var previa = palabras.get(clave);
            var id = previa != null ? previa.idRevisionPalabra() : (long) palabras.size() + 1;
            var guardada =
                    new RevisionPalabra(
                            id,
                            d.idRevision(),
                            d.idRespuesta(),
                            d.decision(),
                            d.motivo(),
                            d.idCuenta(),
                            d.fechaDecision());
            palabras.put(clave, guardada);
            return guardada;
        }

        @Override
        public long contarPendientes(Long idRevision) {
            return palabras.values().stream()
                    .filter(
                            p ->
                                    p.idRevision().equals(idRevision)
                                            && p.decision().name().equals("PENDIENTE"))
                    .count();
        }

        @Override
        public boolean esPalabraFluidezDe(Long idEvaluacion, Long idRespuesta) {
            return idRespuesta == RESPUESTA_FV;
        }

        @Override
        public void publicar(
                Long idResultado, Long idRevision, List<String> audiencia, String motivo) {
            publicaciones++;
            resultado = conVigente(resultado, idRevision);
        }
    }
}
