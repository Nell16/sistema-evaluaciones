package com.universidad.tests.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.universidad.tests.application.service.CatalogoTestsService;
import com.universidad.tests.domain.exception.TestNoEncontradoException;
import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.EstadoVersionFormulario;
import com.universidad.tests.domain.model.Subprueba;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionFormulario;
import com.universidad.tests.domain.repository.ConfiguracionSubpruebaRepository;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.SubpruebaRepository;
import com.universidad.tests.domain.repository.TestRepository;
import com.universidad.tests.domain.repository.VersionFormularioRepository;
import com.universidad.tests.dto.response.ConfiguracionSubpruebaResponse;
import com.universidad.tests.dto.response.TestDetalleResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogoTestsServiceTest {

    @Mock private TestRepository testRepository;
    @Mock private SubpruebaRepository subpruebaRepository;
    @Mock private VersionFormularioRepository versionRepository;
    @Mock private ConfiguracionSubpruebaRepository configuracionRepository;
    @Mock private PreguntaRepository preguntaRepository;
    @Mock private OpcionRespuestaRepository opcionRepository;

    @InjectMocks private CatalogoTestsService servicio;

    private static final com.universidad.tests.domain.model.Test TEST =
            new com.universidad.tests.domain.model.Test(1L, "FICT", "Test ficticio", null, true);

    private static VersionFormulario version(
            Long id, EstadoVersionFormulario estado, Instant publicadaEn) {
        return VersionFormulario.restaurar(
                id,
                1L,
                "v" + id,
                null,
                "FUENTE",
                estado,
                publicadaEn,
                publicadaEn == null ? null : "ACTA");
    }

    @Test
    void listaTestsActivosConSuNumeroDeSubpruebas() {

        when(testRepository.listarActivos()).thenReturn(List.of(TEST));
        when(subpruebaRepository.listarPorTest(1L))
                .thenReturn(List.of(new Subprueba(7L, 1L, "A", "Sub A", null, null)));

        assertThat(servicio.listarTests())
                .singleElement()
                .satisfies(t -> assertThat(t.numeroSubpruebas()).isEqualTo(1));
    }

    @Test
    void detalleUsaLaUltimaEdicionPublicadaYDerivaLosConteos() {

        Instant antes = Instant.parse("2026-11-01T00:00:00Z");
        when(testRepository.buscarPorId(1L)).thenReturn(Optional.of(TEST));
        when(versionRepository.listarPorTest(1L))
                .thenReturn(
                        List.of(
                                version(10L, EstadoVersionFormulario.PUBLICADA, antes),
                                version(
                                        11L,
                                        EstadoVersionFormulario.PUBLICADA,
                                        antes.plusSeconds(60)),
                                version(12L, EstadoVersionFormulario.BORRADOR, null)));
        when(subpruebaRepository.listarPorTest(1L))
                .thenReturn(
                        List.of(
                                new Subprueba(7L, 1L, "SEL", "Seleccion", null, "F1"),
                                new Subprueba(8L, 1L, "LIB", "Texto libre", null, "F2")));
        when(configuracionRepository.listarPorVersion(11L))
                .thenReturn(
                        List.of(
                                new ConfiguracionSubprueba(
                                        21L,
                                        11L,
                                        8L,
                                        2,
                                        300,
                                        TipoRespuesta.TEXTO_LIBRE,
                                        "INS",
                                        "PRA",
                                        75),
                                new ConfiguracionSubprueba(
                                        20L,
                                        11L,
                                        7L,
                                        1,
                                        180,
                                        TipoRespuesta.SELECCION,
                                        "INS",
                                        null,
                                        null)));
        when(preguntaRepository.contarPorConfiguracion(20L)).thenReturn(3);
        when(preguntaRepository.contarPorConfiguracion(21L)).thenReturn(0);
        when(opcionRepository.contarPorPreguntaDeConfiguracion(20L))
                .thenReturn(List.of(4L, 4L, 4L));

        TestDetalleResponse detalle = servicio.obtenerTest(1L);

        assertThat(detalle.versionFormularioId()).isEqualTo(11L);
        assertThat(detalle.subpruebas())
                .extracting(ConfiguracionSubpruebaResponse::codigo)
                .containsExactly("SEL", "LIB");
        assertThat(detalle.subpruebas().get(0))
                .isEqualTo(
                        new ConfiguracionSubpruebaResponse(
                                "SEL", "Seleccion", 1, "F1", 180, "SELECCION", 4, 3, null, false));
        assertThat(detalle.subpruebas().get(1))
                .isEqualTo(
                        new ConfiguracionSubpruebaResponse(
                                "LIB",
                                "Texto libre",
                                2,
                                "F2",
                                300,
                                "TEXTO_LIBRE",
                                null,
                                0,
                                75,
                                true));
    }

    @Test
    void opcionesEsNulaSiNoTodasLasPreguntasTienenLasMismas() {

        when(testRepository.buscarPorId(1L)).thenReturn(Optional.of(TEST));
        when(versionRepository.listarPorTest(1L))
                .thenReturn(
                        List.of(version(10L, EstadoVersionFormulario.PUBLICADA, Instant.now())));
        when(subpruebaRepository.listarPorTest(1L))
                .thenReturn(List.of(new Subprueba(7L, 1L, "SEL", "Seleccion", null, null)));
        when(configuracionRepository.listarPorVersion(10L))
                .thenReturn(
                        List.of(
                                new ConfiguracionSubprueba(
                                        20L,
                                        10L,
                                        7L,
                                        1,
                                        null,
                                        TipoRespuesta.SELECCION,
                                        "INS",
                                        null,
                                        null)));
        when(preguntaRepository.contarPorConfiguracion(20L)).thenReturn(2);
        when(opcionRepository.contarPorPreguntaDeConfiguracion(20L)).thenReturn(List.of(4L, 5L));

        assertThat(servicio.obtenerTest(1L).subpruebas().get(0).opciones()).isNull();
    }

    @Test
    void testInactivoNoSeEncuentra() {

        when(testRepository.buscarPorId(1L))
                .thenReturn(
                        Optional.of(
                                new com.universidad.tests.domain.model.Test(
                                        1L, "FICT", "Test ficticio", null, false)));

        assertThatThrownBy(() -> servicio.obtenerTest(1L))
                .isInstanceOf(TestNoEncontradoException.class);
    }

    @Test
    void testSinEdicionPublicadaNoSeEncuentra() {

        when(testRepository.buscarPorId(1L)).thenReturn(Optional.of(TEST));
        when(versionRepository.listarPorTest(1L))
                .thenReturn(List.of(version(12L, EstadoVersionFormulario.BORRADOR, null)));

        assertThatThrownBy(() -> servicio.obtenerTest(1L))
                .isInstanceOf(TestNoEncontradoException.class);
    }
}
