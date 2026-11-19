package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.universidad.tests.application.service.EdicionContenidoVersionadoService;
import com.universidad.tests.application.service.OpcionesVersionadasService;
import com.universidad.tests.application.service.OpcionesVersionadasService.DatosOpcion;
import com.universidad.tests.domain.model.*;
import com.universidad.tests.domain.port.AutorizacionContenidoPort;
import com.universidad.tests.domain.repository.*;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpcionesVersionadasServiceTest {
    @Test
    void noModificaUnaVersionPublicada() {
        VersionFormularioRepository versiones = mock(VersionFormularioRepository.class);
        PreguntaRepository preguntas = mock(PreguntaRepository.class);
        OpcionRespuestaRepository opciones = mock(OpcionRespuestaRepository.class);
        EdicionContenidoVersionadoService edicion = mock(EdicionContenidoVersionadoService.class);
        VersionFormulario publicada = new VersionFormulario(11L, 1L, "v2", null, "DOC");
        publicada.publicar("ACTA");
        when(versiones.buscarPorId(11L)).thenReturn(Optional.of(publicada));
        OpcionesVersionadasService servicio =
                new OpcionesVersionadasService(
                        versiones,
                        mock(ConfiguracionSubpruebaRepository.class),
                        preguntas,
                        opciones,
                        mock(AutorizacionContenidoPort.class),
                        edicion);

        assertThrows(
                IllegalStateException.class,
                () -> servicio.guardar(11L, 120L, List.of(new DatosOpcion("A", 1, "Uno", null))));
        verifyNoInteractions(preguntas, opciones, edicion);
    }
}
