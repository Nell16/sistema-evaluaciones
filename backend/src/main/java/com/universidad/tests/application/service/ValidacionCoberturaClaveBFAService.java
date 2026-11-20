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

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

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

    // =====================================================
    // 1. VALIDAR COBERTURA PARA CONSULTA Y APROBACION
    // =====================================================

    @Transactional(readOnly = true)
    public void validarCoberturaSeleccion(Long idClave) {

        // Validar el identificador antes de consultar datos.
        validarId(idClave);

        // Exigir permiso de consulta de claves.
        // Este metodo se conserva para el proceso
        // administrativo de revision y aprobacion.
        autorizacion.exigirPermisoConsultarClave(idClave);

        // Ejecutar la validacion de cobertura.
        comprobarCoberturaSeleccion(idClave);
    }

    // =====================================================
    // 2. VALIDAR COBERTURA PARA CALIFICACION INTERNA
    // =====================================================

    @Transactional(readOnly = true)
    public void validarCoberturaSeleccionParaCalificacion(Long idClave) {

        // Validar el identificador.
        validarId(idClave);

        // Exigir permiso especifico para calificar.
        // No se requiere permiso administrativo
        // de consulta de claves.
        autorizacion.exigirPermisoCalificarConClave(idClave);

        // Reutilizar las mismas reglas de cobertura.
        comprobarCoberturaSeleccion(idClave);
    }

    // =====================================================
    // 3. COMPROBAR COBERTURA DE PREGUNTAS DE SELECCION
    // =====================================================

    private void comprobarCoberturaSeleccion(Long idClave) {

        // Obtener la version de clave.
        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        // Obtener las configuraciones del formulario
        // al que pertenece la clave.
        List<ConfiguracionSubprueba> configuraciones =
                configuracionRepository.listarPorVersion(clave.getIdVersionFormulario());

        // El formulario debe tener configuraciones.
        if (configuraciones.isEmpty()) {

            throw new IllegalStateException("El formulario no tiene subpruebas configuradas");
        }

        // =================================================
        // IDENTIFICAR LAS PREGUNTAS ESPERADAS
        // =================================================

        Set<Long> preguntasEsperadas = new HashSet<>();

        for (ConfiguracionSubprueba configuracion : configuraciones) {

            // Las claves de seleccion no se aplican
            // a subpruebas de texto libre.
            if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION) {

                continue;
            }

            // Recuperar las preguntas de la subprueba.
            List<Pregunta> preguntas =
                    preguntaRepository.listarPorConfiguracion(
                            configuracion.getIdConfiguracionSubprueba());

            // Una subprueba de seleccion debe
            // tener preguntas registradas.
            if (preguntas.isEmpty()) {

                throw new IllegalStateException("Una subprueba de seleccion no tiene preguntas");
            }

            // Incorporar los identificadores esperados.
            for (Pregunta pregunta : preguntas) {

                preguntasEsperadas.add(pregunta.getIdPregunta());
            }
        }

        // Debe existir al menos una pregunta de seleccion.
        if (preguntasEsperadas.isEmpty()) {

            throw new IllegalStateException("El formulario no tiene preguntas de seleccion");
        }

        // =================================================
        // IDENTIFICAR LAS PREGUNTAS CON CLAVE
        // =================================================

        List<EntradaClaveBFA> entradas = entradaRepository.listarPorClave(idClave);

        Set<Long> preguntasRegistradas = new HashSet<>();

        for (EntradaClaveBFA entrada : entradas) {

            preguntasRegistradas.add(entrada.getIdPregunta());
        }

        // =================================================
        // VALIDAR COBERTURA EXACTA
        // =================================================

        // La cantidad debe coincidir.
        // El conjunto de preguntas debe ser identico.
        //
        // Esto permite detectar:
        // - Preguntas sin respuesta correcta.
        // - Entradas adicionales o incompatibles.
        // - Preguntas duplicadas en las entradas.

        if (entradas.size() != preguntasEsperadas.size()
                || !preguntasRegistradas.equals(preguntasEsperadas)) {

            throw new IllegalStateException(
                    "La clave no cubre exactamente todas "
                            + "las preguntas de seleccion del formulario");
        }
    }

    // =====================================================
    // 4. VALIDAR IDENTIFICADORES
    // =====================================================

    private void validarId(Long idClave) {

        if (idClave == null || idClave <= 0) {

            throw new IllegalArgumentException("El identificador de la clave debe ser positivo");
        }
    }
}
