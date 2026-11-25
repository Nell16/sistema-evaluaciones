package com.universidad.tests;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.application.service.CargarBaremoBFAService;
import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoDuplicadoException;
import com.universidad.tests.application.service.CargarBaremoBFAService.BaremoInvalidoException;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.controller.BaremoBFAController;
import com.universidad.tests.controller.BaremoBFAErroresApi;
import com.universidad.tests.domain.exception.SinPermisoException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BaremoBFAController.class)
@Import(BaremoBFAErroresApi.class)
class BaremoBFARestControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private CargarBaremoBFAService servicio;
    @MockitoBean private AutenticacionService autenticacion;
    private static final String BEARER = "Bearer token-ficticio";
    private static final String CUERPO =
            "{\"nombre\":\"BFA\",\"version\":\"v1\",\"anioNorma\":1992,"
                    + "\"rangos\":[{\"idFactor\":\"VOC1\",\"percentil\":5,"
                    + "\"limiteInferior\":1,\"limiteSuperior\":5,\"estadoCelda\":\"RANGO\"}]}";

    @BeforeEach
    void sesionValida() {
        when(autenticacion.validar("token-ficticio"))
                .thenReturn(
                        Optional.of(
                                new IdentidadSesion(
                                        1L, 1L, "ACADEMICO", Set.of("CONFIGURAR_BFA"))));
    }

    @Test
    void creaBaremo() throws Exception {
        when(servicio.cargarDatos(any())).thenReturn(2L);
        mvc.perform(
                        post("/api/v1/baremos")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUERPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBaremo").value(2));
    }

    @Test
    void rechazaRangosInvalidos() throws Exception {
        when(servicio.cargarDatos(any())).thenThrow(new BaremoInvalidoException());
        mvc.perform(
                        post("/api/v1/baremos")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUERPO))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("BAREMO_INVALIDO"));
    }

    @Test
    void rechazaSolicitudSinSesion() throws Exception {
        mvc.perform(post("/api/v1/baremos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void rechazaSolicitudIncompleta() throws Exception {
        mvc.perform(
                        post("/api/v1/baremos")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"BFA\",\"rangos\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    void rechazaSinPermiso() throws Exception {
        when(servicio.cargarDatos(any())).thenThrow(new SinPermisoException());
        mvc.perform(
                        post("/api/v1/baremos")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUERPO))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    @Test
    void rechazaBaremoDuplicado() throws Exception {
        when(servicio.cargarDatos(any())).thenThrow(new BaremoDuplicadoException());
        mvc.perform(
                        post("/api/v1/baremos")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CUERPO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("BAREMO_DUPLICADO"));
    }
}
