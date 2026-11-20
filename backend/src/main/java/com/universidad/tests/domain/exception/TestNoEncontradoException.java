package com.universidad.tests.domain.exception;

public class TestNoEncontradoException extends RuntimeException {

    public TestNoEncontradoException(Long idTest) {
        super("El test " + idTest + " no existe o no esta disponible");
    }
}
