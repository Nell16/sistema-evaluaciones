package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EstadoVersionFormulario;
import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.port.AutorizacionContenidoPort;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpcionesVersionadasService {
    private final VersionFormularioRepository versiones;
    private final ConfiguracionSubpruebaRepository configuraciones;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository opciones;
    private final AutorizacionContenidoPort autorizacion;
    private final EdicionContenidoVersionadoService edicion;

    public OpcionesVersionadasService(
            VersionFormularioRepository versiones,
            ConfiguracionSubpruebaRepository configuraciones,
            PreguntaRepository preguntas,
            OpcionRespuestaRepository opciones,
            AutorizacionContenidoPort autorizacion,
            EdicionContenidoVersionadoService edicion) {
        this.versiones = versiones;
        this.configuraciones = configuraciones;
        this.preguntas = preguntas;
        this.opciones = opciones;
        this.autorizacion = autorizacion;
        this.edicion = edicion;
    }

    @Transactional
    public List<DatosOpcion> guardar(Long idVersion, Long idPregunta, List<DatosOpcion> datos) {
        if (idVersion == null || idVersion <= 0 || idPregunta == null || idPregunta <= 0) {
            throw new IllegalArgumentException("Los identificadores deben ser positivos");
        }
        if (datos == null || datos.isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos una opcion");
        }
        Set<String> codigos = new HashSet<>();
        Set<Integer> ordenes = new HashSet<>();
        for (DatosOpcion dato : datos) {
            if (dato == null
                    || dato.letra() == null
                    || dato.letra().isBlank()
                    || dato.orden() <= 0
                    || !codigos.add(dato.letra())
                    || !ordenes.add(dato.orden())) {
                throw new IllegalArgumentException(
                        "Las letras y los ordenes deben ser unicos y validos");
            }
        }
        autorizacion.exigirPermisoEditarContenido(idVersion);
        VersionFormulario version =
                versiones
                        .buscarPorId(idVersion)
                        .orElseThrow(() -> new NoSuchElementException("Version no encontrada"));
        if (version.getEstado() != EstadoVersionFormulario.BORRADOR) {
            throw new IllegalStateException("VERSION_NO_EDITABLE");
        }
        Pregunta pregunta =
                preguntas
                        .buscarPorId(idPregunta)
                        .orElseThrow(() -> new NoSuchElementException("Pregunta no encontrada"));
        ConfiguracionSubprueba configuracion =
                configuraciones
                        .buscarPorId(pregunta.getIdConfiguracionSubprueba())
                        .orElseThrow(
                                () -> new NoSuchElementException("Configuracion no encontrada"));
        if (!Objects.equals(configuracion.getIdVersionFormulario(), idVersion)) {
            throw new NoSuchElementException("Pregunta no encontrada en esta version");
        }
        if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION) {
            throw new IllegalArgumentException("La subprueba de texto libre no admite opciones");
        }
        if (configuracion.getOpcionesEsperadas() != null
                && datos.size() != configuracion.getOpcionesEsperadas()) {
            throw new IllegalArgumentException(
                    "La cantidad de opciones no coincide con la configuracion");
        }
        List<OpcionRespuesta> anteriores = opciones.listarPorPregunta(idPregunta);
        for (DatosOpcion dato : datos) {
            OpcionRespuesta anterior =
                    anteriores.stream()
                            .filter(o -> o.getCodigo().equals(dato.letra()))
                            .findFirst()
                            .orElse(null);
            edicion.guardarOpcion(
                    new OpcionRespuesta(
                            anterior == null ? null : anterior.getIdOpcion(),
                            idPregunta,
                            dato.letra(),
                            dato.orden(),
                            dato.texto(),
                            dato.recursoRef()));
        }
        for (OpcionRespuesta anterior : anteriores) {
            if (!codigos.contains(anterior.getCodigo())) {
                opciones.eliminarPorId(anterior.getIdOpcion());
            }
        }
        return opciones.listarPorPregunta(idPregunta).stream()
                .map(
                        o ->
                                new DatosOpcion(
                                        o.getCodigo(),
                                        o.getOrdenPresentacion(),
                                        o.getTexto(),
                                        o.getRecursoRef()))
                .toList();
    }

    public record DatosOpcion(String letra, int orden, String texto, String recursoRef) {}
}
