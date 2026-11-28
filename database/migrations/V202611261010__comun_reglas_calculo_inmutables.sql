-- conserva la identidad de las reglas que originaron un cálculo o una revisión publicada.
-- retirar una regla impide nuevos cálculos sin cambiar su trazabilidad histórica.
CREATE FUNCTION proteger_regla_calificacion_utilizada()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM revision_resultado
        WHERE id_regla_calificacion = OLD.id_regla_calificacion
          AND estado_revision IN ('CALCULADA', 'VALIDADA')
    ) AND (
        ROW(NEW.id_version_formulario, NEW.version, NEW.referencia_implementacion,
            NEW.referencia_especificacion, NEW.evidencia_aprobacion)
        IS DISTINCT FROM
        ROW(OLD.id_version_formulario, OLD.version, OLD.referencia_implementacion,
            OLD.referencia_especificacion, OLD.evidencia_aprobacion)
        OR NEW.estado = 'BORRADOR'
    ) THEN
        RAISE EXCEPTION 'La regla utilizada es inmutable; cree otra versión.'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER trg_proteger_regla_calificacion_utilizada
BEFORE UPDATE ON regla_calificacion
FOR EACH ROW EXECUTE FUNCTION proteger_regla_calificacion_utilizada();
