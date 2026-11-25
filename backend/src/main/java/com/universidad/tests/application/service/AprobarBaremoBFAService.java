package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.AprobacionBaremoBFA;
import com.universidad.tests.domain.port.AutorizacionBaremoPort;
import com.universidad.tests.domain.repository.AsignacionBaremoBFARepository;
import com.universidad.tests.domain.repository.BaremoBFARepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registra la compatibilidad ya revisada; E30 solo carga el baremo en borrador. */
@Service
public class AprobarBaremoBFAService {

    private final BaremoBFARepository baremos;
    private final VersionFormularioRepository versiones;
    private final AsignacionBaremoBFARepository asignaciones;
    private final AutorizacionBaremoPort autorizacion;

    public AprobarBaremoBFAService(
            BaremoBFARepository baremos,
            VersionFormularioRepository versiones,
            AsignacionBaremoBFARepository asignaciones,
            AutorizacionBaremoPort autorizacion) {
        this.baremos = baremos;
        this.versiones = versiones;
        this.asignaciones = asignaciones;
        this.autorizacion = autorizacion;
    }

    @Transactional
    public void aprobar(
            Long idBaremo,
            Long idVersionFormulario,
            String versionReglas,
            String evidenciaAprobacion) {
        if (idBaremo == null
                || idBaremo <= 0
                || idVersionFormulario == null
                || idVersionFormulario <= 0
                || versionReglas == null
                || versionReglas.isBlank()
                || versionReglas.length() > 80) {
            throw new IllegalArgumentException("Baremo, formulario y reglas son obligatorios");
        }
        AprobacionBaremoBFA aprobacion =
                AprobacionBaremoBFA.borrador().aprobar(evidenciaAprobacion);
        autorizacion.exigirPermisoCargarBaremo();
        if (!baremos.existePorId(idBaremo)
                || versiones.buscarPorId(idVersionFormulario).isEmpty()) {
            throw new NoSuchElementException("Baremo o version de formulario no encontrada");
        }
        asignaciones.registrarAprobacion(
                idBaremo, idVersionFormulario, versionReglas, aprobacion.evidencia());
    }

    @Transactional
    public void retirar(Long idBaremo, Long idVersionFormulario, String versionReglas) {
        if (idBaremo == null
                || idBaremo <= 0
                || idVersionFormulario == null
                || idVersionFormulario <= 0
                || versionReglas == null
                || versionReglas.isBlank()) {
            throw new IllegalArgumentException("Baremo, formulario y reglas son obligatorios");
        }
        autorizacion.exigirPermisoCargarBaremo();
        AprobacionBaremoBFA actual =
                asignaciones
                        .buscar(idBaremo, idVersionFormulario, versionReglas)
                        .orElseThrow(() -> new NoSuchElementException("Asignacion no encontrada"));
        asignaciones.actualizarEstado(
                idBaremo, idVersionFormulario, versionReglas, actual.retirar());
    }
}
