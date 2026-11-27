package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.port.AutorizacionContenidoPort;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CargarClaveVersionadaService {
    private final VersionFormularioRepository versiones;
    private final ConfiguracionSubpruebaRepository configuraciones;
    private final PreguntaRepository preguntas;
    private final OpcionRespuestaRepository.Edicion opciones;
    private final VersionClaveBFARepository claves;
    private final EntradaClaveBFARepository entradas;
    private final AutorizacionContenidoPort autorizacion;

    public CargarClaveVersionadaService(
            VersionFormularioRepository versiones,
            ConfiguracionSubpruebaRepository configuraciones,
            PreguntaRepository preguntas,
            OpcionRespuestaRepository.Edicion opciones,
            VersionClaveBFARepository claves,
            EntradaClaveBFARepository entradas,
            AutorizacionContenidoPort autorizacion) {
        this.versiones = versiones;
        this.configuraciones = configuraciones;
        this.preguntas = preguntas;
        this.opciones = opciones;
        this.claves = claves;
        this.entradas = entradas;
        this.autorizacion = autorizacion;
    }

    @Transactional
    public Long cargar(Long idVersion, List<DatoClave> datos) {
        if (datos == null || datos.isEmpty()) {
            throw new IllegalArgumentException("La version y las entradas son obligatorias");
        }
        autorizacion.exigirPermisoEditarOpciones(idVersion);
        if (idVersion == null || idVersion <= 0) {
            throw new NoSuchElementException("Version no encontrada");
        }
        opciones.bloquearVersion(idVersion);
        VersionFormulario version =
                versiones
                        .buscarPorId(idVersion)
                        .orElseThrow(() -> new NoSuchElementException("Version no encontrada"));
        if (!version.esEditable()) {
            throw new IllegalStateException("VERSION_NO_EDITABLE");
        }

        Set<Long> esperadas = new HashSet<>();
        for (ConfiguracionSubprueba configuracion : configuraciones.listarPorVersion(idVersion)) {
            if (configuracion.getTipoRespuesta() == TipoRespuesta.SELECCION) {
                for (Pregunta pregunta :
                        preguntas.listarPorConfiguracion(
                                configuracion.getIdConfiguracionSubprueba())) {
                    esperadas.add(pregunta.getIdPregunta());
                }
            }
        }

        Set<Long> recibidas = new HashSet<>();
        List<OpcionElegida> opcionesElegidas = new ArrayList<>();
        for (DatoClave dato : datos) {
            if (dato == null
                    || dato.preguntaId() == null
                    || dato.preguntaId() <= 0
                    || dato.opcion() == null
                    || dato.opcion().isBlank()
                    || !recibidas.add(dato.preguntaId())
                    || !esperadas.contains(dato.preguntaId())) {
                throw new IllegalArgumentException("CLAVE_INVALIDA");
            }
            OpcionRespuesta opcion =
                    opciones.buscarPorCodigo(dato.preguntaId(), dato.opcion())
                            .orElseThrow(() -> new IllegalArgumentException("CLAVE_INVALIDA"));
            if (!dato.preguntaId().equals(opcion.getIdPregunta())) {
                throw new IllegalArgumentException("CLAVE_INVALIDA");
            }
            opcionesElegidas.add(new OpcionElegida(dato.preguntaId(), opcion.getIdOpcion()));
        }
        if (!recibidas.equals(esperadas)) {
            throw new IllegalArgumentException("CLAVE_INCOMPLETA");
        }

        VersionClaveBFA clave =
                claves.buscarPorFormularioYVersion(idVersion, version.getCodigoVersion())
                        .orElse(null);
        if (clave == null) {
            clave =
                    claves.guardar(
                            new VersionClaveBFA(
                                    null,
                                    idVersion,
                                    version.getCodigoVersion(),
                                    version.getFuenteRef()));
        } else if (!clave.esEditable()) {
            throw new IllegalStateException("CLAVE_NO_EDITABLE");
        }

        List<EntradaClaveBFA> nuevas = new ArrayList<>();
        for (OpcionElegida elegida : opcionesElegidas) {
            nuevas.add(
                    new EntradaClaveBFA(
                            null, clave.getIdClave(), elegida.preguntaId(), elegida.opcionId()));
        }
        entradas.reemplazarEntradasBorrador(clave.getIdClave(), nuevas);
        return clave.getIdClave();
    }

    public record DatoClave(Long preguntaId, String opcion) {}

    private record OpcionElegida(Long preguntaId, Long opcionId) {}
}
