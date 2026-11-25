package com.universidad.tests.domain.repository;

import com.universidad.tests.domain.model.AprobacionBaremoBFA;
import java.util.Optional;

public interface AsignacionBaremoBFARepository {
    void registrarAprobacion(
            long idBaremo,
            long idVersionFormulario,
            String versionReglas,
            String evidenciaAprobacion);

    Optional<AprobacionBaremoBFA> buscar(
            long idBaremo, long idVersionFormulario, String versionReglas);

    void actualizarEstado(
            long idBaremo,
            long idVersionFormulario,
            String versionReglas,
            AprobacionBaremoBFA aprobacion);
}
