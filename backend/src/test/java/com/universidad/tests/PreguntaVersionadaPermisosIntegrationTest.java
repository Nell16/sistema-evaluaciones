package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.universidad.tests.application.service.AutenticacionService;
import com.universidad.tests.domain.model.Cuenta;
import com.universidad.tests.domain.model.EstadoCuenta;
import com.universidad.tests.domain.model.Rol;
import com.universidad.tests.domain.port.HashContrasenasPort;
import com.universidad.tests.domain.repository.CuentaRepository;
import com.universidad.tests.infrastructure.persistence.entity.SubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import jakarta.persistence.EntityManager;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * GEN-43-BE: E27 con sesión Bearer real y la matriz aprobada (MatrizPermisos), sin simular la
 * autenticación ni la autorización, contra PostgreSQL y con datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PreguntaVersionadaPermisosIntegrationTest {

    private static final String CONTRASENA = "clave-ficticia-e27";
    private static final String CONFIGURACION =
            "{\"orden\":1,\"tipoRespuesta\":\"SELECCION\",\"opciones\":4,\"tiempoLimiteSegundos\":300,\"instrucciones\":\"Instruccion ficticia\"}";
    private static final String PREGUNTA =
            "{\"numeroVisible\":1,\"ordenPresentacion\":1,\"enunciado\":\"Reactivo ficticio\"}";

    @Autowired private MockMvc mvc;
    @Autowired private EntityManager em;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CuentaRepository cuentas;
    @Autowired private HashContrasenasPort hash;
    @Autowired private AutenticacionService autenticacion;

    private VersionFormularioJpaEntity version;
    private String rutaPreguntas;

    @BeforeEach
    void datos() throws Exception {
        var test = new TestJpaEntity("E27-" + UUID.randomUUID(), "Test ficticio", "Fixture", true);
        em.persist(test);
        version = new VersionFormularioJpaEntity(test, "v1", "Fixture", "fuente-ficticia");
        em.persist(version);
        em.persist(new SubpruebaJpaEntity(test, "R1", "Subprueba ficticia", "Fixture"));
        em.flush();
        String base =
                "/api/v1/versiones/" + version.getIdVersionFormulario() + "/configuraciones/R1";
        rutaPreguntas = base + "/preguntas";

        // La configuración de R1 la crea E26 con una sesión real de COORDINADOR.
        mvc.perform(
                        put(base)
                                .header("Authorization", bearer(Rol.COORDINADOR))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(CONFIGURACION))
                .andExpect(status().isOk());
    }

    @Test
    void coordinadorConSesionRealAgregaLaPregunta() throws Exception {
        mvc.perform(
                        post(rutaPreguntas)
                                .header("Authorization", bearer(Rol.COORDINADOR))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PREGUNTA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numeroVisible").value(1))
                .andExpect(jsonPath("$.ordenPresentacion").value(1))
                .andExpect(jsonPath("$.enunciado").value("Reactivo ficticio"));
        em.flush();
        assertThat(contarPreguntas()).isEqualTo(1);
    }

    @Test
    void unRolSinContenidoEditarRecibe403YNoGuarda() throws Exception {
        mvc.perform(
                        post(rutaPreguntas)
                                .header("Authorization", bearer(Rol.SUJETO))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(PREGUNTA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        em.flush();
        assertThat(contarPreguntas()).isZero();
    }

    @Test
    void sinTokenRecibe401() throws Exception {
        mvc.perform(post(rutaPreguntas).contentType(MediaType.APPLICATION_JSON).content(PREGUNTA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    /** Abre una sesión real (E14) para una cuenta ficticia con el rol indicado. */
    private String bearer(Rol rol) {
        String correo = "e27-" + UUID.randomUUID() + "@ejemplo.test";
        cuentas.guardar(
                new Cuenta(
                        null,
                        correo,
                        hash.crear(CONTRASENA.toCharArray()),
                        "Nombre",
                        "Ficticio",
                        EstadoCuenta.ACTIVA,
                        Set.of(rol)));
        return "Bearer " + autenticacion.abrirSesion(correo, CONTRASENA.toCharArray()).token();
    }

    private long contarPreguntas() {
        return jdbc.queryForObject(
                "select count(*) from pregunta p join configuracion_subprueba c"
                        + " on c.id_configuracion_subprueba = p.id_configuracion_subprueba"
                        + " where c.id_version_formulario = ?",
                Long.class,
                version.getIdVersionFormulario());
    }
}
