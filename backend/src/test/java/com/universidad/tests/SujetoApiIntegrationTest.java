package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * HTTP y PostgreSQL reales. Las pruebas de 403 y de alta simulan la validación del token (spy sobre
 * AutenticacionService) porque la matriz aprobada da SUJETO_LEER a los cinco roles y no existe un
 * rol real sin ese permiso; la prueba de E23 con identificador vacío usa un Bearer emitido por E14.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SujetoApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private AutenticacionService autenticacion;
    @Autowired private ObjectMapper json;
    @Autowired private CuentaRepository cuentas;
    @Autowired private HashContrasenasPort hash;
    private Long cuentaId;

    @BeforeEach
    void cuentaFicticia() {
        cuentaId =
                jdbc.queryForObject(
                        "insert into cuenta(correo,resumen_contrasena,nombres,apellidos,estado) values (?,?,?,?,'ACTIVA') returning id_cuenta",
                        Long.class,
                        "busqueda-" + UUID.randomUUID() + "@ejemplo.test",
                        "hash-ficticio-sin-credencial",
                        "Persona",
                        "Ficticia");
    }

    private static final String RUTA = "/api/v1/sujetos";
    private static final String TOKEN = "sesion-sintetica-sujeto-test";

    private void permisos(String... permisos) {
        doReturn(Optional.of(new IdentidadSesion(cuentaId, 1L, "SUJETO", Set.of(permisos))))
                .when(autenticacion)
                .validar(TOKEN);
    }

    private String codigo() {
        return "PRUEBA-" + UUID.randomUUID();
    }

    private String cuerpo(String codigo) {
        return "{\"identificadorInstitucional\":\""
                + codigo
                + "\",\"nombres\":\"Persona Ficticia\",\"apellidos\":\"Prueba\"}";
    }

    @Test
    void sinTokenNoCreaNiConsulta() throws Exception {
        String codigo = codigo();
        mvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content(cuerpo(codigo)))
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
    void sesionSinPermisosRecibe403YNoEscribe() throws Exception {
        permisos();
        String codigo = codigo();
        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo(codigo)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        mvc.perform(
                        get(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .param("identificadorInstitucional", codigo))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from sujeto where codigo_institucional = ?",
                                Long.class,
                                codigo))
                .isZero();
    }

    @Test
    void permisosDeLecturaYCreacionNoSeIntercambian() throws Exception {
        String codigo = codigo();
        permisos("SUJETO_LEER");
        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo(codigo)))
                .andExpect(status().isForbidden());
        permisos("SUJETO_CREAR");
        mvc.perform(
                        get(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .param("identificadorInstitucional", codigo))
                .andExpect(status().isForbidden());
    }

    @Test
    void creaPersisteBuscaYRechazaDuplicado() throws Exception {
        permisos("SUJETO_CREAR", "SUJETO_LEER");
        String codigo = codigo();
        var creada =
                mvc.perform(
                                post(RUTA)
                                        .header("Authorization", "Bearer " + TOKEN)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo(codigo)))
                        .andExpect(status().isCreated())
                        .andExpect(header().exists("Location"))
                        .andExpect(jsonPath("$.identificadorInstitucional").value(codigo))
                        .andReturn();
        Long id =
                jdbc.queryForObject(
                        "select id_sujeto from sujeto where codigo_institucional = ?",
                        Long.class,
                        codigo);
        assertThat(creada.getResponse().getHeader("Location")).isEqualTo(RUTA + "/" + id);
        mvc.perform(
                        get(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .param("identificadorInstitucional", codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        jdbc.update(
                "insert into cuenta_sujeto_asignado(id_cuenta,id_sujeto) values (?,?)",
                cuentaId,
                id);
        mvc.perform(
                        get(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .param("identificadorInstitucional", codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(
                        get(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .param("identificadorInstitucional", codigo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo(codigo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SUJETO_DUPLICADO"));
    }

    @Test
    void busquedaConIdentificadorVacioDevuelveListaVaciaConBearerDeE14() throws Exception {
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
        String token =
                json.readTree(
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
    void datosInvalidosNoCreanSujeto() throws Exception {
        permisos("SUJETO_CREAR");
        mvc.perform(
                        post(RUTA)
                                .header("Authorization", "Bearer " + TOKEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }
}
