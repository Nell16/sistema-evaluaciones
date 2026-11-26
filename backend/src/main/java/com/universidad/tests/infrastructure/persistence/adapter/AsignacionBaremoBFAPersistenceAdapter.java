package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.AprobacionBaremoBFA;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort.BaremoCandidato;
import com.universidad.tests.domain.port.ConsultaBaremoBFAParaConversionPort.RangoCandidato;
import com.universidad.tests.domain.repository.AsignacionBaremoBFARepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AsignacionBaremoBFAPersistenceAdapter
        implements AsignacionBaremoBFARepository, ConsultaBaremoBFAParaConversionPort {

    private final JdbcTemplate jdbc;

    public AsignacionBaremoBFAPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void registrarAprobacion(
            long idBaremo,
            long idVersionFormulario,
            String versionReglas,
            String evidenciaAprobacion) {
        jdbc.update(
                """
                INSERT INTO asignacion_baremo_bfa
                    (id_baremo, id_version_formulario, version_reglas, estado, evidencia_aprobacion)
                VALUES (?, ?, ?, 'APROBADO', ?)
                """,
                idBaremo,
                idVersionFormulario,
                versionReglas,
                evidenciaAprobacion);
    }

    @Override
    public Optional<AprobacionBaremoBFA> buscar(
            long idBaremo, long idVersionFormulario, String versionReglas) {
        return jdbc.query(
                """
                SELECT estado, evidencia_aprobacion
                FROM asignacion_baremo_bfa
                WHERE id_baremo = ? AND id_version_formulario = ? AND version_reglas = ?
                """,
                rs ->
                        rs.next()
                                ? Optional.of(
                                        new AprobacionBaremoBFA(
                                                AprobacionBaremoBFA.Estado.valueOf(
                                                        rs.getString("estado")),
                                                rs.getString("evidencia_aprobacion")))
                                : Optional.empty(),
                idBaremo,
                idVersionFormulario,
                versionReglas);
    }

    @Override
    public void actualizarEstado(
            long idBaremo,
            long idVersionFormulario,
            String versionReglas,
            AprobacionBaremoBFA aprobacion) {
        int actualizadas =
                jdbc.update(
                        """
                        UPDATE asignacion_baremo_bfa SET estado = ?
                        WHERE id_baremo = ? AND id_version_formulario = ?
                          AND version_reglas = ? AND estado = 'APROBADO'
                        """,
                        aprobacion.estado().name(),
                        idBaremo,
                        idVersionFormulario,
                        versionReglas);
        if (actualizadas != 1) {
            throw new IllegalStateException("La asignacion ya no esta aprobada");
        }
    }

    @Override
    public List<BaremoCandidato> buscarAprobadosCompatibles(
            long versionFormularioId, String versionReglas) {
        return jdbc.query(
                """
                SELECT b.id_baremo, r.id_rango, r.id_factor, r.percentil,
                       r.limite_inferior, r.limite_superior
                FROM asignacion_baremo_bfa a
                JOIN baremo_bfa b ON b.id_baremo = a.id_baremo
                LEFT JOIN rango_baremo_bfa r
                       ON r.id_baremo = b.id_baremo AND r.estado_celda = 'RANGO'
                WHERE a.id_version_formulario = ?
                  AND a.version_reglas = ?
                  AND a.estado = 'APROBADO'
                ORDER BY b.id_baremo, r.id_factor, r.percentil
                """,
                rs -> {
                    Map<Long, List<RangoCandidato>> rangosPorBaremo = new LinkedHashMap<>();
                    while (rs.next()) {
                        long idBaremo = rs.getLong("id_baremo");
                        List<RangoCandidato> rangos =
                                rangosPorBaremo.computeIfAbsent(
                                        idBaremo, ignorado -> new ArrayList<>());
                        if (rs.getObject("id_rango") != null) {
                            rangos.add(
                                    new RangoCandidato(
                                            rs.getLong("id_rango"),
                                            rs.getString("id_factor"),
                                            rs.getInt("percentil"),
                                            rs.getInt("limite_inferior"),
                                            rs.getInt("limite_superior")));
                        }
                    }
                    return rangosPorBaremo.entrySet().stream()
                            .map(e -> new BaremoCandidato(e.getKey(), e.getValue()))
                            .toList();
                },
                versionFormularioId,
                versionReglas);
    }
}
