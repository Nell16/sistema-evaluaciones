package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.application.service.IdentidadSesion;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** E20 con HTTP y PostgreSQL reales. Solo se sustituye la validación del token. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PeriodoApiIntegrationTest {
    private static final String RUTA = "/api/v1/periodos";
    private static final String TOKEN = "sesion-sintetica-periodo-test";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    @MockitoSpyBean private AutenticacionService autenticacion;
    private Long cuentaId;

    @BeforeEach
    void cuentaFicticia() {
        cuentaId =
                jdbc.queryForObject(
                        "insert into cuenta(correo,resumen_contrasena,nombres,apellidos,estado) values (?,?,?,?,'ACTIVA') returning id_cuenta",
                        Long.class,
                        "periodo-" + UUID.randomUUID() + "@ejemplo.test",
                        "hash-ficticio-sin-credencial",
                        "Persona",
                        "Ficticia");
    }

    private void permisos(String... permisos) {
        doReturn(Optional.of(new IdentidadSesion(cuentaId, 1L, "COORDINADOR", Set.of(permisos))))
                .when(autenticacion)
                .validar(TOKEN);
    }

    private String cuerpo(String codigo, String inicio, String fin) throws Exception {
        return json.writeValueAsString(
                Map.of(
                        "codigo", codigo,
                        "denominacion", "Periodo ficticio",
                        "inicio", inicio,
                        "fin", fin));
    }

    private ResultActions crear(String cuerpo) throws Exception {
        return mvc.perform(
                post(RUTA)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo));
    }

    private String codigo() {
        return "P-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private long cuenta(String codigo) {
        return jdbc.queryForObject(
                "select count(*) from periodo where codigo = ?", Long.class, codigo);
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(
                        post(RUTA)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo(codigo(), "2026-08-01", "2026-12-15")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sesionSinPermisoRecibe403YNoEscribe() throws Exception {
        permisos();
        String codigo = codigo();
        crear(cuerpo(codigo, "2026-08-01", "2026-12-15"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        assertThat(cuenta(codigo)).isZero();
    }

    @Test
    void creaPersisteYRechazaDuplicado() throws Exception {
        permisos("PERIODO_CREAR");
        String codigo = codigo();
        var creado =
                crear(cuerpo(codigo, "2026-08-01", "2026-12-15"))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.codigo").value(codigo))
                        .andReturn();
        Long id =
                jdbc.queryForObject(
                        "select id_periodo from periodo where codigo = ?", Long.class, codigo);
        assertThat(creado.getResponse().getHeader("Location")).isEqualTo(RUTA + "/" + id);
        crear(cuerpo(codigo, "2027-02-01", "2027-06-15"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("PERIODO_DUPLICADO"));
        assertThat(cuenta(codigo)).isEqualTo(1);
    }

    @Test
    void inicioPosteriorAlFinRecibe422() throws Exception {
        permisos("PERIODO_CREAR");
        String codigo = codigo();
        crear(cuerpo(codigo, "2026-12-20", "2026-08-01"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("PERIODO_INVALIDO"));
        assertThat(cuenta(codigo)).isZero();
    }
}
