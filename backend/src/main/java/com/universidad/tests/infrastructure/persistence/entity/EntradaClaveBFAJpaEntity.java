package com.universidad.tests.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "entrada_clave_bfa",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_entrada_clave_pregunta",
                        columnNames = {"id_clave", "id_pregunta"}))
public class EntradaClaveBFAJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrada_clave")
    private Long idEntradaClave;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_clave", nullable = false)
    private VersionClaveBFAJpaEntity clave;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pregunta", nullable = false)
    private PreguntaJpaEntity pregunta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_opcion_correcta", nullable = false)
    private OpcionRespuestaJpaEntity opcionCorrecta;

    protected EntradaClaveBFAJpaEntity() {
        // Constructor requerido por JPA
    }

    public EntradaClaveBFAJpaEntity(
            VersionClaveBFAJpaEntity clave,
            PreguntaJpaEntity pregunta,
            OpcionRespuestaJpaEntity opcionCorrecta) {

        if (clave == null) {
            throw new IllegalArgumentException("La version de clave es obligatoria");
        }

        if (pregunta == null) {
            throw new IllegalArgumentException("La pregunta es obligatoria");
        }

        if (opcionCorrecta == null) {
            throw new IllegalArgumentException("La opcion correcta es obligatoria");
        }

        this.clave = clave;
        this.pregunta = pregunta;
        this.opcionCorrecta = opcionCorrecta;
    }

    public Long getIdEntradaClave() {
        return idEntradaClave;
    }

    public VersionClaveBFAJpaEntity getClave() {
        return clave;
    }

    public PreguntaJpaEntity getPregunta() {
        return pregunta;
    }

    public OpcionRespuestaJpaEntity getOpcionCorrecta() {
        return opcionCorrecta;
    }

    public Long getIdClave() {
        return clave.getIdClave();
    }

    public Long getIdPregunta() {
        return pregunta.getIdPregunta();
    }

    public Long getIdOpcionCorrecta() {
        return opcionCorrecta.getIdOpcion();
    }
}
