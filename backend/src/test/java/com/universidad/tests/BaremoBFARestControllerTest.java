package com.universidad.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.application.service.CargarBaremoBFAService;
import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoInvalidoException;
import com.universidad.tests.controller.BaremoBFAController;
import com.universidad.tests.controller.VersionadoErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BaremoBFAController.class)
@Import(VersionadoErrorHandler.class)
class BaremoBFARestControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private CargarBaremoBFAService servicio;
    private static final String CUERPO =
            "{\"nombre\":\"BFA\",\"version\":\"v1\",\"anioNorma\":1992,"
                    + "\"rangos\":[{\"idFactor\":\"VOC1\",\"percentil\":5,"
                    + "\"limiteInferior\":1,\"limiteSuperior\":5,\"estadoCelda\":\"RANGO\"}]}";

    @Test
    void creaBaremo() throws Exception {
        when(servicio.cargarDatos(any())).thenReturn(2L);
        mvc.perform(post("/api/v1/baremos").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBaremo").value(2));
    }

    @Test
    void rechazaRangosInvalidos() throws Exception {
        when(servicio.cargarDatos(any())).thenThrow(new BaremoInvalidoException());
        mvc.perform(post("/api/v1/baremos").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("BAREMO_INVALIDO"));
    }
}
