package com.universidad.tests.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.universidad.tests.application.service.AutorizacionService;
import com.universidad.tests.application.service.IdentidadSesion;
import com.universidad.tests.application.service.PublicacionVersionFormularioService;
import com.universidad.tests.application.service.RegistroAuditoria;
import com.universidad.tests.application.service.VersionFormularioRestService;
import com.universidad.tests.application.service.VersionFormularioService;
import com.universidad.tests.domain.exception.NoAutenticadoException;
import com.universidad.tests.domain.model.VersionFormulario;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// GEN-45-BE (aporte Carlos): E31 audita solo cuando la publicacion se concreta.
@ExtendWith(MockitoExtension.class)
class VersionFormularioRestServicePublicarTest {

    @Mock private VersionFormularioService versiones;
    @Mock private PublicacionVersionFormularioService publicacion;
    @Mock private RegistroAuditoria auditoria;
    @Mock private AutorizacionService autorizacion;

    @InjectMocks private VersionFormularioRestService servicio;

    private IdentidadSesion sesion() {
        return new IdentidadSesion(40L, 1L, "COORDINADOR", Set.of("CONFIGURAR_BFA"));
    }

    @Test
    void registraAuditoriaConLaCuentaQuePublica() {
        VersionFormulario version = new VersionFormulario(11L, 1L, "v2", null, "fuente-ficticia");
        version.publicar("E31");
        when(autorizacion.exigirSesion()).thenReturn(sesion());
        when(publicacion.publicar(eq(11L), anyString())).thenReturn(version);

        var resumen = servicio.publicar(11L);

        assertEquals("PUBLICADA", resumen.estado());
        assertEquals(11L, resumen.id());
        verify(auditoria).registrar(40L, "PUBLICAR_VERSION", "VERSION_FORMULARIO", 11L);
    }

    @Test
    void noAuditaSiLaVersionEstaIncompleta() {
        when(autorizacion.exigirSesion()).thenReturn(sesion());
        when(publicacion.publicar(eq(11L), anyString()))
                .thenThrow(
                        new PublicacionVersionFormularioService.VersionIncompletaException(
                                "Falta clave"));

        assertThrows(
                PublicacionVersionFormularioService.VersionIncompletaException.class,
                () -> servicio.publicar(11L));

        verifyNoInteractions(auditoria);
    }

    @Test
    void sinSesionNoPublicaNiAudita() {
        when(autorizacion.exigirSesion()).thenThrow(new NoAutenticadoException());

        assertThrows(NoAutenticadoException.class, () -> servicio.publicar(11L));

        verifyNoInteractions(publicacion, auditoria);
    }
}
