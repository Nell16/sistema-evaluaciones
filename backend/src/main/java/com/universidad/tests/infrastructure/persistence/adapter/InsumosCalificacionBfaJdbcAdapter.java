package com.universidad.tests.infrastructure.persistence.adapter;

import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.port.InsumosCalificacionBfaPort;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lee solo respuestas de bloques cerrados y la unica clave aprobada de la version aplicada. */
@Repository
public class InsumosCalificacionBfaJdbcAdapter implements InsumosCalificacionBfaPort {
    private final JdbcTemplate jdbc;

    public InsumosCalificacionBfaJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<List<Bloque>> leer(Long id, Long version) {
        var esBfa =
                jdbc.queryForObject(
                        "select exists(select 1 from evaluacion e join test t using(id_test) where e.id_evaluacion=? and e.id_version_formulario=? and upper(t.codigo)='BFA')",
                        Boolean.class,
                        id,
                        version);
        if (!Boolean.TRUE.equals(esBfa)) return Optional.empty();
        boolean cabecera =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "select to_regclass('version_clave_bfa') is not null",
                                Boolean.class));
        boolean entradas =
                Boolean.TRUE.equals(
                        jdbc.queryForObject(
                                "select to_regclass('entrada_clave_bfa') is not null",
                                Boolean.class));
        if (!cabecera && !entradas) return Optional.of(List.of());
        if (!cabecera || !entradas) throw new IllegalStateException("Modulo de claves incompleto");
        var claves =
                jdbc.queryForList(
                        "select id_clave from version_clave_bfa where id_version_formulario=? and estado='APROBADA' and nullif(btrim(evidencia_aprobacion),'') is not null",
                        Long.class,
                        version);
        if (claves.isEmpty()) return Optional.of(List.of());
        if (claves.size() != 1)
            throw new IllegalStateException("La version no tiene una unica clave aprobada");
        var bloques =
                jdbc.queryForList(
                        """
                select es.codigo,es.id_configuracion_subprueba
                from evaluacion_subprueba es join configuracion_subprueba c using(id_configuracion_subprueba)
                join subprueba s using(id_subprueba) join version_formulario v using(id_version_formulario)
                where es.id_evaluacion=? and es.estado='CERRADA' and c.id_version_formulario=?
                  and s.id_test=v.id_test and s.codigo=es.codigo and c.tipo_respuesta='SELECCION'
                  and es.codigo in ('VOC1','VOC2','CV') order by es.orden
                """,
                        id,
                        version);
        List<Bloque> salida = new ArrayList<>();
        for (var bloque : bloques) {
            String codigo = (String) bloque.get("codigo");
            Long configuracion = ((Number) bloque.get("id_configuracion_subprueba")).longValue();
            var preguntas =
                    jdbc.queryForList(
                            "select id_pregunta from pregunta where id_configuracion_subprueba=? and not (? in ('VOC1','VOC2') and upper(numero_visible) in ('A','B')) order by orden_presentacion",
                            Long.class,
                            configuracion,
                            codigo);
            if (preguntas.isEmpty()) continue;
            Map<Long, Long> clave = new LinkedHashMap<>();
            List<OpcionRespuesta> opciones = new ArrayList<>();
            List<Respuesta> respuestas = new ArrayList<>();
            for (Long pregunta : preguntas) {
                var correctas =
                        jdbc.queryForList(
                                "select id_opcion_correcta from entrada_clave_bfa where id_clave=? and id_pregunta=?",
                                Long.class,
                                claves.get(0),
                                pregunta);
                if (correctas.size() != 1)
                    throw new IllegalStateException(
                            "Clave aprobada incompleta para el bloque aplicado");
                clave.put(pregunta, correctas.get(0));
                opciones.addAll(
                        jdbc.query(
                                "select id_opcion,id_pregunta,codigo,orden_presentacion from opcion_respuesta where id_pregunta=? order by orden_presentacion",
                                (rs, n) ->
                                        new OpcionRespuesta(
                                                rs.getLong(1),
                                                rs.getLong(2),
                                                rs.getString(3),
                                                rs.getInt(4),
                                                "Opcion",
                                                null),
                                pregunta));
            }
            var registros =
                    jdbc.query(
                            "select id_pregunta,id_opcion from respuesta_evaluacion where id_evaluacion=? and subprueba_codigo=? and id_pregunta is not null order by orden_entrada",
                            (rs, n) -> new Respuesta(rs.getLong(1), (Long) rs.getObject(2)),
                            id,
                            codigo);
            var todas =
                    jdbc.queryForList(
                            "select id_pregunta from pregunta where id_configuracion_subprueba=?",
                            Long.class,
                            configuracion);
            for (var r : registros)
                if (!todas.contains(r.pregunta()))
                    throw new IllegalStateException("Respuesta ajena al bloque aplicado");
            // Los ejemplos A/B no participan en la calificacion, aunque exista respuesta de
            // practica.
            for (var r : registros) if (preguntas.contains(r.pregunta())) respuestas.add(r);
            salida.add(new Bloque(claves.get(0), codigo, clave, opciones, respuestas));
        }
        return Optional.of(List.copyOf(salida));
    }
}
