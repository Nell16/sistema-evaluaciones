package com.universidad.tests;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.application.service.VersionFormularioRestService;
import com.universidad.tests.application.service.VersionFormularioRestService.ResumenVersion;
import com.universidad.tests.application.service.VersionFormularioService.VersionDuplicadaException;
import com.universidad.tests.controller.VersionFormularioController;
import com.universidad.tests.controller.VersionadoErrorHandler;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VersionFormularioController.class)
@Import(VersionadoErrorHandler.class)
class VersionFormularioRestControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private VersionFormularioRestService servicio;
    private static final String RUTA = "/api/v1/tests/1/versiones";
    private static final String CUERPO = "{\"codigoVersion\":\"v2\",\"fuenteRef\":\"DOC-BFA\"}";

    @Test
    void creaBorrador() throws Exception {
        when(servicio.crear(1L, "v2", null, "DOC-BFA"))
                .thenReturn(new ResumenVersion(11L, 1L, "BORRADOR"));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(header().string("Location", "/api/v1/versiones/11"));
    }

    @Test
    void validaCuerpoYRecursos() throws Exception {
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        when(servicio.crear(anyLong(), anyString(), any(), anyString()))
                .thenThrow(new NoSuchElementException("test ausente"));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaVersionDuplicada() throws Exception {
        when(servicio.crear(anyLong(), anyString(), any(), anyString()))
                .thenThrow(new VersionDuplicadaException("duplicada"));
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_DUPLICADA"));
    }
}
