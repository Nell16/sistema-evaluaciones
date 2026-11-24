package com.universidad.tests.application.usecase.bfa;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga la clave de respuestas FICTICIA de la version demo del BFA (BFA-BE-39). Cada pregunta de
 * seleccion recibe como correcta una opcion inventada y rotativa. Nunca contiene claves reales.
 * Escribe directo en los repositorios: el servicio E29 exige un permiso que en el arranque no
 * existe.
 */
@Service
public class CargarClaveDemoBfaUseCase {

    static final String FUENTE_CLAVE_DEMO = "demo/clave-ficticia";

    private final VersionFormularioRepository versionRepository;
    private final ConfiguracionSubpruebaRepository configuracionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionRespuestaRepository opcionRepository;
    private final VersionClaveBFARepository claveRepository;
    private final EntradaClaveBFARepository entradaRepository;

    public CargarClaveDemoBfaUseCase(
            VersionFormularioRepository versionRepository,
            ConfiguracionSubpruebaRepository configuracionRepository,
            PreguntaRepository preguntaRepository,
            OpcionRespuestaRepository opcionRepository,
            VersionClaveBFARepository claveRepository,
            EntradaClaveBFARepository entradaRepository) {
        this.versionRepository = versionRepository;
        this.configuracionRepository = configuracionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.claveRepository = claveRepository;
        this.entradaRepository = entradaRepository;
    }

    /** Devuelve el id de la clave de la version. Si ya esta cargada, no la toca. */
    @Transactional
    public Long cargarClave(Long idVersionFormulario) {
        VersionFormulario version =
                versionRepository
                        .buscarPorId(idVersionFormulario)
                        .orElseThrow(() -> new NoSuchElementException("Version no encontrada"));

        Optional<VersionClaveBFA> existente =
                claveRepository.buscarPorFormularioYVersion(
                        idVersionFormulario, version.getCodigoVersion());
        if (existente.isPresent()
                && (!existente.get().esEditable()
                        || entradaRepository.contarPorClave(existente.get().getIdClave()) > 0)) {
            return existente.get().getIdClave();
        }

        List<Long[]> elegidas = elegirOpcionesCorrectas(idVersionFormulario);
        if (elegidas.isEmpty()) {
            throw new IllegalStateException("La version demo no tiene preguntas de seleccion");
        }

        VersionClaveBFA clave =
                existente.orElseGet(
                        () ->
                                claveRepository.guardar(
                                        new VersionClaveBFA(
                                                null,
                                                idVersionFormulario,
                                                version.getCodigoVersion(),
                                                FUENTE_CLAVE_DEMO)));

        List<EntradaClaveBFA> entradas = new ArrayList<>();
        for (Long[] par : elegidas) {
            entradas.add(new EntradaClaveBFA(null, clave.getIdClave(), par[0], par[1]));
        }
        entradaRepository.reemplazarEntradasBorrador(clave.getIdClave(), entradas);
        return clave.getIdClave();
    }

    // Pares {idPregunta, idOpcionCorrecta}; la correcta rota A, B, C... segun la pregunta
    private List<Long[]> elegirOpcionesCorrectas(Long idVersionFormulario) {
        List<Long[]> elegidas = new ArrayList<>();
        for (ConfiguracionSubprueba configuracion :
                configuracionRepository.listarPorVersion(idVersionFormulario)) {
            if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION) {
                continue;
            }
            List<Pregunta> preguntas =
                    new ArrayList<>(
                            preguntaRepository.listarPorConfiguracion(
                                    configuracion.getIdConfiguracionSubprueba()));
            preguntas.sort(Comparator.comparingInt(Pregunta::getOrdenPresentacion));
            for (int i = 0; i < preguntas.size(); i++) {
                Pregunta pregunta = preguntas.get(i);
                int alternativas =
                        opcionRepository.listarPorPregunta(pregunta.getIdPregunta()).size();
                String codigo = String.valueOf((char) ('A' + (i % alternativas)));
                OpcionRespuesta opcion =
                        opcionRepository
                                .buscarPorCodigo(pregunta.getIdPregunta(), codigo)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Falta la opcion " + codigo));
                elegidas.add(new Long[] {pregunta.getIdPregunta(), opcion.getIdOpcion()});
            }
        }
        return elegidas;
    }
}
