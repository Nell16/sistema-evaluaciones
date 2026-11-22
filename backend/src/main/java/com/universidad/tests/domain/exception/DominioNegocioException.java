package com.universidad.tests.domain.exception;

public class DominioNegocioException extends RuntimeException {

    private final int statusHttp;
    private final String codigo;

    public DominioNegocioException(int statusHttp, String codigo, String mensaje) {
        super(mensaje);
        this.statusHttp = statusHttp;
        this.codigo = codigo;
    }

    public int getStatusHttp() {
        return statusHttp;
    }

    public String getCodigo() {
        return codigo;
    }
}
