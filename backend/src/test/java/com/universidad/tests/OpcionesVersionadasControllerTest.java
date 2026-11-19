package com.universidad.tests;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.application.service.OpcionesVersionadasService;
import com.universidad.tests.application.service.OpcionesVersionadasService.DatosOpcion;
import com.universidad.tests.controller.OpcionesVersionadasController;
import com.universidad.tests.controller.VersionadoErrorHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpcionesVersionadasController.class)
@Import(VersionadoErrorHandler.class)
class OpcionesVersionadasControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private OpcionesVersionadasService servicio;
    private static final String RUTA = "/api/v1/versiones/11/preguntas/120/opciones";
    private static final String CUERPO =
            "{\"opciones\":[{\"letra\":\"A\",\"orden\":1,\"texto\":\"Uno\"}]}";

    @Test
    void guardaOpciones() throws Exception {
        when(servicio.guardar(eq(11L), eq(120L), anyList()))
                .thenReturn(List.of(new DatosOpcion("A", 1, "Uno", null)));
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opciones[0].letra").value("A"));
    }

    @Test
    void rechazaVersionPublicadaYSolicitudVacia() throws Exception {
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        when(servicio.guardar(eq(11L), eq(120L), anyList()))
                .thenThrow(new IllegalStateException("VERSION_NO_EDITABLE"));
        mvc.perform(put(RUTA).contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isConflict());
    }
}
