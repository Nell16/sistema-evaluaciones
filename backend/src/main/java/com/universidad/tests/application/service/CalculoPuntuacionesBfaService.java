package com.universidad.tests.application.service;

import com.universidad.tests.application.evaluator.bfa.CalificadorVerbalBfa;
import com.universidad.tests.application.evaluator.bfa.RespuestaSeleccionBfa;
import com.universidad.tests.domain.port.InsumosCalificacionBfaPort;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Conecta las respuestas persistidas con los calificadores disponibles, sin fabricar faltantes. */
@Service
public class CalculoPuntuacionesBfaService {
    private final InsumosCalificacionBfaPort insumos;

    public CalculoPuntuacionesBfaService(InsumosCalificacionBfaPort insumos) {
        this.insumos = insumos;
    }

    public Optional<Calculo> calcular(Long evaluacion, Long version) {
        return insumos.leer(evaluacion, version)
                .map(
                        bloques -> {
                            Map<String, Integer> puntuaciones = new LinkedHashMap<>();
                            Long claveUsada = null;
                            var calificador = new CalificadorVerbalBfa();
                            for (var bloque : bloques) {
                                if (claveUsada != null && !claveUsada.equals(bloque.idClave()))
                                    throw new IllegalStateException(
                                            "Claves incompatibles en la evaluacion");
                                claveUsada = bloque.idClave();
                                var respuestas =
                                        bloque.respuestas().stream()
                                                .map(
                                                        r ->
                                                                new RespuestaSeleccionBfa(
                                                                        r.pregunta(), r.opcion()))
                                                .toList();
                                int aciertos =
                                        calificador
                                                .calificar(
                                                        bloque.codigo(),
                                                        respuestas,
                                                        bloque.clave(),
                                                        bloque.opciones())
                                                .aciertos();
                                if (puntuaciones.put(bloque.codigo(), aciertos) != null)
                                    throw new IllegalStateException("Bloque BFA duplicado");
                            }
                            return new Calculo(Map.copyOf(puntuaciones), claveUsada);
                        });
    }

    public record Calculo(Map<String, Integer> puntuaciones, Long idClave) {
        public Calculo {
            puntuaciones = Map.copyOf(puntuaciones);
        }
    }
}
