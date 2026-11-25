# Casos de prueba GEN-04 — finalizar una evaluación (v2)

## Identificación

- **Tarea de Testing:** [GEN-04-TEST](https://app.clickup.com/t/86bcx2z67)
- **Tareas de Desarrollo:** [GEN-04-FE](https://app.clickup.com/t/86bcvah5k) (sin PR) · [GEN-04-BE](https://app.clickup.com/t/86bcvah5j) (sin PR)
- **Historia:** [GEN-04](https://app.clickup.com/t/86bcvah5d)
- **Tester:** Rodrigo Romero
- **Versión y fecha:** v2 · 2026-11-18
- **Estado del documento:** diseño de casos; ninguno ejecutado, porque GEN-04-BE y GEN-04-FE todavía no tienen PR.

## Historial de cambios

| Versión | Fecha | Cambios |
| --- | --- | --- |
| v1 | 2026-11-06 | Diseño inicial de 23 casos (PR #35). |
| v2 | 2026-11-18 | DOC-04 aprobado el 2026-11-10: se quitan las marcas «provisional (DOC-04)». Documentación respondió las consultas C-1 y C-2 el 2026-11-18 y actualizó el contrato de E8: se fijan los resultados de TC-GEN-04-011 y 014. E4 y E8 incluyen `versionFormularioId` (ajuste del 2026-11-08) y `cerradaEn` por subprueba (2026-11-18); TC-GEN-04-014 y 016 lo verifican. Se actualizan la dependencia de GEN-03 (BUG-01 corregido; la entrega de GEN-03-BE es el PR #41) y la numeración de casos de uso. |

## Fuentes y estado

| Fuente | Estado al 2026-11-18 | Uso en este documento |
| --- | --- | --- |
| Criterios de aceptación de GEN-04, GEN-04-BE, GEN-04-FE y GEN-04-TEST (ClickUp) | Publicados en las tareas | Base de todos los casos |
| Documento BFA — §5.3 (contratos E4 y E8) y Documento general — §5.2 (convenciones REST) | **Aprobados** ([DOC-05](https://app.clickup.com/t/86bctt9cy) `completed`), con los ajustes del 2026-11-08 (`versionFormularioId`) y del 2026-11-18 (`cerradaEn` en E4; cierre de subprueba vencida y `estadoResultado` en E8) | Códigos HTTP, campos y formato de error |
| `docs/api/openapi.yaml` (GEN-00-API, en `main` desde el PR #37) | Refleja el contrato del 2026-11-08; todavía no incluye los ajustes del 2026-11-18 | Referencia; ante una diferencia manda el Documento BFA |
| Documento general — §5.1.7 (motor común) | **Aprobado** ([DOC-06](https://app.clickup.com/t/86bctt9db) `completed`) | Separación entre motor común y evaluador del test |
| Documento general — §4.2 (ENT-COM-05 Evaluacion, ENT-COM-06 y ENT-COM-10 Resultado) | **Aprobado** ([DOC-03](https://app.clickup.com/t/86bctt9bt) `completed`) | Estados de evaluación, cierre de subprueba y estados de resultado |
| Documento general — §3 (DOC-04 rev. 2: §3.4, §3.7 y CU-BFA-P08) | **Aprobado** ([DOC-04](https://app.clickup.com/t/86bctt9cm) `completed` el 2026-11-10) | Reglas de finalización y parcialidad |
| [DOC-07](https://app.clickup.com/t/86bctt9dn) — Calidad documental | `in progress` | Dependencia de GEN-04-TEST en ClickUp |
| [GEN-06](https://app.clickup.com/t/86bcvaj7e) — Acceso y roles | `Open` | Casos de acceso **bloqueados** |
| E12 — Publicar revisión vigente (`PUT /api/v1/evaluaciones/{evaluacionId}/resultados/vigente`) | Documentado en el Documento BFA §5.5; [BFA-BE-13](https://app.clickup.com/t/86bcw9yw5) `Open`, sin implementar | Caso de resultado disponible **bloqueado** |
| Estructura y reglas del repositorio GitHub | Vigente | Formato del archivo, PR y datos de prueba |

Todos los resultados esperados provienen de documentos aprobados. Desde que DOC-04 se aprobó (2026-11-10), el resultado que la v1 marcaba como «provisional (DOC-04)» queda firme.

**Numeración de casos de uso:** estos casos usan la numeración de DOC-04 rev. 2 del Documento general §3 (CU-BFA-P08 «Finalizar evaluación y consolidar resultados»), que es la que cita el contrato aprobado de E8. Desde el 2026-11-08 el Documento BFA §3 usa la misma numeración.

Los criterios de las tareas se citan con su texto de ClickUp, porque todavía no tienen ID. Cuando Documentación asigne los IDs `CA-`, se usarán aquí.

### Dependencia para ejecutar

Finalizar exige subpruebas cerradas, y el cierre lo registra GEN-03-BE. El fallo del cierre por tiempo ([BUG-01](https://app.clickup.com/t/1k5fzqaz5ae)) se corrigió y se verificó en el PR #34; la entrega de GEN-03-BE es el PR #41, todavía sin fusionar, y ahí se vuelve a verificar. Mientras GEN-03-BE no esté en `main`, las evaluaciones con subpruebas `CERRADA` solo se pueden preparar en local sobre el PR #41 o cargándolas con SQL.

## Glosario

| Término | Significado |
| --- | --- |
| CA | Criterio de aceptación: condición que la funcionalidad debe cumplir para darse por buena. Documentación les asignará IDs `CA-<MOD>-NN` (manual §4.2). |
| TC | Caso de prueba (*test case*): datos, pasos y resultado esperado para comprobar un criterio. ID `TC-GEN-04-NNN`. |
| FE / BE | Frontend (interfaz en el navegador) / backend (API). |
| E3, E4, E5, E7 | Endpoints aprobados: crear evaluación, consultar su estado, iniciar una subprueba y registrar respuestas (Documento BFA §5.3). |
| E8 | Endpoint aprobado para finalizar la evaluación: `POST /api/v1/evaluaciones/{evaluacionId}/finalizar` (Documento BFA §5.3). |
| E9 | Endpoint aprobado para consultar resultados; pertenece a GEN-05. |
| E12 | Endpoint para publicar la revisión vigente de un resultado (Documento BFA §5.5); lo implementa BFA-BE-13, que todavía no tiene PR. |
| E13, E14, E18, E20 | Endpoints aprobados para registrar una cuenta, iniciar sesión, crear un Sujeto y crear un Periodo (Documento general §5.2.3). |
| EN_CURSO / FINALIZADA | Estados de la evaluación (ENT-COM-05). |
| PENDIENTE / ACTIVA / CERRADA | Estados de una subprueba dentro de una evaluación, según el contrato aprobado. |
| `cerradaEn` | Fecha del cierre registrado de cada subprueba en E4 y E8, o nulo si no se cerró (ENT-COM-06). |
| CALCULANDO / PENDIENTE_VALIDACION / DISPONIBLE | Estados del resultado (ENT-COM-10). Son independientes del estado de la evaluación: una evaluación puede estar `FINALIZADA` con su resultado todavía pendiente. |
| PR / SHA | *Pull request* que se prueba / identificador del commit exacto probado. |
| e2e | Prueba de extremo a extremo automatizada con Playwright (`tests/e2e/`). |
| EV-PRUEBA | Evaluaciones ficticias definidas en «Datos de prueba». |

## Matriz de cobertura

**GEN-04-FE**

| Criterio | Casos |
| --- | --- |
| La interfaz permite solicitar la finalización cuando el estado de la evaluación lo permite. | TC-GEN-04-001, TC-GEN-04-002 |
| La solicitud se realiza mediante el endpoint E8 documentado. | TC-GEN-04-003 |
| Se muestra correctamente la confirmación de una evaluación finalizada. | TC-GEN-04-004 |
| Se distinguen los resultados pendientes de validación de los disponibles. | TC-GEN-04-005, TC-GEN-04-006 |
| Se manejan errores de conexión, permisos y subpruebas pendientes. | TC-GEN-04-002, TC-GEN-04-007, TC-GEN-04-008 |
| Se evitan solicitudes duplicadas por acciones repetidas del usuario. | TC-GEN-04-009 |
| El frontend no realiza cálculos psicométricos ni publica resultados automáticamente. | TC-GEN-04-010 |

**GEN-04-BE**

| Criterio | Casos |
| --- | --- |
| El endpoint cumple el contrato E8 de DOC-05. | TC-GEN-04-011, TC-GEN-04-012, TC-GEN-04-013, TC-GEN-04-020 |
| Se rechaza la finalización cuando existen subpruebas incluidas sin cerrar. | TC-GEN-04-012, TC-GEN-04-013, TC-GEN-04-014 |
| Se utiliza el evaluador del test, sin incorporar fórmulas específicas en el motor común. | Sin caso funcional (ver abajo) |
| La evaluación finalizada conserva su resultado y estado de cálculo. | TC-GEN-04-011, TC-GEN-04-016 |
| Si faltan insumos aprobados o validaciones, el resultado permanece pendiente sin asignar puntuaciones ficticias. | TC-GEN-04-017 |
| Repetir la solicitud devuelve el cierre existente sin duplicar cálculos ni resultados. | TC-GEN-04-018, TC-GEN-04-019 |
| Se manejan correctamente los errores de permisos, evaluación inexistente y subpruebas pendientes. | TC-GEN-04-012, TC-GEN-04-020, TC-GEN-04-021 |
| Las pruebas automatizadas se ejecutan satisfactoriamente. | TC-GEN-04-023 |
| Convención aprobada (Documento general §5.2.1): sin sesión se responde `401 NO_AUTENTICADO`. | TC-GEN-04-022 |

**GEN-04 (historia)**

| Criterio | Casos |
| --- | --- |
| Solo se finalizan evaluaciones cuyas subpruebas incluidas estén cerradas. | TC-GEN-04-012, TC-GEN-04-013, TC-GEN-04-015 |
| El sistema utiliza el evaluador del test aplicado. | Sin caso funcional (ver abajo) |
| Los resultados y sus estados quedan almacenados correctamente. | TC-GEN-04-011, TC-GEN-04-016 |
| Una solicitud repetida de finalización no genera resultados duplicados. | TC-GEN-04-018, TC-GEN-04-019 |
| Se manejan correctamente los errores y los resultados pendientes de validación. | TC-GEN-04-005, TC-GEN-04-017, TC-GEN-04-020 |
| Backend y frontend funcionan integrados y superan las pruebas correspondientes. | TEST-01-EJEC (ver abajo) |

**GEN-04-TEST (criterios funcionales de la propia tarea)**

| Criterio | Casos |
| --- | --- |
| Solo se finaliza con todas las subpruebas cerradas. | TC-GEN-04-012, TC-GEN-04-013 |
| Una evaluación finalizada no muestra como disponible un resultado pendiente. | TC-GEN-04-005 |
| Esquemas, códigos HTTP, campos y errores cumplen el contrato aprobado. | TC-GEN-04-011, TC-GEN-04-012, TC-GEN-04-013, TC-GEN-04-020, y el formato común de error en todos los casos de backend |

Los otros dos criterios de GEN-04-TEST son de proceso: «cada criterio tiene al menos un caso» se cumple con esta matriz, y «cada PR tiene `testing-ok` o sus fallos comentados» se cumple al ejecutar los casos sobre los PR.

**Criterios del modelo relacionados (Documento general §4.6)**

Son criterios documentales del modelo de datos, no de la tarea GEN-04; se listan como referencia adicional.

| Criterio del modelo | Casos |
| --- | --- |
| CA-05 — Parcialidad: los componentes ausentes permanecen nulos con estado y motivo, no como cero. | TC-GEN-04-015, TC-GEN-04-017 |

**Criterios sin caso funcional en este documento:**

- **GEN-04-BE: "Se utiliza el evaluador del test, sin incorporar fórmulas específicas en el motor común."** y **GEN-04: "El sistema utiliza el evaluador del test aplicado."** No son observables desde la API mientras exista un solo test (BFA); corresponden a la revisión de código de Integración. Las fórmulas de BFA se prueban en BFA-TEST-02 y BFA-TEST-04.
- **GEN-04: "Backend y frontend funcionan integrados y superan las pruebas correspondientes."** Se ejecuta en [TEST-01-EJEC](https://app.clickup.com/t/86bcvajag).

## Consultas a Documentación

Huecos encontrados al diseñar los casos. Se publicaron en la tarea [DOC-05](https://app.clickup.com/t/86bctt9cy) el 2026-11-17 y Documentación las respondió el 2026-11-18, actualizando E8 en el Documento BFA §5.3.

| ID | Consulta | Fuentes | Casos afectados | Estado |
| --- | --- | --- | --- | --- |
| C-1 | ¿Qué hace E8 si una subprueba incluida está `ACTIVA` con el tiempo vencido pero su cierre todavía no se registró? ¿La cierra y finaliza, o responde `409 SUBPRUEBAS_PENDIENTES`? | Documento BFA E8; Documento general ENT-COM-06; Documento general §3, CU-BFA-P07 | TC-GEN-04-014 | **Resuelta (2026-11-18):** una subprueba `ACTIVA` con `finLimite` vencido no bloquea: E8 registra su cierre (`cerradaEn` = `finLimite`, idempotente) y finaliza con `200`. `409 SUBPRUEBAS_PENDIENTES` solo si queda una subprueba incluida `PENDIENTE` o `ACTIVA` con tiempo. |
| C-2 | Con todos los insumos aprobados (claves y revisión de FV), ¿qué `estadoResultado` devuelve E8: `CALCULANDO` o `PENDIENTE_VALIDACION`? `DISPONIBLE` exige una revisión validada y publicada (ENT-COM-10, E12), que E8 no realiza. | Documento BFA E8 y §5.5; Documento general ENT-COM-10 | TC-GEN-04-011, TC-GEN-04-006 | **Resuelta (2026-11-18):** `PENDIENTE_VALIDACION`. `CALCULANDO` solo se ve mientras el cálculo está en curso; `DISPONIBLE` llega cuando la revisión se valida y se publica con E12. |

## Datos de prueba (ficticios)

Se usa la estructura de configuración del BFA publicada en el Documento BFA (códigos, orden y tiempos), con **preguntas, opciones y claves inventadas**. No se usan ítems reales.

**Toda finalización correcta devuelve `estadoResultado: PENDIENTE_VALIDACION`**, tanto si faltan insumos aprobados (como las claves ficticias sin aprobar) como si están todos (Documento BFA E8; respuesta a C-2). El catálogo de GEN-07 guarda las claves aparte (E29, BFA-BE-09).

| Evaluación | Subpruebas incluidas y estado | Usada en |
| --- | --- | --- |
| EV-PRUEBA-20 | VOC1 `CERRADA` y VOC2 `CERRADA` | TC-GEN-04-001, 003, 004, 007, 009, 010, 011, 016, 018, 021, 022 |
| EV-PRUEBA-21 | VOC1 `CERRADA` y VOC2 `PENDIENTE` | TC-GEN-04-002, 012 |
| EV-PRUEBA-22 | VOC1 `ACTIVA` dentro de plazo | TC-GEN-04-013 |
| EV-PRUEBA-23 | VOC1 `ACTIVA` con `finLimite` vencido y sin cierre registrado | TC-GEN-04-014 |
| EV-PRUEBA-24 | Solo VOC1 (aplicación parcial), `CERRADA` | TC-GEN-04-015 |
| EV-PRUEBA-25 | FV `CERRADA` con palabras sin revisar | TC-GEN-04-005, 017 |
| EV-PRUEBA-26 | VOC1 `CERRADA` y VOC2 `CERRADA`, sin finalizar | TC-GEN-04-019 |

- **Una evaluación solo se puede finalizar una vez.** Por eso, cada caso que finaliza con éxito usa su propia copia de la evaluación indicada (por ejemplo, EV-PRUEBA-20a para TC-GEN-04-003 y EV-PRUEBA-20b para TC-GEN-04-004). TC-GEN-04-016 y 018 son la excepción: usan a propósito la misma evaluación finalizada en TC-GEN-04-011.
- **EV-PRUEBA-23** se prepara llamando a E5 y dejando pasar el tiempo sin hacer otras peticiones. Si el backend ya cerró la subprueba al vencer (por ejemplo, con un cierre programado), el resultado esperado de TC-GEN-04-014 es el mismo: E8 finaliza y `cerradaEn` es igual a `finLimite`.
- **Cuentas:** una cuenta ficticia de Sujeto dueña de todas las evaluaciones y otra sin permiso sobre EV-PRUEBA-20. **Salvo que el caso indique otra, todas las llamadas usan la cuenta del Sujeto dueño.**

Las migraciones no cargan datos: la colección Postman crea sus propios datos por la API (cuenta y sesión con E13/E14, Sujeto y Periodo con E18/E20, evaluación con E3, inicio y respuestas con E5 y E7). Para observar el vencimiento en local se pueden usar duraciones cortas, indicándolo en la evidencia.

## Entorno

- **Frontend:** PR de GEN-04-FE sobre `pull/<n>/merge`, con `npm ci` y `npm run dev` en `frontend/`.
- **Backend:** PR de GEN-04-BE sobre `pull/<n>/merge`, con `docker compose up -d db` y `mvn -f backend/pom.xml spring-boot:run`; colección Postman en `tests/api/` con variables `{{base_url}}` y `{{token}}` de una cuenta ficticia. Requiere GEN-03-BE (PR #41) y el catálogo de GEN-07-BE (en `main` desde el 2026-11-17). Mientras no exista GEN-06, se usa el perfil de desarrollo que autorizó Integración para GEN-03-BE (`SPRING_PROFILES_ACTIVE=desarrollo-sin-autenticacion`), si GEN-04-BE usa el mismo control de acceso.
- **Comprobaciones en la base local:** algunos casos verifican la base de datos de la ejecución local, porque la API no expone ese dato. Se indica en cada caso.

## Casos — Frontend (navegador)

### TC-GEN-04-001 — Finalización disponible con todas las subpruebas cerradas

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-001 | GEN-04-FE: "La interfaz permite solicitar la finalización cuando el estado de la evaluación lo permite." | EV-PRUEBA-20 | 1. Abrir la evaluación en la interfaz.<br>2. Buscar la acción de finalizar. | La interfaz ofrece la acción de finalizar la evaluación. | Manual; candidato e2e (TEST-01-EJEC) |

### TC-GEN-04-002 — Subpruebas pendientes

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-002 | GEN-04-FE: "La interfaz permite solicitar la finalización cuando el estado de la evaluación lo permite."<br>GEN-04-FE: "Se manejan errores de conexión, permisos y subpruebas pendientes." | EV-PRUEBA-21 | 1. Abrir la evaluación.<br>2. Intentar finalizar. | Pasa con cualquiera de estos dos comportamientos: (a) la acción de finalizar no está disponible; (b) si se solicita, se informa que hay subpruebas pendientes (`409 SUBPRUEBAS_PENDIENTES`). En ambos casos la evaluación no aparece como finalizada. | Manual (navegador) |

### TC-GEN-04-003 — La solicitud usa E8

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-003 | GEN-04-FE: "La solicitud se realiza mediante el endpoint E8 documentado." | EV-PRUEBA-20; pestaña de red del navegador abierta | 1. Finalizar la evaluación.<br>2. Revisar la petición enviada. | Se envía `POST /api/v1/evaluaciones/{evaluacionId}/finalizar`, sin cuerpo, con el identificador de EV-PRUEBA-20. | Manual (navegador) |

### TC-GEN-04-004 — Confirmación de evaluación finalizada

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-004 | GEN-04-FE: "Se muestra correctamente la confirmación de una evaluación finalizada." | EV-PRUEBA-20 | 1. Finalizar la evaluación.<br>2. Observar la pantalla tras la respuesta `200`. | La pantalla indica que la evaluación quedó finalizada. | Manual; candidato e2e (TEST-01-EJEC) |

### TC-GEN-04-005 — Resultado pendiente de validación

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-005 | GEN-04-FE: "Se distinguen los resultados pendientes de validación de los disponibles."<br>GEN-04-TEST: "Una evaluación finalizada no muestra como disponible un resultado pendiente." | EV-PRUEBA-25 | 1. Finalizar la evaluación.<br>2. Observar cómo se presenta el resultado. | La pantalla indica que la evaluación está finalizada y, por separado, que el resultado está pendiente de validación. No muestra el resultado como disponible ni presenta puntajes. | Manual (navegador) |

### TC-GEN-04-006 — Resultado disponible

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-006 | GEN-04-FE: "Se distinguen los resultados pendientes de validación de los disponibles." | Evaluación finalizada con resultado `DISPONIBLE` | 1. Abrir la evaluación finalizada.<br>2. Observar cómo se presenta el resultado. | El resultado se muestra como disponible, diferenciado de uno pendiente. **BLOQUEADO:** un resultado solo llega a `DISPONIBLE` con una revisión validada y publicada con E12 (ENT-COM-10; Documento BFA E8), que BFA-BE-13 todavía no implementa. | Manual (navegador) |

### TC-GEN-04-007 — Error de conexión al finalizar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-007 | GEN-04-FE: "Se manejan errores de conexión, permisos y subpruebas pendientes." | EV-PRUEBA-20; la petición a E8 se hace fallar bloqueándola en las herramientas del navegador o deteniendo el backend | 1. Intentar finalizar.<br>2. Observar el mensaje y el estado. | Se informa el error y la evaluación no aparece como finalizada. | Manual (navegador) |

### TC-GEN-04-008 — Sin permiso para finalizar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-008 | GEN-04-FE: "Se manejan errores de conexión, permisos y subpruebas pendientes." | EV-PRUEBA-20; sesión de la cuenta sin permiso | 1. Intentar finalizar. | Se informa la falta de permiso (`403 SIN_PERMISO`) y la evaluación no cambia. **BLOQUEADO** hasta que GEN-06 implemente el acceso. | Manual (navegador) |

### TC-GEN-04-009 — Doble solicitud de finalización

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-009 | GEN-04-FE: "Se evitan solicitudes duplicadas por acciones repetidas del usuario." | EV-PRUEBA-20; pestaña de red abierta | 1. Hacer doble clic en la acción de finalizar.<br>2. Contar las peticiones a E8. | En la pestaña de red aparece una sola petición `POST` a E8. | Manual (navegador) |

### TC-GEN-04-010 — Sin cálculos ni publicación en el navegador

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-010 | GEN-04-FE: "El frontend no realiza cálculos psicométricos ni publica resultados automáticamente." | EV-PRUEBA-20; pestaña de red abierta | 1. Finalizar la evaluación.<br>2. Revisar la pantalla y las peticiones posteriores. | La pantalla solo muestra el estado de la evaluación y el `estadoResultado` devueltos por el backend; no presenta puntajes ni percentiles. No se envía ninguna petición a `PUT /api/v1/evaluaciones/{evaluacionId}/resultados/vigente` (E12). | Manual (navegador) |

## Casos — Backend (Postman)

Contrato aprobado del Documento BFA §5.3 y convenciones del Documento general §5.2:

- E4: `GET /api/v1/evaluaciones/{evaluacionId}`
- E8: `POST /api/v1/evaluaciones/{evaluacionId}/finalizar` (sin cuerpo). Respuesta correcta: `200` con los campos de E4 (incluidos `versionFormularioId` y `cerradaEn` por subprueba) más `resultadoId` y `estadoResultado`. Errores: `403 SIN_PERMISO`, `404 EVALUACION_NO_ENCONTRADA` y `409 SUBPRUEBAS_PENDIENTES` (hay una subprueba incluida `PENDIENTE` o `ACTIVA` con tiempo).

En todo error se verifica además el cuerpo común (Documento general §5.2.2): `timestamp`, `status`, `codigo`, `mensaje`, `ruta` y, en errores de validación, `detalles`.

### TC-GEN-04-011 — Finalizar con todas las subpruebas cerradas

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-011 | GEN-04-BE: "El endpoint cumple el contrato E8 de DOC-05."<br>GEN-04-BE: "La evaluación finalizada conserva su resultado y estado de cálculo."<br>GEN-04: "Los resultados y sus estados quedan almacenados correctamente." | EV-PRUEBA-20 | 1. Llamar a E8. | `200` con `estado: FINALIZADA`, un `resultadoId`, `estadoResultado: PENDIENTE_VALIDACION` y todas las subpruebas `CERRADA` con su `cerradaEn` (Documento BFA E8: el valor es el mismo con o sin insumos aprobados). | Manual (Postman) |

### TC-GEN-04-012 — Subprueba incluida pendiente

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-012 | GEN-04-BE: "Se rechaza la finalización cuando existen subpruebas incluidas sin cerrar."<br>GEN-04-BE: "Se manejan correctamente los errores de permisos, evaluación inexistente y subpruebas pendientes."<br>GEN-04: "Solo se finalizan evaluaciones cuyas subpruebas incluidas estén cerradas." | EV-PRUEBA-21 | 1. Llamar a E8.<br>2. Llamar a E4. | Paso 1: `409 SUBPRUEBAS_PENDIENTES`. Paso 2: la evaluación sigue `EN_CURSO`. | Manual (Postman) |

### TC-GEN-04-013 — Subprueba incluida activa

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-013 | GEN-04-BE: "Se rechaza la finalización cuando existen subpruebas incluidas sin cerrar."<br>GEN-04: "Solo se finalizan evaluaciones cuyas subpruebas incluidas estén cerradas." | EV-PRUEBA-22 | 1. Llamar a E8.<br>2. Llamar a E4. | Paso 1: `409 SUBPRUEBAS_PENDIENTES`. Paso 2: la evaluación sigue `EN_CURSO` y VOC1 sigue `ACTIVA`. | Manual (Postman) |

### TC-GEN-04-014 — Subprueba vencida sin cierre registrado

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-014 | GEN-04-BE: "Se rechaza la finalización cuando existen subpruebas incluidas sin cerrar." | EV-PRUEBA-23 | 1. Llamar a E8.<br>2. Llamar a E4. | Paso 1: `200` con `estado: FINALIZADA`. Paso 2: VOC1 `CERRADA` con `cerradaEn` igual a su `finLimite`, y la evaluación `FINALIZADA` (Documento BFA E8). La evaluación nunca puede quedar `FINALIZADA` con VOC1 `ACTIVA` (Documento general ENT-COM-05). | Manual (Postman) |

### TC-GEN-04-015 — Aplicación parcial

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-015 | GEN-04: "Solo se finalizan evaluaciones cuyas subpruebas incluidas estén cerradas." | EV-PRUEBA-24 | 1. Llamar a E8.<br>2. Revisar en la base local los factores del resultado. | Paso 1: `200` con `estado: FINALIZADA`: las subpruebas no incluidas no bloquean la finalización. Paso 2: los factores que dependen de subpruebas no aplicadas no tienen valor (nulo), nunca 0 (DOC-04: CU-BFA-P08 A3). | Manual (Postman) |

### TC-GEN-04-016 — La finalización se conserva

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-016 | GEN-04-BE: "La evaluación finalizada conserva su resultado y estado de cálculo."<br>GEN-04: "Los resultados y sus estados quedan almacenados correctamente." | EV-PRUEBA-20 ya finalizada en TC-GEN-04-011 | 1. Llamar a E4.<br>2. Esperar un minuto y repetir E4. | En las dos consultas la evaluación aparece `FINALIZADA` y todas sus subpruebas `CERRADA`, con el mismo `cerradaEn` en ambas. | Manual (Postman) |

### TC-GEN-04-017 — Insumos faltantes: resultado pendiente sin puntajes ficticios

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-017 | GEN-04-BE: "Si faltan insumos aprobados o validaciones, el resultado permanece pendiente sin asignar puntuaciones ficticias."<br>GEN-04: "Se manejan correctamente los errores y los resultados pendientes de validación." | EV-PRUEBA-25 | 1. Llamar a E8.<br>2. Revisar en la base local los factores del resultado. | Paso 1: `200` con `estado: FINALIZADA` y `estadoResultado: PENDIENTE_VALIDACION`. Paso 2: los factores que dependen de FV no tienen valor (nulo), nunca 0 (Documento BFA E8: «sin convertir esos faltantes en cero»). Se revisa en la base porque E9 no muestra factores hasta que se publica una revisión. | Manual (Postman) |

### TC-GEN-04-018 — Repetir la finalización

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-018 | GEN-04-BE: "Repetir la solicitud devuelve el cierre existente sin duplicar cálculos ni resultados."<br>GEN-04: "Una solicitud repetida de finalización no genera resultados duplicados." | EV-PRUEBA-20 ya finalizada en TC-GEN-04-011 | 1. Anotar `resultadoId` y `estadoResultado` de TC-GEN-04-011.<br>2. Repetir E8.<br>3. Revisar la base local. | Paso 2: `200` con el mismo `resultadoId` y el mismo `estadoResultado` (Documento BFA E8: «devuelve el cierre existente sin otro cálculo»). Paso 3: hay un solo resultado y una sola revisión de cálculo para EV-PRUEBA-20. | Manual (Postman) |

### TC-GEN-04-019 — Finalizaciones simultáneas

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-019 | GEN-04-BE: "Repetir la solicitud devuelve el cierre existente sin duplicar cálculos ni resultados."<br>GEN-04: "Una solicitud repetida de finalización no genera resultados duplicados." | EV-PRUEBA-26 | 1. Enviar dos llamadas a E8 al mismo tiempo.<br>2. Revisar la base local. | Paso 1: las dos responden `200` con el mismo `resultadoId`; ninguna responde `500`. Paso 2: hay un solo resultado para EV-PRUEBA-26. | Manual (script) |

### TC-GEN-04-020 — Evaluación inexistente

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-020 | GEN-04-BE: "El endpoint cumple el contrato E8 de DOC-05."<br>GEN-04-BE: "Se manejan correctamente los errores de permisos, evaluación inexistente y subpruebas pendientes." | Identificador de evaluación que no existe | 1. Llamar a E8. | `404 EVALUACION_NO_ENCONTRADA` con el cuerpo común de error; no se crea ningún resultado. | Manual (Postman) |

### TC-GEN-04-021 — Sin permiso

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-021 | GEN-04-BE: "Se manejan correctamente los errores de permisos, evaluación inexistente y subpruebas pendientes." | EV-PRUEBA-20 sin finalizar; sesión de la cuenta sin permiso | 1. Llamar a E8.<br>2. Con la cuenta del Sujeto dueño, llamar a E4. | Paso 1: `403 SIN_PERMISO`. Paso 2: la evaluación sigue `EN_CURSO`. **BLOQUEADO** hasta que GEN-06 implemente el acceso. | Manual (Postman) |

### TC-GEN-04-022 — Sin sesión

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-022 | Convención aprobada (Documento general §5.2.1). | EV-PRUEBA-20 sin finalizar; petición sin cabecera `Authorization` | 1. Llamar a E8 sin token.<br>2. Con la cuenta del Sujeto dueño, llamar a E4. | Paso 1: `401 NO_AUTENTICADO`. Paso 2: la evaluación sigue `EN_CURSO`. **BLOQUEADO** hasta que GEN-06 implemente el acceso. | Manual (Postman) |

### TC-GEN-04-023 — Pruebas automatizadas del PR

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e |
| --- | --- | --- | --- | --- | --- |
| TC-GEN-04-023 | GEN-04-BE: "Las pruebas automatizadas se ejecutan satisfactoriamente." | PR de GEN-04-BE abierto | 1. Revisar los checks del PR.<br>2. Ejecutar `mvn -f backend/pom.xml verify` sobre `pull/<n>/merge`. | El job `backend-build` y la ejecución local terminan sin fallos, incluido el chequeo de formato (Spotless). | Revisión de CI |

## Orden sugerido de ejecución

1. TC-GEN-04-020, 012, 013, 014 y 023: no requieren finalizar ninguna evaluación.
2. TC-GEN-04-021 y 022, cuando GEN-06 esté implementado: requieren una copia de EV-PRUEBA-20 **sin finalizar**.
3. TC-GEN-04-011 y, después, 016 y 018 sobre esa misma evaluación ya finalizada.
4. TC-GEN-04-015, 017 y 019 sobre sus propias evaluaciones.
5. Casos de frontend (001 a 010), cada uno con su propia copia de la evaluación indicada.

## Registro de ejecución

| ID | PR/SHA | Fecha | Resultado | Evidencia / bug |
| --- | --- | --- | --- | --- |
| TC-GEN-04-001 a 010 | — | — | BLOQUEADO: sin PR de GEN-04-FE | — |
| TC-GEN-04-011 a 020, 023 | — | — | BLOQUEADO: sin PR de GEN-04-BE (al 2026-11-18). Además requieren GEN-03-BE (PR #41) para preparar subpruebas cerradas, salvo 013, 014, 020 y 023 | — |
| TC-GEN-04-021, 022 | — | — | BLOQUEADO: GEN-06 sin implementar | — |
