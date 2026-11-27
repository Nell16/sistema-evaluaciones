package com.universidad.tests.application.usecase;

import com.universidad.tests.application.usecase.vista.PeriodoVista;
import com.universidad.tests.domain.exception.ErrorNegocioException;
import com.universidad.tests.domain.exception.TipoErrorNegocio;
import com.universidad.tests.domain.model.Periodo;
import com.universidad.tests.domain.port.AutorizacionEvaluacionPort;
import com.universidad.tests.domain.repository.PeriodoRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** E20 — Crear un Periodo para nuevas asignaciones (CU-GEN-20). */
@Service
public class CrearPeriodoUseCase {

    private final AutorizacionEvaluacionPort autorizacion;
    private final PeriodoRepository periodoRepository;

    public CrearPeriodoUseCase(
            AutorizacionEvaluacionPort autorizacion, PeriodoRepository periodoRepository) {
        this.autorizacion = autorizacion;
        this.periodoRepository = periodoRepository;
    }

    @Transactional
    public PeriodoVista crear(String codigo, String denominacion, LocalDate inicio, LocalDate fin) {
        autorizacion.exigirPermisoCrearPeriodo();

        if (inicio != null && fin != null && inicio.isAfter(fin)) {
            throw new ErrorNegocioException(
                    TipoErrorNegocio.REGLA_DE_NEGOCIO,
                    "PERIODO_INVALIDO",
                    "La fecha de inicio del período no puede ser posterior a la fecha de fin.");
        }

        String codigoLimpio = codigo == null ? null : codigo.trim();
        String nombreLimpio = denominacion == null ? null : denominacion.trim();

        if (codigoLimpio != null && periodoRepository.buscarPorCodigo(codigoLimpio).isPresent()) {
            throw new ErrorNegocioException(
                    TipoErrorNegocio.CONFLICTO,
                    "PERIODO_DUPLICADO",
                    "Ya existe un período registrado con el código: " + codigoLimpio);
        }

        // Un conflicto por carrera (uq_periodo_codigo) lo traduce el manejador de errores.
        Periodo nuevo = new Periodo(null, codigoLimpio, nombreLimpio, inicio, fin, true);
        Periodo guardado = periodoRepository.guardar(nuevo);
        return new PeriodoVista(
                guardado.idPeriodo(),
                guardado.codigo(),
                guardado.nombre(),
                guardado.fechaInicio(),
                guardado.fechaFin(),
                guardado.admiteNuevasEvaluaciones());
    }
}
