package com.universidad.tests.domain.model;

public class VersionClaveBFA {

    private final Long idClave;
    private final Long idVersionFormulario;
    private final String version;
    private final String referenciaFuente;

    private EstadoVersionClaveBFA estado;
    private String evidenciaAprobacion;

    public VersionClaveBFA(
            Long idClave, Long idVersionFormulario, String version, String referenciaFuente) {

        if (idClave != null && idClave <= 0) {
            throw new IllegalArgumentException("El ID de la clave debe ser positivo");
        }

        if (idVersionFormulario == null || idVersionFormulario <= 0) {
            throw new IllegalArgumentException("El formulario es obligatorio");
        }

        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("La version de la clave es obligatoria");
        }

        if (referenciaFuente == null || referenciaFuente.isBlank()) {
            throw new IllegalArgumentException("La referencia de fuente es obligatoria");
        }

        this.idClave = idClave;
        this.idVersionFormulario = idVersionFormulario;
        this.version = version;
        this.referenciaFuente = referenciaFuente;
        this.estado = EstadoVersionClaveBFA.BORRADOR;
        this.evidenciaAprobacion = null;
    }

    public Long getIdClave() {
        return idClave;
    }

    public Long getIdVersionFormulario() {
        return idVersionFormulario;
    }

    public String getVersion() {
        return version;
    }

    public String getReferenciaFuente() {
        return referenciaFuente;
    }

    public EstadoVersionClaveBFA getEstado() {
        return estado;
    }

    public String getEvidenciaAprobacion() {
        return evidenciaAprobacion;
    }

    // =====================================================
    // 1. VERIFICAR SI LA CLAVE ES EDITABLE
    // =====================================================

    public boolean esEditable() {
        return estado == EstadoVersionClaveBFA.BORRADOR;
    }

    // =====================================================
    // 2. VERIFICAR SI LA CLAVE PUEDE CALIFICAR
    // =====================================================

    public boolean esAptaParaNuevaCalificacion() {
        return estado == EstadoVersionClaveBFA.APROBADA;
    }

    public void exigirAptaParaNuevaCalificacion() {

        if (!esAptaParaNuevaCalificacion()) {
            throw new IllegalStateException(
                    "Solo una clave APROBADA puede utilizarse " + "para nuevas calificaciones");
        }
    }

    // =====================================================
    // 3. APROBAR UNA CLAVE
    // =====================================================

    public void aprobar(String evidencia) {

        if (estado != EstadoVersionClaveBFA.BORRADOR) {
            throw new IllegalStateException("Solo se puede aprobar una clave en BORRADOR");
        }

        if (idClave == null) {
            throw new IllegalStateException("La clave debe estar guardada antes de aprobarse");
        }

        if (evidencia == null || evidencia.isBlank()) {
            throw new IllegalArgumentException("La aprobacion requiere evidencia institucional");
        }

        this.evidenciaAprobacion = evidencia;
        this.estado = EstadoVersionClaveBFA.APROBADA;
    }

    // =====================================================
    // 4. RETIRAR UNA CLAVE
    // =====================================================

    public void retirar() {

        if (estado != EstadoVersionClaveBFA.APROBADA) {
            throw new IllegalStateException("Solo se puede retirar una clave APROBADA");
        }

        // Se conserva la evidencia de aprobacion.
        // El retiro no borra los datos historicos.
        this.estado = EstadoVersionClaveBFA.RETIRADA;
    }

    // =====================================================
    // 5. RESTAURAR UNA CLAVE DESDE PERSISTENCIA
    // =====================================================

    public static VersionClaveBFA restaurar(
            Long idClave,
            Long idVersionFormulario,
            String version,
            String referenciaFuente,
            EstadoVersionClaveBFA estado,
            String evidenciaAprobacion) {

        if (idClave == null || idClave <= 0) {
            throw new IllegalArgumentException("La clave almacenada debe tener un ID valido");
        }

        if (estado == null) {
            throw new IllegalArgumentException("El estado de la clave es obligatorio");
        }

        if (estado == EstadoVersionClaveBFA.BORRADOR) {

            if (evidenciaAprobacion != null) {
                throw new IllegalArgumentException(
                        "Una clave en BORRADOR no debe tener " + "evidencia de aprobacion");
            }

        } else {

            if (evidenciaAprobacion == null || evidenciaAprobacion.isBlank()) {

                throw new IllegalArgumentException(
                        "Una clave aprobada o retirada " + "debe tener evidencia");
            }
        }

        VersionClaveBFA clave =
                new VersionClaveBFA(idClave, idVersionFormulario, version, referenciaFuente);

        clave.estado = estado;
        clave.evidenciaAprobacion = evidenciaAprobacion;

        return clave;
    }
}
