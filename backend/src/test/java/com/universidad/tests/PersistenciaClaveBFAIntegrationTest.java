package com.universidad.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.TipoRespuesta;
import com.universidad.tests.domain.model.VersionClaveBFA;
import com.universidad.tests.domain.repository.EntradaClaveBFARepository;
import com.universidad.tests.domain.repository.VersionClaveBFARepository;
import com.universidad.tests.infrastructure.persistence.entity.ConfiguracionSubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.EntradaClaveBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.OpcionRespuestaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.PreguntaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.SubpruebaJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.TestJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionClaveBFAJpaEntity;
import com.universidad.tests.infrastructure.persistence.entity.VersionFormularioJpaEntity;
import com.universidad.tests.infrastructure.persistence.repository.EntradaClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.SubpruebaJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.TestJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionClaveBFAJpaRepository;
import com.universidad.tests.infrastructure.persistence.repository.VersionFormularioJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PersistenciaClaveBFAIntegrationTest {

    @Autowired private TestJpaRepository testRepository;

    @Autowired private VersionFormularioJpaRepository formularioRepository;

    @Autowired private VersionClaveBFAJpaRepository claveRepository;

    @Autowired private SubpruebaJpaRepository subpruebaRepository;

    @Autowired private EntradaClaveBFAJpaRepository entradaRepository;

    @Autowired private EntradaClaveBFARepository entradaDominioRepository;

    @Autowired private VersionClaveBFARepository claveDominioRepository;

    @PersistenceContext private EntityManager entityManager;

    // =====================================================
    // PRUEBA 1: Persistencia de versiones de claves BFA
    // =====================================================

    @Test
    void debeGuardarYConsultarVersionClaveEnBorrador() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA09_" + UUID.randomUUID(),
                                "Test ficticio BFA",
                                "Prueba de persistencia",
                                true));

        // Crear una version de formulario
        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario de prueba", "FUENTE-SINTETICA"));

        // Crear una version de clave
        VersionClaveBFAJpaEntity clave =
                new VersionClaveBFAJpaEntity(formulario, "CLAVE-V1", "MATERIAL-SINTETICO");

        claveRepository.saveAndFlush(clave);

        // Consultar la clave almacenada
        VersionClaveBFAJpaEntity encontrada =
                claveRepository.findById(clave.getIdClave()).orElseThrow();

        // Verificar los datos
        assertThat(encontrada.getIdClave()).isNotNull();

        assertThat(encontrada.getIdVersionFormulario())
                .isEqualTo(formulario.getIdVersionFormulario());

        assertThat(encontrada.getVersion()).isEqualTo("CLAVE-V1");

        assertThat(encontrada.getEstado()).isEqualTo(EstadoVersionClaveBFA.BORRADOR);

        assertThat(
                        claveRepository.existsByVersionFormulario_IdVersionFormularioAndVersion(
                                formulario.getIdVersionFormulario(), "CLAVE-V1"))
                .isTrue();
    }

    // =====================================================
    // PRUEBA 2: Persistencia de entradas de claves BFA
    // =====================================================

    @Test
    void debeGuardarYConsultarEntradaClaveBFA() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_ENTRADA_" + UUID.randomUUID(),
                                "Test BFA ficticio",
                                "Prueba de entradas",
                                true));

        // Crear una subprueba ficticia
        SubpruebaJpaEntity subprueba =
                subpruebaRepository.saveAndFlush(
                        new SubpruebaJpaEntity(
                                test, "SUB_BFA", "Subprueba ficticia", "Datos sinteticos"));

        // Crear una version de formulario
        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario ficticio", "FUENTE-SINTETICA"));

        // Configurar la subprueba
        ConfiguracionSubpruebaJpaEntity configuracion =
                new ConfiguracionSubpruebaJpaEntity(
                        formulario,
                        subprueba,
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "Seleccione una opcion",
                        null,
                        null);

        entityManager.persist(configuracion);

        // Crear una pregunta ficticia
        PreguntaJpaEntity pregunta =
                new PreguntaJpaEntity(configuracion, "1", 1, "Pregunta ficticia", null);

        entityManager.persist(pregunta);

        // Crear una opcion de respuesta
        OpcionRespuestaJpaEntity opcion =
                new OpcionRespuestaJpaEntity(pregunta, "A", 1, "Opcion ficticia", null);

        entityManager.persist(opcion);
        entityManager.flush();

        // Crear una version de clave
        VersionClaveBFAJpaEntity clave =
                claveRepository.saveAndFlush(
                        new VersionClaveBFAJpaEntity(formulario, "CLAVE-V1", "MATERIAL-SINTETICO"));

        // Registrar la respuesta correcta
        EntradaClaveBFAJpaEntity entrada =
                entradaRepository.saveAndFlush(
                        new EntradaClaveBFAJpaEntity(clave, pregunta, opcion));

        // Consultar la entrada almacenada
        EntradaClaveBFAJpaEntity encontrada =
                entradaRepository
                        .findByClave_IdClaveAndPregunta_IdPregunta(
                                clave.getIdClave(), pregunta.getIdPregunta())
                        .orElseThrow();

        // Verificar el identificador
        assertThat(encontrada.getIdEntradaClave()).isEqualTo(entrada.getIdEntradaClave());

        // Verificar la opcion correcta
        assertThat(encontrada.getOpcionCorrecta().getIdOpcion()).isEqualTo(opcion.getIdOpcion());

        // Verificar que existe exactamente una entrada
        assertThat(entradaRepository.countByClave_IdClave(clave.getIdClave())).isEqualTo(1);
    }

    @Test
    void debeRechazarVersionClaveDuplicadaEnMismoFormulario() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_DUP_" + UUID.randomUUID(),
                                "Test BFA ficticio",
                                "Prueba de versiones duplicadas",
                                true));

        // Crear una version de formulario
        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario ficticio", "FUENTE-SINTETICA"));

        // Guardar la primera clave
        claveRepository.saveAndFlush(
                new VersionClaveBFAJpaEntity(formulario, "CLAVE-V1", "MATERIAL-SINTETICO"));

        // Intentar guardar otra clave con la misma version
        VersionClaveBFAJpaEntity claveDuplicada =
                new VersionClaveBFAJpaEntity(formulario, "CLAVE-V1", "MATERIAL-SINTETICO-2");

        // PostgreSQL debe rechazar el duplicado
        assertThatThrownBy(() -> claveRepository.saveAndFlush(claveDuplicada))
                .hasStackTraceContaining("uq_clave_formulario_version");
    }

    @Test
    void debeGuardarYConsultarClaveMedianteAdaptador() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_ADAPTADOR_" + UUID.randomUUID(),
                                "Test BFA ficticio",
                                "Prueba del adaptador de persistencia",
                                true));

        // Crear una version de formulario
        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario ficticio", "FUENTE-SINTETICA"));

        // Crear el modelo de dominio
        VersionClaveBFA nuevaClave =
                new VersionClaveBFA(
                        null,
                        formulario.getIdVersionFormulario(),
                        "CLAVE-ADAPTADOR-V1",
                        "MATERIAL-SINTETICO");

        // Guardar utilizando el repositorio de dominio
        VersionClaveBFA guardada = claveDominioRepository.guardar(nuevaClave);

        assertThat(guardada.getIdClave()).isNotNull();

        assertThat(guardada.getEstado()).isEqualTo(EstadoVersionClaveBFA.BORRADOR);

        // Recuperar utilizando el adaptador
        VersionClaveBFA encontrada =
                claveDominioRepository.buscarPorId(guardada.getIdClave()).orElseThrow();

        assertThat(encontrada.getIdVersionFormulario())
                .isEqualTo(formulario.getIdVersionFormulario());

        assertThat(encontrada.getVersion()).isEqualTo("CLAVE-ADAPTADOR-V1");

        assertThat(encontrada.getReferenciaFuente()).isEqualTo("MATERIAL-SINTETICO");

        // Verificar que puede consultarse por formulario y version
        assertThat(
                        claveDominioRepository.existePorFormularioYVersion(
                                formulario.getIdVersionFormulario(), "CLAVE-ADAPTADOR-V1"))
                .isTrue();
    }

    @Test
    void debeGuardarYConsultarEntradaMedianteAdaptador() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_ADAPT_ENTRADA_" + UUID.randomUUID(),
                                "Test BFA ficticio",
                                "Prueba del adaptador de entradas",
                                true));

        // Crear una subprueba ficticia
        SubpruebaJpaEntity subprueba =
                subpruebaRepository.saveAndFlush(
                        new SubpruebaJpaEntity(
                                test, "SUB_BFA", "Subprueba ficticia", "Datos sinteticos"));

        // Crear una version de formulario
        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario ficticio", "FUENTE-SINTETICA"));

        // Configurar la subprueba
        ConfiguracionSubpruebaJpaEntity configuracion =
                new ConfiguracionSubpruebaJpaEntity(
                        formulario,
                        subprueba,
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "Seleccione una opcion",
                        null,
                        null);

        entityManager.persist(configuracion);

        // Crear una pregunta
        PreguntaJpaEntity pregunta =
                new PreguntaJpaEntity(configuracion, "1", 1, "Pregunta ficticia", null);

        entityManager.persist(pregunta);

        // Crear una opcion de respuesta
        OpcionRespuestaJpaEntity opcion =
                new OpcionRespuestaJpaEntity(pregunta, "A", 1, "Opcion ficticia", null);

        entityManager.persist(opcion);
        entityManager.flush();

        // Crear una clave mediante el repositorio de dominio
        VersionClaveBFA clave =
                claveDominioRepository.guardar(
                        new VersionClaveBFA(
                                null,
                                formulario.getIdVersionFormulario(),
                                "CLAVE-V1",
                                "MATERIAL-SINTETICO"));

        // Crear una entrada mediante el repositorio de dominio
        EntradaClaveBFA entrada =
                entradaDominioRepository.guardar(
                        new EntradaClaveBFA(
                                null,
                                clave.getIdClave(),
                                pregunta.getIdPregunta(),
                                opcion.getIdOpcion()));

        // Comprobar que se genero su identificador
        assertThat(entrada.getIdEntradaClave()).isNotNull();

        // Recuperar la entrada mediante el adaptador
        EntradaClaveBFA encontrada =
                entradaDominioRepository
                        .buscarPorClaveYPregunta(clave.getIdClave(), pregunta.getIdPregunta())
                        .orElseThrow();

        // Verificar las relaciones
        assertThat(encontrada.getIdClave()).isEqualTo(clave.getIdClave());

        assertThat(encontrada.getIdPregunta()).isEqualTo(pregunta.getIdPregunta());

        assertThat(encontrada.getIdOpcionCorrecta()).isEqualTo(opcion.getIdOpcion());

        // Verificar la cantidad de entradas registradas
        assertThat(entradaDominioRepository.contarPorClave(clave.getIdClave())).isEqualTo(1);

        // Verificar que la entrada existe
        assertThat(
                        entradaDominioRepository.existePorClaveYPregunta(
                                clave.getIdClave(), pregunta.getIdPregunta()))
                .isTrue();

        // Intentar registrar nuevamente la misma pregunta
        EntradaClaveBFA entradaDuplicada =
                new EntradaClaveBFA(
                        null, clave.getIdClave(), pregunta.getIdPregunta(), opcion.getIdOpcion());

        // El adaptador debe impedir la duplicacion
        assertThatThrownBy(() -> entradaDominioRepository.guardar(entradaDuplicada))
                .hasMessageContaining("La pregunta ya tiene una entrada en esta clave");
    }

    @Test
    void debeRechazarOpcionDeOtraPregunta() {

        // Crear datos ficticios
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_COMP_" + UUID.randomUUID(),
                                "Test ficticio",
                                "Prueba de compatibilidad",
                                true));

        SubpruebaJpaEntity subprueba =
                subpruebaRepository.saveAndFlush(
                        new SubpruebaJpaEntity(
                                test, "SUB_COMP", "Subprueba ficticia", "Datos sinteticos"));

        VersionFormularioJpaEntity formulario =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario ficticio", "FUENTE-SINTETICA"));

        ConfiguracionSubpruebaJpaEntity configuracion =
                new ConfiguracionSubpruebaJpaEntity(
                        formulario,
                        subprueba,
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "Seleccione una opcion",
                        null,
                        null);

        entityManager.persist(configuracion);

        // Crear dos preguntas diferentes
        PreguntaJpaEntity pregunta1 =
                new PreguntaJpaEntity(configuracion, "1", 1, "Primera pregunta ficticia", null);

        PreguntaJpaEntity pregunta2 =
                new PreguntaJpaEntity(configuracion, "2", 2, "Segunda pregunta ficticia", null);

        entityManager.persist(pregunta1);
        entityManager.persist(pregunta2);

        // Esta opcion pertenece a la pregunta 2
        OpcionRespuestaJpaEntity opcionPregunta2 =
                new OpcionRespuestaJpaEntity(pregunta2, "A", 1, "Opcion ficticia", null);

        entityManager.persist(opcionPregunta2);
        entityManager.flush();

        // Crear una clave en borrador
        VersionClaveBFA clave =
                claveDominioRepository.guardar(
                        new VersionClaveBFA(
                                null,
                                formulario.getIdVersionFormulario(),
                                "CLAVE-COMPATIBILIDAD",
                                "MATERIAL-SINTETICO"));

        // Intentar asociar la pregunta 1
        // con una opcion de la pregunta 2
        EntradaClaveBFA entradaInvalida =
                new EntradaClaveBFA(
                        null,
                        clave.getIdClave(),
                        pregunta1.getIdPregunta(),
                        opcionPregunta2.getIdOpcion());

        // El adaptador debe rechazar la asociacion

        assertThatThrownBy(() -> entradaDominioRepository.guardar(entradaInvalida))
                .hasMessageContaining("La opcion correcta no pertenece a la pregunta");
    }

    @Test
    void debeRechazarPreguntaDeOtroFormulario() {

        // Crear un test ficticio
        TestJpaEntity test =
                testRepository.saveAndFlush(
                        new TestJpaEntity(
                                "BFA_FORM_" + UUID.randomUUID(),
                                "Test ficticio BFA",
                                "Prueba de compatibilidad entre formularios",
                                true));

        // Crear una subprueba ficticia
        SubpruebaJpaEntity subprueba =
                subpruebaRepository.saveAndFlush(
                        new SubpruebaJpaEntity(
                                test, "SUB_FORM", "Subprueba ficticia", "Datos sinteticos"));

        // Crear dos versiones diferentes del formulario
        VersionFormularioJpaEntity formulario1 =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v1", "Formulario 1", "FUENTE-SINTETICA"));

        VersionFormularioJpaEntity formulario2 =
                formularioRepository.saveAndFlush(
                        new VersionFormularioJpaEntity(
                                test, "v2", "Formulario 2", "FUENTE-SINTETICA"));

        // Crear una configuracion en el formulario 2
        ConfiguracionSubpruebaJpaEntity configuracion =
                new ConfiguracionSubpruebaJpaEntity(
                        formulario2,
                        subprueba,
                        1,
                        null,
                        TipoRespuesta.SELECCION,
                        "Seleccione una opcion",
                        null,
                        null);

        entityManager.persist(configuracion);

        // Crear una pregunta que pertenece al formulario 2
        PreguntaJpaEntity pregunta =
                new PreguntaJpaEntity(
                        configuracion, "1", 1, "Pregunta ficticia del formulario 2", null);

        entityManager.persist(pregunta);

        OpcionRespuestaJpaEntity opcion =
                new OpcionRespuestaJpaEntity(pregunta, "A", 1, "Opcion ficticia", null);

        entityManager.persist(opcion);
        entityManager.flush();

        // Crear una clave asociada al formulario 1
        VersionClaveBFA clave =
                claveDominioRepository.guardar(
                        new VersionClaveBFA(
                                null,
                                formulario1.getIdVersionFormulario(),
                                "CLAVE-FORM-1",
                                "MATERIAL-SINTETICO"));

        // Intentar agregar una pregunta del formulario 2
        EntradaClaveBFA entradaInvalida =
                new EntradaClaveBFA(
                        null, clave.getIdClave(), pregunta.getIdPregunta(), opcion.getIdOpcion());

        // El adaptador debe rechazar la incompatibilidad
        assertThatThrownBy(() -> entradaDominioRepository.guardar(entradaInvalida))
                .hasMessageContaining("La pregunta no pertenece al formulario de la clave");
    }
}
