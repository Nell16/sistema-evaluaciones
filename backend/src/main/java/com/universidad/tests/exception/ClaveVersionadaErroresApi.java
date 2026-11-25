package com.universidad.tests.exception;

import com.universidad.tests.controller.ClaveVersionadaController;
import com.universidad.tests.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClaveVersionadaController.class)
public class ClaveVersionadaErroresApi {
    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> solicitudInvalida(
            Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", ex.getMessage(), request);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> sinPermiso(
            SecurityException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "SIN_PERMISO", ex.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> noEncontrado(
            NoSuchElementException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "VERSION_NO_ENCONTRADA", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> conflicto(
            IllegalStateException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), ex.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> error(
            HttpStatus status, String codigo, String mensaje, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                status.value(),
                                codigo,
                                mensaje,
                                request.getRequestURI()));
    }
}
