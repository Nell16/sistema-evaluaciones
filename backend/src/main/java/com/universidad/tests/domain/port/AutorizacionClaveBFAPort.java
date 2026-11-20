package com.universidad.tests.domain.port;

public interface AutorizacionClaveBFAPort {

    // =====================================================
    // 1. CREAR CLAVES
    // =====================================================

    void exigirPermisoCrearClave(Long idVersionFormulario);

    // =====================================================
    // 2. REGISTRAR RESPUESTAS CORRECTAS
    // =====================================================

    void exigirPermisoAgregarEntrada(Long idClave);

    // =====================================================
    // 3. CONSULTAR RESPUESTAS CORRECTAS
    // =====================================================

    void exigirPermisoConsultarClave(Long idClave);

    // =====================================================
    // 4. APROBAR CLAVES
    // =====================================================

    void exigirPermisoAprobarClave(Long idClave);

    // =====================================================
    // 5. RETIRAR CLAVES
    // =====================================================

    void exigirPermisoRetirarClave(Long idClave);

    // =====================================================
    // 6. UTILIZAR CLAVES PARA CALIFICACIONES
    // =====================================================

    default void exigirPermisoCalificarConClave(Long idClave) {

        // Por seguridad, se deniega el acceso mientras
        // no exista una implementacion autorizada.
        //
        // El permiso de calificar es independiente
        // del permiso de consultar respuestas correctas.

        throw new SecurityException(
                "Operacion no autorizada: "
                        + "el permiso de calificacion BFA "
                        + "aun no esta configurado");
    }
}
