package com.universidad.tests.mapper;

import com.universidad.tests.application.usecase.ConsultarResultadoEvaluacionUseCase.ResultadoConsultado;
import com.universidad.tests.domain.model.RevisionResultado;
import com.universidad.tests.dto.response.FactorResultadoResponse;
import com.universidad.tests.dto.response.ResultadoResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResultadoResponseMapper {

    public ResultadoResponse toResponse(ResultadoConsultado consultado) {
        RevisionResultado rev = consultado.revisionVigente();
        if (rev == null) {
            return new ResultadoResponse(
                consultado.evaluacionId(),
                consultado.resultadoId(),
                consultado.estadoEvaluacion(),
                consultado.estadoResultado(),
                consultado.versionFormularioId(),
                consultado.modalidad(),
                consultado.alcance(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                consultado.motivo(),
                List.of());
        }

        List<FactorResultadoResponse> factoresDto =
            rev.getFactores().stream()
                .map(
                    f ->
                        new FactorResultadoResponse(
                            f.getCodigo(),
                            f.getPuntuacionDirecta(),
                            f.getEstadoCalculo(),
                            f.getPercentil(),
                            f.getIdRango(),
                            f.getEstadoNormativo(),
                            f.getMotivo()))
                .toList();

        return new ResultadoResponse(
            consultado.evaluacionId(),
            consultado.resultadoId(),
            consultado.estadoEvaluacion(),
            consultado.estadoResultado(),
            consultado.versionFormularioId(),
            consultado.modalidad(),
            consultado.alcance(),
            rev.getCalificacionId(),
            rev.getRevision(),
            rev.getIdReglas(),
            rev.getVersionReglas(),
            rev.getIdClave(),
            rev.getVersionClave(),
            rev.getIdBaremo(),
            rev.getVersionBaremo(),
            consultado.motivo(),
            factoresDto);
    }
}
