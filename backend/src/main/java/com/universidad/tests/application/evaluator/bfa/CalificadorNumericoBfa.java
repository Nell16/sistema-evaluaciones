package com.universidad.tests.application.evaluator.bfa;

import com.universidad.tests.domain.model.OpcionRespuesta;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Valida las alternativas de Numerico y entrega conteos a la consolidacion BFA-BE-08.
 *
 * <p>Funcion interna sin HTTP, acceso a datos ni claves incorporadas. El llamador debe seleccionar
 * las preguntas puntuables y la clave aprobada compatible con la version aplicada. La seleccion de
 * esa version, la persistencia al cierre y la integracion con el motor quedan fuera de esta pieza.
 * No calcula ni trunca N1/N2; una omision permanece separada de los errores.
 */
public final class CalificadorNumericoBfa {

    /**
     * Comprueba el contexto del catalogo y la cobertura exacta de la clave antes del conteo. La
     * procedencia y aprobacion de la clave deben resolverse en el flujo de seleccion versionada.
     */
    public ConteoBloqueBfa calificarVersionado(
            BloqueNumericoVersionado bloque,
            Collection<RespuestaSeleccionBfa> respuestas,
            Map<Long, Long> clave,
            Collection<OpcionRespuesta> opciones) {
        Objects.requireNonNull(bloque, "El bloque versionado es obligatorio");
        Objects.requireNonNull(clave, "La clave es obligatoria");
        if (!bloque.idsPreguntas().equals(clave.keySet())) {
            throw new IllegalArgumentException(
                    "La clave no cubre exactamente las preguntas del bloque aplicado");
        }
        return calificar(bloque.codigoSubprueba(), respuestas, clave, opciones);
    }

    public ConteoBloqueBfa calificar(
            String codigoSubprueba,
            Collection<RespuestaSeleccionBfa> respuestas,
            Map<Long, Long> clave,
            Collection<OpcionRespuesta> opciones) {
        Objects.requireNonNull(respuestas, "respuestas es obligatorio");
        Objects.requireNonNull(clave, "clave es obligatoria");
        Objects.requireNonNull(opciones, "opciones es obligatorio");
        Set<String> permitidas;
        if ("N1-A".equals(codigoSubprueba) || "N1-B".equals(codigoSubprueba)) {
            permitidas = Set.of("A", "B");
        } else if ("N2-A".equals(codigoSubprueba) || "N2-B".equals(codigoSubprueba)) {
            permitidas = Set.of("A", "B", "C", "D");
        } else {
            throw new IllegalArgumentException("La subprueba no pertenece a la familia Numerico");
        }
        Map<Long, Map<Long, String>> opcionesPorPregunta = new HashMap<>();
        Set<Long> idsOpciones = new HashSet<>();
        for (OpcionRespuesta opcion : opciones) {
            Objects.requireNonNull(opcion, "Una opcion del bloque es nula");
            if (opcion.getIdOpcion() == null || !clave.containsKey(opcion.getIdPregunta())) {
                throw new IllegalArgumentException(
                        "La opcion no pertenece a una pregunta del bloque");
            }
            if (!permitidas.contains(opcion.getCodigo())) {
                throw new IllegalArgumentException(
                        "La alternativa no esta permitida para el bloque");
            }
            Map<Long, String> porId =
                    opcionesPorPregunta.computeIfAbsent(
                            opcion.getIdPregunta(), ignorada -> new HashMap<>());
            if (!idsOpciones.add(opcion.getIdOpcion()) || porId.containsValue(opcion.getCodigo())) {
                throw new IllegalArgumentException("El catalogo tiene opciones duplicadas");
            }
            porId.put(opcion.getIdOpcion(), opcion.getCodigo());
        }
        for (Map.Entry<Long, Long> entrada : clave.entrySet()) {
            Map<Long, String> porId = opcionesPorPregunta.get(entrada.getKey());
            if (porId == null || !new HashSet<>(porId.values()).equals(permitidas)) {
                throw new IllegalArgumentException("Las alternativas del bloque estan incompletas");
            }
            if (entrada.getValue() == null || !porId.containsKey(entrada.getValue())) {
                throw new IllegalArgumentException("La opcion correcta no pertenece a su pregunta");
            }
        }
        for (RespuestaSeleccionBfa respuesta : respuestas) {
            Objects.requireNonNull(respuesta, "Una respuesta del bloque es nula");
            Map<Long, String> porId = opcionesPorPregunta.get(respuesta.idPregunta());
            if (porId == null
                    || (respuesta.idOpcion() != null && !porId.containsKey(respuesta.idOpcion()))) {
                throw new IllegalArgumentException(
                        "La respuesta no pertenece a las opciones de su pregunta");
            }
        }
        return new CalificadorSeleccionBfa().calificar(respuestas, clave);
    }
}
