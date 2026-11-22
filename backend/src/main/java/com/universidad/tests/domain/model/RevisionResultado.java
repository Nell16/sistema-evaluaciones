package com.universidad.tests.domain.model;

import java.util.Collections;
import java.util.List;

public class RevisionResultado {

    private final Long calificacionId;
    private final Integer revision;
    private final Long idReglas;
    private final String versionReglas;
    private final Long idClave;
    private final String versionClave;
    private final Long idBaremo;
    private final String versionBaremo;
    private final List<FactorResultado> factores;

    public RevisionResultado(
        Long calificacionId,
        Integer revision,
        Long idReglas,
        String versionReglas,
        Long idClave,
        String versionClave,
        Long idBaremo,
        String versionBaremo,
        List<FactorResultado> factores) {
        this.calificacionId = calificacionId;
        this.revision = revision;
        this.idReglas = idReglas;
        this.versionReglas = versionReglas;
        this.idClave = idClave;
        this.versionClave = versionClave;
        this.idBaremo = idBaremo;
        this.versionBaremo = versionBaremo;
        this.factores = factores == null ? List.of() : Collections.unmodifiableList(factores);
    }

    public Long getCalificacionId() {
        return calificacionId;
    }

    public Integer getRevision() {
        return revision;
    }

    public Long getIdReglas() {
        return idReglas;
    }

    public String getVersionReglas() {
        return versionReglas;
    }

    public Long getIdClave() {
        return idClave;
    }

    public String getVersionClave() {
        return versionClave;
    }

    public Long getIdBaremo() {
        return idBaremo;
    }

    public String getVersionBaremo() {
        return versionBaremo;
    }

    public List<FactorResultado> getFactores() {
        return factores;
    }
}
