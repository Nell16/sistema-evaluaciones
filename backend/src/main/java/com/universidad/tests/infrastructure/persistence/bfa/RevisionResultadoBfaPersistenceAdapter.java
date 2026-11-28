package com.universidad.tests.infrastructure.persistence.bfa;

import com.universidad.tests.domain.model.bfa.DecisionPalabra;
import com.universidad.tests.domain.model.bfa.RevisionPalabra;
import com.universidad.tests.domain.model.bfa.RevisionResultado;
import com.universidad.tests.domain.port.PublicacionResultadoPort;
import com.universidad.tests.domain.port.RespuestaFluidezPort;
import com.universidad.tests.domain.repository.bfa.RevisionPalabraRepository;
import com.universidad.tests.domain.repository.bfa.RevisionResultadoRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class RevisionResultadoBfaPersistenceAdapter
        implements RevisionResultadoRepository,
                RevisionPalabraRepository,
                RespuestaFluidezPort,
                PublicacionResultadoPort {

    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager contexto;

    private static final String COLUMNAS =
            "r.id_revision_resultado,r.id_resultado,r.numero_revision,r.motivo,g.version AS version_reglas,"
                    + "r.id_clave,r.id_baremo,r.id_solicitud,r.id_cuenta_autor,r.fecha_calculo AS creada_en";
    private static final String FUENTE =
            " FROM revision_resultado r LEFT JOIN regla_calificacion g USING(id_regla_calificacion)";

    private final JdbcClient jdbc;

    public RevisionResultadoBfaPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RevisionResultado> buscarPorId(Long idRevision) {
        return jdbc.sql("SELECT " + COLUMNAS + FUENTE + " WHERE r.id_revision_resultado = :id")
                .param("id", idRevision)
                .query(this::mapear)
                .optional();
    }

    @Override
    public Optional<RevisionResultado> buscarPorSolicitud(Long idResultado, String idSolicitud) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + FUENTE
                                + " WHERE r.id_resultado = :r AND r.id_solicitud = :s")
                .param("r", idResultado)
                .param("s", idSolicitud)
                .query(this::mapear)
                .optional();
    }

    @Override
    public Optional<RevisionResultado> buscarUltima(Long idResultado) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + FUENTE
                                + " WHERE r.id_resultado = :r"
                                + " ORDER BY numero_revision DESC LIMIT 1")
                .param("r", idResultado)
                .query(this::mapear)
                .optional();
    }

    @Override
    public RevisionResultado guardarNueva(RevisionResultado revision) {
        // Serializa por resultado para que dos recálculos no tomen el mismo número.
        jdbc.sql("SELECT id_resultado FROM resultado WHERE id_resultado = :r FOR UPDATE")
                .param("r", revision.idResultado())
                .query()
                .listOfRows();
        Long regla =
                jdbc.sql(
                                """
            SELECT g.id_regla_calificacion FROM resultado r JOIN evaluacion e USING(id_evaluacion)
            JOIN test t USING(id_test) JOIN regla_calificacion g ON g.id_version_formulario=e.id_version_formulario
            WHERE r.id_resultado=:r AND t.codigo='BFA' AND g.version=:version AND g.estado='APROBADA'
              AND nullif(btrim(g.evidencia_aprobacion),'') IS NOT NULL AND g.referencia_implementacion=:implementacion
            """)
                        .param("r", revision.idResultado())
                        .param("version", revision.versionReglas())
                        .param(
                                "implementacion",
                                com.universidad.tests.domain.port.PreparacionRevisionBfaPort
                                        .IMPLEMENTACION)
                        .query(Long.class)
                        .optional()
                        .orElseThrow(
                                () ->
                                        new com.universidad.tests.domain.exception.bfa
                                                .RevisionBfaException(
                                                com.universidad.tests.domain.exception.bfa
                                                        .RevisionBfaException.Codigo
                                                        .SOLICITUD_INVALIDA,
                                                "La versión de reglas debe estar aprobada para el formulario BFA aplicado."));
        Long id =
                jdbc.sql(
                                """
                                INSERT INTO revision_resultado (
                                    id_resultado, numero_revision, motivo, id_regla_calificacion,
                                    id_clave, id_baremo, id_solicitud, id_cuenta_autor, fecha_calculo, estado_revision)
                                VALUES (
                                    :r,
                                    (SELECT COALESCE(MAX(numero_revision), 0) + 1
                                       FROM revision_resultado WHERE id_resultado = :r),
                                    :motivo, :reglas, :clave, :baremo, :solicitud, :autor, :creada, 'PENDIENTE')
                                RETURNING id_revision_resultado
                                """)
                        .param("r", revision.idResultado())
                        .param("motivo", revision.motivo())
                        .param("reglas", regla)
                        .param("clave", revision.idClave())
                        .param("baremo", revision.idBaremo())
                        .param("solicitud", revision.idSolicitud())
                        .param("autor", revision.idCuentaAutor())
                        .param("creada", java.sql.Timestamp.from(revision.creadaEn()))
                        .query(Long.class)
                        .single();
        jdbc.sql(
                        """
            INSERT INTO revision_palabra(id_revision_resultado,id_respuesta_evaluacion,decision,motivo,id_cuenta)
            SELECT :rev,p.id_respuesta_evaluacion,'PENDIENTE','pendiente de revisión profesional',:cuenta
            FROM respuesta_evaluacion p JOIN resultado r ON r.id_evaluacion=p.id_evaluacion
            WHERE r.id_resultado=:resultado AND p.id_pregunta IS NULL AND upper(p.subprueba_codigo)='FV'
            """)
                .param("rev", id)
                .param("cuenta", revision.idCuentaAutor())
                .param("resultado", revision.idResultado())
                .update();
        return buscarPorId(id).orElseThrow();
    }

    @Override
    public RevisionPalabra guardar(RevisionPalabra d) {
        Long id =
                jdbc.sql(
                                """
                                INSERT INTO revision_palabra (
                                    id_revision_resultado, id_respuesta_evaluacion, decision,
                                    motivo, id_cuenta, fecha_decision)
                                VALUES (:rev, :resp, :decision, :motivo, :cuenta, :fecha)
                                ON CONFLICT (id_revision_resultado, id_respuesta_evaluacion)
                                DO UPDATE SET decision = EXCLUDED.decision,
                                              motivo = EXCLUDED.motivo,
                                              id_cuenta = EXCLUDED.id_cuenta,
                                              fecha_decision = EXCLUDED.fecha_decision
                                RETURNING id_revision_palabra
                                """)
                        .param("rev", d.idRevision())
                        .param("resp", d.idRespuesta())
                        .param("decision", d.decision().name())
                        .param("motivo", d.motivo())
                        .param("cuenta", d.idCuenta())
                        .param("fecha", java.sql.Timestamp.from(d.fechaDecision()))
                        .query(Long.class)
                        .single();
        return new RevisionPalabra(
                id,
                d.idRevision(),
                d.idRespuesta(),
                d.decision(),
                d.motivo(),
                d.idCuenta(),
                d.fechaDecision());
    }

    @Override
    public long contarPendientes(Long idRevision) {
        return jdbc.sql(
                        "SELECT COUNT(*) FROM revision_palabra"
                                + " WHERE id_revision_resultado = :r AND decision = :d")
                .param("r", idRevision)
                .param("d", DecisionPalabra.PENDIENTE.name())
                .query(Long.class)
                .single();
    }

    @Override
    public boolean esPalabraFluidezDe(Long idEvaluacion, Long idRespuesta) {
        return jdbc.sql(
                                "SELECT COUNT(*) FROM respuesta_evaluacion"
                                        + " WHERE id_respuesta_evaluacion = :resp"
                                        + " AND id_evaluacion = :ev AND id_pregunta IS NULL"
                                        + " AND UPPER(subprueba_codigo) = 'FV'")
                        .param("resp", idRespuesta)
                        .param("ev", idEvaluacion)
                        .query(Long.class)
                        .single()
                > 0;
    }

    @Override
    public boolean estaValidada(Long idRevision) {
        return jdbc.sql(
                        "select exists(select 1 from revision_resultado where id_revision_resultado=:id and estado_revision='VALIDADA')")
                .param("id", idRevision)
                .query(Boolean.class)
                .single();
    }

    @Override
    public void guardarFactores(
            Long idRevision, List<com.universidad.tests.domain.model.ResultadoFactor> factores) {
        long test =
                jdbc.sql(
                                "select e.id_test from revision_resultado r join resultado c using(id_resultado) join evaluacion e using(id_evaluacion) where r.id_revision_resultado=:id and r.estado_revision='PENDIENTE'")
                        .param("id", idRevision)
                        .query(Long.class)
                        .single();
        int orden = 0;
        for (var f :
                factores.stream()
                        .sorted(
                                java.util.Comparator.comparing(
                                        com.universidad.tests.domain.model.ResultadoFactor::codigo))
                        .toList()) {
            jdbc.sql(
                            "insert into escala(id_test,codigo,nombre) values (:test,:codigo,:codigo) on conflict(id_test,codigo) do nothing")
                    .param("test", test)
                    .param("codigo", f.codigo())
                    .update();
            long idFactor =
                    jdbc.sql(
                                    """
                insert into resultado_escala(id_revision_resultado,id_escala,orden,puntaje_bruto,estado_calculo,estado_normativo,motivo)
                select :revision,id_escala,:orden,:bruto,:calculo,:normativo,:motivo from escala where id_test=:test and codigo=:codigo
                returning id_resultado_escala
                """)
                            .param("revision", idRevision)
                            .param("orden", ++orden)
                            .param("bruto", f.puntuacionDirecta())
                            .param("calculo", f.estadoCalculo())
                            .param("normativo", f.estadoNormativo())
                            .param("motivo", f.motivo())
                            .param("test", test)
                            .param("codigo", f.codigo())
                            .query(Long.class)
                            .single();
            if (f.percentil() != null)
                jdbc.sql(
                                "insert into conversion_resultado_escala(id_resultado_escala,tipo_salida,puntaje_convertido,id_rango) values (:factor,'PERCENTIL',:percentil,:rango)")
                        .param("factor", idFactor)
                        .param("percentil", f.percentil())
                        .param("rango", f.idRango())
                        .update();
        }
        jdbc.sql(
                        "update revision_resultado set estado_revision='CALCULADA' where id_revision_resultado=:id")
                .param("id", idRevision)
                .update();
    }

    @Override
    public void publicar(
            Long idResultado,
            Long idRevision,
            List<String> audiencia,
            String motivo,
            Long idCuenta) {
        int actualizadas =
                jdbc.sql(
                                """
            update revision_resultado r set audiencia=string_to_array(:aud,','),motivo=:motivo,
                fecha_revision=statement_timestamp(),id_cuenta=:cuenta,estado_revision='VALIDADA'
            from regla_calificacion g, resultado c, evaluacion e
            where r.id_revision_resultado=:rev and r.id_resultado=:res and r.estado_revision='CALCULADA'
              and c.id_resultado=r.id_resultado and e.id_evaluacion=c.id_evaluacion
              and g.id_regla_calificacion=r.id_regla_calificacion and g.id_version_formulario=e.id_version_formulario
              and g.estado='APROBADA' and nullif(btrim(g.evidencia_aprobacion),'') is not null
              and exists(select 1 from resultado_escala f where f.id_revision_resultado=r.id_revision_resultado)
            """)
                        .param("aud", String.join(",", audiencia))
                        .param("motivo", motivo)
                        .param("cuenta", idCuenta)
                        .param("rev", idRevision)
                        .param("res", idResultado)
                        .update();
        if (actualizadas != 1)
            throw new com.universidad.tests.domain.exception.bfa.RevisionBfaException(
                    com.universidad.tests.domain.exception.bfa.RevisionBfaException.Codigo
                            .REVISION_NO_PUBLICABLE,
                    "La revisión no tiene un cálculo completo con reglas aprobadas del formulario aplicado.");
        jdbc.sql(
                        "update resultado set referencia_revision_vigente=:rev,estado='DISPONIBLE' where id_resultado=:res")
                .param("rev", idRevision)
                .param("res", idResultado)
                .update();
        contexto.clear();
    }

    private RevisionResultado mapear(ResultSet rs, int fila) throws SQLException {
        Instant creada = rs.getTimestamp("creada_en").toInstant();
        return new RevisionResultado(
                rs.getLong("id_revision_resultado"),
                rs.getLong("id_resultado"),
                rs.getInt("numero_revision"),
                rs.getString("motivo"),
                rs.getString("version_reglas"),
                (Long) rs.getObject("id_clave"),
                (Long) rs.getObject("id_baremo"),
                rs.getString("id_solicitud"),
                rs.getLong("id_cuenta_autor"),
                creada);
    }
}
