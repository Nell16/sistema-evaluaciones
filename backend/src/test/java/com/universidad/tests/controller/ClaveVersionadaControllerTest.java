package com.universidad.tests.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.application.service.CargarClaveVersionadaService;
import com.universidad.tests.application.service.IdentidadSesion;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClaveVersionadaController.class)
class ClaveVersionadaControllerTest {
    private static final String BEARER = "Bearer token-ficticio";

    @Autowired private MockMvc mvc;
    @MockitoBean private CargarClaveVersionadaService servicio;
    @MockitoBean private AutenticacionService autenticacion;

    @BeforeEach
    void sesionValida() {
        when(autenticacion.validar("token-ficticio"))
                .thenReturn(
                        Optional.of(
                                new IdentidadSesion(
                                        1L, 1L, "ACADEMICO", Set.of("OPCIONES_EDITAR"))));
    }

    @Test
    void e29DevuelveIdDeClaveSinExponerEntradas() throws Exception {
        when(servicio.cargar(eq(5L), anyList())).thenReturn(30L);
        mvc.perform(
                        put("/api/v1/versiones/5/clave")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"entradas\":[{\"preguntaId\":10,\"opcion\":\"B\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idClave").value(30))
                .andExpect(jsonPath("$.entradas").doesNotExist());
    }

    @Test
    void e29RechazaSolicitudIncompleta() throws Exception {
        mvc.perform(
                        put("/api/v1/versiones/5/clave")
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"entradas\":[{\"preguntaId\":10}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles[0].campo").value("entradas[0].opcion"));
    }

    @Test
    void e29DistinguePermisoIdentificadorInvalidoYConflicto() throws Exception {
        String ruta = "/api/v1/versiones/5/clave";
        String cuerpo = "{\"entradas\":[{\"preguntaId\":10,\"opcion\":\"B\"}]}";
        when(servicio.cargar(eq(5L), anyList())).thenThrow(new SecurityException("sin permiso"));
        mvc.perform(
                        put(ruta)
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));

        when(servicio.cargar(eq(5L), anyList()))
                .thenThrow(new NoSuchElementException("Version no encontrada"));
        mvc.perform(
                        put(ruta)
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles[0].campo").value("versionFormularioId"));

        when(servicio.cargar(eq(5L), anyList()))
                .thenThrow(new IllegalStateException("CLAVE_NO_EDITABLE"));
        mvc.perform(
                        put(ruta)
                                .header("Authorization", BEARER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_NO_EDITABLE"));
    }

    @Test
    void e29SinSesionResponde401() throws Exception {
        mvc.perform(put("/api/v1/versiones/5/clave"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }
}
