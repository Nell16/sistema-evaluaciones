package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.ValidacionClaveBFAParaCalificacionService;
import com.universidad.tests.application.service.ValidacionCoberturaClaveBFAService;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidacionClaveBFAParaCalificacionServiceTest {

    @Mock private VersionClaveBFARepository claveRepository;

    @Mock private EntradaClaveBFARepository entradaRepository;

    @Mock private ValidacionCoberturaClaveBFAService validadorCobertura;

    @Mock private AutorizacionClaveBFAPort autorizacion;

    @InjectMocks private ValidacionClaveBFAParaCalificacionService servicio;

    // =====================================================
    // DATOS SINTETICOS PARA LAS PRUEBAS
    // =====================================================

    private VersionClaveBFA claveBorrador() {

        return new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");
    }

    private VersionClaveBFA claveAprobada() {

        return VersionClaveBFA.restaurar(
                10L,
                5L,
                "CLAVE-V1",
                "MATERIAL-SINTETICO",
                EstadoVersionClaveBFA.APROBADA,
                "EVIDENCIA-OFICIAL-SINTETICA");
    }

    private VersionClaveBFA claveRetirada() {

        return VersionClaveBFA.restaurar(
                10L,
                5L,
                "CLAVE-V1",
                "MATERIAL-SINTETICO",
                EstadoVersionClaveBFA.RETIRADA,
                "EVIDENCIA-OFICIAL-SINTETICA");
    }

    private List<EntradaClaveBFA> entradasSinteticas() {

        return List.of(
                new EntradaClaveBFA(1L, 10L, 101L, 405L), new EntradaClaveBFA(2L, 10L, 102L, 409L));
    }

    // =====================================================
    // 1. PERMITIR CLAVE APROBADA
    // =====================================================

    @Test
    void debePermitirClaveAprobadaDelFormularioCorrecto() {

        VersionClaveBFA clave = claveAprobada();

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        VersionClaveBFA resultado = servicio.obtenerClaveAprobada(10L, 5L);

        assertSame(clave, resultado);

        assertEquals(EstadoVersionClaveBFA.APROBADA, resultado.getEstado());

        verify(autorizacion).exigirPermisoCalificarConClave(10L);

        verify(claveRepository).buscarPorId(10L);
    }

    // =====================================================
    // 2. RECHAZAR CLAVE EN BORRADOR
    // =====================================================

    @Test
    void debeRechazarClaveEnBorrador() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveBorrador()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class, () -> servicio.obtenerClaveAprobada(10L, 5L));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        verify(claveRepository).buscarPorId(10L);
    }

    // =====================================================
    // 3. RECHAZAR CLAVE RETIRADA
    // =====================================================

    @Test
    void debeRechazarClaveRetirada() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveRetirada()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class, () -> servicio.obtenerClaveAprobada(10L, 5L));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        verify(claveRepository).buscarPorId(10L);
    }

    // =====================================================
    // 4. RECHAZAR CLAVE DE OTRO FORMULARIO
    // =====================================================

    @Test
    void debeRechazarClaveDeFormularioDiferente() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveAprobada()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class, () -> servicio.obtenerClaveAprobada(10L, 8L));

        assertTrue(
                excepcion
                        .getMessage()
                        .contains("no pertenece a la version " + "de formulario indicada"));

        verify(claveRepository).buscarPorId(10L);
    }

    // =====================================================
    // 5. RECHAZAR CLAVE INEXISTENTE
    // =====================================================

    @Test
    void debeRechazarClaveInexistente() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.empty());

        NoSuchElementException excepcion =
                assertThrows(
                        NoSuchElementException.class, () -> servicio.obtenerClaveAprobada(10L, 5L));

        assertEquals("No existe la clave BFA indicada", excepcion.getMessage());

        verify(claveRepository).buscarPorId(10L);
    }

    // =====================================================
    // 6. RECHAZAR ID DE CLAVE INVALIDO
    // =====================================================

    @Test
    void debeRechazarIdentificadorDeClaveInvalido() {

        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(null, 5L));

        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(0L, 5L));

        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(-1L, 5L));

        verifyNoInteractions(claveRepository);
        verifyNoInteractions(autorizacion);
    }

    // =====================================================
    // 7. RECHAZAR ID DE FORMULARIO INVALIDO
    // =====================================================

    @Test
    void debeRechazarIdentificadorDeFormularioInvalido() {

        assertThrows(
                IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(10L, null));

        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(10L, 0L));

        assertThrows(IllegalArgumentException.class, () -> servicio.obtenerClaveAprobada(10L, -1L));

        verifyNoInteractions(claveRepository);
        verifyNoInteractions(autorizacion);
    }

    // =====================================================
    // 8. RECHAZAR ACCESO SIN AUTORIZACION
    // =====================================================

    @Test
    void debeRechazarAccesoSinPermisoAntesDeConsultarClave() {

        doThrow(new SecurityException("Operacion no autorizada: permiso BFA denegado"))
                .when(autorizacion)
                .exigirPermisoCalificarConClave(10L);

        SecurityException excepcion =
                assertThrows(SecurityException.class, () -> servicio.obtenerClaveAprobada(10L, 5L));

        assertTrue(excepcion.getMessage().contains("permiso BFA denegado"));

        verify(autorizacion).exigirPermisoCalificarConClave(10L);

        verifyNoInteractions(claveRepository);
    }

    // =====================================================
    // 9. ENTREGAR RESPUESTAS CORRECTAS AUTORIZADAS
    // =====================================================

    @Test
    void debeEntregarEntradasDeClaveAprobadaYCompleta() {

        VersionClaveBFA clave = claveAprobada();
        List<EntradaClaveBFA> esperadas = entradasSinteticas();

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        when(entradaRepository.listarPorClave(10L)).thenReturn(esperadas);

        List<EntradaClaveBFA> resultado =
                servicio.obtenerEntradasAprobadasParaCalificacion(10L, 5L);

        assertEquals(2, resultado.size());

        assertSame(esperadas.get(0), resultado.get(0));
        assertSame(esperadas.get(1), resultado.get(1));

        assertEquals(101L, resultado.get(0).getIdPregunta());

        assertEquals(405L, resultado.get(0).getIdOpcionCorrecta());

        assertEquals(102L, resultado.get(1).getIdPregunta());

        assertEquals(409L, resultado.get(1).getIdOpcionCorrecta());

        // La lista no puede modificarse.
        assertThrows(UnsupportedOperationException.class, () -> resultado.clear());

        verify(autorizacion).exigirPermisoCalificarConClave(10L);

        verify(claveRepository).buscarPorId(10L);

        // CORRECCION:
        // Utilizar el metodo exclusivo de calificacion.
        verify(validadorCobertura).validarCoberturaSeleccionParaCalificacion(10L);

        verify(entradaRepository).listarPorClave(10L);
    }

    // =====================================================
    // 10. RECHAZAR CLAVE CON COBERTURA INCOMPLETA
    // =====================================================

    @Test
    void debeRechazarClaveIncompletaAntesDeEntregarEntradas() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveAprobada()));

        // CORRECCION:
        // Simular la excepcion en el metodo que realmente
        // utiliza el servicio de calificacion.
        doThrow(
                        new IllegalStateException(
                                "La clave no cubre exactamente todas "
                                        + "las preguntas de seleccion del formulario"))
                .when(validadorCobertura)
                .validarCoberturaSeleccionParaCalificacion(10L);

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 5L));

        assertTrue(excepcion.getMessage().contains("La clave no cubre exactamente"));

        verify(autorizacion).exigirPermisoCalificarConClave(10L);

        // CORRECCION:
        // Verificar el metodo correcto.
        verify(validadorCobertura).validarCoberturaSeleccionParaCalificacion(10L);

        // No deben entregarse respuestas.
        verifyNoInteractions(entradaRepository);
    }

    // =====================================================
    // 11. IMPEDIR ENTREGA SIN AUTORIZACION
    // =====================================================

    @Test
    void debeRechazarConsultaDeEntradasSinPermiso() {

        doThrow(new SecurityException("Operacion no autorizada: permiso BFA denegado"))
                .when(autorizacion)
                .exigirPermisoCalificarConClave(10L);

        SecurityException excepcion =
                assertThrows(
                        SecurityException.class,
                        () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 5L));

        assertTrue(excepcion.getMessage().contains("permiso BFA denegado"));

        verify(autorizacion).exigirPermisoCalificarConClave(10L);

        verifyNoInteractions(claveRepository);
        verifyNoInteractions(entradaRepository);
        verifyNoInteractions(validadorCobertura);
    }

    // =====================================================
    // 12. IMPEDIR ENTREGA DE CLAVE EN BORRADOR
    // =====================================================

    @Test
    void debeRechazarEntradasDeClaveEnBorrador() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveBorrador()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 5L));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        verifyNoInteractions(validadorCobertura);
        verifyNoInteractions(entradaRepository);
    }

    // =====================================================
    // 13. IMPEDIR ENTREGA DE CLAVE RETIRADA
    // =====================================================

    @Test
    void debeRechazarEntradasDeClaveRetirada() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveRetirada()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 5L));

        assertTrue(excepcion.getMessage().contains("Solo una clave APROBADA"));

        verifyNoInteractions(validadorCobertura);
        verifyNoInteractions(entradaRepository);
    }

    // =====================================================
    // 14. IMPEDIR ENTREGA DE OTRO FORMULARIO
    // =====================================================

    @Test
    void debeRechazarEntradasDeFormularioIncompatible() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveAprobada()));

        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class,
                        () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 8L));

        assertTrue(excepcion.getMessage().contains("no pertenece a la version"));

        verifyNoInteractions(validadorCobertura);
        verifyNoInteractions(entradaRepository);
    }

    // =====================================================
    // 15. VALIDAR IDENTIFICADORES ANTES DE CONSULTAR
    // =====================================================

    @Test
    void debeRechazarIdentificadoresInvalidosAlSolicitarEntradas() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(null, 5L));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(0L, 5L));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(-1L, 5L));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, null));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, 0L));

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.obtenerEntradasAprobadasParaCalificacion(10L, -1L));

        verifyNoInteractions(claveRepository);
        verifyNoInteractions(entradaRepository);
        verifyNoInteractions(validadorCobertura);
        verifyNoInteractions(autorizacion);
    }
}
