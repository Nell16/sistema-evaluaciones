package com.universidad.tests.domain.model;

import java.util.Objects;

public record AprobacionBaremoBFA(Estado estado, String evidencia) {

    public enum Estado {
        BORRADOR,
        APROBADO,
        RETIRADO
    }

    public AprobacionBaremoBFA {
        Objects.requireNonNull(estado, "El estado del baremo es obligatorio");
        if (estado == Estado.BORRADOR && evidencia != null) {
            throw new IllegalArgumentException("Un borrador no tiene evidencia de aprobación");
        }
        if (estado != Estado.BORRADOR && (evidencia == null || evidencia.isBlank())) {
            throw new IllegalArgumentException("La evidencia de aprobación es obligatoria");
        }
    }

    public static AprobacionBaremoBFA borrador() {
        return new AprobacionBaremoBFA(Estado.BORRADOR, null);
    }

    /** Registra una aprobación cuya compatibilidad ya validó el caso de uso. */
    public AprobacionBaremoBFA aprobar(String evidenciaAprobacion) {
        if (estado != Estado.BORRADOR) {
            throw new IllegalStateException("Solo puede aprobarse un baremo en borrador");
        }
        return new AprobacionBaremoBFA(Estado.APROBADO, evidenciaAprobacion);
    }

    public AprobacionBaremoBFA retirar() {
        if (estado != Estado.APROBADO) {
            throw new IllegalStateException("Solo puede retirarse un baremo aprobado");
        }
        return new AprobacionBaremoBFA(Estado.RETIRADO, evidencia);
    }

    public boolean estaAprobado() {
        return estado == Estado.APROBADO;
    }
}
