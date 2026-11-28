package com.universidad.tests.application.service;

import com.universidad.tests.application.port.ConsultaBaremoBFAParaConversionPort;
import com.universidad.tests.application.port.ConsultaBaremoBFAParaConversionPort.BaremoCandidato;
import com.universidad.tests.application.service.ConversionPercentilBFAService.EstadoNormativo;
import com.universidad.tests.application.service.ConversionPercentilBFAService.ResultadoConversion;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AplicarBaremoBFAService {

    private final ConsultaBaremoBFAParaConversionPort consultaBaremos;
    private final ConversionPercentilBFAService conversion;

    public AplicarBaremoBFAService(
            ConsultaBaremoBFAParaConversionPort consultaBaremos,
            ConversionPercentilBFAService conversion) {
        this.consultaBaremos = Objects.requireNonNull(consultaBaremos);
        this.conversion = Objects.requireNonNull(conversion);
    }

    public ResultadoAplicacion aplicar(
            long versionFormularioId,
            String versionReglas,
            String codigoFactor,
            Integer puntuacionDirecta) {
        ResultadoRevision revision =
                aplicarRevision(
                        versionFormularioId,
                        versionReglas,
                        Collections.singletonMap(codigoFactor, puntuacionDirecta));

        return new ResultadoAplicacion(revision.idBaremo(), revision.factores().get(codigoFactor));
    }

    public ResultadoRevision aplicarRevision(
            long versionFormularioId,
            String versionReglas,
            Map<String, Integer> puntuacionesPorFactor) {
        Objects.requireNonNull(puntuacionesPorFactor, "Las puntuaciones son obligatorias");

        if (puntuacionesPorFactor.values().stream().noneMatch(Objects::nonNull)) {
            return convertirFactores(puntuacionesPorFactor, null, null);
        }

        List<BaremoCandidato> candidatos =
                consultaBaremos.buscarAprobadosCompatibles(versionFormularioId, versionReglas);

        if (candidatos.isEmpty()) {
            return convertirFactores(
                    puntuacionesPorFactor,
                    null,
                    "No existe un baremo aprobado y compatible con el formulario y las reglas.");
        }

        if (candidatos.size() > 1) {
            return convertirFactores(
                    puntuacionesPorFactor,
                    null,
                    "Hay más de un baremo aprobado y compatible para este cálculo.");
        }

        return convertirFactores(puntuacionesPorFactor, candidatos.get(0), null);
    }

    private ResultadoRevision convertirFactores(
            Map<String, Integer> puntuacionesPorFactor,
            BaremoCandidato baremo,
            String motivoSinBaremo) {
        Map<String, ResultadoConversion> resultados = new HashMap<>();

        puntuacionesPorFactor.forEach(
                (factor, puntuacion) -> {
                    ResultadoConversion resultado;
                    if (puntuacion == null) {
                        resultado = conversion.convertir(factor, null, List.of());
                    } else if (baremo == null) {
                        resultado =
                                new ResultadoConversion(
                                        null,
                                        null,
                                        EstadoNormativo.SIN_BAREMO_VALIDADO,
                                        motivoSinBaremo);
                    } else {
                        resultado = conversion.convertir(factor, puntuacion, baremo.rangos());
                    }
                    resultados.put(factor, resultado);
                });

        return new ResultadoRevision(baremo == null ? null : baremo.idBaremo(), resultados);
    }

    public record ResultadoAplicacion(Long idBaremo, ResultadoConversion conversion) {}

    public record ResultadoRevision(Long idBaremo, Map<String, ResultadoConversion> factores) {

        public ResultadoRevision {
            factores = Map.copyOf(factores);
        }
    }
}
