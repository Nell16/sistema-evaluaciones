package com.universidad.tests.infrastructure.persistence.entity;

import com.universidad.tests.domain.model.EstadoCeldaBaremo;
import com.universidad.tests.domain.model.RangoBaremoBFA;
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

@Entity
@Table(name = "rango_baremo_bfa")
public class RangoBaremoBFAJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rango")
    private Long idRango;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_baremo", nullable = false)
    private BaremoBFAJpaEntity baremo;

    @Column(name = "id_factor", nullable = false, length = 80)
    private String idFactor;

    @Column(name = "percentil", nullable = false)
    private int percentil;

    @Column(name = "limite_inferior")
    private Integer limiteInferior;

    @Column(name = "limite_superior")
    private Integer limiteSuperior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_celda", nullable = false, length = 20)
    private EstadoCeldaBaremo estadoCelda;

    @Column(name = "valor_original", columnDefinition = "text")
    private String valorOriginal;

    @Column(name = "localizador_fuente", columnDefinition = "text")
    private String localizadorFuente;

    protected RangoBaremoBFAJpaEntity() {}

    public RangoBaremoBFAJpaEntity(BaremoBFAJpaEntity baremo, RangoBaremoBFA rango) {
        this.baremo = baremo;
        this.idFactor = rango.idFactor();
        this.percentil = rango.percentil();
        this.limiteInferior = rango.limiteInferior();
        this.limiteSuperior = rango.limiteSuperior();
        this.estadoCelda = rango.estadoCelda();
        this.valorOriginal = rango.valorOriginal();
        this.localizadorFuente = rango.localizadorFuente();
    }
}
