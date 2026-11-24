package com.universidad.tests.application.evaluator.bfa;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.Subprueba;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionFormulario;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Instantanea interna del catalogo aplicado, sin persistencia ni contrato HTTP.
 *
 * <p>Comprueba las relaciones de los objetos recibidos; no carga ni certifica su procedencia en
 * base de datos. La seleccion y aprobacion de la clave corresponde al flujo de BFA-BE-09/01. El
 * calificador comprueba las alternativas y que la clave cubra exactamente estas preguntas.
 */
public final class BloqueNumericoVersionado {

    private final Long idVersionFormulario;
    private final String codigoSubprueba;
    private final Set<Long> idsPreguntas;

    private BloqueNumericoVersionado(Long version, String codigo, Set<Long> preguntas) {
        idVersionFormulario = version;
        codigoSubprueba = codigo;
        idsPreguntas = Set.copyOf(preguntas);
    }

    public static BloqueNumericoVersionado desde(
            VersionFormulario versionAplicada,
            ConfiguracionSubprueba configuracion,
            Subprueba subprueba,
            Collection<Pregunta> preguntas) {
        Objects.requireNonNull(versionAplicada, "La version aplicada es obligatoria");
        Objects.requireNonNull(configuracion, "La configuracion es obligatoria");
        Objects.requireNonNull(subprueba, "La subprueba es obligatoria");
        Objects.requireNonNull(preguntas, "Las preguntas son obligatorias");
        if (!tieneIdValido(versionAplicada.getIdVersionFormulario())
                || !versionAplicada
                        .getIdVersionFormulario()
                        .equals(configuracion.getIdVersionFormulario())) {
            throw new IllegalArgumentException(
                    "La configuracion no pertenece a la version aplicada");
        }
        if (!tieneIdValido(configuracion.getIdConfiguracionSubprueba())
                || !tieneIdValido(subprueba.getIdSubprueba())
                || !subprueba.getIdSubprueba().equals(configuracion.getIdSubprueba())
                || !subprueba.getIdTest().equals(versionAplicada.getIdTest())) {
            throw new IllegalArgumentException(
                    "La subprueba y su configuracion no pertenecen al test aplicado");
        }
        if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION) {
            throw new IllegalArgumentException("Numerico requiere una configuracion de seleccion");
        }
        if (preguntas.isEmpty()) {
            throw new IllegalArgumentException("El bloque no tiene preguntas puntuables");
        }
        Set<Long> ids = new HashSet<>();
        for (Pregunta pregunta : preguntas) {
            Objects.requireNonNull(pregunta, "Una pregunta del bloque es nula");
            if (!tieneIdValido(pregunta.getIdPregunta())
                    || !configuracion
                            .getIdConfiguracionSubprueba()
                            .equals(pregunta.getIdConfiguracionSubprueba())
                    || !ids.add(pregunta.getIdPregunta())) {
                throw new IllegalArgumentException(
                        "Las preguntas no identifican un bloque unico de la configuracion");
            }
        }
        return new BloqueNumericoVersionado(
                versionAplicada.getIdVersionFormulario(), subprueba.getCodigo(), ids);
    }

    private static boolean tieneIdValido(Long id) {
        return id != null && id > 0;
    }

    public Long idVersionFormulario() {
        return idVersionFormulario;
    }

    public String codigoSubprueba() {
        return codigoSubprueba;
    }

    public Set<Long> idsPreguntas() {
        return idsPreguntas;
    }
}
