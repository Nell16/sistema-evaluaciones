# Casos de prueba GEN-02 — iniciar y retomar una evaluación (v1)

## Identificación

- **Tarea de Testing:** [GEN-02-TEST](https://app.clickup.com/t/86bcw9z32). Responsable desde el reparto del 17-nov: José Ariel Zeledón, en relevo de Víctor Cabrera.
- **Tareas de Desarrollo:** [GEN-02-FE](https://app.clickup.com/t/86bcvah53) (Daysi Miranda, con Andrea Cuadra y Maura Quintanilla) y [GEN-02-BE](https://app.clickup.com/t/86bcvah52) (Juan Carlos Robleda)
- **Historia:** [GEN-02](https://app.clickup.com/t/86bcvah50)
- **Tester:** José Ariel Zeledón
- **Versión y fecha:** v1 · 2026-11-18
- **Código probado:** `main` `d33f6a8`, interfaz de `frontend/src/features/evaluacion/` y `services/evaluacion*`, con los mocks por defecto. No es un PR: Integración informó que los archivos del PR #27 ya están en `main` por otra entrega, y decide su cierre.
- **Alcance de esta versión:** regresión de interfaz con mocks, como pide la tarea. **No** valida el backend real (E3 y E4 de GEN-02-BE todavía no existen), ni permisos reales, ni que la evaluación conserve su versión en el backend.
- **Resultado:** 25 PASÓ (TC-GEN-02-001 a 024 y 029) · 4 BLOQUEADO (025 a 028). Observación O-1, que no bloquea. **No** se emite `testing-ok`: no hay un PR vigente que probar.

## Historial de cambios

| Versión | Fecha | Cambios |
| --- | --- | --- |
| v1 | 2026-11-18 | Casos de inicio y recuperación y su ejecución sobre `main` `d33f6a8` con mocks: recorrido en navegador con capturas y pruebas automáticas. |

## Fuentes y estado

| Fuente | Estado al 2026-11-18 | Uso en este documento |
| --- | --- | --- |
| Criterios de GEN-02-TEST, GEN-02-FE y GEN-02-BE, y las acciones de Testing del 17-nov (ClickUp) | Publicados en las tareas | Base de todos los casos y alcance de la regresión |
| Documento BFA §5 y `docs/api/openapi.yaml`: E2, E3, E4, E19 y E21, con sus errores | **Aprobado** ([DOC-05](https://app.clickup.com/t/86bctt9cy) `completed`) | Códigos de error esperados |
| Documento general §5.2.2 (formato de error y códigos HTTP) | **Aprobado** (DOC-05) | Categorías de error |
| CU-BFA-P03 y CU-BFA-P04 (Documento general y resumen del Documento BFA §3) y CU-GEN-19, revisados el 2026-11-18 | **Aprobado** ([DOC-04](https://app.clickup.com/t/86bctt9cm) `completed`) | Iniciar, recuperar, no duplicar y no reabrir |
| Cambio de E2 del PR #51 (`factor` y `tiempoLimiteSegundos` nulos) | Ratificado el 2026-11-17; el #51 todavía no está fusionado | Observación O-1 |
| [DOC-07](https://app.clickup.com/t/86bctt9dn) y [DOC-08](https://app.clickup.com/t/86bcvaj7m) | `in progress` y `Open` | Dependencia de GEN-02-TEST en ClickUp; no cambia lo que se prueba con mocks |
| Verificación de apoyo de Nelson Lazo (comentario del 2026-11-18 en GEN-02-TEST) | Registrada en ClickUp | Typecheck, 109 pruebas y build de `main`; sin prueba de navegador |

Los resultados esperados salen de los criterios y del contrato. Los textos de la pantalla que aparecen en «Obtenido» son lo observado, no un requisito.

## Matriz de cobertura

**GEN-02-TEST**

| Criterio | Casos | Estado |
| --- | --- | --- |
| Iniciar o retomar no duplica intentos ni reinicia los tiempos oficiales. | TC-GEN-02-013 a 018 (interfaz); TC-GEN-02-025, 026 (backend) | PASÓ con mocks · BLOQUEADO en backend |
| La interfaz solo avanza cuando el backend confirma. | TC-GEN-02-004 a 013, 015 a 017, 020 a 024 | PASÓ |
| Esquemas, códigos HTTP, campos y errores cumplen el contrato aprobado. | TC-GEN-02-029 (cliente y mock); TC-GEN-02-025, 026 (backend) | PASÓ en el cliente · BLOQUEADO en backend |
| Cada criterio de las subtareas del grupo tiene al menos un caso `TC-`. | Esta matriz | Cumplido |
| Cada PR del grupo tiene `testing-ok` o sus fallos comentados antes de la fusión. | — | Sin PR vigente: el #27 ya está en `main`; GEN-02-BE no tiene PR |

**GEN-02-FE**

| Criterio | Casos | Estado |
| --- | --- | --- |
| El usuario autorizado puede iniciar una evaluación con datos válidos. | TC-GEN-02-010 a 012 | PASÓ (con mocks; la autorización real es TC-028) |
| La interfaz presenta las configuraciones permitidas para el test seleccionado. | TC-GEN-02-001 a 003 | PASÓ |
| Solo permite avanzar cuando el backend confirma la creación o recuperación. | TC-GEN-02-006, 013, 015 a 017, 020, 024 | PASÓ |
| Al retomar una evaluación, se muestra el progreso y estado devueltos por la API. | TC-GEN-02-018, 019 | PASÓ |
| Se manejan correctamente errores de validación, acceso y conexión. | TC-GEN-02-002 a 009, 020 a 024 | PASÓ |
| La interfaz puede desarrollarse con mocks de DOC-05 e integrarse posteriormente con GEN-02-BE. | TC-GEN-02-029 (cliente real y mock con el mismo contrato); TC-GEN-02-025, 026 | PASÓ con mocks · BLOQUEADO la integración |

**GEN-02-BE**

| Criterio | Casos | Estado |
| --- | --- | --- |
| Cumplir E3 y E4; crear y recuperar evaluaciones desde la base de datos. | TC-GEN-02-025, 026 | BLOQUEADO (sin PR) |
| Rechazar datos o configuraciones inválidas y accesos no autorizados. | TC-GEN-02-025, 028 | BLOQUEADO |
| Ejecutar correctamente las pruebas automatizadas. | TC-GEN-02-025 | BLOQUEADO |

## Consultas pendientes

Sin consultas nuevas. Lo que depende del backend queda bloqueado hasta que exista el PR de GEN-02-BE.

## Datos de prueba (ficticios)

Son los del mock `services/evaluacion.mock.ts`, que siguen los ejemplos de DOC-05. No hay datos personales reales.

| Dato | Contenido | Usado en |
| --- | --- | --- |
| Test 1 (BFA), versión publicada 10 | 13 subpruebas en orden oficial | Todos |
| Sujetos 25 y 26 | Existen; el 25 ya tiene la evaluación 15 abierta en 2026-2 | 010 a 016 |
| Sujeto 409 | Existe; E3 responde `EVALUACION_EN_CURSO` | 017 |
| Sujetos 403, 500 y 503 | `SIN_PERMISO`, `ERROR_INTERNO` y fallo de conexión | 022 a 024 |
| Sujeto 99 | No existe | 007 |
| Períodos 3 (2026-2) y 4 (2027-1) | Disponibles; el 2 (2026-1) no se ofrece | 001, 010 a 017 |
| Evaluación 15 | En curso: VOC1 cerrada, VOC2 activa con 214 s restantes, CV pendiente | 018 |
| Evaluación 16 | Finalizada | 019 |
| Evaluaciones 99, 403, 500 y 503 | No existe, sin permiso, error del servidor y sin conexión | 020, 022 a 024 |

## Entorno

- **Código:** `main` `d33f6a8`, copiado aparte con `git archive`. `npm ci` y `npm run build` en la laptop del tester (Node v22.23.4, npm 10.9.9), sin `.env.local`, así que se usan los mocks (`VITE_USAR_MOCKS` distinto de `false`).
- **Navegador:** la compilación se sirvió en local y se recorrió con Chromium (Playwright) con el script [`docs/evidencias/GEN-02/GEN-02-recorrido-interfaz.js`](../evidencias/GEN-02/GEN-02-recorrido-interfaz.js). Cada caso abre una página nueva, así que el mock empieza siempre desde cero. El mock tarda unos 700 ms en responder, como una red lenta.
- **Evidencia:** [`GEN-02-ejecucion-2026-11-18.txt`](../evidencias/GEN-02/GEN-02-ejecucion-2026-11-18.txt), [`GEN-02-resultados-2026-11-18.json`](../evidencias/GEN-02/GEN-02-resultados-2026-11-18.json) y una captura por caso en [`docs/evidencias/GEN-02/capturas/`](../evidencias/GEN-02/capturas/).
- **«Continuar con la evaluación»** lleva hoy a una página pendiente (`evaluaciones/:id`); la aplicación de subpruebas es de otra tarea. No se confunde con el flujo completo.

## Casos — Carga y configuraciones

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-001 | GEN-02-FE: "La interfaz presenta las configuraciones permitidas para el test seleccionado." · E2 | `?testId=1` | 1. Abrir `/evaluaciones/inicio?testId=1`. | Se ven las subpruebas de la edición publicada, en orden de aplicación, y solo los períodos disponibles. | 13 subpruebas en orden 1 a 13 (VOC1 a FV) con tiempo, tipo de respuesta y preguntas; se ofrecen 2026-2 y 2027-1, no 2026-1. | **PASÓ** |
| TC-GEN-02-002 | GEN-02-FE: errores de validación | `?testId=abc` y `?testId=0` | 1. Abrir cada URL. | Un mensaje de validación y ningún formulario. | «El ID del test debe ser un número entero positivo. Vuelva al catálogo…»; no muestra el formulario. | **PASÓ** |
| TC-GEN-02-003 | E2: `404 TEST_NO_ENCONTRADO` | `?testId=2` | 1. Abrir la URL. | Aviso de test no encontrado. | «No encontrado: El test seleccionado no existe o no está activo.», con reintentar. | **PASÓ** |

## Casos — Validaciones del formulario

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-004 | GEN-02-FE: errores de validación; "Solo permite avanzar cuando el backend confirma…" | Formulario vacío | 1. «Iniciar evaluación». | Errores por campo obligatorio y nada creado. E3 exige `sujetoId`, `periodoId` y `versionFormularioId`. | Errores en evaluado, período y letra FV; no se crea nada. | **PASÓ** |
| TC-GEN-02-005 | GEN-02-FE: errores de validación | ID `abc` | 1. Escribir `abc` y «Buscar». | Error de formato, sin consultar. | «El ID del evaluado debe ser un número entero positivo.» | **PASÓ** |
| TC-GEN-02-006 | GEN-02-FE: "Solo permite avanzar cuando el backend confirma…" | ID 26 escrito pero sin «Buscar» | 1. Llenar período y letra. 2. «Iniciar evaluación». | No avanza sin un evaluado confirmado por el servidor. | «Busque y confirme al evaluado antes de iniciar.» | **PASÓ** |
| TC-GEN-02-007 | E19: `404 SUJETO_NO_ENCONTRADO` | Sujeto 99 | 1. «Buscar». | Aviso de evaluado no encontrado. | «No encontrado: No existe un evaluado con ese ID. Debe registrarse antes de evaluarlo.» | **PASÓ** |
| TC-GEN-02-008 | CU-BFA-P03 (Documento BFA §3): "Elegir versión y subconjunto no vacío, o todas las configuraciones" | Sujeto 26, 2026-2, parcial | 1. Sin marcar subpruebas, «Iniciar evaluación». | No avanza. | «Seleccione al menos una subprueba.» | **PASÓ** |
| TC-GEN-02-009 | E3: FV exige `fluidez.letraInicial` (`409 LETRA_NO_ASIGNADA`) | Sujeto 26, 2026-2, completa | 1. Letra vacía. 2. Letra «5». 3. Intentar escribir «ab». | No avanza sin una letra válida. | «Fluidez Verbal requiere una sola letra inicial.» con vacía y con «5»; el campo solo admite un carácter («ab» queda «a»). | **PASÓ** |

## Casos — Iniciar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-010 | GEN-02-FE: "El usuario autorizado puede iniciar una evaluación con datos válidos." · E3 201 | Sujeto 26, 2026-2, completa, letra «m» | 1. «Buscar». 2. «Iniciar evaluación». | Se muestra la evaluación que devolvió el servidor: EN_CURSO, todas las subpruebas PENDIENTE. | «Evaluación creada», En curso, #100, 13 subpruebas Pendiente, progreso 0 de 13. | **PASÓ** |
| TC-GEN-02-011 | E3: "con subpruebas solo se aplican esas, en el orden oficial." | Parcial: marcar CV y luego VOC1 | 1. «Iniciar evaluación». | Solo las elegidas, en el orden oficial; sin letra porque no hay FV. | Creada con VOC1 y CV en ese orden, sin pedir letra; progreso 0 de 2. | **PASÓ** |
| TC-GEN-02-012 | E3: FV exige letra | Parcial con FV | 1. Sin letra, iniciar. 2. Letra «S», iniciar. | Pide la letra; con letra se crea. | La letra aparece al marcar FV; sin ella no avanza; con «S» se crea con FV. | **PASÓ** |
| TC-GEN-02-013 | GEN-02-FE: "Solo permite avanzar cuando el backend confirma…" · GEN-02-TEST: no duplicar | Datos válidos | 1. «Iniciar evaluación». 2. Durante la espera, intentar otro clic. | No avanza antes de la respuesta ni crea dos. | Campos y botón deshabilitados, «Creando la evaluación en el servidor…», sin «creada»; un clic forzado no crea otra (queda #100). | **PASÓ** |

## Casos — No duplicar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-014 | GEN-02-TEST: "Iniciar o retomar no duplica intentos…" · CU-BFA-P03 (Documento BFA §3): "reintento técnico no crea otra aplicación" · CU-BFA-P03 (Documento general): "Dos solicitudes equivalentes no generan dos evaluaciones abiertas para el mismo contexto." | Datos válidos | 1. Doble clic en «Iniciar evaluación». 2. Volver a iniciar con el mismo evaluado y período. | Una sola evaluación; el segundo intento se rechaza. | Una sola (#100); el nuevo intento da el aviso de evaluación abierta. | **PASÓ** |
| TC-GEN-02-015 | E3: `409 EVALUACION_EN_CURSO` ("el sujeto ya tiene una abierta de ese test en ese periodo") | Evaluación creada para 26 en 2026-2 | 1. «Iniciar otra evaluación» con los mismos datos. | Aviso de conflicto; no se crea otra. | «No se puede continuar: El evaluado ya tiene una evaluación abierta… Retómela en lugar de iniciar otra.» | **PASÓ** |
| TC-GEN-02-016 | E3: `409 EVALUACION_EN_CURSO` | Sujeto 25 (evaluación 15 abierta en 2026-2) | 1. Iniciar en 2026-2. | Aviso de conflicto. | Aviso de evaluación abierta. | **PASÓ** |
| TC-GEN-02-017 | E3: `409 EVALUACION_EN_CURSO` | Sujeto 409, 2027-1 | 1. Iniciar. | Aviso de conflicto. | Aviso de evaluación abierta; no avanza. | **PASÓ** |

## Casos — Retomar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-018 | GEN-02-FE: "Al retomar una evaluación, se muestra el progreso y estado devueltos por la API." · GEN-02-TEST: "…ni reinicia los tiempos oficiales." · CU-BFA-P04: "Calcula tiempo restante con el vencimiento original" | Evaluación 15 | 1. Recuperar. 2. Recuperar otra vez. 3. Recargar y recuperar. | Se ve el estado de E4 (subpruebas y tiempo restante del servidor), igual cada vez. | #15 En curso: VOC1 Cerrada, VOC2 Activa, CV Pendiente, «tiempo restante 3:34 (según el servidor)»; igual las tres veces. | **PASÓ** |
| TC-GEN-02-019 | CU-BFA-P04 A3: "Evaluación cerrada: lectura de estado permitida; ninguna reapertura ni tiempo adicional." · CU-GEN-19 A5: "Una repetición posterior al cierre requiere una nueva asignación; nunca reabre el intento anterior." | Evaluación 16 (FINALIZADA) | 1. Recuperar. | Muestra el estado final y no permite continuar. | #16 Finalizada, 2 de 2 cerradas, «La evaluación ya finalizó; no admite más respuestas.», sin «Continuar». | **PASÓ** |
| TC-GEN-02-020 | E4: `404 EVALUACION_NO_ENCONTRADA` | Evaluación 99 | 1. Recuperar. | Aviso de no encontrada. | «No encontrado: No existe una evaluación con ese ID.» | **PASÓ** |
| TC-GEN-02-021 | GEN-02-FE: errores de validación | ID `x` | 1. Recuperar. | Error de formato, sin consultar. | «El ID de la evaluación debe ser un número entero positivo.» | **PASÓ** |

## Casos — Acceso y conexión

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-022 | GEN-02-FE: errores de acceso · `403 SIN_PERMISO` | Sujeto 403 y evaluación 403 | 1. Buscar al sujeto. 2. Recuperar la evaluación. | Aviso de acceso denegado; nada se muestra ni se crea. | «Acceso denegado: No tiene permiso para realizar esta acción o ver esta evaluación.» en los dos. | **PASÓ** |
| TC-GEN-02-023 | §5.2.2: `500 ERROR_INTERNO` | Evaluación 500 | 1. Recuperar. | Aviso de error del servidor, con reintentar. | «Error del servidor: Ocurrió un error en el servidor. Intente de nuevo más tarde.», con Reintentar. | **PASÓ** |
| TC-GEN-02-024 | GEN-02-FE: errores de conexión | Evaluación 503 y sujeto 503 | 1. Recuperar. 2. Buscar al sujeto. | Aviso de conexión, con reintentar; no avanza. | «Sin conexión: No se pudo conectar con el servidor…» en los dos, con Reintentar. | **PASÓ** |

## Casos bloqueados — Backend real

| ID | CA cubierto | Qué falta | Estado |
| --- | --- | --- | --- |
| TC-GEN-02-025 | GEN-02-BE: "Cumplir E3 y E4; crear y recuperar evaluaciones desde la base de datos." · "Rechazar datos o configuraciones inválidas…" · "Ejecutar correctamente las pruebas automatizadas." | PR de GEN-02-BE. Se probará con Postman en `pull/<n>/merge`: los mismos escenarios de 004 a 017 contra E3, y la colección en `tests/api/`. | **BLOQUEADO** |
| TC-GEN-02-026 | GEN-02-TEST: "Iniciar o retomar no duplica intentos ni reinicia los tiempos oficiales." · CU-BFA-P04 | PR de GEN-02-BE: E4 repetido y tras recargar devuelve el mismo `id` y el mismo `finLimite` sin crear otra evaluación (E22). | **BLOQUEADO** |
| TC-GEN-02-027 | GEN-07-BE: "Las evaluaciones mantienen asociada la versión utilizada al momento de su creación." | GEN-02-BE. Se ejecutan los casos ya diseñados TC-GEN-07-036 a 039 (José Ariel Zeledón, `docs/pruebas/GEN-07-casos-prueba-v2.md`), sin repetir el retest de BUG-04. | **BLOQUEADO** |
| TC-GEN-02-028 | GEN-02-FE: "El usuario autorizado…" · §5.2.1: 401 `NO_AUTENTICADO` y 403 `SIN_PERMISO` reales | GEN-06 (autenticación y permisos) y GEN-02-BE. | **BLOQUEADO** |

## Casos — Pruebas automáticas

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-02-029 | GEN-02-FE: "La interfaz puede desarrollarse con mocks de DOC-05 e integrarse posteriormente con GEN-02-BE." | `main` `d33f6a8` | 1. `npx vitest run src/features/evaluacion src/services/evaluacion.mock.test.ts src/services/evaluacion.test.ts src/services/http.test.ts`. | Todas pasan: el mock y el cliente real cumplen el mismo contrato (E2, E3, E4, E19 y E21). | 6 archivos, 63 de 63 pruebas. | **PASÓ** |

## Observaciones (no bloquean)

| ID | Observación | Dónde |
| --- | --- | --- |
| O-1 | El tipo `ConfiguracionSubprueba` de GEN-02 declara `tiempoLimiteSegundos: number` y `factor: string`, y el resumen suma los tiempos sin contemplar nulos. El cambio de E2 del #51 (ratificado el 17-nov) los vuelve nulos. Con el mock no pasa nada porque siempre manda números, pero con el backend real un tiempo nulo podría dar un «Tiempo oficial» incorrecto. Conviene ajustarlo cuando entre el #51; aquí no se prueba porque Testing no modifica el mock. | `features/evaluacion/tipos.ts`, `componentes/FormularioInicio.tsx` |

## Registro de ejecución

| Fecha | Código | Tester | Resultado | Evidencia / bug |
| --- | --- | --- | --- | --- |
| 2026-11-18 | `main` `d33f6a8`, con mocks | José Ariel Zeledón | 25 PASÓ (001 a 024 y 029) · 4 BLOQUEADO (025 a 028). Sin fallos. | [salida](../evidencias/GEN-02/GEN-02-ejecucion-2026-11-18.txt) · [resultados](../evidencias/GEN-02/GEN-02-resultados-2026-11-18.json) · [capturas](../evidencias/GEN-02/capturas/) · [script](../evidencias/GEN-02/GEN-02-recorrido-interfaz.js) |

### Revisión final

- [x] Cada caso corresponde a un criterio aprobado y a la tarea correcta.
- [x] Se ejecutó contra el commit vigente de `main` (`d33f6a8`).
- [x] Se registraron resultados reales y evidencia verificable.
- [x] No contiene secretos ni datos personales reales.
- [ ] Se informó en GEN-02-TEST.
- [ ] `testing-ok`: no aplica en esta versión (no hay PR vigente; se emitirá sobre el PR de GEN-02-BE).
