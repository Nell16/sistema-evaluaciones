package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.universidad.tests.application.service.GestionClaveBFAService;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CargaMultipleClaveBFAIntegrationTest {

    @Autowired private GestionClaveBFAService servicio;

    @Autowired private JdbcTemplate jdbc;

    // Autorizar operaciones solo durante estas pruebas
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

        String sufijo = UUID.randomUUID().toString().replace("-", "");

        // Crear test ficticio
        idTest =
                insertar(
                        "INSERT INTO test "
                                + "(codigo, nombre, descripcion, activo) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_test",
                        "BFA_CARGA_" + sufijo,
                        "Test sintetico BFA",
                        "Solo para prueba de integracion",
                        true);

        // Crear subprueba
        idSubprueba =
                insertar(
                        "INSERT INTO subprueba "
                                + "(id_test, codigo, nombre, descripcion) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_subprueba",
                        idTest,
                        "SUB_CARGA",
                        "Subprueba sintetica",
                        "Datos de prueba");

        // Crear formulario
        idFormulario =
                insertar(
                        "INSERT INTO version_formulario "
                                + "(id_test, codigo_version, descripcion, fuente_ref) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_version_formulario",
                        idTest,
                        "v1",
                        "Formulario sintetico",
                        "FUENTE-SINTETICA");

        // Configurar subprueba
        idConfiguracion =
                insertar(
                        "INSERT INTO configuracion_subprueba "
                                + "(id_version_formulario, id_subprueba, "
                                + "orden_aplicacion, tipo_respuesta, "
                                + "instrucciones_ref) "
                                + "VALUES (?, ?, ?, ?, ?) "
                                + "RETURNING id_configuracion_subprueba",
                        idFormulario,
                        idSubprueba,
                        1,
                        "SELECCION",
                        "Instrucciones sinteticas");

        // Crear primera pregunta
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

        // Crear segunda pregunta
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

        // Opcion perteneciente a pregunta 1
        idOpcion1 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta1,
                        "A",
                        1,
                        "Opcion sintetica uno");

        // Opcion perteneciente a pregunta 2
        idOpcion2 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) RETURNING id_opcion",
                        idPregunta2,
                        "A",
                        1,
                        "Opcion sintetica dos");

        // Crear version de clave BFA en borrador
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
    // 2. PRUEBA: CARGA MULTIPLE CORRECTA
    // =====================================================

    @Test
    void debePersistirTodaLaCargaMultipleValida() {

        // Preparar dos entradas validas
        List<EntradaClaveBFA> entradas =
                List.of(
                        new EntradaClaveBFA(null, idClave, idPregunta1, idOpcion1),
                        new EntradaClaveBFA(null, idClave, idPregunta2, idOpcion2));

        // Ejecutar la carga mediante el servicio
        List<EntradaClaveBFA> resultado = servicio.registrarEntradas(idClave, entradas);

        // Verificar que se procesaron las dos
        assertThat(resultado).hasSize(2);

        // Comprobar los registros reales en PostgreSQL
        assertThat(contarEntradas()).isEqualTo(2);
    }

    // =====================================================
    // 3. PRUEBA: REVERSION DE TODA LA TRANSACCION
    // =====================================================

    @Test
    void debeRevertirCargaCompletaSiFallaLaSegundaEntrada() {

        // Primera entrada valida
        EntradaClaveBFA entradaValida = new EntradaClaveBFA(null, idClave, idPregunta1, idOpcion1);

        // Segunda entrada invalida:
        // la opcion 1 no pertenece a la pregunta 2
        EntradaClaveBFA entradaInvalida =
                new EntradaClaveBFA(null, idClave, idPregunta2, idOpcion1);

        List<EntradaClaveBFA> entradas = List.of(entradaValida, entradaInvalida);

        // La operacion completa debe fallar
        assertThatThrownBy(() -> servicio.registrarEntradas(idClave, entradas))
                .hasMessageContaining("La opcion correcta no pertenece a la pregunta");

        // Verificar que PostgreSQL revirtio
        // tambien la primera entrada
        assertThat(contarEntradas()).isZero();
    }

    // =====================================================
    // 4. METODOS AUXILIARES
    // =====================================================

    private Long insertar(String sql, Object... parametros) {

        return jdbc.queryForObject(sql, Long.class, parametros);
    }

    private long contarEntradas() {

        Long cantidad =
                jdbc.queryForObject(
                        "SELECT COUNT(*) " + "FROM entrada_clave_bfa " + "WHERE id_clave = ?",
                        Long.class,
                        idClave);

        return cantidad == null ? -1L : cantidad;
    }

    // =====================================================
    // 5. ELIMINAR DATOS SINTETICOS
    // =====================================================

    @AfterEach
    void eliminarDatosSinteticos() {

        // Eliminar registros en orden inverso
        // para respetar las claves foraneas

        if (idClave != null) {

            jdbc.update("DELETE FROM entrada_clave_bfa " + "WHERE id_clave = ?", idClave);

            jdbc.update("DELETE FROM version_clave_bfa " + "WHERE id_clave = ?", idClave);
        }

        if (idOpcion1 != null) {
            jdbc.update("DELETE FROM opcion_respuesta WHERE id_opcion = ?", idOpcion1);
        }

        if (idOpcion2 != null) {
            jdbc.update("DELETE FROM opcion_respuesta WHERE id_opcion = ?", idOpcion2);
        }

        if (idPregunta1 != null) {
            jdbc.update("DELETE FROM pregunta WHERE id_pregunta = ?", idPregunta1);
        }

        if (idPregunta2 != null) {
            jdbc.update("DELETE FROM pregunta WHERE id_pregunta = ?", idPregunta2);
        }

        if (idConfiguracion != null) {
            jdbc.update(
                    "DELETE FROM configuracion_subprueba " + "WHERE id_configuracion_subprueba = ?",
                    idConfiguracion);
        }

        if (idFormulario != null) {
            jdbc.update(
                    "DELETE FROM version_formulario " + "WHERE id_version_formulario = ?",
                    idFormulario);
        }

        if (idSubprueba != null) {
            jdbc.update("DELETE FROM subprueba WHERE id_subprueba = ?", idSubprueba);
        }

        if (idTest != null) {
            jdbc.update("DELETE FROM test WHERE id_test = ?", idTest);
        }
    }
}
