package com.universidad.tests.application.service;

import com.universidad.tests.domain.model.VersionFormulario;

import org.springframework.stereotype.Service;

@Service
public class VersionFormularioRestService {
    private final VersionFormularioService versiones;

    private final PublicacionVersionFormularioService publicacion;
    private final RegistroAuditoria auditoria;
    private final AutorizacionService autorizacion;

    public VersionFormularioRestService(
            VersionFormularioService versiones,
            PublicacionVersionFormularioService publicacion,
            RegistroAuditoria auditoria,
            AutorizacionService autorizacion) {
        this.publicacion = publicacion;
        this.auditoria = auditoria;
        this.autorizacion = autorizacion;
        this.versiones = versiones;
    }

    public ResumenVersion crear(Long idTest, String codigo, String descripcion, String fuenteRef) {
        return resumen(
                versiones.crearBorradorDesdeUltimaPublicada(
                        idTest, codigo, descripcion, fuenteRef));
    }

    @org.springframework.transaction.annotation.Transactional
    public ResumenVersion publicar(Long id) {
        long cuenta = autorizacion.exigirSesion().idCuenta();
        var publicada =
                publicacion.publicar(id, "E31: validación de publicación de la versión " + id);
        auditoria.registrar(cuenta, "PUBLICAR_VERSION", "VERSION_FORMULARIO", id);
        return resumen(publicada);
    }

    private ResumenVersion resumen(VersionFormulario version) {
        return new ResumenVersion(
                version.getIdVersionFormulario(), version.getIdTest(), version.getEstado().name());
    }

    public record ResumenVersion(Long id, Long testId, String estado) {}
}
