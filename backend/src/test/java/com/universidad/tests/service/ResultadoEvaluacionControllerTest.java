package com.universidad.tests.service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.usecase.ConsultarResultadoEvaluacionUseCase;
import com.universidad.tests.controller.ResultadoEvaluacionController;
import com.universidad.tests.domain.exception.DominioNegocioException;
import com.universidad.tests.domain.model.FactorResultado;
import com.universidad.tests.domain.model.RevisionResultado;
import com.universidad.tests.exception.GlobalExceptionHandler;
import com.universidad.tests.mapper.ResultadoResponseMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResultadoEvaluacionController.class)
@Import({ResultadoResponseMapper.class, GlobalExceptionHandler.class})
class ResultadoEvaluacionControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ConsultarResultadoEvaluacionUseCase useCase;

    @Test
    void debeDevolverResultadoPublicadoVigenteParaUsuarioAutorizado() throws Exception {
        List<FactorResultado> factores =
            List.of(
                new FactorResultado(
                    "VOC1",
                    null,
                    "NO_CALCULABLE",
                    null,
                    null,
                    "NO_APLICA",
                    "Aplicacion parcial sin componentes"),
                new FactorResultado(
                    "FV",
                    2,
                    "CALCULADO",
                    null,
                    null,
                    "SIN_BAREMO_VALIDADO",
                    "Sin baremo validado"));

        RevisionResultado revision =
            new RevisionResultado(
                701L, 2, 4L, "REGLAS-EJEMPLO-1", null, null, null, null, factores);

        var consultado =
            new ConsultarResultadoEvaluacionUseCase.ResultadoConsultado(
                15L,
                70L,
                "FINALIZADA",
                "DISPONIBLE",
                10L,
                "INDIVIDUAL",
                "PARCIAL",
                revision,
                null);

        when(useCase.ejecutar(15L)).thenReturn(consultado);

        mockMvc.perform(get("/api/v1/evaluaciones/15/resultados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.evaluacionId").value(15))
            .andExpect(jsonPath("$.resultadoId").value(70))
            .andExpect(jsonPath("$.estadoEvaluacion").value("FINALIZADA"))
            .andExpect(jsonPath("$.estadoResultado").value("DISPONIBLE"))
            .andExpect(jsonPath("$.calificacionId").value(701))
            .andExpect(jsonPath("$.revision").value(2))
            .andExpect(jsonPath("$.factores[0].codigo").value("VOC1"))
            .andExpect(jsonPath("$.factores[0].puntuacionDirecta").doesNotExist())
            .andExpect(jsonPath("$.factores[1].codigo").value("FV"))
            .andExpect(jsonPath("$.factores[1].puntuacionDirecta").value(2));
    }

    @Test
    void debeDevolverPendienteSinFiltrarCandidatasParaProfesional() throws Exception {
        var pendiente =
            new ConsultarResultadoEvaluacionUseCase.ResultadoConsultado(
                15L,
                70L,
                "FINALIZADA",
                "PENDIENTE_VALIDACION",
                10L,
                "INDIVIDUAL",
                "PARCIAL",
                null,
                "SIN_REVISION_PUBLICADA");

        when(useCase.ejecutar(15L)).thenReturn(pendiente);

        mockMvc.perform(get("/api/v1/evaluaciones/15/resultados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estadoResultado").value("PENDIENTE_VALIDACION"))
            .andExpect(jsonPath("$.calificacionId").doesNotExist())
            .andExpect(jsonPath("$.motivo").value("SIN_REVISION_PUBLICADA"))
            .andExpect(jsonPath("$.factores").isEmpty());
    }

    @Test
    void debeDevolver403CuandoSujetoConsultaResultadoNoPublicado() throws Exception {
        when(useCase.ejecutar(15L))
            .thenThrow(
                new DominioNegocioException(
                    403,
                    "RESULTADO_NO_PUBLICADO",
                    "Los resultados de esta evaluacion aun no han sido publicados."));

        mockMvc.perform(get("/api/v1/evaluaciones/15/resultados"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.codigo").value("RESULTADO_NO_PUBLICADO"))
            .andExpect(jsonPath("$.ruta").value("/api/v1/evaluaciones/15/resultados"));
    }

    @Test
    void debeDevolver409CuandoEvaluacionNoEstaFinalizada() throws Exception {
        when(useCase.ejecutar(15L))
            .thenThrow(
                new DominioNegocioException(
                    409,
                    "EVALUACION_NO_FINALIZADA",
                    "La evaluacion aun se encuentra en curso."));

        mockMvc.perform(get("/api/v1/evaluaciones/15/resultados"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.codigo").value("EVALUACION_NO_FINALIZADA"));
    }

    @Test
    void debeDevolver404CuandoEvaluacionNoExiste() throws Exception {
        when(useCase.ejecutar(999L))
            .thenThrow(
                new DominioNegocioException(
                    404,
                    "EVALUACION_NO_ENCONTRADA",
                    "No existe la evaluacion con ID 999."));

        mockMvc.perform(get("/api/v1/evaluaciones/999/resultados"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.codigo").value("EVALUACION_NO_ENCONTRADA"));
    }
}
