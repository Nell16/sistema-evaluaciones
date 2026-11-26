package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.universidad.tests.application.service.AutorizacionBaremoService;
import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.ContextoSesion;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.domain.exception.NoAutenticadoException;
import com.universidad.tests.domain.exception.SinPermisoException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AutorizacionBaremoServiceTest {

    private final AutorizacionBaremoService autorizacion =
            new AutorizacionBaremoService(new AutorizacionService());

    @AfterEach
    void limpiarSesion() {
        ContextoSesion.limpiar();
    }

    @Test
    void exigeSesionYPermisoConfigurarBfa() {
        assertThrows(NoAutenticadoException.class, autorizacion::exigirPermisoCargarBaremo);

        ContextoSesion.establecer(new IdentidadSesion(1L, 1L, "SUJETO", Set.of()));
        assertThrows(SinPermisoException.class, autorizacion::exigirPermisoCargarBaremo);

        ContextoSesion.establecer(
                new IdentidadSesion(1L, 1L, "ACADEMICO", Set.of("CONFIGURAR_BFA")));
        assertDoesNotThrow(autorizacion::exigirPermisoCargarBaremo);
    }
}
