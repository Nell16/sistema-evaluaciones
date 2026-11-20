package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;

import com.universidad.tests.application.service.AprobacionClaveBFAService;
import com.universidad.tests.application.service.GestionClaveBFAService;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AprobacionClaveBFAIntegrationTest {

    private static final String EVIDENCIA = "APROBACION-SINTETICA-BFA";

    @Autowired private AprobacionClaveBFAService servicio;

    @Autowired private GestionClaveBFAService gestionClave;

    @Autowired private JdbcTemplate jdbc;

    // Autorizacion simulada exclusivamente para pruebas.
    // La seguridad de produccion no se modifica.
    @MockitoBean private AutorizacionClaveBFAPort autorizacion;

    private Long idTest;
    private Long idSubprueba;
    private Long idFormulario;
    private Long idConfiguracion;
    private Long idPregunta1;
    private Long idPregunta2;
    private Long idOpcion1;
    private Long idOpcion2;
    private Long idClave;

    // =====================================================
    // 1. PREPARAR DATOS SINTETICOS
    // =====================================================

    @BeforeEach
    void prepararDatosSinteticos() {

        // Impedir que estas pruebas utilicen la base normal.
        String base = jdbc.queryForObject("SELECT current_database()", String.class);

        if (!"evaluaciones_bfa_test".equals(base)) {
            throw new IllegalStateException(
                    "Las pruebas BFA solo pueden ejecutarse " + "en evaluaciones_bfa_test");
        }

        String codigo = "BFA_APR_" + UUID.randomUUID().toString().replace("-", "");

        // Crear test ficticio.
        idTest =
                insertar(
                        "INSERT INTO test "
                                + "(codigo, nombre, descripcion, activo) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_test",
                        codigo,
                        "Test sintetico de aprobacion",
                        "Datos exclusivos para pruebas",
                        true);

        // Crear subprueba ficticia.
        idSubprueba =
                insertar(
                        "INSERT INTO subprueba "
                                + "(id_test, codigo, nombre, descripcion) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_subprueba",
                        idTest,
                        "SUB_APROBACION",
                        "Subprueba sintetica",
                        "Datos de prueba");

        // Crear version del formulario.
        idFormulario =
                insertar(
                        "INSERT INTO version_formulario "
                                + "(id_test, codigo_version, descripcion, fuente_ref) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_version_formulario",
                        idTest,
                        "v1",
                        "Formulario sintetico",
                        "FUENTE-SINTETICA");

        // Configurar la subprueba.
        idConfiguracion =
                insertar(
                        "INSERT INTO configuracion_subprueba "
                                + "(id_version_formulario, id_subprueba, "
                                + "orden_aplicacion, tipo_respuesta, instrucciones_ref) "
                                + "VALUES (?, ?, ?, ?, ?) "
                                + "RETURNING id_configuracion_subprueba",
                        idFormulario,
                        idSubprueba,
                        1,
                        "SELECCION",
                        "Instrucciones sinteticas");

        // Primera pregunta.
        idPregunta1 =
                insertar(
                        "INSERT INTO pregunta "
                                + "(id_configuracion_subprueba, numero_visible, "
                                + "orden_presentacion, enunciado) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_pregunta",
                        idConfiguracion,
                        "1",
                        1,
                        "Pregunta sintetica uno");

        // Segunda pregunta.
        idPregunta2 =
                insertar(
                        "INSERT INTO pregunta "
                                + "(id_configuracion_subprueba, numero_visible, "
                                + "orden_presentacion, enunciado) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_pregunta",
                        idConfiguracion,
                        "2",
                        2,
                        "Pregunta sintetica dos");

        // Opcion correcta de la pregunta 1.
        idOpcion1 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta1,
                        "A",
                        1,
                        "Opcion sintetica uno");

        // Opcion correcta de la pregunta 2.
        idOpcion2 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta2,
                        "A",
                        1,
                        "Opcion sintetica dos");

        // Crear clave de correccion en BORRADOR.
        idClave =
                insertar(
                        "INSERT INTO version_clave_bfa "
                                + "(id_version_formulario, version, referencia_fuente) "
                                + "VALUES (?, ?, ?) RETURNING id_clave",
                        idFormulario,
                        "CLAVE-V1",
                        "MATERIAL-SINTETICO");
    }

    // =====================================================
    // 2. APROBACION CON COBERTURA COMPLETA
    // =====================================================

    @Test
    void debeAprobarClaveCompletaYGuardarEvidenciaEnPostgreSQL() {

        registrarDosEntradasValidas();

        VersionClaveBFA aprobada = servicio.aprobarClave(idClave, EVIDENCIA);

        assertThat(aprobada.getEstado()).isEqualTo(EstadoVersionClaveBFA.APROBADA);

        assertThat(aprobada.getEvidenciaAprobacion()).isEqualTo(EVIDENCIA);

        assertThat(estadoEnBaseDeDatos()).isEqualTo("APROBADA");

        assertThat(evidenciaEnBaseDeDatos()).isEqualTo(EVIDENCIA);

        assertThat(contarEntradas()).isEqualTo(2);
    }

    // =====================================================
    // 3. RECHAZAR COBERTURA INCOMPLETA
    // =====================================================

    @Test
    void debeRechazarAprobacionIncompletaSinCambiarEstado() {

        gestionClave.registrarEntrada(idClave, idPregunta1, idOpcion1);

        assertThatThrownBy(() -> servicio.aprobarClave(idClave, EVIDENCIA))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La clave no cubre exactamente todas");

        assertThat(estadoEnBaseDeDatos()).isEqualTo("BORRADOR");

        assertThat(evidenciaEnBaseDeDatos()).isNull();

        assertThat(contarEntradas()).isEqualTo(1);
    }

    // =====================================================
    // 4. RETIRAR CLAVE Y CONSERVAR EVIDENCIA
    // =====================================================

    @Test
    void debeRetirarClaveAprobadaYConservarEvidencia() {

        registrarDosEntradasValidas();

        servicio.aprobarClave(idClave, EVIDENCIA);

        VersionClaveBFA retirada = servicio.retirarClave(idClave);

        assertThat(retirada.getEstado()).isEqualTo(EstadoVersionClaveBFA.RETIRADA);

        assertThat(retirada.getEvidenciaAprobacion()).isEqualTo(EVIDENCIA);

        assertThat(estadoEnBaseDeDatos()).isEqualTo("RETIRADA");

        assertThat(evidenciaEnBaseDeDatos()).isEqualTo(EVIDENCIA);

        assertThat(contarEntradas()).isEqualTo(2);
    }

    // =====================================================
    // 5. RECHAZAR APROBACION SIN AUTORIZACION
    // =====================================================

    @Test
    void debeDenegarAprobacionSinPermisoDeAprobar() {

        registrarDosEntradasValidas();

        doThrow(new SecurityException("Sin permiso para aprobar"))
                .when(autorizacion)
                .exigirPermisoAprobarClave(idClave);

        assertThatThrownBy(() -> servicio.aprobarClave(idClave, EVIDENCIA))
                .isInstanceOf(SecurityException.class);

        assertThat(estadoEnBaseDeDatos()).isEqualTo("BORRADOR");

        assertThat(evidenciaEnBaseDeDatos()).isNull();
    }

    // =====================================================
    // 6. RECHAZAR APROBACION SIN PERMISO DE CONSULTA
    // =====================================================

    @Test
    void debeDenegarAprobacionSinPermisoDeConsultarCobertura() {

        registrarDosEntradasValidas();

        doThrow(new SecurityException("Sin permiso para consultar la clave"))
                .when(autorizacion)
                .exigirPermisoConsultarClave(idClave);

        assertThatThrownBy(() -> servicio.aprobarClave(idClave, EVIDENCIA))
                .isInstanceOf(SecurityException.class);

        assertThat(estadoEnBaseDeDatos()).isEqualTo("BORRADOR");

        assertThat(evidenciaEnBaseDeDatos()).isNull();
    }

    // =====================================================
    // 7. RECHAZAR RETIRO SIN AUTORIZACION
    // =====================================================

    @Test
    void debeDenegarRetiroSinPermiso() {

        registrarDosEntradasValidas();

        servicio.aprobarClave(idClave, EVIDENCIA);

        doThrow(new SecurityException("Sin permiso para retirar"))
                .when(autorizacion)
                .exigirPermisoRetirarClave(idClave);

        assertThatThrownBy(() -> servicio.retirarClave(idClave))
                .isInstanceOf(SecurityException.class);

        assertThat(estadoEnBaseDeDatos()).isEqualTo("APROBADA");

        assertThat(evidenciaEnBaseDeDatos()).isEqualTo(EVIDENCIA);
    }

    // =====================================================
    // 8. RECHAZAR APROBACION SIN EVIDENCIA
    // =====================================================

    @Test
    void debeRechazarAprobacionSinEvidencia() {

        registrarDosEntradasValidas();

        assertThatThrownBy(() -> servicio.aprobarClave(idClave, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evidencia institucional");

        assertThat(estadoEnBaseDeDatos()).isEqualTo("BORRADOR");

        assertThat(evidenciaEnBaseDeDatos()).isNull();
    }

    // =====================================================
    // 9. METODOS AUXILIARES
    // =====================================================

    private void registrarDosEntradasValidas() {

        gestionClave.registrarEntradas(
                idClave,
                List.of(
                        new EntradaClaveBFA(null, idClave, idPregunta1, idOpcion1),
                        new EntradaClaveBFA(null, idClave, idPregunta2, idOpcion2)));
    }

    private Long insertar(String sql, Object... parametros) {

        return jdbc.queryForObject(sql, Long.class, parametros);
    }

    private String estadoEnBaseDeDatos() {

        return jdbc.queryForObject(
                "SELECT estado FROM version_clave_bfa " + "WHERE id_clave = ?",
                String.class,
                idClave);
    }

    private String evidenciaEnBaseDeDatos() {

        return jdbc.queryForObject(
                "SELECT evidencia_aprobacion " + "FROM version_clave_bfa WHERE id_clave = ?",
                String.class,
                idClave);
    }

    private long contarEntradas() {

        Long cantidad =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM entrada_clave_bfa " + "WHERE id_clave = ?",
                        Long.class,
                        idClave);

        return cantidad == null ? -1 : cantidad;
    }

    // No eliminamos registros sinteticos al finalizar.
    // Las claves aprobadas o retiradas deben conservar
    // sus registros historicos.
    //
    // Esta clase solo puede ejecutarse en
    // evaluaciones_bfa_test.
}
