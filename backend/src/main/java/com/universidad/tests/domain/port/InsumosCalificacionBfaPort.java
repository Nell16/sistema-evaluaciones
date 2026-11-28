package com.universidad.tests.domain.port;

import com.universidad.tests.domain.model.OpcionRespuesta;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Insumos privados de la version aplicada. No incluye textos ni expone el contenido de la clave.
 */
public interface InsumosCalificacionBfaPort {
    Optional<List<Bloque>> leer(Long evaluacionId, Long versionFormularioId);

    record Respuesta(Long pregunta, Long opcion) {}

    record Bloque(
            Long idClave,
            String codigo,
            Map<Long, Long> clave,
            List<OpcionRespuesta> opciones,
            List<Respuesta> respuestas) {
        public Bloque {
            java.util.Objects.requireNonNull(idClave, "La clave usada es obligatoria");
            clave = Map.copyOf(clave);
            opciones = List.copyOf(opciones);
            respuestas = List.copyOf(respuestas);
        }
    }
}
