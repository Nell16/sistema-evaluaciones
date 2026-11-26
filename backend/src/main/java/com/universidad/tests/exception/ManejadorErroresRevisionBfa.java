package com.universidad.tests.exception;

import com.universidad.tests.domain.exception.bfa.RevisionBfaException;
import com.universidad.tests.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErroresRevisionBfa {

    @ExceptionHandler(RevisionBfaException.class)
    public ResponseEntity<ErrorResponse> revisionBfa(
            RevisionBfaException ex, HttpServletRequest request) {
        int estado = ex.codigo().estadoHttp();
        return ResponseEntity.status(estado)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                estado,
                                ex.codigo().name(),
                                ex.getMessage(),
                                request.getRequestURI()));
    }
}
