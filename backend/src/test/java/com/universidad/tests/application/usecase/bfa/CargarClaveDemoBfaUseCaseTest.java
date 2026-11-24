package com.universidad.tests.application.usecase.bfa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.universidad.tests.domain.model.EntradaClaveBFA;
import com.universidad.tests.domain.model.EstadoVersionClaveBFA;
import com.universidad.tests.domain.model.VersionFormulario;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CargarClaveDemoBfaUseCaseTest {

    private ReposEnMemoria repos;
    private CargarCatalogoDemoBfaUseCase catalogo;
    private CargarClaveDemoBfaUseCase clave;

    @BeforeEach
    void setUp() {
        repos = new ReposEnMemoria();
        ConsultarConfiguracionBfaUseCase consultar =
                new ConsultarConfiguracionBfaUseCase(
                        repos.tests, repos.versiones, repos.subpruebas, repos.configuraciones);
        catalogo =
                new CargarCatalogoDemoBfaUseCase(
                        repos.tests,
                        repos.subpruebas,
                        repos.versiones,
                        repos.configuraciones,
                        repos.preguntas,
                        repos.opciones,
                        consultar);
        clave =
                new CargarClaveDemoBfaUseCase(
                        repos.versiones,
                        repos.configuraciones,
                        repos.preguntas,
                        repos.opciones,
                        repos.claves,
                        repos.entradasClave);
    }

    @Test
    void laClaveCubreTodasLasPreguntasDeSeleccionYNingunaDeTextoLibre() {
        VersionFormulario version = catalogo.cargarContenido();

        Long idClave = clave.cargarClave(version.getIdVersionFormulario());

        // 12 subpruebas de seleccion x 3 preguntas; FV (texto libre) no lleva clave
        assertEquals(36, repos.entradasClave.contarPorClave(idClave));
        assertEquals(
                EstadoVersionClaveBFA.BORRADOR,
                repos.claves.buscarPorId(idClave).get().getEstado());
        assertEquals(
                "demo/clave-ficticia",
                repos.claves.buscarPorId(idClave).get().getReferenciaFuente());
    }

    @Test
    void cadaOpcionCorrectaPerteneceASuPregunta() {
        VersionFormulario version = catalogo.cargarContenido();

        Long idClave = clave.cargarClave(version.getIdVersionFormulario());

        for (EntradaClaveBFA entrada : repos.entradasClave.listarPorClave(idClave)) {
            var opcion = repos.opciones.buscarPorId(entrada.getIdOpcionCorrecta()).get();
            assertEquals(entrada.getIdPregunta(), opcion.getIdPregunta());
        }
    }

    @Test
    void laOpcionCorrectaRotaYNoEsSiempreLaMisma() {
        VersionFormulario version = catalogo.cargarContenido();

        Long idClave = clave.cargarClave(version.getIdVersionFormulario());

        long codigosDistintos =
                repos.entradasClave.listarPorClave(idClave).stream()
                        .map(
                                e ->
                                        repos.opciones
                                                .buscarPorId(e.getIdOpcionCorrecta())
                                                .get()
                                                .getCodigo())
                        .distinct()
                        .count();
        assertTrue(codigosDistintos > 1);
    }

    @Test
    void cargarDosVecesNoDuplicaNiCambiaLaClave() {
        VersionFormulario version = catalogo.cargarContenido();
        Long primera = clave.cargarClave(version.getIdVersionFormulario());

        Long segunda = clave.cargarClave(version.getIdVersionFormulario());

        assertEquals(primera, segunda);
        assertEquals(1, repos.claves.datos.size());
        assertEquals(36, repos.entradasClave.datos.size());
    }

    @Test
    void sinPreguntasDeSeleccionNoCreaClave() {
        VersionFormulario version = catalogo.cargarContenido();
        repos.preguntas.datos.clear();

        assertThrows(
                IllegalStateException.class,
                () -> clave.cargarClave(version.getIdVersionFormulario()));
        assertTrue(repos.claves.datos.isEmpty());
    }

    @Test
    void siLaVersionNoExisteFalla() {
        assertThrows(NoSuchElementException.class, () -> clave.cargarClave(999L));
    }
}
