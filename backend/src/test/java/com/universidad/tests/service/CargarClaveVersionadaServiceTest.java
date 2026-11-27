package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.CargarClaveVersionadaService;
import com.universidad.tests.application.service.CargarClaveVersionadaService.DatoClave;
import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.OpcionRespuesta;
import com.universidad.tests.domain.model.Pregunta;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.port.AutorizacionContenidoPort;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CargarClaveVersionadaServiceTest {
    @Mock private VersionFormularioRepository versiones;
    @Mock private ConfiguracionSubpruebaRepository configuraciones;
    @Mock private PreguntaRepository preguntas;
    @Mock private OpcionRespuestaRepository.Edicion opciones;
    @Mock private VersionClaveBFARepository claves;
    @Mock private EntradaClaveBFARepository entradas;
    @Mock private AutorizacionContenidoPort autorizacion;

    private CargarClaveVersionadaService servicio;
    private VersionFormulario version;

    @BeforeEach
    void preparar() {
        servicio =
                new CargarClaveVersionadaService(
                        versiones,
                        configuraciones,
                        preguntas,
                        opciones,
                        claves,
                        entradas,
                        autorizacion);
        version = new VersionFormulario(5L, 1L, "V1", null, "FUENTE-SINTETICA");
    }

    @Test
    void cargaClaveCompletaYExcluyeTextoLibre() {
        configurarContenido();
        when(opciones.buscarPorCodigo(10L, "B"))
                .thenReturn(Optional.of(new OpcionRespuesta(20L, 10L, "B", 2, "Respuesta", null)));
        when(claves.buscarPorFormularioYVersion(5L, "V1")).thenReturn(Optional.empty());
        when(claves.guardar(any(VersionClaveBFA.class)))
                .thenReturn(new VersionClaveBFA(30L, 5L, "V1", "FUENTE-SINTETICA"));

        assertEquals(30L, servicio.cargar(5L, List.of(new DatoClave(10L, "B"))));

        ArgumentCaptor<List<EntradaClaveBFA>> captor = ArgumentCaptor.forClass(List.class);
        verify(entradas).reemplazarEntradasBorrador(eq(30L), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertEquals(10L, captor.getValue().get(0).getIdPregunta());
        assertEquals(20L, captor.getValue().get(0).getIdOpcionCorrecta());
    }

    @Test
    void reemplazaClaveExistenteConElMismoIdentificador() {
        configurarContenido();
        when(opciones.buscarPorCodigo(10L, "B"))
                .thenReturn(Optional.of(new OpcionRespuesta(20L, 10L, "B", 2, "Respuesta", null)));
        when(claves.buscarPorFormularioYVersion(5L, "V1"))
                .thenReturn(Optional.of(new VersionClaveBFA(30L, 5L, "V1", "FUENTE-SINTETICA")));

        assertEquals(30L, servicio.cargar(5L, List.of(new DatoClave(10L, "B"))));
        verify(claves, never()).guardar(any());
        verify(entradas).reemplazarEntradasBorrador(eq(30L), any());
    }

    @Test
    void rechazaPreguntaAjenaYOpcionAjena() {
        configurarContenido();
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.cargar(5L, List.of(new DatoClave(99L, "B"))));
        when(opciones.buscarPorCodigo(10L, "B")).thenReturn(Optional.empty());
        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.cargar(5L, List.of(new DatoClave(10L, "B"))));
        verify(entradas, never()).reemplazarEntradasBorrador(any(), any());
    }

    @Test
    void rechazaClaveIncompletaAntesDePersistir() {
        configurarContenido();
        when(preguntas.listarPorConfiguracion(7L))
                .thenReturn(
                        List.of(
                                new Pregunta(10L, 7L, "1", 1, "Reactivo 1", null),
                                new Pregunta(11L, 7L, "2", 2, "Reactivo 2", null)));
        when(opciones.buscarPorCodigo(10L, "B"))
                .thenReturn(Optional.of(new OpcionRespuesta(20L, 10L, "B", 2, "Respuesta", null)));

        assertEquals(
                "CLAVE_INCOMPLETA",
                assertThrows(
                                IllegalArgumentException.class,
                                () -> servicio.cargar(5L, List.of(new DatoClave(10L, "B"))))
                        .getMessage());
        verify(claves, never()).guardar(any());
    }

    @Test
    void rechazaReemplazoDeClaveAprobada() {
        configurarContenido();
        when(opciones.buscarPorCodigo(10L, "B"))
                .thenReturn(Optional.of(new OpcionRespuesta(20L, 10L, "B", 2, "Respuesta", null)));
        VersionClaveBFA aprobada = new VersionClaveBFA(30L, 5L, "V1", "FUENTE-SINTETICA");
        aprobada.aprobar("EVIDENCIA-SINTETICA");
        when(claves.buscarPorFormularioYVersion(5L, "V1")).thenReturn(Optional.of(aprobada));

        assertEquals(
                "CLAVE_NO_EDITABLE",
                assertThrows(
                                IllegalStateException.class,
                                () -> servicio.cargar(5L, List.of(new DatoClave(10L, "B"))))
                        .getMessage());
        verify(entradas, never()).reemplazarEntradasBorrador(any(), any());
    }

    @Test
    void rechazaVersionPublicadaAntesDeModificarClave() {
        version.publicar("VALIDACION-SINTETICA");
        when(versiones.buscarPorId(5L)).thenReturn(Optional.of(version));
        assertEquals(
                "VERSION_NO_EDITABLE",
                assertThrows(
                                IllegalStateException.class,
                                () -> servicio.cargar(5L, List.of(new DatoClave(10L, "B"))))
                        .getMessage());
        verify(claves, never()).guardar(any());
    }

    @Test
    void respetaDenegacionDePermisos() {
        org.mockito.Mockito.doThrow(new SecurityException("sin permiso"))
                .when(autorizacion)
                .exigirPermisoEditarOpciones(5L);
        assertThrows(
                SecurityException.class,
                () -> servicio.cargar(5L, List.of(new DatoClave(10L, "B"))));
        verify(versiones, never()).buscarPorId(any());
    }

    private void configurarContenido() {
        when(versiones.buscarPorId(5L)).thenReturn(Optional.of(version));
        ConfiguracionSubprueba seleccion =
                new ConfiguracionSubprueba(
                        7L, 5L, 1L, 1, 60, TipoRespuesta.SELECCION, "INS", null, null);
        ConfiguracionSubprueba libre =
                new ConfiguracionSubprueba(
                        8L, 5L, 2L, 2, 60, TipoRespuesta.TEXTO_LIBRE, "INS", null, 20);
        when(configuraciones.listarPorVersion(5L)).thenReturn(List.of(seleccion, libre));
        when(preguntas.listarPorConfiguracion(7L))
                .thenReturn(List.of(new Pregunta(10L, 7L, "1", 1, "Reactivo", null)));
    }
}
