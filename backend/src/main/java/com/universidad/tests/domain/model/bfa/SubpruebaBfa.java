package com.universidad.tests.domain.model.bfa;

import com.universidad.tests.domain.model.TipoRespuesta;

/** Las 13 subpruebas oficiales del BFA, con orden y limites definidos en el documento BFA. */
public enum SubpruebaBfa {
    VOC1(1, "VOC1", "Vocabulario - Forma A", 300, TipoRespuesta.SELECCION, null, "VOC1"),
    VOC2(2, "VOC2", "Vocabulario - Forma B", 360, TipoRespuesta.SELECCION, null, "VOC2"),
    S1(3, "S1", "Espacial - Figuras Idénticas", 180, TipoRespuesta.SELECCION, null, "S1"),
    S2(4, "S2", "Espacial - Desplazamiento", 300, TipoRespuesta.SELECCION, null, "S2"),
    S1B(5, "S1b", "Espacial - Ladrillos-Cubos", 210, TipoRespuesta.SELECCION, null, "S1"),
    R1(6, "R1", "Razonamiento - Forma A", 600, TipoRespuesta.SELECCION, null, "R1"),
    R2(7, "R2", "Razonamiento - Forma B", 720, TipoRespuesta.SELECCION, null, "R2"),
    CV(8, "CV", "Comprensión Verbal", 900, TipoRespuesta.SELECCION, null, "CV"),
    N1_A(9, "N1-A", "Numérico - Sumas", 240, TipoRespuesta.SELECCION, null, "N1"),
    N1_B(10, "N1-B", "Numérico - Multiplicaciones", 240, TipoRespuesta.SELECCION, null, "N1"),
    N2_A(11, "N2-A", "Numérico - Operaciones", 360, TipoRespuesta.SELECCION, null, "N2"),
    N2_B(12, "N2-B", "Numérico - Problemas", 360, TipoRespuesta.SELECCION, null, "N2"),
    FV(13, "FV", "Fluidez Verbal", 300, TipoRespuesta.TEXTO_LIBRE, 75, "FV");

    private final int orden;
    private final String codigo;
    private final String nombre;
    private final int tiempoLimiteSegundos;
    private final TipoRespuesta tipoRespuesta;
    private final Integer maximoEntradas;
    private final String factor;

    SubpruebaBfa(
            int orden,
            String codigo,
            String nombre,
            int tiempoLimiteSegundos,
            TipoRespuesta tipoRespuesta,
            Integer maximoEntradas,
            String factor) {
        this.orden = orden;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tiempoLimiteSegundos = tiempoLimiteSegundos;
        this.tipoRespuesta = tipoRespuesta;
        this.maximoEntradas = maximoEntradas;
        this.factor = factor;
    }

    public int getOrden() {
        return orden;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public int getTiempoLimiteSegundos() {
        return tiempoLimiteSegundos;
    }

    public TipoRespuesta getTipoRespuesta() {
        return tipoRespuesta;
    }

    public Integer getMaximoEntradas() {
        return maximoEntradas;
    }

    public String getFactor() {
        return factor;
    }
}
