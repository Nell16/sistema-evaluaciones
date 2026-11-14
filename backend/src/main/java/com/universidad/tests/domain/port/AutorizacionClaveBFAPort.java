package com.universidad.tests.domain.port;

public interface AutorizacionClaveBFAPort {

    // Verificar permiso para crear una clave
    void exigirPermisoCrearClave(Long idVersionFormulario);

    // Verificar permiso para registrar respuestas correctas
    void exigirPermisoAgregarEntrada(Long idClave);

    // Verificar permiso para consultar respuestas correctas
    void exigirPermisoConsultarClave(Long idClave);

    // Verificar permiso para aprobar una clave oficial
    void exigirPermisoAprobarClave(Long idClave);

    // Verificar permiso para retirar una clave
    void exigirPermisoRetirarClave(Long idClave);
}
