package com.universidad.tests.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** E20 — Solicitud para crear un Periodo (CU-GEN-20). */
public record PeriodoRequest(
        @NotBlank(message = "El código del período es obligatorio")
                @Size(max = 40, message = "El código no debe exceder 40 caracteres")
                String codigo,
        @NotBlank(message = "La denominación del período es obligatoria")
                @Size(max = 200, message = "La denominación no debe exceder 200 caracteres")
                String denominacion,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate inicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fin) {}
