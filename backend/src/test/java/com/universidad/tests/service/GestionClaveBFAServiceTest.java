package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.GestionClaveBFAService;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GestionClaveBFAServiceTest {

    @Mock private VersionClaveBFARepository claveRepository;

    @Mock private EntradaClaveBFARepository entradaRepository;

    @Mock private VersionFormularioRepository formularioRepository;

    @Mock private AutorizacionClaveBFAPort autorizacion;

    @InjectMocks private GestionClaveBFAService servicio;

    // =====================================================
    // PRUEBA 1: Rechazar creacion sin autorizacion
    // =====================================================

    @Test
    void debeRechazarCreacionDeClaveSinAutorizacion() {

        // Simular que el usuario no tiene permisos
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoCrearClave(5L);

        // Intentar crear una clave ficticia
        assertThrows(
                SecurityException.class,
                () -> servicio.crearClave(5L, "CLAVE-V1", "MATERIAL-SINTETICO"));

        // Comprobar que no se consultaron los repositorios
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBA 2: Rechazar registro sin autorizacion
    // =====================================================

    @Test
    void debeRechazarRegistroDeEntradaSinAutorizacion() {

        // Simular que el usuario no tiene permisos
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoAgregarEntrada(10L);

        // Intentar registrar una respuesta correcta ficticia
        assertThrows(SecurityException.class, () -> servicio.registrarEntrada(10L, 20L, 30L));

        // Comprobar que no se accedio a los repositorios
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBA 3: Rechazar consulta sin autorizacion
    // =====================================================

    @Test
    void debeRechazarConsultaDeClaveSinAutorizacion() {

        // Simular que el usuario no tiene permisos
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoConsultarClave(10L);

        // Intentar consultar una clave sin autorizacion
        assertThrows(SecurityException.class, () -> servicio.consultarClave(10L));

        // Comprobar que no se accedio a los repositorios
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBA 4: Crear clave con autorizacion
    // =====================================================

    @Test
    void debeCrearClaveEnBorradorConAutorizacion() {

        // Simular un formulario ficticio existente
        VersionFormulario formulario =
                new VersionFormulario(
                        5L, 2L, "FORM-V1", "Formulario sintetico", "FUENTE-SINTETICA");

        when(formularioRepository.buscarPorId(5L)).thenReturn(Optional.of(formulario));

        // Simular el almacenamiento de la nueva clave
        when(claveRepository.guardar(any(VersionClaveBFA.class)))
                .thenAnswer(
                        invocacion -> {
                            VersionClaveBFA clave = invocacion.getArgument(0);

                            return new VersionClaveBFA(
                                    100L,
                                    clave.getIdVersionFormulario(),
                                    clave.getVersion(),
                                    clave.getReferenciaFuente());
                        });

        // Crear una clave mediante el servicio
        // El mock de autorizacion permite la operacion
        VersionClaveBFA resultado = servicio.crearClave(5L, "CLAVE-V1", "MATERIAL-SINTETICO");

        // Verificar el identificador generado
        assertEquals(100L, resultado.getIdClave());

        // Verificar el formulario asociado
        assertEquals(5L, resultado.getIdVersionFormulario());

        // Verificar la version registrada
        assertEquals("CLAVE-V1", resultado.getVersion());

        // Verificar que la clave comienza en BORRADOR
        assertEquals(EstadoVersionClaveBFA.BORRADOR, resultado.getEstado());

        // Comprobar que se solicito autorizacion
        verify(autorizacion).exigirPermisoCrearClave(5L);

        // Comprobar que se consulto el formulario
        verify(formularioRepository).buscarPorId(5L);

        // Comprobar que se guardo la clave
        verify(claveRepository).guardar(any(VersionClaveBFA.class));
    }

    @Test
    void debeRegistrarEntradaConAutorizacion() {

        // Simular el almacenamiento de una entrada ficticia
        when(entradaRepository.guardar(any(EntradaClaveBFA.class)))
                .thenAnswer(
                        invocacion -> {
                            EntradaClaveBFA entrada = invocacion.getArgument(0);

                            return new EntradaClaveBFA(
                                    100L,
                                    entrada.getIdClave(),
                                    entrada.getIdPregunta(),
                                    entrada.getIdOpcionCorrecta());
                        });

        // Registrar una respuesta con autorizacion simulada
        EntradaClaveBFA resultado = servicio.registrarEntrada(10L, 20L, 30L);

        // Verificar que la entrada se registro correctamente
        assertEquals(100L, resultado.getIdEntradaClave());

        assertEquals(10L, resultado.getIdClave());

        assertEquals(20L, resultado.getIdPregunta());

        assertEquals(30L, resultado.getIdOpcionCorrecta());

        // Comprobar que se solicito autorizacion
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);

        // Comprobar que el servicio utilizo el repositorio
        verify(entradaRepository).guardar(any(EntradaClaveBFA.class));

        // Verificar que no se utilizaron otros repositorios
        verifyNoInteractions(claveRepository, formularioRepository);
    }

    @Test
    void debeRegistrarMultiplesEntradasConAutorizacion() {

        // Crear una clave ficticia en estado BORRADOR
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        // Preparar tres respuestas correctas ficticias
        EntradaClaveBFA entrada1 = new EntradaClaveBFA(null, 10L, 20L, 30L);

        EntradaClaveBFA entrada2 = new EntradaClaveBFA(null, 10L, 21L, 31L);

        EntradaClaveBFA entrada3 = new EntradaClaveBFA(null, 10L, 22L, 32L);

        List<EntradaClaveBFA> entradas = List.of(entrada1, entrada2, entrada3);

        // Simular que el repositorio guarda cada entrada
        when(entradaRepository.guardar(any(EntradaClaveBFA.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        // Ejecutar la carga multiple
        List<EntradaClaveBFA> resultado = servicio.registrarEntradas(10L, entradas);

        // Verificar que se procesaron las tres entradas
        assertEquals(3, resultado.size());

        // Verificar los identificadores de las preguntas
        assertEquals(20L, resultado.get(0).getIdPregunta());
        assertEquals(21L, resultado.get(1).getIdPregunta());
        assertEquals(22L, resultado.get(2).getIdPregunta());

        // Verificar las opciones correctas
        assertEquals(30L, resultado.get(0).getIdOpcionCorrecta());
        assertEquals(31L, resultado.get(1).getIdOpcionCorrecta());
        assertEquals(32L, resultado.get(2).getIdOpcionCorrecta());

        // Comprobar que se exigio autorizacion
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);

        // Comprobar que se consulto la clave
        verify(claveRepository).buscarPorId(10L);

        // Comprobar que se guardaron las tres entradas
        org.mockito.Mockito.verify(entradaRepository, org.mockito.Mockito.times(3))
                .guardar(any(EntradaClaveBFA.class));
    }

    @Test
    void debeRechazarCargaConPreguntasDuplicadas() {

        // Preparar dos entradas para la misma pregunta
        EntradaClaveBFA entrada1 = new EntradaClaveBFA(null, 10L, 20L, 30L);

        EntradaClaveBFA entrada2 = new EntradaClaveBFA(null, 10L, 20L, 31L);

        List<EntradaClaveBFA> entradas = List.of(entrada1, entrada2);

        // Intentar registrar entradas duplicadas
        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, entradas));

        // Comprobar el motivo del rechazo
        assertEquals("La carga contiene preguntas duplicadas", excepcion.getMessage());

        // Confirmar que se solicito autorizacion
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);

        // No debe consultar ni modificar los repositorios
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaConEntradasDeOtraClave() {

        // Entrada correspondiente a la clave 10
        EntradaClaveBFA entrada1 = new EntradaClaveBFA(null, 10L, 20L, 30L);

        // Entrada correspondiente a otra clave (99)
        EntradaClaveBFA entrada2 = new EntradaClaveBFA(null, 99L, 21L, 31L);

        List<EntradaClaveBFA> entradas = List.of(entrada1, entrada2);

        // Intentar cargar ambas entradas en la clave 10
        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, entradas));

        // Comprobar el motivo del rechazo
        assertEquals(
                "Todas las entradas deben pertenecer a la misma clave", excepcion.getMessage());

        // Confirmar que se solicito autorizacion
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);

        // No debe consultar ni modificar los repositorios
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaEnClaveAprobada() {

        // Simular una clave aprobada con evidencia ficticia
        VersionClaveBFA clave =
                VersionClaveBFA.restaurar(
                        10L,
                        5L,
                        "CLAVE-V1",
                        "MATERIAL-SINTETICO",
                        EstadoVersionClaveBFA.APROBADA,
                        "EVIDENCIA-SINTETICA");

        // Simular que la clave existe en el repositorio
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        // Preparar una entrada de correccion ficticia
        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);

        // Intentar cargar una entrada en una clave aprobada
        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.registrarEntradas(10L, java.util.List.of(entrada)));

        // Verificar el mensaje de rechazo
        assertEquals(
                "No se pueden agregar entradas a una clave aprobada o retirada",
                excepcion.getMessage());

        // Confirmar que se solicito autorizacion
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);

        // Comprobar que se consulto la clave
        verify(claveRepository).buscarPorId(10L);

        // Confirmar que no se guardaron respuestas
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBAS ADICIONALES: ESTADOS Y SEGURIDAD DE CARGA
    // =====================================================

    @Test
    void debeRechazarCargaEnClaveRetirada() {
        VersionClaveBFA clave =
                VersionClaveBFA.restaurar(
                        10L,
                        5L,
                        "CLAVE-V1",
                        "MATERIAL-SINTETICO",
                        EstadoVersionClaveBFA.RETIRADA,
                        "EVIDENCIA-SINTETICA");
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.registrarEntradas(10L, List.of(entrada)));

        assertEquals(
                "No se pueden agregar entradas a una clave aprobada o retirada",
                excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verify(claveRepository).buscarPorId(10L);
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleSinAutorizacion() {
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoAgregarEntrada(10L);

        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);
        assertThrows(
                SecurityException.class, () -> servicio.registrarEntradas(10L, List.of(entrada)));

        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleConIdentificadorDeClaveInvalido() {
        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarEntradas(null, List.of(entrada)));
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarEntradas(0L, List.of(entrada)));
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarEntradas(-10L, List.of(entrada)));

        verifyNoInteractions(
                autorizacion, claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleConListaNula() {
        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, null));

        assertEquals("La lista de entradas no puede estar vacia", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleConListaVacia() {
        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, List.of()));

        assertEquals("La lista de entradas no puede estar vacia", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleQueContengaEntradaNula() {
        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, Collections.singletonList(null)));

        assertEquals("La lista contiene una entrada nula", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleConEntradaQueYaTieneId() {
        EntradaClaveBFA entradaExistente = new EntradaClaveBFA(77L, 10L, 20L, 30L);

        IllegalArgumentException excepcion =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio.registrarEntradas(10L, List.of(entradaExistente)));

        assertEquals("Las entradas nuevas no deben tener identificador", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCargaMultipleSiLaClaveNoExiste() {
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.empty());
        EntradaClaveBFA entrada = new EntradaClaveBFA(null, 10L, 20L, 30L);

        NoSuchElementException excepcion =
                assertThrows(
                        NoSuchElementException.class,
                        () -> servicio.registrarEntradas(10L, List.of(entrada)));

        assertEquals("No existe la clave indicada", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verify(claveRepository).buscarPorId(10L);
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    @Test
    void debeDetenerCargaMultipleCuandoFallaUnGuardado() {
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        EntradaClaveBFA entrada1 = new EntradaClaveBFA(null, 10L, 20L, 30L);
        EntradaClaveBFA entrada2 = new EntradaClaveBFA(null, 10L, 21L, 31L);
        EntradaClaveBFA entrada3 = new EntradaClaveBFA(null, 10L, 22L, 32L);

        when(entradaRepository.guardar(any(EntradaClaveBFA.class)))
                .thenReturn(entrada1)
                .thenThrow(new IllegalStateException("Fallo simulado de persistencia"));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                servicio.registrarEntradas(
                                        10L, List.of(entrada1, entrada2, entrada3)));

        assertEquals("Fallo simulado de persistencia", excepcion.getMessage());
        verify(autorizacion).exigirPermisoAgregarEntrada(10L);
        verify(claveRepository).buscarPorId(10L);
        verify(entradaRepository, times(2)).guardar(any(EntradaClaveBFA.class));
        verifyNoInteractions(formularioRepository);
        // Un mock no demuestra rollback en PostgreSQL: eso requiere una prueba de integracion.
    }

    // =====================================================
    // PRUEBAS ADICIONALES: CREACION DE CLAVES
    // =====================================================

    @Test
    void debeRechazarCreacionConIdDeFormularioInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearClave(null, "CLAVE-V1", "MATERIAL-SINTETICO"));
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearClave(0L, "CLAVE-V1", "MATERIAL-SINTETICO"));
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearClave(-1L, "CLAVE-V1", "MATERIAL-SINTETICO"));

        verifyNoInteractions(
                autorizacion, claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarCreacionSiElFormularioNoExiste() {
        when(formularioRepository.buscarPorId(5L)).thenReturn(Optional.empty());

        NoSuchElementException excepcion =
                assertThrows(
                        NoSuchElementException.class,
                        () -> servicio.crearClave(5L, "CLAVE-V1", "MATERIAL-SINTETICO"));

        assertEquals("No existe la version de formulario indicada", excepcion.getMessage());
        verify(autorizacion).exigirPermisoCrearClave(5L);
        verify(formularioRepository).buscarPorId(5L);
        verifyNoInteractions(claveRepository, entradaRepository);
    }

    @Test
    void debeRechazarCreacionConVersionVacia() {
        VersionFormulario formulario =
                new VersionFormulario(
                        5L, 2L, "FORM-V1", "Formulario sintetico", "FUENTE-SINTETICA");
        when(formularioRepository.buscarPorId(5L)).thenReturn(Optional.of(formulario));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.crearClave(5L, "   ", "MATERIAL-SINTETICO"));

        verify(autorizacion).exigirPermisoCrearClave(5L);
        verify(formularioRepository).buscarPorId(5L);
        verifyNoInteractions(claveRepository, entradaRepository);
    }

    @Test
    void debeRechazarCreacionConReferenciaFuenteVacia() {
        VersionFormulario formulario =
                new VersionFormulario(
                        5L, 2L, "FORM-V1", "Formulario sintetico", "FUENTE-SINTETICA");
        when(formularioRepository.buscarPorId(5L)).thenReturn(Optional.of(formulario));

        assertThrows(
                IllegalArgumentException.class, () -> servicio.crearClave(5L, "CLAVE-V1", "  "));

        verify(autorizacion).exigirPermisoCrearClave(5L);
        verify(formularioRepository).buscarPorId(5L);
        verifyNoInteractions(claveRepository, entradaRepository);
    }

    // =====================================================
    // PRUEBAS ADICIONALES: REGISTRO INDIVIDUAL
    // =====================================================

    @Test
    void debeRechazarRegistroIndividualConIdentificadoresInvalidos() {
        assertThrows(
                IllegalArgumentException.class, () -> servicio.registrarEntrada(null, 20L, 30L));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEntrada(0L, 20L, 30L));
        assertThrows(
                IllegalArgumentException.class, () -> servicio.registrarEntrada(10L, null, 30L));
        assertThrows(
                IllegalArgumentException.class, () -> servicio.registrarEntrada(10L, -20L, 30L));
        assertThrows(
                IllegalArgumentException.class, () -> servicio.registrarEntrada(10L, 20L, null));
        assertThrows(IllegalArgumentException.class, () -> servicio.registrarEntrada(10L, 20L, 0L));

        verifyNoInteractions(
                autorizacion, claveRepository, entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBAS ADICIONALES: CONSULTAS DE CLAVES
    // =====================================================

    @Test
    void debeConsultarClaveConAutorizacion() {
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        VersionClaveBFA resultado = servicio.consultarClave(10L);

        assertSame(clave, resultado);
        verify(autorizacion).exigirPermisoConsultarClave(10L);
        verify(claveRepository).buscarPorId(10L);
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarConsultaDeClaveInexistente() {
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.empty());

        NoSuchElementException excepcion =
                assertThrows(NoSuchElementException.class, () -> servicio.consultarClave(10L));

        assertEquals("No existe la clave indicada", excepcion.getMessage());
        verify(autorizacion).exigirPermisoConsultarClave(10L);
        verify(claveRepository).buscarPorId(10L);
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarConsultaDeClaveConIdentificadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarClave(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarClave(0L));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarClave(-1L));

        verifyNoInteractions(
                autorizacion, claveRepository, entradaRepository, formularioRepository);
    }

    // =====================================================
    // PRUEBAS ADICIONALES: CONSULTA DE ENTRADAS
    // =====================================================

    @Test
    void debeConsultarEntradasConAutorizacion() {
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));
        List<EntradaClaveBFA> entradas =
                List.of(
                        new EntradaClaveBFA(100L, 10L, 20L, 30L),
                        new EntradaClaveBFA(101L, 10L, 21L, 31L));
        when(entradaRepository.listarPorClave(10L)).thenReturn(entradas);

        List<EntradaClaveBFA> resultado = servicio.consultarEntradas(10L);

        assertSame(entradas, resultado);
        assertEquals(2, resultado.size());
        verify(autorizacion).exigirPermisoConsultarClave(10L);
        verify(claveRepository).buscarPorId(10L);
        verify(entradaRepository).listarPorClave(10L);
        verifyNoInteractions(formularioRepository);
    }

    @Test
    void debeConsultarListaVaciaSiLaClaveNoTieneEntradas() {
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));
        when(entradaRepository.listarPorClave(10L)).thenReturn(List.of());

        assertEquals(List.of(), servicio.consultarEntradas(10L));
        verify(autorizacion).exigirPermisoConsultarClave(10L);
        verify(claveRepository).buscarPorId(10L);
        verify(entradaRepository).listarPorClave(10L);
        verifyNoInteractions(formularioRepository);
    }

    @Test
    void debeRechazarConsultaDeEntradasSinAutorizacion() {
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoConsultarClave(10L);

        assertThrows(SecurityException.class, () -> servicio.consultarEntradas(10L));

        verifyNoInteractions(claveRepository, entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarConsultaDeEntradasSiNoExisteLaClave() {
        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.empty());

        NoSuchElementException excepcion =
                assertThrows(NoSuchElementException.class, () -> servicio.consultarEntradas(10L));

        assertEquals("No existe la clave indicada", excepcion.getMessage());
        verify(autorizacion).exigirPermisoConsultarClave(10L);
        verify(claveRepository).buscarPorId(10L);
        verifyNoInteractions(entradaRepository, formularioRepository);
    }

    @Test
    void debeRechazarConsultaDeEntradasConIdentificadorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarEntradas(null));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarEntradas(0L));
        assertThrows(IllegalArgumentException.class, () -> servicio.consultarEntradas(-1L));

        verifyNoInteractions(
                autorizacion, claveRepository, entradaRepository, formularioRepository);
    }
}
