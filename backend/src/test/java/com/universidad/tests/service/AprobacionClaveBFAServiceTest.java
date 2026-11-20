package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.AprobacionClaveBFAService;
import com.universidad.tests.application.service.ValidacionCoberturaClaveBFAService;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AprobacionClaveBFAServiceTest {

    @Mock private VersionClaveBFARepository claveRepository;

    @Mock private ValidacionCoberturaClaveBFAService validadorCobertura;

    @Mock private AutorizacionClaveBFAPort autorizacion;

    @InjectMocks private AprobacionClaveBFAService servicio;

    // ================================================
    // DATOS SINTETICOS
    // ================================================

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
                "EVIDENCIA-SINTETICA");
    }

    private VersionClaveBFA claveRetirada() {
        return VersionClaveBFA.restaurar(
                10L,
                5L,
                "CLAVE-V1",
                "MATERIAL-SINTETICO",
                EstadoVersionClaveBFA.RETIRADA,
                "EVIDENCIA-SINTETICA");
    }

    // ================================================
    // 1. APROBACION CORRECTA
    // ================================================

    @Test
    void debeAprobarClaveConCoberturaCompleta() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveBorrador()));

        VersionClaveBFA aprobada = claveAprobada();

        when(claveRepository.aprobar(10L, "EVIDENCIA-SINTETICA")).thenReturn(aprobada);

        VersionClaveBFA resultado = servicio.aprobarClave(10L, "EVIDENCIA-SINTETICA");

        assertSame(aprobada, resultado);

        assertEquals(EstadoVersionClaveBFA.APROBADA, resultado.getEstado());

        // Verificar que se valida la cobertura
        // antes de persistir la aprobacion.
        InOrder orden = inOrder(autorizacion, claveRepository, validadorCobertura);

        orden.verify(autorizacion).exigirPermisoAprobarClave(10L);

        orden.verify(claveRepository).buscarPorId(10L);

        orden.verify(validadorCobertura).validarCoberturaSeleccion(10L);

        orden.verify(claveRepository).aprobar(10L, "EVIDENCIA-SINTETICA");
    }

    // ================================================
    // 2. APROBACION SIN PERMISOS
    // ================================================

    @Test
    void debeRechazarAprobacionSinAutorizacion() {

        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoAprobarClave(10L);

        assertThrows(
                SecurityException.class, () -> servicio.aprobarClave(10L, "EVIDENCIA-SINTETICA"));

        verifyNoInteractions(claveRepository, validadorCobertura);
    }

    // ================================================
    // 3. APROBACION SIN EVIDENCIA
    // ================================================

    @Test
    void debeRechazarAprobacionSinEvidencia() {

        assertThrows(IllegalArgumentException.class, () -> servicio.aprobarClave(10L, null));

        assertThrows(IllegalArgumentException.class, () -> servicio.aprobarClave(10L, "   "));

        verify(autorizacion, times(2)).exigirPermisoAprobarClave(10L);

        verifyNoInteractions(claveRepository, validadorCobertura);
    }

    // ================================================
    // 4. IDENTIFICADORES INVALIDOS
    // ================================================

    @Test
    void debeRechazarAprobacionConIdInvalido() {

        assertThrows(
                IllegalArgumentException.class, () -> servicio.aprobarClave(null, "EVIDENCIA"));

        assertThrows(IllegalArgumentException.class, () -> servicio.aprobarClave(0L, "EVIDENCIA"));

        assertThrows(IllegalArgumentException.class, () -> servicio.aprobarClave(-1L, "EVIDENCIA"));

        verifyNoInteractions(autorizacion, claveRepository, validadorCobertura);
    }

    // ================================================
    // 5. CLAVE INEXISTENTE
    // ================================================

    @Test
    void debeRechazarAprobacionDeClaveInexistente() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> servicio.aprobarClave(10L, "EVIDENCIA-SINTETICA"));

        verifyNoInteractions(validadorCobertura);

        verify(claveRepository, never()).aprobar(10L, "EVIDENCIA-SINTETICA");
    }

    // ================================================
    // 6. APROBACION DUPLICADA
    // ================================================

    @Test
    void debeRechazarClaveYaAprobada() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveAprobada()));

        assertThrows(
                IllegalStateException.class, () -> servicio.aprobarClave(10L, "OTRA-EVIDENCIA"));

        verifyNoInteractions(validadorCobertura);

        verify(claveRepository, never()).aprobar(10L, "OTRA-EVIDENCIA");
    }

    // ================================================
    // 7. APROBACION DE CLAVE RETIRADA
    // ================================================

    @Test
    void debeRechazarAprobacionDeClaveRetirada() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveRetirada()));

        assertThrows(
                IllegalStateException.class, () -> servicio.aprobarClave(10L, "NUEVA-EVIDENCIA"));

        verifyNoInteractions(validadorCobertura);

        verify(claveRepository, never()).aprobar(10L, "NUEVA-EVIDENCIA");
    }

    // ================================================
    // 8. COBERTURA INCOMPLETA
    // ================================================

    @Test
    void debeRechazarClaveConCoberturaIncompleta() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveBorrador()));

        doThrow(
                        new IllegalStateException(
                                "La clave no cubre exactamente todas "
                                        + "las preguntas de seleccion del formulario"))
                .when(validadorCobertura)
                .validarCoberturaSeleccion(10L);

        assertThrows(
                IllegalStateException.class,
                () -> servicio.aprobarClave(10L, "EVIDENCIA-SINTETICA"));

        verify(claveRepository, never()).aprobar(10L, "EVIDENCIA-SINTETICA");
    }

    // ================================================
    // 9. CONSULTA DE COBERTURA NO AUTORIZADA
    // ================================================

    @Test
    void debeRechazarAprobacionSiFallaPermisoDeConsulta() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveBorrador()));

        doThrow(new SecurityException("Consulta de clave no autorizada"))
                .when(validadorCobertura)
                .validarCoberturaSeleccion(10L);

        assertThrows(
                SecurityException.class, () -> servicio.aprobarClave(10L, "EVIDENCIA-SINTETICA"));

        verify(claveRepository, never()).aprobar(10L, "EVIDENCIA-SINTETICA");
    }

    // ================================================
    // 10. RETIRO CORRECTO
    // ================================================

    @Test
    void debeRetirarClaveAprobada() {

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(claveAprobada()));

        VersionClaveBFA retirada = claveRetirada();

        when(claveRepository.retirar(10L)).thenReturn(retirada);

        VersionClaveBFA resultado = servicio.retirarClave(10L);

        assertSame(retirada, resultado);

        assertEquals(EstadoVersionClaveBFA.RETIRADA, resultado.getEstado());

        assertEquals("EVIDENCIA-SINTETICA", resultado.getEvidenciaAprobacion());

        verify(autorizacion).exigirPermisoRetirarClave(10L);

        verify(claveRepository).retirar(10L);

        verifyNoInteractions(validadorCobertura);
    }

    // ================================================
    // 11. RETIRO SIN AUTORIZACION
    // ================================================

    @Test
    void debeRechazarRetiroSinAutorizacion() {

        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoRetirarClave(10L);

        assertThrows(SecurityException.class, () -> servicio.retirarClave(10L));

        verifyNoInteractions(claveRepository, validadorCobertura);
    }

    // ================================================
    // 12. RETIRO EN ESTADO NO PERMITIDO
    // ================================================

    @Test
    void debeRechazarRetiroDeClaveNoAprobada() {

        when(claveRepository.buscarPorId(10L))
                .thenReturn(Optional.of(claveBorrador()), Optional.of(claveRetirada()));

        // No puede retirarse una clave en BORRADOR.
        assertThrows(IllegalStateException.class, () -> servicio.retirarClave(10L));

        // No puede retirarse dos veces.
        assertThrows(IllegalStateException.class, () -> servicio.retirarClave(10L));

        verify(claveRepository, never()).retirar(10L);

        verifyNoInteractions(validadorCobertura);
    }

    // ================================================
    // 13. RETIRO CON IDENTIFICADOR INVALIDO
    // ================================================

    @Test
    void debeRechazarRetiroConIdInvalido() {

        assertThrows(IllegalArgumentException.class, () -> servicio.retirarClave(null));

        assertThrows(IllegalArgumentException.class, () -> servicio.retirarClave(0L));

        assertThrows(IllegalArgumentException.class, () -> servicio.retirarClave(-1L));

        verifyNoInteractions(autorizacion, claveRepository, validadorCobertura);
    }
}
