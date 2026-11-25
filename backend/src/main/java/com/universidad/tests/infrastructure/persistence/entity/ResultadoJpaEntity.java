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

    @Column(name = "estado_resultado", nullable = false, length = 30)
    private String estadoResultado;

    @Column(name = "version_formulario_id")
    private Long versionFormularioId;

    @Column(name = "fecha_calculo", nullable = false)
    private Instant fechaCalculo;

    @Column(name = "referencia_revision_vigente", length = 100)
    private String referenciaRevisionVigente;

    @Column(name = "detalle", columnDefinition = "text")
    private String detalle;

    protected ResultadoJpaEntity() {
        // Constructor requerido por JPA
    }

    public ResultadoJpaEntity(
            Long idEvaluacion,
            String estadoResultado,
            Long versionFormularioId,
            Instant fechaCalculo,
            String referenciaRevisionVigente,
            String detalle) {
        this.idEvaluacion = idEvaluacion;
        this.estadoResultado = estadoResultado;
        this.versionFormularioId = versionFormularioId;
        this.fechaCalculo = fechaCalculo;
        this.referenciaRevisionVigente = referenciaRevisionVigente;
        this.detalle = detalle;
    }

    public Long getIdResultado() {
        return idResultado;
    }

    public Long getIdEvaluacion() {
        return idEvaluacion;
    }

    public String getEstadoResultado() {
        return estadoResultado;
    }

    public Long getVersionFormularioId() {
        return versionFormularioId;
    }

    public Instant getFechaCalculo() {
        return fechaCalculo;
    }

    public String getReferenciaRevisionVigente() {
        return referenciaRevisionVigente;
    }

    public String getDetalle() {
        return detalle;
    }
}
