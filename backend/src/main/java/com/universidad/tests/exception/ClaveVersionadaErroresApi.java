package com.universidad.tests.exception;

import com.universidad.tests.controller.ClaveVersionadaController;
import com.universidad.tests.dto.response.ErrorResponse;
import com.universidad.tests.dto.response.ErrorResponse.DetalleCampo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Order(0)
@RestControllerAdvice(assignableTypes = ClaveVersionadaController.class)
public class ClaveVersionadaErroresApi {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(
            IllegalArgumentException ex, HttpServletRequest request) {
        return solicitudInvalida(List.of(new DetalleCampo("entradas", ex.getMessage())), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> camposInvalidos(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        return solicitudInvalida(
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> new DetalleCampo(error.getField(), error.getDefaultMessage()))
                        .toList(),
                request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return solicitudInvalida(
                List.of(new DetalleCampo(ex.getName(), "Debe ser un identificador numérico.")),
                request);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> sinPermiso(
            SecurityException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "SIN_PERMISO", ex.getMessage(), request);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> versionInvalida(HttpServletRequest request) {
        return solicitudInvalida(
                List.of(new DetalleCampo("versionFormularioId", "La versión no existe.")), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> conflicto(
            IllegalStateException ex, HttpServletRequest request) {
        String mensaje =
                "CLAVE_NO_EDITABLE".equals(ex.getMessage())
                        ? "La clave existente no está en BORRADOR."
                        : "La versión del formulario no está en BORRADOR.";
        return error(HttpStatus.CONFLICT, "VERSION_NO_EDITABLE", mensaje, request);
    }

    private ResponseEntity<ErrorResponse> solicitudInvalida(
            List<DetalleCampo> detalles, HttpServletRequest request) {
        return ResponseEntity.badRequest()
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                400,
                                "SOLICITUD_INVALIDA",
                                "La solicitud tiene datos inválidos.",
                                request.getRequestURI(),
                                detalles));
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
