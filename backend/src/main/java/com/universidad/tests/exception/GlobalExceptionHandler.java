package com.universidad.tests.exception;

import com.universidad.tests.domain.exception.DominioNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DominioNegocioException.class)
    public ResponseEntity<ErrorApiResponse> manejarDominioNegocio(
        DominioNegocioException ex, HttpServletRequest request) {
        ErrorApiResponse cuerpo =
            new ErrorApiResponse(
                Instant.now(),
                ex.getStatusHttp(),
                ex.getCodigo(),
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(ex.getStatusHttp()).body(cuerpo);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorApiResponse> manejarSeguridad(
        SecurityException ex, HttpServletRequest request) {
        ErrorApiResponse cuerpo =
            new ErrorApiResponse(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "SIN_PERMISO",
                ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(cuerpo);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorApiResponse> manejarParametroInvalido(
        MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorApiResponse cuerpo =
            new ErrorApiResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDACION",
                "Parametro invalido en la solicitud: " + ex.getName(),
                request.getRequestURI());
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorApiResponse> manejarErrorGeneral(
        Exception ex, HttpServletRequest request) {
        ErrorApiResponse cuerpo =
            new ErrorApiResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "ERROR_INTERNO",
                "Ocurrio un error interno al procesar la solicitud.",
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cuerpo);
    }
}
