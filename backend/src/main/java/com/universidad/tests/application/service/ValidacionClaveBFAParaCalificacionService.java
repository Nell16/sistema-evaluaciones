package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValidacionClaveBFAParaCalificacionService {

    private final VersionClaveBFARepository claveRepository;
    private final EntradaClaveBFARepository entradaRepository;
    private final ValidacionCoberturaClaveBFAService validadorCobertura;
    private final AutorizacionClaveBFAPort autorizacion;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ValidacionClaveBFAParaCalificacionService(
            VersionClaveBFARepository claveRepository,
            EntradaClaveBFARepository entradaRepository,
            ValidacionCoberturaClaveBFAService validadorCobertura,
            AutorizacionClaveBFAPort autorizacion) {

        this.claveRepository = claveRepository;
        this.entradaRepository = entradaRepository;
        this.validadorCobertura = validadorCobertura;
        this.autorizacion = autorizacion;
    }

    // =====================================================
    // 1. VALIDAR UNA CLAVE PARA NUEVAS CALIFICACIONES
    // =====================================================

    @Transactional(readOnly = true)
    public VersionClaveBFA obtenerClaveAprobada(Long idClave, Long idVersionFormulario) {

        // Validar identificadores.
        validarId(idClave);
        validarId(idVersionFormulario);

        // Exigir autorizacion antes de consultar datos.
        autorizacion.exigirPermisoCalificarConClave(idClave);

        // Comprobar que la clave exista.
        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () ->
                                        new NoSuchElementException(
                                                "No existe la clave BFA indicada"));

        // Verificar la version del formulario.
        if (!idVersionFormulario.equals(clave.getIdVersionFormulario())) {

            throw new IllegalStateException(
                    "La clave BFA no pertenece " + "a la version de formulario indicada");
        }

        // Solo permitir claves aprobadas.
        clave.exigirAptaParaNuevaCalificacion();

        return clave;
    }

    // =====================================================
    // 2. OBTENER RESPUESTAS CORRECTAS PARA CALIFICACION
    // =====================================================

    @Transactional(readOnly = true)
    public List<EntradaClaveBFA> obtenerEntradasAprobadasParaCalificacion(
            Long idClave, Long idVersionFormulario) {

        // PASO 1:
        // Verificar permiso, existencia,
        // compatibilidad y aprobacion.
        obtenerClaveAprobada(idClave, idVersionFormulario);

        // PASO 2:
        // Validar la cobertura utilizando el permiso
        // especifico de calificacion BFA.
        //
        // Ya no se exige el permiso administrativo
        // de consulta de claves.
        validadorCobertura.validarCoberturaSeleccionParaCalificacion(idClave);

        // PASO 3:
        // Recuperar las respuestas correctas
        // de la version aprobada.
        List<EntradaClaveBFA> entradas = entradaRepository.listarPorClave(idClave);

        // PASO 4:
        // Devolver una lista no modificable.
        return List.copyOf(entradas);
    }

    // =====================================================
    // 3. VALIDAR IDENTIFICADORES
    // =====================================================

    private void validarId(Long id) {

        if (id == null || id <= 0) {

            throw new IllegalArgumentException("El identificador debe ser positivo");
        }
    }
}
