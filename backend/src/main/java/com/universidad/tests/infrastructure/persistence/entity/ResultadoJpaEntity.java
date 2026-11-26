package com.universidad.tests.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "resultado")
public class ResultadoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long idResultado;

    @Column(name = "id_evaluacion", nullable = false, unique = true)
    private Long idEvaluacion;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "referencia_revision_vigente")
    private Long referenciaRevisionVigente;

    @Column(name = "version_formulario_id")
    private Long versionFormularioId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected ResultadoJpaEntity() {
        // Constructor requerido por JPA
    }

    public ResultadoJpaEntity(
            Long idEvaluacion,
            String estado,
            Long versionFormularioId,
            Instant creadoEn,
            Long referenciaRevisionVigente) {
        this.idEvaluacion = idEvaluacion;
        this.estado = estado;
        this.versionFormularioId = versionFormularioId;
        this.creadoEn = creadoEn;
        this.referenciaRevisionVigente = referenciaRevisionVigente;
    }

    public Long getIdResultado() {
        return idResultado;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public String getEstado() {
        return estado;
    }

    public Long getReferenciaRevisionVigente() {
        return referenciaRevisionVigente;
    }

    public Long getVersionFormularioId() {
        return versionFormularioId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
