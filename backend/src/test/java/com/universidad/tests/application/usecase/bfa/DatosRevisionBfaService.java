package com.universidad.tests.application.usecase.bfa;

import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.ContextoSesion;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.application.service.RegistroAuditoria;
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

/** Datos ficticios. Persistencia en memoria. */
abstract class DatosRevisionBfaService {

    protected static final long EVALUACION = 10L;
    protected static final long RESULTADO = 20L;
    protected static final long RESPUESTA_FV = 500L;

    protected Memoria memoria;
    protected List<String> eventos;
    protected RevisionResultadoBfaService servicio;
    protected EstadoEvaluacion estadoEvaluacion;

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
                                new com.universidad.tests.domain.port.EventoAuditoriaPort() {
                                    public void insertar(
                                            long cuenta, String tipo, String entidad, long id) {
                                        eventos.add(tipo + ":" + entidad + ":" + id);
                                    }

                                    public void insertar(
                                            long cuenta,
                                            String tipo,
                                            String entidad,
                                            long id,
                                            Long evaluacion) {
                                        insertar(cuenta, tipo, entidad, id);
                                    }
                                }),
                        (cuenta, sujeto) -> true,
                        (evaluacion, version, reglas, clave, baremo) ->
                                new com.universidad.tests.domain.port.PreparacionRevisionBfaPort
                                        .Calculo(
                                        clave != null ? clave : 7L,
                                        baremo != null ? baremo : 8L,
                                        List.of()));
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

    protected void entrar(Set<String> permisos) {
        ContextoSesion.establecer(new IdentidadSesion(99L, 1L, "PSICOLOGO", permisos));
    }

    protected RevisionPalabraRequest decision(String valor) {
        return new RevisionPalabraRequest(valor, "Palabra ficticia revisada");
    }

    // ---------- E10 ----------

    // ---------- E11 ----------

    // ---------- E12 ----------

    protected PublicarRevisionRequest publicar(Long calificacion, Integer esperada) {
        return new PublicarRevisionRequest(calificacion, "Validado", esperada, List.of("SUJETO"));
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
        public String resolverReglas(Long resultado, String versionPedida) {
            return versionPedida != null ? versionPedida : "DEMO-1";
        }

        @Override
        public boolean estaValidada(Long idRevision) {
            return java.util.Objects.equals(resultado.referenciaRevisionVigente(), idRevision);
        }

        @Override
        public void guardarFactores(
                Long idRevision,
                List<com.universidad.tests.domain.model.ResultadoFactor> factores) {}

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
                Long idResultado,
                Long idRevision,
                List<String> audiencia,
                String motivo,
                Long idCuenta) {
            publicaciones++;
            resultado = conVigente(resultado, idRevision);
        }
    }
}
