package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidacionCoberturaClaveBFAService {

    private final VersionClaveBFARepository claveRepository;
    private final EntradaClaveBFARepository entradaRepository;
    private final ConfiguracionSubpruebaRepository configuracionRepository;
    private final PreguntaRepository preguntaRepository;
    private final AutorizacionClaveBFAPort autorizacion;

    public ValidacionCoberturaClaveBFAService(
            VersionClaveBFARepository claveRepository,
            EntradaClaveBFARepository entradaRepository,
            ConfiguracionSubpruebaRepository configuracionRepository,
            PreguntaRepository preguntaRepository,
            AutorizacionClaveBFAPort autorizacion) {

        this.claveRepository = claveRepository;
        this.entradaRepository = entradaRepository;
        this.configuracionRepository = configuracionRepository;
        this.preguntaRepository = preguntaRepository;
        this.autorizacion = autorizacion;
    }

    @Transactional(readOnly = true)
    public void validarCoberturaSeleccion(Long idClave) {

        if (idClave == null || idClave <= 0) {
            throw new IllegalArgumentException("El identificador de la clave debe ser positivo");
        }

        // Exigir permiso antes de consultar respuestas correctas
        autorizacion.exigirPermisoConsultarClave(idClave);

        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        List<ConfiguracionSubprueba> configuraciones =
                configuracionRepository.listarPorVersion(clave.getIdVersionFormulario());

        if (configuraciones.isEmpty()) {
            throw new IllegalStateException("El formulario no tiene subpruebas configuradas");
        }

        // Reunir las preguntas de seleccion del formulario
        Set<Long> preguntasEsperadas = new HashSet<>();

        for (ConfiguracionSubprueba configuracion : configuraciones) {

            if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION) {
                continue;
            }

            List<Pregunta> preguntas =
                    preguntaRepository.listarPorConfiguracion(
                            configuracion.getIdConfiguracionSubprueba());

            if (preguntas.isEmpty()) {
                throw new IllegalStateException("Una subprueba de seleccion no tiene preguntas");
            }

            for (Pregunta pregunta : preguntas) {
                preguntasEsperadas.add(pregunta.getIdPregunta());
            }
        }

        if (preguntasEsperadas.isEmpty()) {
            throw new IllegalStateException("El formulario no tiene preguntas de seleccion");
        }

        // Consultar las entradas registradas en la clave
        List<EntradaClaveBFA> entradas = entradaRepository.listarPorClave(idClave);

        Set<Long> preguntasRegistradas = new HashSet<>();

        for (EntradaClaveBFA entrada : entradas) {
            preguntasRegistradas.add(entrada.getIdPregunta());
        }

        // Comprobar cobertura exacta
        if (entradas.size() != preguntasEsperadas.size()
                || !preguntasRegistradas.equals(preguntasEsperadas)) {

            throw new IllegalStateException(
                    "La clave no cubre exactamente todas "
                            + "las preguntas de seleccion del formulario");
        }
    }
}
