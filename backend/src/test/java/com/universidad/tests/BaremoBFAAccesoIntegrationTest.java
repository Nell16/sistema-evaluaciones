package com.universidad.tests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BaremoBFAAccesoIntegrationTest {
    private static final String RUTA = "/api/v1/baremos";
    private static final String CLAVE = "clave-ficticia-baremo";

    @Autowired private MockMvc mvc;
    @Autowired private CuentaRepository cuentas;
    @Autowired private HashContrasenasPort hash;
    @Autowired private AutenticacionService autenticacion;

    @Test
    void coordinadorCreaBaremoYDuplicadoDevuelve422ConSesionReal() throws Exception {
        String token = abrirSesion(Rol.COORDINADOR);
        String nombre = "BFA_INT_" + UUID.randomUUID().toString().replace("-", "");
        String solicitud = solicitud(nombre);

        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idBaremo").isNumber());

        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("BAREMO_INVALIDO"));
    }

    @Test
    void psicologoNoPuedeCargarBaremoConSesionReal() throws Exception {
        String token = abrirSesion(Rol.PSICOLOGO);

        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(solicitud("BFA_INT_" + UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    private String abrirSesion(Rol rol) {
        String correo = "baremo-" + UUID.randomUUID() + "@ejemplo.test";
        cuentas.guardar(
                new Cuenta(
                        null,
                        correo,
                        hash.crear(CLAVE.toCharArray()),
                        "Nombre",
                        "Ficticio",
                        EstadoCuenta.ACTIVA,
                        Set.of(rol)));
        return autenticacion.abrirSesion(correo, CLAVE.toCharArray()).token();
    }

    private String solicitud(String nombre) {
        return "{\"nombre\":\""
                + nombre
                + "\",\"version\":\"v1\",\"anioNorma\":1992,\"rangos\":["
                + "{\"idFactor\":\"VOC1\",\"percentil\":5,\"limiteInferior\":1,"
                + "\"limiteSuperior\":5,\"estadoCelda\":\"RANGO\"}]}";
    }
}
