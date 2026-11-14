package com.universidad.tests.infrastructure.persistence.entity;

import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "version_clave_bfa",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_clave_formulario_version",
                        columnNames = {"id_version_formulario", "version"}))
public class VersionClaveBFAJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clave")
    private Long idClave;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_version_formulario", nullable = false)
    private VersionFormularioJpaEntity versionFormulario;

    @Column(name = "version", nullable = false, length = 80)
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVersionClaveBFA estado;

    @Column(name = "referencia_fuente", nullable = false, columnDefinition = "text")
    private String referenciaFuente;

    @Column(name = "evidencia_aprobacion", columnDefinition = "text")
    private String evidenciaAprobacion;

    protected VersionClaveBFAJpaEntity() {
        // Constructor requerido por JPA
    }

    public VersionClaveBFAJpaEntity(
            VersionFormularioJpaEntity versionFormulario, String version, String referenciaFuente) {

        if (versionFormulario == null) {
            throw new IllegalArgumentException(
                    "La clave debe pertenecer a una version de formulario");
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("La version de la clave es obligatoria");
        }

        if (referenciaFuente == null || referenciaFuente.isBlank()) {
            throw new IllegalArgumentException("La referencia de la fuente es obligatoria");
        }

        this.versionFormulario = versionFormulario;
        this.version = version;
        this.referenciaFuente = referenciaFuente;
        this.estado = EstadoVersionClaveBFA.BORRADOR;
    }

    public Long getIdClave() {
        return idClave;
    }

    public VersionFormularioJpaEntity getVersionFormulario() {
        return versionFormulario;
    }

    public Long getIdVersionFormulario() {
        return versionFormulario.getIdVersionFormulario();
    }

    public String getVersion() {
        return version;
    }

    public EstadoVersionClaveBFA getEstado() {
        return estado;
    }

    public String getReferenciaFuente() {
        return referenciaFuente;
    }

    public String getEvidenciaAprobacion() {
        return evidenciaAprobacion;
    }
}
