
-- BFA-BE-09
-- Proteccion de claves de correccion versionadas.
-- No contiene claves ni respuestas oficiales.

-- =====================================================
-- 1. CONTROLAR EL CICLO DE VIDA DE LAS CLAVES
-- =====================================================

CREATE FUNCTION bfa_controlar_cambios_clave()
  RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN

    -- Toda clave nueva debe comenzar en BORRADOR.
    IF TG_OP = 'INSERT' THEN

        IF NEW.estado <> 'BORRADOR'
           OR NEW.evidencia_aprobacion IS NOT NULL THEN

            RAISE EXCEPTION
                'Las claves nuevas deben iniciar en BORRADOR';
END IF;

RETURN NEW;
END IF;

    -- La identidad y los datos de version son inmutables.
    IF NEW.id_clave IS DISTINCT FROM OLD.id_clave
       OR NEW.id_version_formulario
           IS DISTINCT FROM OLD.id_version_formulario
       OR NEW.version IS DISTINCT FROM OLD.version
       OR NEW.referencia_fuente
           IS DISTINCT FROM OLD.referencia_fuente THEN

        RAISE EXCEPTION
            'No se permite modificar la identidad o version de una clave';
END IF;

    -- Mantener una clave en BORRADOR.
    IF OLD.estado = 'BORRADOR'
       AND NEW.estado = 'BORRADOR'
       AND NEW.evidencia_aprobacion IS NULL THEN

        RETURN NEW;
END IF;

    -- Aprobar una clave con evidencia.
    IF OLD.estado = 'BORRADOR'
       AND NEW.estado = 'APROBADA'
       AND NULLIF(BTRIM(NEW.evidencia_aprobacion), '')
           IS NOT NULL THEN

        RETURN NEW;
END IF;

    -- Retirar una clave conservando su evidencia.
    IF OLD.estado = 'APROBADA'
       AND NEW.estado = 'RETIRADA'
       AND NEW.evidencia_aprobacion
           IS NOT DISTINCT FROM OLD.evidencia_aprobacion THEN

        RETURN NEW;
END IF;

    RAISE EXCEPTION
        'Transicion de estado BFA no permitida: % a %',
        OLD.estado,
        NEW.estado;

END;
$$;

CREATE TRIGGER trg_bfa_controlar_cambios_clave
  BEFORE INSERT OR UPDATE
                     ON version_clave_bfa
                     FOR EACH ROW
                     EXECUTE FUNCTION bfa_controlar_cambios_clave();


-- =====================================================
-- 2. PROTEGER LAS ENTRADAS DE CORRECCION
-- =====================================================

CREATE FUNCTION bfa_controlar_cambios_entrada()
  RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
DECLARE
estado_clave VARCHAR(20);
BEGIN

    -- Una respuesta registrada no se modifica.
    -- Si hay que corregir una clave, debe gestionarse
    -- una nueva version controlada.
    IF TG_OP = 'UPDATE' THEN

        RAISE EXCEPTION
            'No se permite modificar entradas de correccion existentes';
END IF;

    -- Bloquear la clave durante la comprobacion
    -- para coordinar insercion y cambio de estado.
SELECT estado
INTO estado_clave
FROM version_clave_bfa
WHERE id_clave = NEW.id_clave
  FOR SHARE;

IF NOT FOUND THEN

        RAISE EXCEPTION
            'No existe la clave de correccion indicada';
END IF;

    -- Solo agregar entradas a claves en BORRADOR.
    IF estado_clave <> 'BORRADOR' THEN

        RAISE EXCEPTION
            'Solo se pueden agregar entradas a claves en BORRADOR';
END IF;

RETURN NEW;

END;
$$;

CREATE TRIGGER trg_bfa_controlar_cambios_entrada
  BEFORE INSERT OR UPDATE
                     ON entrada_clave_bfa
                     FOR EACH ROW
                     EXECUTE FUNCTION bfa_controlar_cambios_entrada();
