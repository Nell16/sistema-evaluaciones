\echo == TC-GEN-01-012
-- TC-GEN-01-012: factor de subprueba usada en edición publicada → rechazo
UPDATE subprueba SET factor = 'FZ'
WHERE codigo = 'SA' AND id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1');
\echo == TC-GEN-01-013
-- TC-GEN-01-013: factor de subprueba solo en borrador → se permite (se revierte)
BEGIN;
UPDATE subprueba SET factor = 'FX'
WHERE codigo = 'SX' AND id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T2');
SELECT codigo, factor FROM subprueba WHERE id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T2');
ROLLBACK;
\echo == TC-GEN-01-014
-- TC-GEN-01-014: factor vacío → rechazo por chk_subprueba_factor
UPDATE subprueba SET factor = '  '
WHERE codigo = 'SX' AND id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T2');
