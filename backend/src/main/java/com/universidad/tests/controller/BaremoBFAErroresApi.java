package com.universidad.tests.controller;

import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoDuplicadoException;
import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoInvalidoException;
import com.universidad.tests.dto.response.ErrorResponse;
import com.universidad.tests.dto.response.ErrorResponse.DetalleCampo;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = BaremoBFAController.class)
public class BaremoBFAErroresApi {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> solicitudInvalida(
            MethodArgumentNotValidException error, HttpServletRequest request) {
        List<DetalleCampo> detalles =
                error.getBindingResult().getFieldErrors().stream()
                        .map(campo -> new DetalleCampo(campo.getField(), campo.getDefaultMessage()))
                        .toList();
        return solicitudInvalida(request, detalles);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> cuerpoInvalido(
            Exception error, HttpServletRequest request) {
        return solicitudInvalida(
                request, List.of(new DetalleCampo("cuerpo", "JSON mal formado o valor inválido")));
    }

    private ResponseEntity<ErrorResponse> solicitudInvalida(
            HttpServletRequest request, List<DetalleCampo> detalles) {
        return responder(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "La solicitud contiene datos inválidos",
                request,
                detalles.isEmpty()
                        ? List.of(new DetalleCampo("cuerpo", "Datos inválidos"))
                        : detalles);
    }

    @ExceptionHandler(BaremoDuplicadoException.class)
    public ResponseEntity<ErrorResponse> duplicado(
            BaremoDuplicadoException error, HttpServletRequest request) {
        return responder(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "BAREMO_INVALIDO",
                error.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(BaremoInvalidoException.class)
    public ResponseEntity<ErrorResponse> invalido(
            BaremoInvalidoException error, HttpServletRequest request) {
        return responder(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "BAREMO_INVALIDO",
                error.getMessage(),
                request,
                List.of());
    }

    private ResponseEntity<ErrorResponse> responder(
            HttpStatus estado,
            String codigo,
            String mensaje,
            HttpServletRequest request,
            List<DetalleCampo> detalles) {
        return ResponseEntity.status(estado)
                .body(
                        new ErrorResponse(
                                Instant.now(),
                                estado.value(),
                                codigo,
                                mensaje,
                                request.getRequestURI(),
                                detalles));
    }
}
