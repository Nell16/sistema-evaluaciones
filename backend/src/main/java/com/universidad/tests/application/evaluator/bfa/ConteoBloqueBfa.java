package com.universidad.tests.application.evaluator.bfa;

/**
 * Conteo de un bloque BFA de selección al compararlo con su clave.
 *
 * <p>Sigue los conteos de ENT-BFA-09 (Documento BFA): aciertos, errores y omisiones por separado, y
 * aciertos + errores + omisiones = preguntas de la clave del bloque. Las omisiones no se convierten
 * en errores; si se penalizan lo decide la regla aprobada (D10), no este conteo.
 */
public record ConteoBloqueBfa(int aciertos, int errores, int omisiones) {

    public ConteoBloqueBfa {
        if (aciertos < 0 || errores < 0 || omisiones < 0) {
            throw new IllegalArgumentException("Los conteos del bloque deben ser no negativos");
        }
    }
}
