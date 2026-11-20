
-- =========================================================
-- BFA-BE-09
-- Proteccion contra eliminacion de claves historicas
--
-- Las claves APROBADAS y RETIRADAS no pueden eliminarse.
-- Tampoco pueden eliminarse sus entradas de correccion.
--
-- Las claves en BORRADOR mantienen la posibilidad
-- de eliminarse, respetando sus llaves foraneas.
-- =========================================================


-- 1. Proteger las claves aprobadas o retiradas.

CREATE OR REPLACE FUNCTION bfa_impedir_eliminar_clave_protegida()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF OLD.estado IN ('APROBADA', 'RETIRADA') THEN
        RAISE EXCEPTION
            'No se permite eliminar claves protegidas';
END IF;

RETURN OLD;

END;
$$;


CREATE TRIGGER trg_bfa_impedir_eliminar_clave_protegida
  BEFORE DELETE ON version_clave_bfa
  FOR EACH ROW
  EXECUTE FUNCTION bfa_impedir_eliminar_clave_protegida();


-- 2. Proteger las entradas de claves aprobadas o retiradas.

CREATE OR REPLACE FUNCTION bfa_impedir_eliminar_entrada_protegida()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
v_estado VARCHAR(20);
BEGIN

SELECT estado
INTO v_estado
FROM version_clave_bfa
WHERE id_clave = OLD.id_clave
  FOR SHARE;

IF v_estado IN ('APROBADA', 'RETIRADA') THEN
        RAISE EXCEPTION
            'No se permite eliminar entradas de claves protegidas';
END IF;

RETURN OLD;

END;
$$;


CREATE TRIGGER trg_bfa_impedir_eliminar_entrada_protegida
  BEFORE DELETE ON entrada_clave_bfa
  FOR EACH ROW
  EXECUTE FUNCTION bfa_impedir_eliminar_entrada_protegida();
