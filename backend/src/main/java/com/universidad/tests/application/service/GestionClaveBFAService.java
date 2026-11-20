package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
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
public class GestionClaveBFAService {

    private final VersionClaveBFARepository claveRepository;
    private final EntradaClaveBFARepository entradaRepository;
    private final VersionFormularioRepository formularioRepository;
    private final AutorizacionClaveBFAPort autorizacion;

    public GestionClaveBFAService(
            VersionClaveBFARepository claveRepository,
            EntradaClaveBFARepository entradaRepository,
            VersionFormularioRepository formularioRepository,
            AutorizacionClaveBFAPort autorizacion) {

        this.claveRepository = claveRepository;
        this.entradaRepository = entradaRepository;
        this.formularioRepository = formularioRepository;
        this.autorizacion = autorizacion;
    }

    // =====================================================
    // 1. CREAR UNA VERSION DE CLAVE BFA
    // =====================================================

    @Transactional
    public VersionClaveBFA crearClave(
            Long idVersionFormulario, String version, String referenciaFuente) {

        validarId(idVersionFormulario);

        // Exigir autorizacion antes de crear la clave
        autorizacion.exigirPermisoCrearClave(idVersionFormulario);

        // Comprobar que el formulario exista
        formularioRepository
                .buscarPorId(idVersionFormulario)
                .orElseThrow(
                        () ->
                                new NoSuchElementException(
                                        "No existe la version de formulario indicada"));

        // Crear la clave en estado BORRADOR
        VersionClaveBFA clave =
                new VersionClaveBFA(null, idVersionFormulario, version, referenciaFuente);

        return claveRepository.guardar(clave);
    }

    // =====================================================
    // 2. REGISTRAR UNA ENTRADA DE CORRECCION
    // =====================================================

    @Transactional
    public EntradaClaveBFA registrarEntrada(Long idClave, Long idPregunta, Long idOpcionCorrecta) {

        validarId(idClave);
        validarId(idPregunta);
        validarId(idOpcionCorrecta);

        // Exigir autorizacion para registrar respuestas correctas
        autorizacion.exigirPermisoAgregarEntrada(idClave);

        // El adaptador verificara la compatibilidad
        // entre clave, formulario, pregunta y opcion
        EntradaClaveBFA entrada = new EntradaClaveBFA(null, idClave, idPregunta, idOpcionCorrecta);

        return entradaRepository.guardar(entrada);
    }

    // =====================================================
    // 3. REGISTRAR MULTIPLES ENTRADAS DE CORRECCION
    // =====================================================

    @Transactional
    public List<EntradaClaveBFA> registrarEntradas(Long idClave, List<EntradaClaveBFA> entradas) {

        // Validar el identificador de la clave
        validarId(idClave);

        // Exigir autorizacion antes de consultar o modificar datos
        autorizacion.exigirPermisoAgregarEntrada(idClave);

        // Rechazar listas nulas o vacias
        if (entradas == null || entradas.isEmpty()) {
            throw new IllegalArgumentException("La lista de entradas no puede estar vacia");
        }

        // Validar todas las entradas antes de guardarlas
        Set<Long> preguntasRegistradas = new HashSet<>();

        for (EntradaClaveBFA entrada : entradas) {

            if (entrada == null) {
                throw new IllegalArgumentException("La lista contiene una entrada nula");
            }

            if (entrada.getIdEntradaClave() != null) {
                throw new IllegalArgumentException(
                        "Las entradas nuevas no deben tener identificador");
            }

            if (!idClave.equals(entrada.getIdClave())) {
                throw new IllegalArgumentException(
                        "Todas las entradas deben pertenecer a la misma clave");
            }

            validarId(entrada.getIdPregunta());
            validarId(entrada.getIdOpcionCorrecta());

            // Impedir preguntas repetidas en la misma carga
            if (!preguntasRegistradas.add(entrada.getIdPregunta())) {
                throw new IllegalArgumentException("La carga contiene preguntas duplicadas");
            }
        }

        // Comprobar que la clave existe
        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        // Solo se permite modificar claves en BORRADOR
        if (!clave.esEditable()) {
            throw new IllegalStateException(
                    "No se pueden agregar entradas a una clave aprobada o retirada");
        }

        // Guardar las entradas en una misma transaccion
        List<EntradaClaveBFA> entradasGuardadas = new ArrayList<>();

        for (EntradaClaveBFA entrada : entradas) {

            EntradaClaveBFA nuevaEntrada =
                    new EntradaClaveBFA(
                            null, idClave, entrada.getIdPregunta(), entrada.getIdOpcionCorrecta());

            entradasGuardadas.add(entradaRepository.guardar(nuevaEntrada));
        }

        return List.copyOf(entradasGuardadas);
    }

    // =====================================================
    // 4. CONSULTAR UNA VERSION DE CLAVE BFA
    // =====================================================

    @Transactional(readOnly = true)
    public VersionClaveBFA consultarClave(Long idClave) {

        validarId(idClave);

        // Comprobar permisos antes de consultar
        autorizacion.exigirPermisoConsultarClave(idClave);

        return claveRepository
                .buscarPorId(idClave)
                .orElseThrow(() -> new NoSuchElementException("No existe la clave indicada"));
    }

    // =====================================================
    // 5. CONSULTAR LAS ENTRADAS DE UNA CLAVE
    // =====================================================

    @Transactional(readOnly = true)
    public List<EntradaClaveBFA> consultarEntradas(Long idClave) {

        validarId(idClave);

        // Impedir consultas sin autorizacion
        autorizacion.exigirPermisoConsultarClave(idClave);

        // Verificar que la clave exista
        claveRepository
                .buscarPorId(idClave)
                .orElseThrow(() -> new NoSuchElementException("No existe la clave indicada"));

        return entradaRepository.listarPorClave(idClave);
    }

    // =====================================================
    // 6. VALIDACION INTERNA DE IDENTIFICADORES
    // =====================================================

    private void validarId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El identificador debe ser positivo");
        }
    }
}
