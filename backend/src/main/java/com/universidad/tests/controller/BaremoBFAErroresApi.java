package com.universidad.tests.controller;

import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoDuplicadoException;
import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoInvalidoException;
import com.universidad.tests.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BaremoBFAController.class)
public class BaremoBFAErroresApi {

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        IllegalArgumentException.class
    })
    public ResponseEntity<ErrorResponse> solicitudInvalida(
            Exception error, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", error, request);
    }

    @ExceptionHandler(BaremoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(
            BaremoDuplicadoException error, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, "BAREMO_DUPLICADO", error, request);
    }

    @ExceptionHandler(BaremoInvalidoException.class)
    public ResponseEntity<ErrorResponse> invalido(
            BaremoInvalidoException error, HttpServletRequest request) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "BAREMO_INVALIDO", error, request);
    }

    private ResponseEntity<ErrorResponse> responder(
            HttpStatus estado, String codigo, Exception error, HttpServletRequest request) {
        return ResponseEntity.status(estado)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                estado.value(),
                                codigo,
                                error.getMessage(),
                                request.getRequestURI()));
    }
}
