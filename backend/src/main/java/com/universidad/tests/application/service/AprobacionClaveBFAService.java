package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AprobacionClaveBFAService {

    private final VersionClaveBFARepository claveRepository;
    private final ValidacionCoberturaClaveBFAService validadorCobertura;
    private final AutorizacionClaveBFAPort autorizacion;

    public AprobacionClaveBFAService(
            VersionClaveBFARepository claveRepository,
            ValidacionCoberturaClaveBFAService validadorCobertura,
            AutorizacionClaveBFAPort autorizacion) {

        this.claveRepository = claveRepository;
        this.validadorCobertura = validadorCobertura;
        this.autorizacion = autorizacion;
    }

    // =====================================================
    // 1. APROBAR UNA CLAVE DE CORRECCION
    // =====================================================

    @Transactional
    public VersionClaveBFA aprobarClave(Long idClave, String evidenciaAprobacion) {

        validarId(idClave);

        // Verificar permisos antes de consultar datos.
        autorizacion.exigirPermisoAprobarClave(idClave);

        // La aprobacion requiere evidencia institucional.
        if (evidenciaAprobacion == null || evidenciaAprobacion.isBlank()) {

            throw new IllegalArgumentException("La aprobacion requiere evidencia institucional");
        }

        // Comprobar que la clave exista.
        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        // Solo se puede aprobar una clave en BORRADOR.
        if (!clave.esEditable()) {
            throw new IllegalStateException("Solo se puede aprobar una clave en BORRADOR");
        }

        // Verificar que todas las preguntas de seleccion
        // del formulario tengan su entrada de correccion.
        // Este validador tambien exige permiso de consulta.
        validadorCobertura.validarCoberturaSeleccion(idClave);

        // La persistencia vuelve a verificar el permiso
        // y las reglas de transicion antes de guardar.
        return claveRepository.aprobar(idClave, evidenciaAprobacion);
    }

    // =====================================================
    // 2. RETIRAR UNA CLAVE APROBADA
    // =====================================================

    @Transactional
    public VersionClaveBFA retirarClave(Long idClave) {

        validarId(idClave);

        // Solo un usuario autorizado puede retirarla.
        autorizacion.exigirPermisoRetirarClave(idClave);

        VersionClaveBFA clave =
                claveRepository
                        .buscarPorId(idClave)
                        .orElseThrow(
                                () -> new NoSuchElementException("No existe la clave indicada"));

        // Impedir retirar claves en BORRADOR
        // o claves que ya fueron retiradas.
        if (clave.getEstado() != EstadoVersionClaveBFA.APROBADA) {
            throw new IllegalStateException("Solo se puede retirar una clave APROBADA");
        }

        // La evidencia original debe conservarse.
        return claveRepository.retirar(idClave);
    }

    // =====================================================
    // 3. VALIDAR IDENTIFICADORES
    // =====================================================

    private void validarId(Long idClave) {

        if (idClave == null || idClave <= 0) {
            throw new IllegalArgumentException("El identificador de la clave debe ser positivo");
        }
    }
}
