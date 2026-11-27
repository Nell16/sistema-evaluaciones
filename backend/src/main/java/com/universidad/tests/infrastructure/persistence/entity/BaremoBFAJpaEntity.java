package com.universidad.tests.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "baremo_bfa")
public class BaremoBFAJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_baremo")
    private Long idBaremo;

    @Column(name = "nombre", nullable = false, length = 200)
    private String nombre;

    @Column(name = "version", nullable = false, length = 80)
    private String version;

    @Column(name = "anio_norma", nullable = false)
    private int anioNorma;

    @Column(name = "poblacion_referencia", columnDefinition = "text")
    private String poblacionReferencia;

    @Column(name = "referencia_fuente", columnDefinition = "text")
    private String referenciaFuente;

    protected BaremoBFAJpaEntity() {}

    public BaremoBFAJpaEntity(
            String nombre,
            String version,
            int anioNorma,
            String poblacionReferencia,
            String referenciaFuente) {
        this.nombre = nombre;
        this.version = version;
        this.anioNorma = anioNorma;
        this.poblacionReferencia = poblacionReferencia;
        this.referenciaFuente = referenciaFuente;
    }

    public Long getIdBaremo() {
        return idBaremo;
    }
}
