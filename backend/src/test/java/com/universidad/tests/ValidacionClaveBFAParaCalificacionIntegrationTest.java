package com.universidad.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.universidad.tests.application.service.AprobacionClaveBFAService;
import com.universidad.tests.application.service.GestionClaveBFAService;
import com.universidad.tests.application.service.ValidacionClaveBFAParaCalificacionService;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ValidacionClaveBFAParaCalificacionIntegrationTest {

    private static final String EVIDENCIA = "APROBACION-SINTETICA-CALIFICACION";

    @Autowired private ValidacionClaveBFAParaCalificacionService servicio;

    @Autowired private AprobacionClaveBFAService aprobacionService;

    @Autowired private GestionClaveBFAService gestionService;

    @Autowired private JdbcTemplate jdbc;

    @MockitoBean private AutorizacionClaveBFAPort autorizacion;

    private Long idTest;
    private Long idFormulario;
    private Long idClave;

    private Long idPregunta1;
    private Long idPregunta2;
    private Long idOpcion1;
    private Long idOpcion2;

    // =====================================================
    // PREPARAR DATOS SINTETICOS
    // =====================================================

    @BeforeEach
    void prepararDatosSinteticos() {

        String base = jdbc.queryForObject("SELECT current_database()", String.class);

        if (!"evaluaciones_bfa_test".equals(base)) {
            throw new IllegalStateException(
                    "Estas pruebas solo pueden ejecutarse " + "en evaluaciones_bfa_test");
        }

        String codigo = "BFA_CAL_" + UUID.randomUUID().toString().replace("-", "");

        idTest =
                insertar(
                        "INSERT INTO test "
                                + "(codigo, nombre, descripcion, activo) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_test",
                        codigo,
                        "Test sintetico de calificacion BFA",
                        "Prueba de integracion",
                        true);

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

        idClave =
                insertar(
                        "INSERT INTO version_clave_bfa "
                                + "(id_version_formulario, version, referencia_fuente) "
                                + "VALUES (?, ?, ?) "
                                + "RETURNING id_clave",
                        idFormulario,
                        "CLAVE-V1",
                        "MATERIAL-SINTETICO");
    }

    // =====================================================
    // 1. PERMITIR CLAVE APROBADA
    // =====================================================

    @Test
    void debeAceptarClaveAprobadaDesdePostgreSQL() {

        aprobarClaveDirectamente();

        VersionClaveBFA resultado = servicio.obtenerClaveAprobada(idClave, idFormulario);

        assertEquals(EstadoVersionClaveBFA.APROBADA, resultado.getEstado());

        assertEquals(idClave, resultado.getIdClave());

        assertEquals(idFormulario, resultado.getIdVersionFormulario());

        assertEquals(EVIDENCIA, resultado.getEvidenciaAprobacion());
    }

    // =====================================================
    // 2. RECHAZAR CLAVE EN BORRADOR
    // =====================================================

    @Test
    void debeRechazarClaveEnBorradorDesdePostgreSQL() {

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerClaveAprobada(idClave, idFormulario));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        assertEquals("BORRADOR", estadoEnBaseDeDatos());
    }

    // =====================================================
    // 3. RECHAZAR CLAVE RETIRADA
    // =====================================================

    @Test
    void debeRechazarClaveRetiradaDesdePostgreSQL() {

        aprobarClaveDirectamente();
        retirarClave();

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerClaveAprobada(idClave, idFormulario));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        assertEquals("RETIRADA", estadoEnBaseDeDatos());

        assertEquals(EVIDENCIA, evidenciaEnBaseDeDatos());
    }

    // =====================================================
    // 4. RECHAZAR CLAVE DE OTRO FORMULARIO
    // =====================================================

    @Test
    void debeRechazarClaveDeOtroFormularioDesdePostgreSQL() {

        aprobarClaveDirectamente();

        Long idFormularioDiferente = idFormulario + 10000L;

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerClaveAprobada(idClave, idFormularioDiferente));

        assertTrue(excepcion.getMessage().contains("no pertenece a la version"));

        assertEquals("APROBADA", estadoEnBaseDeDatos());
    }

    // =====================================================
    // 5. RECHAZAR CLAVE INEXISTENTE
    // =====================================================

    @Test
    void debeRechazarClaveInexistenteDesdePostgreSQL() {

        assertThrows(
                NoSuchElementException.class,
                () -> servicio.obtenerClaveAprobada(Long.MAX_VALUE, idFormulario));
    }

    // =====================================================
    // 6. RECHAZAR CLAVE SIN AUTORIZACION
    // =====================================================

    @Test
    void debeRechazarAccesoSinPermisoDesdePostgreSQL() {

        aprobarClaveDirectamente();

        doThrow(new SecurityException("Operacion no autorizada: permiso BFA denegado"))
                .when(autorizacion)
                .exigirPermisoCalificarConClave(idClave);

        SecurityException excepcion =
                assertThrows(
                        SecurityException.class,
                        () -> servicio.obtenerClaveAprobada(idClave, idFormulario));

        assertTrue(excepcion.getMessage().contains("permiso BFA denegado"));

        verify(autorizacion).exigirPermisoCalificarConClave(idClave);

        assertEquals("APROBADA", estadoEnBaseDeDatos());

        assertEquals(EVIDENCIA, evidenciaEnBaseDeDatos());
    }

    // =====================================================
    // 7. RECUPERAR RESPUESTAS CORRECTAS AUTORIZADAS
    // =====================================================

    @Test
    void debeEntregarEntradasDeClaveCompletaYAutorizada() {

        prepararPreguntasSeleccion();
        registrarDosEntradasValidas();

        // Aprobar con la validacion real de cobertura.
        aprobacionService.aprobarClave(idClave, EVIDENCIA);

        // No contar permisos usados en la preparacion.
        clearInvocations(autorizacion);

        List<EntradaClaveBFA> resultado =
                servicio.obtenerEntradasAprobadasParaCalificacion(idClave, idFormulario);

        assertEquals(2, resultado.size());

        assertTrue(
                resultado.stream()
                        .anyMatch(
                                entrada ->
                                        idPregunta1.equals(entrada.getIdPregunta())
                                                && idOpcion1.equals(
                                                        entrada.getIdOpcionCorrecta())));

        assertTrue(
                resultado.stream()
                        .anyMatch(
                                entrada ->
                                        idPregunta2.equals(entrada.getIdPregunta())
                                                && idOpcion2.equals(
                                                        entrada.getIdOpcionCorrecta())));

        assertThrows(UnsupportedOperationException.class, () -> resultado.clear());

        // CORRECCION:
        // El servicio puede verificar el permiso de
        // calificacion mas de una vez durante el flujo.
        verify(autorizacion, atLeastOnce()).exigirPermisoCalificarConClave(idClave);

        // El permiso de consulta administrativa no
        // es necesario para el flujo de calificacion.
        verify(autorizacion, never()).exigirPermisoConsultarClave(idClave);

        assertEquals("APROBADA", estadoEnBaseDeDatos());

        assertEquals(EVIDENCIA, evidenciaEnBaseDeDatos());
    }

    // =====================================================
    // 8. RECHAZAR CLAVE APROBADA PERO INCOMPLETA
    // =====================================================

    @Test
    void debeRechazarClaveAprobadaPeroIncompleta() {

        prepararPreguntasSeleccion();

        // Solo una de dos respuestas correctas.
        registrarPrimeraEntrada();

        // Simular un registro aprobado que no paso
        // por la validacion del servicio de aprobacion.
        aprobarClaveDirectamente();

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                servicio.obtenerEntradasAprobadasParaCalificacion(
                                        idClave, idFormulario));

        assertTrue(excepcion.getMessage().contains("La clave no cubre exactamente"));

        assertEquals("APROBADA", estadoEnBaseDeDatos());

        assertEquals(1L, contarEntradas());
    }

    // =====================================================
    // 9. RECHAZAR ENTREGA SIN PERMISO DE CALIFICACION
    // =====================================================

    @Test
    void debeDenegarEntregaSinPermisoDeCalificacion() {

        prepararPreguntasSeleccion();
        registrarDosEntradasValidas();

        aprobacionService.aprobarClave(idClave, EVIDENCIA);

        clearInvocations(autorizacion);

        doThrow(new SecurityException("Sin permiso para calificar"))
                .when(autorizacion)
                .exigirPermisoCalificarConClave(idClave);

        SecurityException excepcion =
                assertThrows(
                        SecurityException.class,
                        () ->
                                servicio.obtenerEntradasAprobadasParaCalificacion(
                                        idClave, idFormulario));

        assertTrue(excepcion.getMessage().contains("Sin permiso para calificar"));

        verify(autorizacion).exigirPermisoCalificarConClave(idClave);

        verify(autorizacion, never()).exigirPermisoConsultarClave(idClave);

        assertEquals("APROBADA", estadoEnBaseDeDatos());

        assertEquals(2L, contarEntradas());
    }

    // =====================================================
    // 10. CALIFICACION INDEPENDIENTE DE CONSULTA ADMIN
    // =====================================================

    @Test
    void debePermitirCalificacionSinPermisoDeConsultaAdministrativa() {

        prepararPreguntasSeleccion();
        registrarDosEntradasValidas();

        aprobacionService.aprobarClave(idClave, EVIDENCIA);

        clearInvocations(autorizacion);

        // Simular que el componente no tiene permiso
        // de consulta administrativa de claves.
        doThrow(new SecurityException("Sin permiso de consulta administrativa"))
                .when(autorizacion)
                .exigirPermisoConsultarClave(idClave);

        // La calificacion tiene un permiso especifico.
        // No debe depender del permiso administrativo.
        List<EntradaClaveBFA> resultado =
                servicio.obtenerEntradasAprobadasParaCalificacion(idClave, idFormulario);

        assertEquals(2, resultado.size());

        verify(autorizacion, atLeastOnce()).exigirPermisoCalificarConClave(idClave);

        verify(autorizacion, never()).exigirPermisoConsultarClave(idClave);

        assertEquals("APROBADA", estadoEnBaseDeDatos());

        assertEquals(EVIDENCIA, evidenciaEnBaseDeDatos());
    }

    // =====================================================
    // METODOS AUXILIARES
    // =====================================================

    private void prepararPreguntasSeleccion() {

        Long idSubprueba =
                insertar(
                        "INSERT INTO subprueba "
                                + "(id_test, codigo, nombre, descripcion) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_subprueba",
                        idTest,
                        "SUB_CAL",
                        "Subprueba sintetica",
                        "Datos de prueba");

        Long idConfiguracion =
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
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_pregunta",
                        idConfiguracion,
                        "1",
                        1,
                        "Pregunta sintetica uno");

        idPregunta2 =
                insertar(
                        "INSERT INTO pregunta "
                                + "(id_configuracion_subprueba, numero_visible, "
                                + "orden_presentacion, enunciado) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_pregunta",
                        idConfiguracion,
                        "2",
                        2,
                        "Pregunta sintetica dos");

        idOpcion1 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_opcion",
                        idPregunta1,
                        "A",
                        1,
                        "Opcion sintetica uno");

        idOpcion2 =
                insertar(
                        "INSERT INTO opcion_respuesta "
                                + "(id_pregunta, codigo, orden_presentacion, texto) "
                                + "VALUES (?, ?, ?, ?) "
                                + "RETURNING id_opcion",
                        idPregunta2,
                        "A",
                        1,
                        "Opcion sintetica dos");
    }

    private void registrarPrimeraEntrada() {

        gestionService.registrarEntrada(idClave, idPregunta1, idOpcion1);
    }

    private void registrarDosEntradasValidas() {

        gestionService.registrarEntradas(
                idClave,
                List.of(
                        new EntradaClaveBFA(null, idClave, idPregunta1, idOpcion1),
                        new EntradaClaveBFA(null, idClave, idPregunta2, idOpcion2)));
    }

    private void aprobarClaveDirectamente() {

        jdbc.update(
                "UPDATE version_clave_bfa "
                        + "SET estado = 'APROBADA', "
                        + "evidencia_aprobacion = ? "
                        + "WHERE id_clave = ?",
                EVIDENCIA,
                idClave);
    }

    private void retirarClave() {

        jdbc.update(
                "UPDATE version_clave_bfa " + "SET estado = 'RETIRADA' " + "WHERE id_clave = ?",
                idClave);
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
                "SELECT evidencia_aprobacion " + "FROM version_clave_bfa " + "WHERE id_clave = ?",
                String.class,
                idClave);
    }

    private long contarEntradas() {

        Long cantidad =
                jdbc.queryForObject(
                        "SELECT COUNT(*) " + "FROM entrada_clave_bfa " + "WHERE id_clave = ?",
                        Long.class,
                        idClave);

        return cantidad == null ? -1L : cantidad;
    }
}
