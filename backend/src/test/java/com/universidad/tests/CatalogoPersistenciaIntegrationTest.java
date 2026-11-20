package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.universidad.tests.domain.model.Subprueba;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.repository.OpcionRespuestaRepository;
import com.universidad.tests.domain.repository.PreguntaRepository;
import com.universidad.tests.domain.repository.SubpruebaRepository;
import com.universidad.tests.infrastructure.persistence.entity.ConfiguracionSubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.OpcionRespuestaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.PreguntaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.SubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

// GEN-01-BE: factor de la subprueba y conteos de preguntas y opciones
@SpringBootTest
@Transactional
class CatalogoPersistenciaIntegrationTest {

    @Autowired private SubpruebaRepository subpruebaRepository;

    @Autowired private PreguntaRepository preguntaRepository;

    @Autowired private OpcionRespuestaRepository opcionRepository;

    @PersistenceContext private EntityManager entityManager;

    @Test
    void guardaElFactorYCuentaPreguntasYOpciones() {

        TestJpaEntity test =
                new TestJpaEntity("CATALOGO_" + UUID.randomUUID(), "Test ficticio", null, true);
        entityManager.persist(test);
        entityManager.flush();

        Subprueba subprueba =
                subpruebaRepository.guardar(
                        new Subprueba(null, test.getIdTest(), "SUB", "Subprueba", null, "F1"));

        assertThat(subpruebaRepository.buscarPorId(subprueba.getIdSubprueba()).orElseThrow())
                .extracting(Subprueba::getFactor)
                .isEqualTo("F1");

        VersionFormularioJpaEntity version =
                new VersionFormularioJpaEntity(test, "v1", null, "FUENTE-001");
        ConfiguracionSubpruebaJpaEntity config =
                new ConfiguracionSubpruebaJpaEntity(
                        version,
                        entityManager.find(SubpruebaJpaEntity.class, subprueba.getIdSubprueba()),
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "INSTRUCCIONES",
                        null,
                        null);
        PreguntaJpaEntity pregunta1 = new PreguntaJpaEntity(config, "1", 1, "Pregunta 1", null);
        PreguntaJpaEntity pregunta2 = new PreguntaJpaEntity(config, "2", 2, "Pregunta 2", null);
        entityManager.persist(version);
        entityManager.persist(config);
        entityManager.persist(pregunta1);
        entityManager.persist(pregunta2);
        for (PreguntaJpaEntity pregunta : new PreguntaJpaEntity[] {pregunta1, pregunta2}) {
            entityManager.persist(new OpcionRespuestaJpaEntity(pregunta, "A", 1, "Opcion A", null));
            entityManager.persist(new OpcionRespuestaJpaEntity(pregunta, "B", 2, "Opcion B", null));
        }
        entityManager.flush();

        assertThat(preguntaRepository.contarPorConfiguracion(config.getIdConfiguracionSubprueba()))
                .isEqualTo(2);
        assertThat(
                        opcionRepository.contarPorPreguntaDeConfiguracion(
                                config.getIdConfiguracionSubprueba()))
                .containsExactly(2L, 2L);
    }
}
