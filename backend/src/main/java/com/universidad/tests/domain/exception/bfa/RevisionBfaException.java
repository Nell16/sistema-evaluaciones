package com.universidad.tests.domain.exception.bfa;

public class RevisionBfaException extends RuntimeException {

    public enum Codigo {
        SOLICITUD_INVALIDA(400),
        REVISION_NO_ENCONTRADA(404),
        REVISION_COMPLETADA(409),
        REVISION_VIGENTE_CAMBIO(409),
        REVISION_NO_PUBLICABLE(409),
        RESPUESTA_NO_FV(422);

        private final int estadoHttp;

        Codigo(int estadoHttp) {
            this.estadoHttp = estadoHttp;
        }

        public int estadoHttp() {
            return estadoHttp;
        }
    }

    private final Codigo codigo;

    public RevisionBfaException(Codigo codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public Codigo codigo() {
        return codigo;
    }
}
