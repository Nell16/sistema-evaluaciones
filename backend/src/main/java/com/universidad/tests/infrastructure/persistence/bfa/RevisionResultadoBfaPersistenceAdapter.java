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

    private static final String COLUMNAS =
            "id_revision_resultado, id_resultado, numero_revision, motivo, version_reglas,"
                    + " id_clave, id_baremo, id_solicitud, id_cuenta_autor, creada_en";

    private final JdbcClient jdbc;

    public RevisionResultadoBfaPersistenceAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RevisionResultado> buscarPorId(Long idRevision) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM revision_resultado WHERE id_revision_resultado = :id")
                .param("id", idRevision)
                .query(this::mapear)
                .optional();
    }

    @Override
    public Optional<RevisionResultado> buscarPorSolicitud(Long idResultado, String idSolicitud) {
        return jdbc.sql(
                        "SELECT "
                                + COLUMNAS
                                + " FROM revision_resultado"
                                + " WHERE id_resultado = :r AND id_solicitud = :s")
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
                                + " FROM revision_resultado WHERE id_resultado = :r"
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
        Long id =
                jdbc.sql(
                                """
                                INSERT INTO revision_resultado (
                                    id_resultado, numero_revision, motivo, version_reglas,
                                    id_clave, id_baremo, id_solicitud, id_cuenta_autor, creada_en)
                                VALUES (
                                    :r,
                                    (SELECT COALESCE(MAX(numero_revision), 0) + 1
                                       FROM revision_resultado WHERE id_resultado = :r),
                                    :motivo, :reglas, :clave, :baremo, :solicitud, :autor, :creada)
                                RETURNING id_revision_resultado
                                """)
                        .param("r", revision.idResultado())
                        .param("motivo", revision.motivo())
                        .param("reglas", revision.versionReglas())
                        .param("clave", revision.idClave())
                        .param("baremo", revision.idBaremo())
                        .param("solicitud", revision.idSolicitud())
                        .param("autor", revision.idCuentaAutor())
                        .param("creada", java.sql.Timestamp.from(revision.creadaEn()))
                        .query(Long.class)
                        .single();
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
    public void publicar(Long idResultado, Long idRevision, List<String> audiencia, String motivo) {
        jdbc.sql(
                        "UPDATE revision_resultado SET audiencia_publicacion = :aud,"
                            + " motivo_publicacion = :motivo, publicada_en = statement_timestamp()"
                            + " WHERE id_revision_resultado = :rev")
                .param("aud", String.join(",", audiencia))
                .param("motivo", motivo)
                .param("rev", idRevision)
                .update();
        jdbc.sql(
                        "UPDATE resultado SET referencia_revision_vigente = :rev,"
                                + " estado = 'DISPONIBLE' WHERE id_resultado = :res")
                .param("rev", idRevision)
                .param("res", idResultado)
                .update();
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
