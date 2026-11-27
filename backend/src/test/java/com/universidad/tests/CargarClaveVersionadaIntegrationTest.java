package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.universidad.tests.application.service.CargarClaveVersionadaService;
import com.universidad.tests.application.service.CargarClaveVersionadaService.DatoClave;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.port.AutorizacionContenidoPort;
import com.universidad.tests.infrastructure.persistence.entity.ConfiguracionSubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.OpcionRespuestaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.PreguntaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.SubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.ConfiguracionSubpruebaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.EntradaClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.OpcionRespuestaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.PreguntaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.SubpruebaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.TestJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionFormularioJpaRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CargarClaveVersionadaIntegrationTest {
    @Autowired private CargarClaveVersionadaService servicio;
    @Autowired private TestJpaRepository tests;
    @Autowired private SubpruebaJpaRepository subpruebas;
    @Autowired private VersionFormularioJpaRepository versiones;
    @Autowired private ConfiguracionSubpruebaJpaRepository configuraciones;
    @Autowired private PreguntaJpaRepository preguntas;
    @Autowired private OpcionRespuestaJpaRepository opciones;
    @Autowired private VersionClaveBFAJpaRepository claves;
    @Autowired private EntradaClaveBFAJpaRepository entradas;
    @MockitoBean private AutorizacionContenidoPort autorizacion;

    @Test
    void e29PersisteYReemplazaClaveEnBorradorSinCrearOtraVersion() {
        TestJpaEntity test =
                tests.saveAndFlush(
                        new TestJpaEntity(
                                "E29-" + UUID.randomUUID(),
                                "Test ficticio",
                                "Datos sinteticos",
                                true));
        SubpruebaJpaEntity subprueba =
                subpruebas.saveAndFlush(
                        new SubpruebaJpaEntity(test, "R1", "R1 ficticia", "Prueba"));
        VersionFormularioJpaEntity version =
                versiones.saveAndFlush(
                        new VersionFormularioJpaEntity(test, "v1", null, "FUENTE-SINTETICA"));
        ConfiguracionSubpruebaJpaEntity configuracion =
                configuraciones.saveAndFlush(
                        new ConfiguracionSubpruebaJpaEntity(
                                version,
                                subprueba,
                                1,
                                60,
                                TipoRespuesta.SELECCION,
                                "INS",
                                null,
                                null));
        PreguntaJpaEntity pregunta =
                preguntas.saveAndFlush(
                        new PreguntaJpaEntity(configuracion, "1", 1, "Reactivo", null));
        OpcionRespuestaJpaEntity opcionA =
                opciones.saveAndFlush(new OpcionRespuestaJpaEntity(pregunta, "A", 1, "A", null));
        OpcionRespuestaJpaEntity opcionB =
                opciones.saveAndFlush(new OpcionRespuestaJpaEntity(pregunta, "B", 2, "B", null));

        Long idClave =
                servicio.cargar(
                        version.getIdVersionFormulario(),
                        List.of(new DatoClave(pregunta.getIdPregunta(), "A")));
        assertThat(entradas.findByClave_IdClave(idClave))
                .singleElement()
                .extracting(entrada -> entrada.getOpcionCorrecta().getIdOpcion())
                .isEqualTo(opcionA.getIdOpcion());

        Long idReemplazo =
                servicio.cargar(
                        version.getIdVersionFormulario(),
                        List.of(new DatoClave(pregunta.getIdPregunta(), "B")));

        assertThat(idReemplazo).isEqualTo(idClave);
        assertThat(
                        claves.findByVersionFormulario_IdVersionFormularioOrderByVersionAsc(
                                version.getIdVersionFormulario()))
                .hasSize(1);
        assertThat(entradas.findByClave_IdClave(idClave))
                .singleElement()
                .extracting(entrada -> entrada.getOpcionCorrecta().getIdOpcion())
                .isEqualTo(opcionB.getIdOpcion());
    }
}
