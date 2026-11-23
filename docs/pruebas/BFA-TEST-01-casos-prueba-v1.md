# Casos de prueba BFA-TEST-01 — cliente de la API de BFA (PR #45) (v1)

## Identificación

- **Tarea de Testing:** [BFA-TEST-01](https://app.clickup.com/t/86bcw9z39) (coordina Víctor Cabrera; este lote lo atiende José Ariel Zeledón como apoyo)
- **Tarea de Desarrollo:** [BFA-FE-01](https://app.clickup.com/t/86bcvah6j), pieza 1 del #36: cliente y tipos de la API (PR #45, preparado por Kisnner Obando con el código de Jorddy Siezar)
- **Tester:** José Ariel Zeledón
- **Versión y fecha:** v1 · 2026-11-18
- **PR y SHA probados:** #45 · head `c789d13` (`pull/45/merge` `713479e` sobre `main` `d33f6a8`)
- **Alcance de esta versión:** solo el cliente y los tipos (`features/bfa/api/`). Se valida contra el contrato con respuestas simuladas. **No** es una prueba de pantallas, del navegador ni de endpoints reales, y **no** acredita las 13 subpruebas.
- **Resultado:** 18 PASÓ · 1 BLOQUEADO (TC-BFA-FE01-019, autenticación, pendiente de GEN-06) · 6 NO APLICA en esta pieza. Observaciones O-1 a O-4, que no bloquean.

## Historial de cambios

| Versión | Fecha | Cambios |
| --- | --- | --- |
| v1 | 2026-11-18 | Casos del cliente de la API de BFA (PR #45) y su ejecución sobre `pull/45/merge` `713479e`. |

## Fuentes y estado

| Fuente | Estado al 2026-11-18 | Uso en este documento |
| --- | --- | --- |
| Criterios de aceptación de BFA-FE-01 y BFA-TEST-01 (ClickUp), con las acciones de Testing del 17-nov | Publicados en las tareas | Alcance del lote y criterio aplicable |
| Documento general §5.2 (convenciones, formato de error) y `docs/api/openapi.yaml` (E1 a E8 y E24) | **Aprobado** ([DOC-05](https://app.clickup.com/t/86bctt9cy) `completed`) | Rutas, métodos, cuerpos, esquemas y errores |
| Cambio de contrato de E2 en el PR #51: `factor` y `tiempoLimiteSegundos` nulos | Ratificado por el Scrum Master de Desarrollo el 2026-11-17 en el #51; el #51 todavía no está fusionado | TC-BFA-FE01-002 y observación O-4 |
| Documento BFA §3: CU-BFA-P04 a P08 | **Aprobado** ([DOC-04](https://app.clickup.com/t/86bctt9cm) `completed`) | Trazabilidad del cliente |
| [DOC-07](https://app.clickup.com/t/86bctt9dn) y [DOC-08](https://app.clickup.com/t/86bcvaj7m) | `in progress` y `Open` | Dependencias de BFA-TEST-01 en ClickUp; no afectan al cliente, que no usa contenido de las subpruebas |
| Verificación de apoyo de Nelson Lazo (comentario del 2026-11-18 en BFA-TEST-01) | Registrada en ClickUp | Contraste: coincide con esta ejecución independiente |

## Matriz de cobertura

**BFA-FE-01 (criterios que aplican a la pieza 1)**

| Criterio | Casos | Estado |
| --- | --- | --- |
| Utiliza los contratos documentados en DOC-05 y supera las pruebas correspondientes. | TC-BFA-FE01-001 a 018 | PASÓ (con O-1 a O-4) |
| Maneja correctamente carga, recuperación y errores. | TC-BFA-FE01-015 a 017 (el cliente propaga los errores) | PASÓ en el cliente · la parte de pantalla va en las piezas 2 a 4b |
| La interfaz presenta las subpruebas de acuerdo con el orden y la configuración del backend. | TC-BFA-FE01-020 | NO APLICA en esta pieza |
| Las instrucciones y prácticas se muestran antes del inicio del tiempo oficial. | TC-BFA-FE01-021 | NO APLICA en esta pieza |
| No permite saltar subpruebas ni regresar a una que ya fue cerrada. | TC-BFA-FE01-022 | NO APLICA en esta pieza |
| El progreso y las transiciones reflejan los estados oficiales de la evaluación. | TC-BFA-FE01-023 | NO APLICA en esta pieza |
| Permite integrar las interfaces particulares de las familias sin duplicar componentes comunes. | TC-BFA-FE01-024 | NO APLICA en esta pieza |

**Pedido de BFA-TEST-01 para este lote (acciones del 17-nov)**

| Pedido | Casos |
| --- | --- |
| Contrastar E1 a E8 y E24 con el contrato vigente y con los nulos ratificados en el #51 | TC-BFA-FE01-002 a 012 |
| Correr las verificaciones frontend de la guía | TC-BFA-FE01-001 |
| Comprobar solicitudes y manejo de errores | TC-BFA-FE01-013 a 018 |
| Revisar respuestas de selección y palabras de FV con `ordenEntrada` | TC-BFA-FE01-010, 011 |

## Consultas pendientes

| ID | Consulta | Para | Casos afectados | Estado |
| --- | --- | --- | --- | --- |
| C-1 | `PreguntasSubpruebaResponse` declara `letraInicial`, que E6 no devuelve según el contrato (el código lo marca como «SUPUESTO» para retomar FV tras una caída). ¿Se agrega a E6 en DOC-05 o se quita del tipo? | Documentación (DOC-05) y Desarrollo | TC-BFA-FE01-002 (O-2) | Abierta |

## Datos de prueba (ficticios)

| Dato | Contenido |
| --- | --- |
| Test 1, evaluación 15 | Identificadores inventados para armar las rutas |
| Subpruebas `VOC1`, `FV` y `N1 A/B` | Códigos ficticios; el último sirve para comprobar que el código se codifica en la ruta |
| Sujeto 25, periodo 2, versión 3 | Cuerpo ficticio de E3 |
| Palabras `mesa` y `montaña` | Entradas ficticias de FV |
| Errores 400, 401, 403, 404, 409, 422, 500 y 503 | Cuerpos con el formato de §5.2.2 y mensajes inventados |

## Entorno

- **Código:** `pull/45/merge` `713479e` (PR + `main` `d33f6a8`), copiado aparte con `git archive`, sin tocar la rama del PR.
- **Herramientas:** Node v22.23.4, npm 10.9.9, `npm ci`.
- **Pruebas de verificación:** [`docs/evidencias/BFA-TEST-01/PR45-bfaApi.verificacion-testing.test.ts`](../evidencias/BFA-TEST-01/PR45-bfaApi.verificacion-testing.test.ts). Se copian a `frontend/src/features/bfa/api/` solo en la copia aparte y simulan `fetch`; no forman parte del PR. Testing no modifica el código.
- **Evidencia:** [`docs/evidencias/BFA-TEST-01/PR45-verificacion-2026-11-18.txt`](../evidencias/BFA-TEST-01/PR45-verificacion-2026-11-18.txt).

## Casos — Verificaciones y tipos

### TC-BFA-FE01-001 — Verificaciones frontend

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-BFA-FE01-001 | BFA-FE-01: "Utiliza los contratos documentados en DOC-05 y supera las pruebas correspondientes." | Copia de `pull/45/merge` con `npm ci` | 1. `npm run typecheck`, `lint`, `format:check`, `test` y `build` desde `frontend/`. | Todo sin errores. | Automatizado (local) | **PASÓ**: typecheck, lint y formato sin errores; 111 de 111 pruebas (14 archivos); build correcto. |

### TC-BFA-FE01-002 — Tipos contra los esquemas del contrato

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-BFA-FE01-002 | BFA-FE-01: "Utiliza los contratos documentados en DOC-05…" | `tipos.ts` y `openapi.yaml` (con el cambio de E2 del #51) | 1. Comparar cada tipo con su esquema: `TestResumenResponse`, `TestDetalleResponse`, `ConfiguracionSubprueba`, `IniciarEvaluacionRequest`, `EvaluacionResponse`, `SubpruebaActivaResponse`, `PreguntasSubpruebaResponse`, `PalabraFV`, `RegistrarRespuestasRequest`, `AvanceSubpruebaResponse`, `InstruccionesResponse`, `EjemploPractica` y `OpcionRespuesta`. | Mismos campos, tipos y nulos. Los obligatorios de E3 (`versionFormularioId`, `sujetoId`, `periodoId`) son obligatorios en el tipo. | Revisión de código | **PASÓ con observaciones**: todos los campos del contrato están, con su tipo; `factor` y `tiempoLimiteSegundos` ya son nulos (#51). Ver O-1 a O-4. |

## Casos — Rutas, métodos y cuerpos

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-BFA-FE01-003 | DOC-05: E1 | `fetch` simulado | `listarTests()` | `GET /api/v1/tests`, sin cuerpo | Automatizado | **PASÓ** |
| TC-BFA-FE01-004 | DOC-05: E2 | Test 1 | `obtenerTest(1)` | `GET /api/v1/tests/1`, sin cuerpo | Automatizado | **PASÓ** |
| TC-BFA-FE01-005 | DOC-05: E3 | Cuerpo ficticio con `versionFormularioId`, `sujetoId`, `periodoId`, `subpruebas` y `fluidez.letraInicial` | `iniciarEvaluacion(cuerpo)` | `POST /api/v1/evaluaciones` con el mismo JSON | Automatizado | **PASÓ** |
| TC-BFA-FE01-006 | DOC-05: E4 | Evaluación 15 | `obtenerEvaluacion(15)` | `GET /api/v1/evaluaciones/15` | Automatizado | **PASÓ** |
| TC-BFA-FE01-007 | DOC-05: E24 | Evaluación 15, `VOC1` | `consultarInstrucciones(15, 'VOC1')` | `GET …/15/subpruebas/VOC1/instrucciones` | Automatizado | **PASÓ** |
| TC-BFA-FE01-008 | DOC-05: E5 | Evaluación 15, `VOC1` | `iniciarSubprueba(15, 'VOC1')` | `POST …/subpruebas/VOC1/iniciar`, sin cuerpo | Automatizado | **PASÓ** |
| TC-BFA-FE01-009 | DOC-05: E6 | Evaluación 15, `VOC1` | `obtenerPreguntas(15, 'VOC1')` | `GET …/subpruebas/VOC1/preguntas` | Automatizado | **PASÓ** |
| TC-BFA-FE01-010 | DOC-05: E7, selección: `respuestas` con `preguntaId` y `opcion` («La letra que devuelve E6») | Dos respuestas ficticias | `registrarRespuestas(15, 'VOC1', {respuestas})` | `PUT …/subpruebas/VOC1/respuestas` con el lote | Automatizado | **PASÓ** |
| TC-BFA-FE01-011 | DOC-05: E7, FV: `palabras` con `ordenEntrada` y `texto` (§5.4.1: «FV exige texto/ordenEntrada y sin pregunta/opción») | Palabras `mesa` y `montaña` | `registrarRespuestas(15, 'FV', {palabras})` | `PUT …/subpruebas/FV/respuestas` con `palabras` y sin `preguntaId` ni `opcion` | Automatizado | **PASÓ** (también lo cubre la prueba del PR) |
| TC-BFA-FE01-012 | DOC-05: E8 | Evaluación 15 | `finalizarEvaluacion(15)` | `POST …/15/finalizar`, sin cuerpo | Automatizado | **PASÓ** |

## Casos — Formato de la solicitud y errores

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-BFA-FE01-013 | DOC-05 (rutas con `{codigo}`) | Código `N1 A/B` | `obtenerPreguntas(15, 'N1 A/B')` | El código va codificado: `…/subpruebas/N1%20A%2FB/preguntas` | Automatizado | **PASÓ** |
| TC-BFA-FE01-014 | §5.2.1: "JSON en UTF-8 (Content-Type: application/json)" | — | Cualquier llamada | Cabecera `Content-Type: application/json` | Automatizado | **PASÓ** |
| TC-BFA-FE01-015 | §5.2.2: cuerpo de error común; "`codigo` es estable y el frontend puede decidir por él" | Respuestas 400, 401, 403, 404, 409, 422 y 500 con el cuerpo del contrato | `obtenerEvaluacion(15)` | Rechaza con `ApiError`, con el `status` y el `codigo` del cuerpo | Automatizado | **PASÓ** (7 de 7) |
| TC-BFA-FE01-016 | §5.2.2 (errores) | 503 con cuerpo HTML | `obtenerTest(1)` | `ApiError` con status 503 y mensaje «HTTP 503» | Automatizado | **PASÓ** |
| TC-BFA-FE01-017 | BFA-FE-01: "Maneja correctamente carga, recuperación y errores." | `fetch` rechaza (sin conexión) | `registrarRespuestas(...)` | La promesa se rechaza; nunca se da por guardado | Automatizado | **PASÓ** |
| TC-BFA-FE01-018 | DOC-05: E7 200 `AvanceSubpruebaResponse` | Respuesta 200 ficticia | `registrarRespuestas(...)` | Devuelve el cuerpo tal cual | Automatizado | **PASÓ** |
| TC-BFA-FE01-019 | §5.2.1: "Sesión con token Bearer en la cabecera Authorization… Toda operación protegida exige sesión válida" | — | Cualquier llamada | Envía `Authorization: Bearer <token>` | Automatizado | **BLOQUEADO**: hoy no se envía. Lo hace `services/http.ts`, que no es parte de este PR, y depende de GEN-06 (el propio PR lo avisa). |

## Casos que no aplican a esta pieza

Los criterios de pantalla de BFA-FE-01 se prueban en las piezas 2 a 4b del #36, cuando Integración confirme el corte.

| ID | CA cubierto | Estado |
| --- | --- | --- |
| TC-BFA-FE01-020 | "La interfaz presenta las subpruebas de acuerdo con el orden y la configuración del backend." | **NO APLICA** (piezas 3, 4a y 4b) |
| TC-BFA-FE01-021 | "Las instrucciones y prácticas se muestran antes del inicio del tiempo oficial." | **NO APLICA** (piezas 3 y 4b) |
| TC-BFA-FE01-022 | "No permite saltar subpruebas ni regresar a una que ya fue cerrada." | **NO APLICA** (pieza 3) |
| TC-BFA-FE01-023 | "El progreso y las transiciones reflejan los estados oficiales de la evaluación." | **NO APLICA** (piezas 3 y 4b) |
| TC-BFA-FE01-024 | "Permite integrar las interfaces particulares de las familias sin duplicar componentes comunes." | **NO APLICA** (piezas 4a y 4b) |
| TC-BFA-FE01-025 | Recuperación y errores en pantalla | **NO APLICA** (piezas 2 a 4b) |

## Observaciones (no bloquean)

| ID | Observación | Dónde |
| --- | --- | --- |
| O-1 | El contrato marca como `nullable` a `EvaluacionResponse.resultadoId`, `EvaluacionResponse.estadoResultado` y `SubpruebaActivaResponse.letraInicial`. En los tipos están como opcionales (`?: number`, `?: string`), sin `| null`. Si el backend manda `null`, el tipo no lo refleja. No cambia ninguna solicitud. | `tipos.ts` |
| O-2 | `PreguntasSubpruebaResponse.letraInicial` no está en el contrato de E6 (el código lo marca como «SUPUESTO»). Es opcional y el cliente funciona sin él. Ver C-1. | `tipos.ts` |
| O-3 | El comentario inicial de `tipos.ts` dice que se verificó contra el `openapi.yaml` «del PR #37 (aún sin fusionar)». El #37 ya está en `main` desde el 2026-11-10. | `tipos.ts`, línea 2 |
| O-4 | `factor` y `tiempoLimiteSegundos` nulos dependen del #51, que todavía no está fusionado. Si el #51 cambia ese punto, hay que repetir TC-BFA-FE01-002. | `tipos.ts` |

## Contraste con la verificación de apoyo (Nelson Lazo, 2026-11-18)

Nelson informó sobre el mismo `pull/45/merge` (`713479e`): typecheck, lint, formato, 111 de 111 pruebas, build y 17 comprobaciones de apoyo correctas (rutas, métodos y cuerpos de E1 a E8 y E24, selección y palabras con `ordenEntrada`, escape del código, propagación de 401, 403, 404, 409 y 500, y fallo de conexión). Esta ejecución independiente obtiene lo mismo y suma 400, 422, 503 sin cuerpo JSON, `Content-Type`, la respuesta correcta y la revisión de tipos (TC-BFA-FE01-002).

## Registro de ejecución

| Fecha | PR/SHA | Tester | Resultado | Evidencia / bug |
| --- | --- | --- | --- | --- |
| 2026-11-18 | #45 / `c789d13` (`pull/45/merge` `713479e`) | José Ariel Zeledón | 18 PASÓ · 1 BLOQUEADO (019) · 6 NO APLICA. Observaciones O-1 a O-4. | [salida](../evidencias/BFA-TEST-01/PR45-verificacion-2026-11-18.txt) · [pruebas de verificación](../evidencias/BFA-TEST-01/PR45-bfaApi.verificacion-testing.test.ts) |

### Revisión final

- [x] Cada caso corresponde a un criterio aprobado y a la tarea correcta.
- [x] Se ejecutó contra el commit vigente del PR (`c789d13`).
- [x] Se registraron resultados reales y evidencia verificable.
- [x] No contiene secretos ni datos personales reales.
- [ ] Se comentó al PR y se informó a Integración.
- [ ] Solo después de aprobar, se colocó o solicitó `testing-ok` cuando corresponde.
