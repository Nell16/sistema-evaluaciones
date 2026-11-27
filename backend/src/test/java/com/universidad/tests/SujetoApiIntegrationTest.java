package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evidencia de aceptación de E18/E23: HTTP, PostgreSQL de Flyway, matriz real y Bearer emitido por
 * E14, sin dobles. Las pruebas con sesión simulada están aparte en SujetoSesionSimuladaTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SujetoApiIntegrationTest {
    private static final String RUTA = "/api/v1/sujetos";
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @Autowired private CuentaRepository cuentas;
    @Autowired private HashContrasenasPort hash;

    private String tokenE14() throws Exception {
        String clave = "solo-fixture-sintetico-123";
        String correo = "e23-" + UUID.randomUUID() + "@ejemplo.test";
        cuentas.guardar(
                new Cuenta(
                        null,
                        correo,
                        hash.crear(clave.toCharArray()),
                        "Persona",
                        "Ficticia",
                        EstadoCuenta.ACTIVA,
                        Set.of(Rol.SUJETO)));
        return json.readTree(
                        mvc.perform(
                                        post("/api/v1/auth/sesion")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        json.writeValueAsString(
                                                                Map.of(
                                                                        "correo",
                                                                        correo,
                                                                        "contrasena",
                                                                        clave))))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .path("token")
                .asText();
    }

    @Test
    void sinTokenNoCreaNiConsulta() throws Exception {
        String codigo = "PRUEBA-" + UUID.randomUUID();
        mvc.perform(
                        post(RUTA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"identificadorInstitucional\":\""
                                                + codigo
                                                + "\",\"nombres\":\"Persona Ficticia\",\"apellidos\":\"Prueba\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(RUTA).param("identificadorInstitucional", codigo))
                .andExpect(status().isUnauthorized());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from sujeto where codigo_institucional = ?",
                                Long.class,
                                codigo))
                .isZero();
    }

    @Test
    void busquedaConIdentificadorVacioDevuelveListaVaciaConBearerDeE14() throws Exception {
        String token = tokenE14();
        for (String vacio : new String[] {"", " ", "   "}) {
            mvc.perform(
                            get(RUTA)
                                    .header("Authorization", "Bearer " + token)
                                    .param("identificadorInstitucional", vacio))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Test
    void busquedaSinParametroDevuelve400ConElParametroEnDetalles() throws Exception {
        mvc.perform(get(RUTA).header("Authorization", "Bearer " + tokenE14()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.detalles.length()").value(1))
                .andExpect(jsonPath("$.detalles[0].campo").value("identificadorInstitucional"));
    }
}
