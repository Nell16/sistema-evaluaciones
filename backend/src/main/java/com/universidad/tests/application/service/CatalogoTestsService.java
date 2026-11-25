package com.universidad.tests.application.service;

import com.universidad.tests.domain.exception.TestNoEncontradoException;
import com.universidad.tests.domain.model.ConfiguracionSubprueba;
import com.universidad.tests.domain.model.Subprueba;
import com.universidad.tests.domain.model.Test;
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
import com.universidad.tests.dto.response.TestResumenResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Consulta del catalogo de tests: E1 y E2 (CU-BFA-P01, CU-BFA-P02)
@Service
public class CatalogoTestsService {

    private final TestRepository testRepository;
    private final SubpruebaRepository subpruebaRepository;
    private final VersionFormularioRepository versionRepository;
    private final ConfiguracionSubpruebaRepository configuracionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionRespuestaRepository opcionRepository;

    public CatalogoTestsService(
            TestRepository testRepository,
            SubpruebaRepository subpruebaRepository,
            VersionFormularioRepository versionRepository,
            ConfiguracionSubpruebaRepository configuracionRepository,
            PreguntaRepository preguntaRepository,
            OpcionRespuestaRepository opcionRepository) {

        this.testRepository = testRepository;
        this.subpruebaRepository = subpruebaRepository;
        this.versionRepository = versionRepository;
        this.configuracionRepository = configuracionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
    }

    @Transactional(readOnly = true)
    public List<TestResumenResponse> listarTests() {

        return testRepository.listarActivos().stream()
                .map(
                        test ->
                                new TestResumenResponse(
                                        test.getIdTest(),
                                        test.getCodigo(),
                                        test.getNombre(),
                                        subpruebaRepository.listarPorTest(test.getIdTest()).size()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TestDetalleResponse obtenerTest(Long idTest) {

        Test test =
                testRepository
                        .buscarPorId(idTest)
                        .filter(Test::isActivo)
                        .orElseThrow(() -> new TestNoEncontradoException(idTest));

        // Se aplica la edicion publicada mas reciente
        VersionFormulario version =
                versionRepository.listarPorTest(idTest).stream()
                        .filter(VersionFormulario::estaPublicada)
                        .max(Comparator.comparing(VersionFormulario::getPublicadaEn))
                        .orElseThrow(() -> new TestNoEncontradoException(idTest));

        Map<Long, Subprueba> subpruebas =
                subpruebaRepository.listarPorTest(idTest).stream()
                        .collect(Collectors.toMap(Subprueba::getIdSubprueba, Function.identity()));

        List<ConfiguracionSubpruebaResponse> configuraciones =
                configuracionRepository.listarPorVersion(version.getIdVersionFormulario()).stream()
                        .sorted(Comparator.comparingInt(ConfiguracionSubprueba::getOrdenAplicacion))
                        .map(c -> aRespuesta(c, subpruebas.get(c.getIdSubprueba())))
                        .toList();

        return new TestDetalleResponse(
                test.getIdTest(),
                test.getCodigo(),
                test.getNombre(),
                version.getIdVersionFormulario(),
                configuraciones);
    }

    private ConfiguracionSubpruebaResponse aRespuesta(
            ConfiguracionSubprueba configuracion, Subprueba subprueba) {

        int numeroPreguntas =
                preguntaRepository.contarPorConfiguracion(
                        configuracion.getIdConfiguracionSubprueba());

        return new ConfiguracionSubpruebaResponse(
                subprueba.getCodigo(),
                subprueba.getNombre(),
                configuracion.getOrdenAplicacion(),
                subprueba.getFactor(),
                configuracion.getTiempoLimiteSegundos(),
                configuracion.getTipoRespuesta().name(),
                opciones(configuracion, numeroPreguntas),
                numeroPreguntas,
                configuracion.getMaximoEntradas(),
                configuracion.getPracticaRef() != null);
    }

    // Opciones por pregunta; nulo en texto libre o si no es igual en todas
    private Integer opciones(ConfiguracionSubprueba configuracion, int numeroPreguntas) {

        if (configuracion.getTipoRespuesta() != TipoRespuesta.SELECCION || numeroPreguntas == 0) {
            return null;
        }

        List<Long> conteos =
                opcionRepository.contarPorPreguntaDeConfiguracion(
                        configuracion.getIdConfiguracionSubprueba());

        if (conteos.size() != numeroPreguntas || conteos.stream().distinct().count() != 1) {
            return null;
        }

        return conteos.get(0).intValue();
    }
}
