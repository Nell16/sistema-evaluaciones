package com.universidad.tests.application.evaluator.bfa;

/**
 * Conteo de un bloque BFA de selección al compararlo con su clave.
 *
 * <p>Las respuestas sin contestar se cuentan aparte y no se suman a errores: si cuentan o no como
 * error lo decide la regla aprobada de cada factor, no este conteo.
 */
public record ConteoBloqueBfa(int aciertos, int errores, int sinResponder) {

    public ConteoBloqueBfa {
        if (aciertos < 0 || errores < 0 || sinResponder < 0) {
            throw new IllegalArgumentException("Los conteos del bloque deben ser no negativos");
        }
    }
}
