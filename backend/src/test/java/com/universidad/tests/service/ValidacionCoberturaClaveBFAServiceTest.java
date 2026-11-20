package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.ValidacionCoberturaClaveBFAService;
import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.port.AutorizacionClaveBFAPort;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ValidacionCoberturaClaveBFAServiceTest {

    @Mock private VersionClaveBFARepository claveRepository;

    @Mock private EntradaClaveBFARepository entradaRepository;

    @Mock private ConfiguracionSubpruebaRepository configuracionRepository;

    @Mock private PreguntaRepository preguntaRepository;

    @Mock private AutorizacionClaveBFAPort autorizacion;

    @InjectMocks private ValidacionCoberturaClaveBFAService servicio;

    @Test
    void debeAceptarClaveConCoberturaCompleta() {

        // Crear una clave ficticia
        VersionClaveBFA clave = new VersionClaveBFA(10L, 5L, "CLAVE-V1", "MATERIAL-SINTETICO");

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        // Simular una subprueba de seleccion
        ConfiguracionSubprueba configuracion =
                org.mockito.Mockito.mock(ConfiguracionSubprueba.class);

        when(configuracion.getTipoRespuesta()).thenReturn(TipoRespuesta.SELECCION);

        when(configuracion.getIdConfiguracionSubprueba()).thenReturn(7L);

        when(configuracionRepository.listarPorVersion(5L)).thenReturn(List.of(configuracion));

        // Simular dos preguntas
        Pregunta pregunta1 = org.mockito.Mockito.mock(Pregunta.class);

        Pregunta pregunta2 = org.mockito.Mockito.mock(Pregunta.class);

        when(pregunta1.getIdPregunta()).thenReturn(20L);
        when(pregunta2.getIdPregunta()).thenReturn(21L);

        when(preguntaRepository.listarPorConfiguracion(7L))
                .thenReturn(List.of(pregunta1, pregunta2));

        // Crear una entrada para cada pregunta
        EntradaClaveBFA entrada1 = new EntradaClaveBFA(100L, 10L, 20L, 30L);

        EntradaClaveBFA entrada2 = new EntradaClaveBFA(101L, 10L, 21L, 31L);

        when(entradaRepository.listarPorClave(10L)).thenReturn(List.of(entrada1, entrada2));

        // La clave tiene cobertura completa
        assertDoesNotThrow(() -> servicio.validarCoberturaSeleccion(10L));

        // Confirmar que se exigio autorizacion
        verify(autorizacion).exigirPermisoConsultarClave(10L);
    }

    @Test
    void debeRechazarClaveConCoberturaIncompleta() {

        // Crear una clave ficticia
        VersionClaveBFA clave =
                new VersionClaveBFA(10L, 5L, "CLAVE-INCOMPLETA", "MATERIAL-SINTETICO");

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        // Simular una subprueba de seleccion
        ConfiguracionSubprueba configuracion =
                org.mockito.Mockito.mock(ConfiguracionSubprueba.class);

        when(configuracion.getTipoRespuesta()).thenReturn(TipoRespuesta.SELECCION);

        when(configuracion.getIdConfiguracionSubprueba()).thenReturn(7L);

        when(configuracionRepository.listarPorVersion(5L)).thenReturn(List.of(configuracion));

        // El formulario contiene dos preguntas
        Pregunta pregunta1 = org.mockito.Mockito.mock(Pregunta.class);

        Pregunta pregunta2 = org.mockito.Mockito.mock(Pregunta.class);

        when(pregunta1.getIdPregunta()).thenReturn(20L);
        when(pregunta2.getIdPregunta()).thenReturn(21L);

        when(preguntaRepository.listarPorConfiguracion(7L))
                .thenReturn(List.of(pregunta1, pregunta2));

        // Solo existe una entrada de correccion
        EntradaClaveBFA entrada1 = new EntradaClaveBFA(100L, 10L, 20L, 30L);

        when(entradaRepository.listarPorClave(10L)).thenReturn(List.of(entrada1));

        // La clave debe ser rechazada por estar incompleta
        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class, () -> servicio.validarCoberturaSeleccion(10L));

        org.junit.jupiter.api.Assertions.assertTrue(
                excepcion.getMessage().contains("La clave no cubre exactamente todas"));

        // Confirmar que se solicito autorizacion
        verify(autorizacion).exigirPermisoConsultarClave(10L);
    }

    @Test
    void debeRechazarClaveConPreguntaAdicional() {

        // Crear una clave ficticia
        VersionClaveBFA clave =
                new VersionClaveBFA(10L, 5L, "CLAVE-ADICIONAL", "MATERIAL-SINTETICO");

        when(claveRepository.buscarPorId(10L)).thenReturn(Optional.of(clave));

        // Simular una subprueba de seleccion
        ConfiguracionSubprueba configuracion =
                org.mockito.Mockito.mock(ConfiguracionSubprueba.class);

        when(configuracion.getTipoRespuesta()).thenReturn(TipoRespuesta.SELECCION);

        when(configuracion.getIdConfiguracionSubprueba()).thenReturn(7L);

        when(configuracionRepository.listarPorVersion(5L)).thenReturn(List.of(configuracion));

        // El formulario contiene dos preguntas
        Pregunta pregunta1 = org.mockito.Mockito.mock(Pregunta.class);

        Pregunta pregunta2 = org.mockito.Mockito.mock(Pregunta.class);

        when(pregunta1.getIdPregunta()).thenReturn(20L);
        when(pregunta2.getIdPregunta()).thenReturn(21L);

        when(preguntaRepository.listarPorConfiguracion(7L))
                .thenReturn(List.of(pregunta1, pregunta2));

        // Una entrada valida para la pregunta 20
        EntradaClaveBFA entradaValida = new EntradaClaveBFA(100L, 10L, 20L, 30L);

        // Una entrada adicional para la pregunta 99
        EntradaClaveBFA entradaAdicional = new EntradaClaveBFA(101L, 10L, 99L, 31L);

        when(entradaRepository.listarPorClave(10L))
                .thenReturn(List.of(entradaValida, entradaAdicional));

        // La clave debe ser rechazada
        IllegalStateException excepcion =
                assertThrows(
                        IllegalStateException.class, () -> servicio.validarCoberturaSeleccion(10L));

        org.junit.jupiter.api.Assertions.assertTrue(
                excepcion.getMessage().contains("La clave no cubre exactamente todas"));

        // Confirmar que se solicito autorizacion
        verify(autorizacion).exigirPermisoConsultarClave(10L);
    }

    @Test
    void debeRechazarValidacionCoberturaSinAutorizacion() {

        // Simular que el usuario no tiene permisos
        doThrow(new SecurityException("Acceso denegado"))
                .when(autorizacion)
                .exigirPermisoConsultarClave(10L);

        // Intentar validar una clave sin autorizacion
        assertThrows(SecurityException.class, () -> servicio.validarCoberturaSeleccion(10L));

        // Verificar que se solicito autorizacion
        verify(autorizacion).exigirPermisoConsultarClave(10L);

        // Confirmar que no se consultaron los repositorios
        verifyNoInteractions(
                claveRepository, entradaRepository, configuracionRepository, preguntaRepository);
    }
}
