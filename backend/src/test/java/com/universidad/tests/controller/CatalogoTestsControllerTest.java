package com.universidad.tests.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.service.CatalogoTestsService;
import com.universidad.tests.domain.exception.TestNoEncontradoException;
import com.universidad.tests.dto.response.TestResumenResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogoTestsController.class)
class CatalogoTestsControllerTest {

    @Autowired private MockMvc mvc;

    @MockitoBean private CatalogoTestsService catalogoService;

    @Test
    void e1DevuelveLosTestsActivos() throws Exception {

        when(catalogoService.listarTests())
                .thenReturn(List.of(new TestResumenResponse(1L, "FICT", "Test ficticio", 2)));

        mvc.perform(get("/api/v1/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("FICT"))
                .andExpect(jsonPath("$[0].numeroSubpruebas").value(2));
    }

    @Test
    void e2Responde404ConElFormatoComunDeError() throws Exception {

        when(catalogoService.obtenerTest(99L)).thenThrow(new TestNoEncontradoException(99L));

        mvc.perform(get("/api/v1/tests/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.codigo").value("TEST_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.ruta").value("/api/v1/tests/99"))
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
