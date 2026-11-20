package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ProteccionClavesBFAIntegrationTest {

    private static final String EVIDENCIA = "EVIDENCIA-SINTETICA-INTEGRIDAD";

    @Autowired private JdbcTemplate jdbc;

    private Long idTest;
    private Long idSubprueba;
    private Long idFormulario;
    private Long idConfiguracion;
    private Long idPregunta1;
    private Long idPregunta2;
    private Long idOpcion1;
    private Long idOpcionAlternativa;
    private Long idOpcion2;
    private Long idClave;
    private Long idEntrada;

    @BeforeEach
    void prepararDatosSinteticos() {

        String base = jdbc.queryForObject("SELECT current_database()", String.class);

        if (!"evaluaciones_bfa_test".equals(base)) {
            throw new IllegalStateException(
                    "Las pruebas BFA solo pueden ejecutarse " + "en evaluaciones_bfa_test");
        }

        String codigo = "BFA_PROT_" + UUID.randomUUID().toString().replace("-", "");

        idTest =
                insertar(
                        "INSERT INTO test "
                                + "(codigo, nombre, descripcion, activo) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_test",
                        codigo,
                        "Test sintetico BFA",
                        "Pruebas de integridad",
                        true);

        idSubprueba =
                insertar(
                        "INSERT INTO subprueba "
                                + "(id_test, codigo, nombre, descripcion) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_subprueba",
                        idTest,
                        "SUB_INTEGRIDAD",
                        "Subprueba sintetica",
                        "Datos de prueba");

        idFormulario =
                insertar(
                        "INSERT INTO version_formulario "
                                + "(id_test, codigo_version, descripcion, fuente_ref) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_version_formulario",
                        idTest,
                        "v1",
                        "Formulario sintetico",
                        "FUENTE-SINTETICA");

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

        idOpcion1 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta1,
                        "A",
                        1,
                        "Opcion sintetica A");

        idOpcionAlternativa =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta1,
                        "B",
                        2,
                        "Opcion sintetica B");

        idOpcion2 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta2,
                        "A",
                        1,
                        "Opcion sintetica segunda pregunta");

        idClave =
                insertar(
                        "INSERT INTO version_clave_bfa "
                                + "(id_version_formulario, version, referencia_fuente) "
                                + "VALUES (?, ?, ?) RETURNING id_clave",
                        idFormulario,
                        "CLAVE-V1",
                        "MATERIAL-SINTETICO");

        idEntrada =
                insertar(
                        "INSERT INTO entrada_clave_bfa "
                                + "(id_clave, id_pregunta, id_opcion_correcta) "
                                + "VALUES (?, ?, ?) RETURNING id_entrada_clave",
                        idClave,
                        idPregunta1,
                        idOpcion1);
    }

    // =====================================================
    // PRUEBA 1 - IMPEDIR MODIFICAR RESPUESTAS
    // =====================================================

    @Test
    void debeImpedirModificarUnaEntradaYaRegistrada() {

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE entrada_clave_bfa "
                                                + "SET id_opcion_correcta = ? "
                                                + "WHERE id_entrada_clave = ?",
                                        idOpcionAlternativa,
                                        idEntrada))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(
                        "No se permite modificar entradas " + "de correccion existentes");

        Long opcionGuardada =
                jdbc.queryForObject(
                        "SELECT id_opcion_correcta "
                                + "FROM entrada_clave_bfa "
                                + "WHERE id_entrada_clave = ?",
                        Long.class,
                        idEntrada);

        assertThat(opcionGuardada).isEqualTo(idOpcion1);
    }

    // =====================================================
    // PRUEBA 2 - IMPEDIR AGREGAR RESPUESTAS A APROBADAS
    // =====================================================

    @Test
    void debeImpedirAgregarEntradaAClaveAprobada() {

        aprobar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO entrada_clave_bfa "
                                                + "(id_clave, id_pregunta, "
                                                + "id_opcion_correcta) "
                                                + "VALUES (?, ?, ?)",
                                        idClave,
                                        idPregunta2,
                                        idOpcion2))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Solo se pueden agregar entradas " + "a claves en BORRADOR");

        assertThat(contarEntradas()).isEqualTo(1L);
        assertThat(estado()).isEqualTo("APROBADA");
    }

    // =====================================================
    // PRUEBA 3 - IMPEDIR AGREGAR RESPUESTAS A RETIRADAS
    // =====================================================

    @Test
    void debeImpedirAgregarEntradaAClaveRetirada() {

        aprobar();
        retirar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO entrada_clave_bfa "
                                                + "(id_clave, id_pregunta, "
                                                + "id_opcion_correcta) "
                                                + "VALUES (?, ?, ?)",
                                        idClave,
                                        idPregunta2,
                                        idOpcion2))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Solo se pueden agregar entradas " + "a claves en BORRADOR");

        assertThat(contarEntradas()).isEqualTo(1L);
        assertThat(estado()).isEqualTo("RETIRADA");
    }

    // =====================================================
    // PRUEBA 4 - IMPEDIR CAMBIAR VERSION
    // =====================================================

    @Test
    void debeImpedirCambiarLaVersionDeClave() {

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE version_clave_bfa "
                                                + "SET version = ? "
                                                + "WHERE id_clave = ?",
                                        "CLAVE-ALTERADA",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(
                        "No se permite modificar la identidad " + "o version de una clave");

        String version =
                jdbc.queryForObject(
                        "SELECT version FROM version_clave_bfa " + "WHERE id_clave = ?",
                        String.class,
                        idClave);

        assertThat(version).isEqualTo("CLAVE-V1");
    }

    // =====================================================
    // PRUEBA 5 - IMPEDIR REGRESAR A BORRADOR
    // =====================================================

    @Test
    void debeImpedirRegresarDeAprobadaABorrador() {

        aprobar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE version_clave_bfa "
                                                + "SET estado = 'BORRADOR' "
                                                + "WHERE id_clave = ?",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Transicion de estado BFA no permitida");

        assertThat(estado()).isEqualTo("APROBADA");
    }

    // =====================================================
    // PRUEBA 6 - CONSERVAR EVIDENCIA
    // =====================================================

    @Test
    void debeImpedirCambiarEvidenciaDeClaveAprobada() {

        aprobar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE version_clave_bfa "
                                                + "SET evidencia_aprobacion = ? "
                                                + "WHERE id_clave = ?",
                                        "EVIDENCIA-ALTERADA",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Transicion de estado BFA no permitida");

        assertThat(evidencia()).isEqualTo(EVIDENCIA);
    }

    // =====================================================
    // PRUEBA 7 - IMPEDIR REACTIVAR CLAVE RETIRADA
    // =====================================================

    @Test
    void debeImpedirReactivarClaveRetirada() {

        aprobar();
        retirar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE version_clave_bfa "
                                                + "SET estado = 'APROBADA' "
                                                + "WHERE id_clave = ?",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Transicion de estado BFA no permitida");

        assertThat(estado()).isEqualTo("RETIRADA");
        assertThat(evidencia()).isEqualTo(EVIDENCIA);
    }

    // =====================================================
    // PRUEBA 8 - NO CREAR CLAVES DIRECTAMENTE APROBADAS
    // =====================================================

    @Test
    void debeImpedirCrearClaveDirectamenteAprobada() {

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO version_clave_bfa "
                                                + "(id_version_formulario, version, "
                                                + "referencia_fuente, estado, "
                                                + "evidencia_aprobacion) "
                                                + "VALUES (?, ?, ?, ?, ?)",
                                        idFormulario,
                                        "CLAVE-ILEGAL",
                                        "MATERIAL-SINTETICO",
                                        "APROBADA",
                                        EVIDENCIA))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Las claves nuevas deben iniciar en BORRADOR");

        Long cantidad =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM version_clave_bfa "
                                + "WHERE id_version_formulario = ? "
                                + "AND version = ?",
                        Long.class,
                        idFormulario,
                        "CLAVE-ILEGAL");

        assertThat(cantidad).isZero();
    }

    // =====================================================
    // PRUEBA 9 - NO ELIMINAR RESPUESTAS DE CLAVE APROBADA
    // =====================================================

    @Test
    void debeImpedirEliminarEntradaDeClaveAprobada() {

        aprobar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM entrada_clave_bfa "
                                                + "WHERE id_entrada_clave = ?",
                                        idEntrada))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("No se permite eliminar entradas " + "de claves protegidas");

        assertThat(contarEntradas()).isEqualTo(1L);
        assertThat(estado()).isEqualTo("APROBADA");
    }

    // =====================================================
    // PRUEBA 10 - NO ELIMINAR CLAVE APROBADA
    // =====================================================

    @Test
    void debeImpedirEliminarClaveAprobada() {

        aprobar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM version_clave_bfa " + "WHERE id_clave = ?",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("No se permite eliminar claves protegidas");

        assertThat(estado()).isEqualTo("APROBADA");
        assertThat(evidencia()).isEqualTo(EVIDENCIA);
    }

    // =====================================================
    // PRUEBA 11 - NO ELIMINAR RESPUESTAS DE CLAVE RETIRADA
    // =====================================================

    @Test
    void debeImpedirEliminarEntradaDeClaveRetirada() {

        aprobar();
        retirar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM entrada_clave_bfa "
                                                + "WHERE id_entrada_clave = ?",
                                        idEntrada))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("No se permite eliminar entradas " + "de claves protegidas");

        assertThat(contarEntradas()).isEqualTo(1L);
        assertThat(estado()).isEqualTo("RETIRADA");
    }

    // =====================================================
    // PRUEBA 12 - NO ELIMINAR CLAVE RETIRADA
    // =====================================================

    @Test
    void debeImpedirEliminarClaveRetirada() {

        aprobar();
        retirar();

        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "DELETE FROM version_clave_bfa " + "WHERE id_clave = ?",
                                        idClave))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("No se permite eliminar claves protegidas");

        assertThat(estado()).isEqualTo("RETIRADA");
        assertThat(evidencia()).isEqualTo(EVIDENCIA);
    }

    // =====================================================
    // METODOS AUXILIARES
    // =====================================================

    private Long insertar(String sql, Object... parametros) {

        return jdbc.queryForObject(sql, Long.class, parametros);
    }

    private void aprobar() {

        jdbc.update(
                "UPDATE version_clave_bfa "
                        + "SET estado = 'APROBADA', "
                        + "evidencia_aprobacion = ? "
                        + "WHERE id_clave = ?",
                EVIDENCIA,
                idClave);
    }

    private void retirar() {

        jdbc.update(
                "UPDATE version_clave_bfa " + "SET estado = 'RETIRADA' " + "WHERE id_clave = ?",
                idClave);
    }

    private String estado() {

        return jdbc.queryForObject(
                "SELECT estado FROM version_clave_bfa " + "WHERE id_clave = ?",
                String.class,
                idClave);
    }

    private String evidencia() {

        return jdbc.queryForObject(
                "SELECT evidencia_aprobacion " + "FROM version_clave_bfa " + "WHERE id_clave = ?",
                String.class,
                idClave);
    }

    private long contarEntradas() {

        Long cantidad =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM entrada_clave_bfa " + "WHERE id_clave = ?",
                        Long.class,
                        idClave);

        return cantidad == null ? -1L : cantidad;
    }

    // No eliminar datos historicos de las pruebas.
    // Los registros quedan en la base aislada:
    // evaluaciones_bfa_test.
}
