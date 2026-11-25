-- Datos ficticios para TC-GEN-01 (sin ítems reales de pruebas psicológicas).
BEGIN;

-- T1: activo, dos ediciones publicadas y un borrador
INSERT INTO test (codigo, nombre, activo) VALUES ('TC01-T1', 'Test ficticio uno', TRUE);
INSERT INTO subprueba (id_test, codigo, nombre, factor)
SELECT id_test, x.codigo, x.nombre, x.factor
FROM test, (VALUES ('SA', 'Subprueba A', 'FA'), ('SB', 'Subprueba B', 'FA'), ('SC', 'Subprueba C', 'FC'))
     AS x(codigo, nombre, factor)
WHERE test.codigo = 'TC01-T1';

INSERT INTO version_formulario (id_test, codigo_version, fuente_ref)
SELECT id_test, v, 'FUENTE-FICTICIA' FROM test, (VALUES ('v1'), ('v2'), ('v3')) AS x(v)
WHERE test.codigo = 'TC01-T1';

-- v1: una sola configuración
INSERT INTO configuracion_subprueba
  (id_version_formulario, id_subprueba, orden_aplicacion, tiempo_limite_segundos, tipo_respuesta, instrucciones_ref)
SELECT vf.id_version_formulario, s.id_subprueba, 1, 60, 'SELECCION', 'INS-V1'
FROM version_formulario vf JOIN subprueba s ON s.id_test = vf.id_test
WHERE vf.codigo_version = 'v1' AND s.codigo = 'SA'
  AND vf.id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1');

-- v2: tres configuraciones insertadas fuera de orden
INSERT INTO configuracion_subprueba
  (id_version_formulario, id_subprueba, orden_aplicacion, tiempo_limite_segundos, tipo_respuesta,
   instrucciones_ref, practica_ref, maximo_entradas)
SELECT vf.id_version_formulario, s.id_subprueba, x.orden, x.tiempo, x.tipo, 'INS-V2', x.practica, x.maximo
FROM version_formulario vf
JOIN subprueba s ON s.id_test = vf.id_test
JOIN (VALUES ('SC', 3, 120, 'TEXTO_LIBRE', 'PRA-C', 20),
             ('SA', 1, 180, 'SELECCION', 'PRA-A', NULL),
             ('SB', 2, NULL, 'SELECCION', NULL, NULL))
     AS x(codigo, orden, tiempo, tipo, practica, maximo) ON x.codigo = s.codigo
WHERE vf.codigo_version = 'v2' AND vf.id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1');

-- Preguntas: SA (v2) 3 preguntas x 4 opciones; SB (v2) 2 preguntas con 3 y 4 opciones; SA (v1) 1 pregunta
INSERT INTO pregunta (id_configuracion_subprueba, numero_visible, orden_presentacion, enunciado)
SELECT cs.id_configuracion_subprueba, n::text, n, 'Pregunta ficticia ' || n
FROM configuracion_subprueba cs
JOIN subprueba s ON s.id_subprueba = cs.id_subprueba
JOIN version_formulario vf ON vf.id_version_formulario = cs.id_version_formulario
CROSS JOIN generate_series(1, 3) n
WHERE s.codigo IN ('SA', 'SB') AND vf.id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1')
  AND ((vf.codigo_version = 'v2' AND (s.codigo = 'SA' OR n <= 2)) OR (vf.codigo_version = 'v1' AND n = 1));

INSERT INTO opcion_respuesta (id_pregunta, codigo, orden_presentacion, texto)
SELECT p.id_pregunta, chr(64 + o), o, 'Opción ficticia ' || chr(64 + o)
FROM pregunta p
JOIN configuracion_subprueba cs ON cs.id_configuracion_subprueba = p.id_configuracion_subprueba
JOIN subprueba s ON s.id_subprueba = cs.id_subprueba
CROSS JOIN generate_series(1, 4) o
WHERE s.id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1')
  AND NOT (s.codigo = 'SB' AND p.numero_visible = '1' AND o = 4);

UPDATE version_formulario SET estado = 'PUBLICADA', publicada_en = TIMESTAMPTZ '2026-11-01 10:00+00', evidencia_validacion = 'ACTA-FICTICIA-1'
WHERE codigo_version = 'v1' AND id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1');
UPDATE version_formulario SET estado = 'PUBLICADA', publicada_en = TIMESTAMPTZ '2026-11-10 10:00+00', evidencia_validacion = 'ACTA-FICTICIA-2'
WHERE codigo_version = 'v2' AND id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T1');

-- T2: activo, solo borrador. T3: inactivo con edición publicada
INSERT INTO test (codigo, nombre, activo) VALUES ('TC01-T2', 'Test ficticio dos', TRUE), ('TC01-T3', 'Test ficticio tres', FALSE);
INSERT INTO subprueba (id_test, codigo, nombre) SELECT id_test, 'SX', 'Subprueba X' FROM test WHERE codigo IN ('TC01-T2', 'TC01-T3');
INSERT INTO version_formulario (id_test, codigo_version, fuente_ref)
SELECT id_test, 'v1', 'FUENTE-FICTICIA' FROM test WHERE codigo IN ('TC01-T2', 'TC01-T3');
INSERT INTO configuracion_subprueba (id_version_formulario, id_subprueba, orden_aplicacion, tipo_respuesta, instrucciones_ref)
SELECT vf.id_version_formulario, s.id_subprueba, 1, 'SELECCION', 'INS'
FROM version_formulario vf JOIN subprueba s ON s.id_test = vf.id_test
WHERE vf.id_test IN (SELECT id_test FROM test WHERE codigo IN ('TC01-T2', 'TC01-T3'));
UPDATE version_formulario SET estado = 'PUBLICADA', publicada_en = TIMESTAMPTZ '2026-11-05 10:00+00', evidencia_validacion = 'ACTA-FICTICIA-3'
WHERE id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T3');

-- T4: activo, edición publicada con una subprueba sin factor cargado
INSERT INTO test (codigo, nombre, activo) VALUES ('TC01-T4', 'Test ficticio cuatro', TRUE);
INSERT INTO subprueba (id_test, codigo, nombre) SELECT id_test, 'SY', 'Subprueba Y' FROM test WHERE codigo = 'TC01-T4';
INSERT INTO version_formulario (id_test, codigo_version, fuente_ref)
SELECT id_test, 'v1', 'FUENTE-FICTICIA' FROM test WHERE codigo = 'TC01-T4';
INSERT INTO configuracion_subprueba (id_version_formulario, id_subprueba, orden_aplicacion, tipo_respuesta, instrucciones_ref)
SELECT vf.id_version_formulario, s.id_subprueba, 1, 'SELECCION', 'INS'
FROM version_formulario vf JOIN subprueba s ON s.id_test = vf.id_test
WHERE vf.id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T4');
UPDATE version_formulario SET estado = 'PUBLICADA', publicada_en = TIMESTAMPTZ '2026-11-05 10:00+00', evidencia_validacion = 'ACTA-FICTICIA-4'
WHERE id_test = (SELECT id_test FROM test WHERE codigo = 'TC01-T4');

COMMIT;

-- id de TC01-T3 (inactivo): valor de la variable t3Id de la colección

SELECT id_test AS t3_id FROM test WHERE codigo = 'TC01-T3';
