package com.universidad.tests.domain.exception;

public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException() {
        super("Se requiere autenticacion");
    }
}
