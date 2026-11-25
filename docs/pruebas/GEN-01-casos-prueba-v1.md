# Casos de prueba GEN-01 — catálogo y detalle de tests (v1)

## Identificación

- **Tarea de Testing:** [GEN-01-TEST](https://app.clickup.com/t/86bcw9z30) (responsable: Víctor Cabrera)
- **Tarea de Desarrollo:** [GEN-01-BE](https://app.clickup.com/t/86bcvah4w) (PR #51)
- **Ejecutó:** Davide Patiño, autor del PR #51. Por eso la conformidad y `testing-ok` quedan a cargo del equipo de Testing o de Integración, que revisan esta evidencia.
- **Versión y fecha:** v1 · 2026-11-17
- **PR y SHA probados:** #51 · head `9257bdb` (`pull/51/merge` `84520d4` sobre `main` `d33f6a8`)
- **Entorno:** local; PostgreSQL 17 (`postgres:17-alpine` en Podman); backend Java 17 compilado desde `pull/51/merge`; Postman/newman 6.2.1
- **Resultado:** 14 PASÓ · 2 BLOQUEADO · 0 FALLÓ

## Fuentes

| Fuente | Uso |
| --- | --- |
| Criterios de aceptación de GEN-01-BE y GEN-01-TEST (ClickUp) | Base de los casos |
| `docs/api/openapi.yaml` del PR #51 (E1, E2, `ErrorResponse`) | Campos, códigos HTTP y formato de error. E1: «Devuelve los tests activos del catálogo» |
| RF-001, CU-BFA-P01, CU-BFA-P02 | Objetivo |
| `docs/modelo-datos/DOC-03-MODELO.md` del PR #51 | `factor`, `opciones` y `numeroPreguntas` |

## Cobertura de criterios

| Criterio | Casos |
| --- | --- |
| GEN-01-BE: ambos endpoints cumplen el contrato | 001–011 |
| GEN-01-BE: el catálogo devuelve solo los tests activos | 002 |
| GEN-01-BE: el detalle presenta las subpruebas y configuraciones de la edición publicada | 003–008 |
| GEN-01-BE: catálogo vacío y test inexistente | 001, 011 |
| GEN-01-BE: no se exponen entidades JPA, claves de corrección ni información restringida (CA-09 del modelo, que GEN-07 deja para GEN-01-TEST) | 002, 003 (solo los campos del contrato) |
| GEN-01-BE: las pruebas automatizadas pasan | `mvn verify`: 33 pruebas en verde ([log](../evidencias/GEN-01/GEN-01-mvn-verify-local-2026-11-17.txt)) |
| GEN-01-TEST: subpruebas, duraciones y configuraciones | 004–008 |
| GEN-01-TEST: esquemas, códigos HTTP, campos y errores | 001–011 |
| Contrato: 401 y 403 | 015, 016 (bloqueados) |
| `factor` protegido como el resto de la subprueba | 012–014 |

## Datos de prueba

Ficticios, en [`GEN-01-datos-prueba.sql`](../evidencias/GEN-01/GEN-01-datos-prueba.sql):

| Test | Estado | Ediciones | Subpruebas (factor) |
| --- | --- | --- | --- |
| TC01-T1 | activo | v1 publicada (1 configuración), v2 publicada después (3 configuraciones), v3 borrador | SA (FA), SB (FA), SC (FC) |
| TC01-T2 | activo | v1 borrador | SX (sin factor) |
| TC01-T3 | inactivo | v1 publicada | SX (sin factor) |
| TC01-T4 | activo | v1 publicada | SY (sin factor) |

En v2 de T1: SA es selección con 3 preguntas de 4 opciones, 180 s y práctica; SB es selección con 2 preguntas de 3 y 4 opciones, sin tiempo ni práctica; SC es texto libre con 120 s, máximo 20 entradas y práctica. Las configuraciones se insertan fuera de orden.

## Cómo repetir

1. Levantar PostgreSQL 17 vacío y el backend de `pull/51/merge` (Flyway aplica las migraciones).
2. `newman run tests/api/GEN.postman_collection.json --folder "1 Catálogo vacío (antes de cargar datos)" --env-var base_url=<url>`
3. `psql -f docs/evidencias/GEN-01/GEN-01-datos-prueba.sql`: el último `SELECT` muestra el id de TC01-T3.
4. `newman run tests/api/GEN.postman_collection.json --folder "2 Catálogo y detalle (con GEN-01-datos-prueba.sql)" --env-var base_url=<url> --env-var t3Id=<id>`. La carpeta se ejecuta completa y en orden: 002 guarda los ids de T1, T2 y T4.
5. `newman run tests/api/GEN.postman_collection.json --folder "3 Observaciones" --env-var base_url=<url>` para O-2.
6. `psql -f docs/evidencias/GEN-01/GEN-01-casos-bd.sql` para 012–014.

No se ejecuta la colección completa de una vez: la carpeta 1 exige la base sin datos y la 2, la base cargada.

## Casos

Precondición común: base migrada con el PR #51; desde 002, datos de `GEN-01-datos-prueba.sql`. Fecha de ejecución: 2026-11-17. Evidencia: API en [`GEN-01-ejecucion-api-2026-11-17.txt`](../evidencias/GEN-01/GEN-01-ejecucion-api-2026-11-17.txt); BD en [`GEN-01-ejecucion-bd-2026-11-17.txt`](../evidencias/GEN-01/GEN-01-ejecucion-bd-2026-11-17.txt).

| ID | Tipo | Precondición | Pasos | Resultado esperado | Resultado obtenido | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-GEN-01-001 | API | Base sin datos | `GET /api/v1/tests` | 200 y `[]` | 200 y `[]` | PASÓ |
| TC-GEN-01-002 | API | Datos cargados | `GET /api/v1/tests` | 200; T1 (3), T2 (1) y T4 (1); sin T3; cada elemento solo con `id`, `codigo`, `nombre`, `numeroSubpruebas` | Igual al esperado | PASÓ |
| TC-GEN-01-003 | API | Id de T1 | `GET /api/v1/tests/{t1Id}` | 200; solo `id`, `codigo`, `nombre`, `versionFormularioId`, `subpruebas`; cada subprueba solo con los 10 campos del contrato; 3 configuraciones (v2, no v1) | Igual al esperado | PASÓ |
| TC-GEN-01-004 | API | Id de T1 | `GET /api/v1/tests/{t1Id}` | `orden` 1, 2, 3 y códigos SA, SB, SC | Igual al esperado | PASÓ |
| TC-GEN-01-005 | API | Id de T1 | `GET /api/v1/tests/{t1Id}` | SA: `factor` FA, 180 s, SELECCION, `opciones` 4, `numeroPreguntas` 3, `maximoEntradas` nulo, `tienePractica` true | FA, 180, SELECCION, 4, 3, nulo, true | PASÓ |
| TC-GEN-01-006 | API | Id de T1 | `GET /api/v1/tests/{t1Id}` | SB: `factor` FA (compartido), tiempo nulo, `opciones` nulo (3 y 4), `numeroPreguntas` 2, `tienePractica` false | FA, nulo, nulo, 2, false | PASÓ |
| TC-GEN-01-007 | API | Id de T1 | `GET /api/v1/tests/{t1Id}` | SC: TEXTO_LIBRE, 120 s, `opciones` nulo, `numeroPreguntas` 0, `maximoEntradas` 20 | TEXTO_LIBRE, 120, nulo, 0, 20 | PASÓ |
| TC-GEN-01-008 | API | Id de T4 | `GET /api/v1/tests/{t4Id}` | 200; SY con `factor` nulo | Igual al esperado | PASÓ |
| TC-GEN-01-009 | API | Id de T2 (sin edición publicada) | `GET /api/v1/tests/{t2Id}` | 404 `TEST_NO_ENCONTRADO` con `timestamp`, `status`, `codigo`, `mensaje`, `ruta` | Igual al esperado | PASÓ |
| TC-GEN-01-010 | API | Id de T3 (inactivo) | `GET /api/v1/tests/{t3Id}` | 404 `TEST_NO_ENCONTRADO`, formato común | Igual al esperado | PASÓ |
| TC-GEN-01-011 | API | — | `GET /api/v1/tests/999999999` | 404 `TEST_NO_ENCONTRADO`, formato común | Igual al esperado | PASÓ |
| TC-GEN-01-012 | BD | SA en la edición publicada v2 | Cambiar `factor` de SA | Rechazo de `proteger_subprueba()` | «No se puede redefinir una subprueba utilizada en versiones publicadas» | PASÓ |
| TC-GEN-01-013 | BD | SX de T2 solo en borrador | Cambiar `factor` de SX (en transacción revertida) | Se permite | `UPDATE 1`, factor FX | PASÓ |
| TC-GEN-01-014 | BD | SX de T2 | `factor` en blanco | Rechazo por `chk_subprueba_factor` | Violación de `chk_subprueba_factor` | PASÓ |
| TC-GEN-01-015 | API | Sin sesión | E1 y E2 sin token | 401 `NO_AUTENTICADO` | No ejecutable | BLOQUEADO |
| TC-GEN-01-016 | API | Sesión sin permiso | E1 y E2 con cuenta sin rol | 403 `SIN_PERMISO` | No ejecutable | BLOQUEADO |

**Bloqueo de 015 y 016:** la autenticación es [GEN-06-BE](https://app.clickup.com/t/86bcvaj7h), sin PR al 2026-11-17. Próximo paso: ejecutarlos cuando GEN-06-BE se fusione.

## Observaciones (no bloquean)

| ID | Observación | Dónde |
| --- | --- | --- |
| O-1 | E1 incluye tests activos sin edición publicada (T2), porque el contrato dice «activos» y no excluye los que no tienen edición publicada; E2 responde 404 para ellos. Si el catálogo solo debe mostrar tests aplicables, hay que ajustar el contrato. | 002, 009 |
| O-2 | `GET /api/v1/tests/abc` responde 400 con el formato por defecto de Spring, no con `ErrorResponse`. El contrato de E2 no define 400; queda para el manejo común de errores. | Carpeta «3 Observaciones» del log de API |

## Evidencia

- Colección: [`tests/api/GEN.postman_collection.json`](../../tests/api/GEN.postman_collection.json), carpeta `GEN-01` (variables `base_url` y `t3Id`; en la aplicación de Postman, vaciar `t1Id`, `t2Id` y `t4Id` antes de repetir)
- Ejecución API: [`GEN-01-ejecucion-api-2026-11-17.txt`](../evidencias/GEN-01/GEN-01-ejecucion-api-2026-11-17.txt)
- Casos y ejecución BD: [`GEN-01-casos-bd.sql`](../evidencias/GEN-01/GEN-01-casos-bd.sql), [`GEN-01-ejecucion-bd-2026-11-17.txt`](../evidencias/GEN-01/GEN-01-ejecucion-bd-2026-11-17.txt)
- `mvn verify` con PostgreSQL: [`GEN-01-mvn-verify-local-2026-11-17.txt`](../evidencias/GEN-01/GEN-01-mvn-verify-local-2026-11-17.txt)

## Registro de ejecución

| Fecha | PR/SHA | Ejecutó | Resultado | Evidencia / bug |
| --- | --- | --- | --- | --- |
| 2026-11-17 | #51 / `9257bdb` (`pull/51/merge` `84520d4`) | Davide Patiño | 14 PASÓ · 2 BLOQUEADO | Logs de API, BD y `mvn verify` enlazados arriba; sin bugs |

### Revisión final

- [x] Los casos corresponden a criterios aprobados y a la tarea correcta.
- [x] Se ejecutaron contra el commit vigente del PR.
- [x] Se registraron resultados reales y evidencia verificable.
- [x] No contiene secretos ni datos personales reales.
- [ ] Se comentó al PR y se informó a Integración.
- [ ] Testing o Integración revisó la evidencia y colocó `testing-ok`.
