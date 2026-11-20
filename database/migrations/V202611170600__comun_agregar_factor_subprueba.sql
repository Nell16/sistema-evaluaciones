-- GEN-01-BE: factor al que aporta cada subprueba (Documento BFA §5.3); E2 lo expone.
-- Nulo mientras no se cargue; no altera filas existentes.
ALTER TABLE subprueba ADD COLUMN factor VARCHAR(80);

ALTER TABLE subprueba
  ADD CONSTRAINT chk_subprueba_factor
    CHECK (factor IS NULL OR BTRIM(factor) <> '');

-- El factor tambien queda protegido en subpruebas de versiones publicadas o retiradas.
CREATE OR REPLACE FUNCTION proteger_subprueba()
  RETURNS TRIGGER
  LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.id_subprueba IS DISTINCT FROM OLD.id_subprueba
       OR NEW.id_test IS DISTINCT FROM OLD.id_test THEN
        RAISE EXCEPTION
            'No se puede cambiar la identidad o el test de una subprueba';
    END IF;

    IF ROW(NEW.codigo, NEW.nombre, NEW.descripcion, NEW.factor)
       IS DISTINCT FROM
       ROW(OLD.codigo, OLD.nombre, OLD.descripcion, OLD.factor)
       AND EXISTS (
           SELECT 1
           FROM configuracion_subprueba cs
           JOIN version_formulario vf
             ON vf.id_version_formulario = cs.id_version_formulario
           WHERE cs.id_subprueba = OLD.id_subprueba
             AND vf.estado IN ('PUBLICADA', 'RETIRADA')
       ) THEN
        RAISE EXCEPTION
            'No se puede redefinir una subprueba utilizada en versiones publicadas';
    END IF;

    RETURN NEW;
END;
$$;
