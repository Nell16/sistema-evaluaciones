package com.universidad.tests.infrastructure.persistence.resultados;

import com.universidad.tests.domain.model.resultados.ContenedorResultado;
import com.universidad.tests.domain.model.resultados.EvaluacionConsultada;
import com.universidad.tests.domain.model.resultados.FactorResultado;
import com.universidad.tests.domain.model.resultados.RevisionResultado;
import com.universidad.tests.domain.port.resultados.AudienciaResultadoPort;
import com.universidad.tests.domain.port.resultados.EvaluacionConsultaPort;
import com.universidad.tests.domain.port.resultados.ResultadoConsultaPort;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lee exclusivamente la revision referenciada; no publica, recalcula ni escoge la ultima. */
@Repository
public class ConsultaResultadosJdbcAdapter
        implements EvaluacionConsultaPort, ResultadoConsultaPort, AudienciaResultadoPort {
    private final JdbcTemplate jdbc;

    public ConsultaResultadosJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<EvaluacionConsultada> buscarPorId(Long id) {
        return jdbc
                .query(
                        "select id_evaluacion,id_sujeto,id_version_formulario,estado from evaluacion where id_evaluacion=?",
                        (rs, n) ->
                                new EvaluacionConsultada(
                                        rs.getLong(1),
                                        rs.getLong(2),
                                        rs.getLong(3),
                                        rs.getString(4)),
                        id)
                .stream()
                .findFirst();
    }

    public boolean permite(Long id, String rol) {
        var filas =
                jdbc.queryForList(
                        "select id_resultado,estado,referencia_revision_vigente from resultado where id_evaluacion=?",
                        id);
        if (filas.isEmpty() || filas.get(0).get("referencia_revision_vigente") == null) return true;
        // GEN-39-BE vigente: el sujeto no recibe puntajes publicados.
        if ("SUJETO".equals(rol)) return false;
        var fila = filas.get(0);
        return Boolean.TRUE.equals(
                jdbc.queryForObject(
                        "select exists(select 1 from revision_resultado where id_resultado=? and id_revision_resultado=? and ?=any(audiencia))",
                        Boolean.class,
                        fila.get("id_resultado"),
                        fila.get("referencia_revision_vigente"),
                        rol));
    }

    public Optional<ContenedorResultado> buscarPorEvaluacion(Long id) {
        var filas =
                jdbc.queryForList(
                        "select id_resultado,estado,referencia_revision_vigente from resultado where id_evaluacion=?",
                        id);
        if (filas.isEmpty()) return Optional.empty();
        var fila = filas.get(0);
        Long resultadoId = ((Number) fila.get("id_resultado")).longValue();
        Number referencia = (Number) fila.get("referencia_revision_vigente");
        RevisionResultado revision = null;
        if (referencia != null) {
            if (!ContenedorResultado.DISPONIBLE.equals(fila.get("estado"))) throw inconsistente();
            var revisiones =
                    jdbc.queryForList(
                            """
                    select v.*,g.version as version_reglas,g.id_version_formulario,e.id_test
                    from revision_resultado v join regla_calificacion g using(id_regla_calificacion)
                    join evaluacion e on e.id_evaluacion=?
                    where v.id_revision_resultado=? and v.id_resultado=?
                    and g.id_version_formulario=e.id_version_formulario
                    and g.estado in ('APROBADA','RETIRADA')
                    and nullif(btrim(g.evidencia_aprobacion),'') is not null
                    """,
                            id,
                            referencia.longValue(),
                            resultadoId);
            if (revisiones.size() != 1
                    || !"VALIDADA".equals(revisiones.get(0).get("estado_revision")))
                throw inconsistente();
            var vigente = revisiones.get(0);
            List<FactorResultado> factores =
                    jdbc.query(
                            """
                    select e.codigo,e.id_test as factor_test,p.puntaje_bruto,p.estado_calculo,p.estado_normativo,p.motivo,
                           c.puntaje_convertido,c.id_rango
                    from resultado_escala p join escala e using(id_escala)
                    left join conversion_resultado_escala c on c.id_resultado_escala=p.id_resultado_escala
                      and c.tipo_salida='PERCENTIL'
                    where p.id_revision_resultado=? order by p.orden,p.id_resultado_escala
                    """,
                            (rs, n) -> {
                                if (!Objects.equals(
                                        vigente.get("id_test"), rs.getObject("factor_test")))
                                    throw inconsistente();
                                Integer directa = (Integer) rs.getObject("puntaje_bruto");
                                Integer percentil = (Integer) rs.getObject("puntaje_convertido");
                                Long rango = (Long) rs.getObject("id_rango");
                                String normativo = rs.getString("estado_normativo");
                                String codigo = rs.getString("codigo");
                                if ("CONVERTIDO".equals(normativo))
                                    verificarRango(vigente, codigo, directa, percentil, rango);
                                return new FactorResultado(
                                        codigo,
                                        directa,
                                        rs.getString("estado_calculo"),
                                        percentil,
                                        rango,
                                        normativo,
                                        rs.getString("motivo"));
                            },
                            referencia.longValue());
            revision =
                    new RevisionResultado(
                            referencia.longValue(),
                            ((Number) vigente.get("numero_revision")).intValue(),
                            (String) vigente.get("version_reglas"),
                            (Long) vigente.get("id_clave"),
                            (Long) vigente.get("id_baremo"),
                            factores);
        }
        return Optional.of(
                new ContenedorResultado(resultadoId, id, (String) fila.get("estado"), revision));
    }

    private void verificarRango(
            Map<String, Object> revision,
            String codigo,
            Integer directa,
            Integer percentil,
            Long rango) {
        if (directa == null
                || percentil == null
                || rango == null
                || revision.get("id_baremo") == null) throw inconsistente();
        // Si el modulo normativo no esta instalado, la consulta falla cerrada sin exponer valores.
        var rangos =
                jdbc.queryForList(
                        """
                select r.id_baremo,r.id_factor,r.percentil,r.limite_inferior,r.limite_superior,r.estado_celda
                from rango_baremo_bfa r join asignacion_baremo_bfa a using(id_baremo)
                where r.id_rango=? and a.id_version_formulario=? and a.version_reglas=?
                  and a.estado='APROBADO' and nullif(btrim(a.evidencia_aprobacion),'') is not null
                """,
                        rango,
                        revision.get("id_version_formulario"),
                        revision.get("version_reglas"));
        if (rangos.size() != 1) throw inconsistente();
        var r = rangos.get(0);
        if (!Objects.equals(revision.get("id_baremo"), r.get("id_baremo"))
                || !codigo.equals(r.get("id_factor"))
                || !percentil.equals(r.get("percentil"))
                || !"RANGO".equals(r.get("estado_celda"))
                || directa < ((Number) r.get("limite_inferior")).intValue()
                || directa > ((Number) r.get("limite_superior")).intValue()) throw inconsistente();
    }

    private static IllegalStateException inconsistente() {
        return new IllegalStateException("Revision publicada inconsistente");
    }
}
