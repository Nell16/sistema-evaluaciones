# Tema 4 — Identidad, periodos y auditoría (tablas comunes a los 5 tests)

**Tarea:** DOC-35-COM · **Autor:** Diego Martínez · **Fecha:** 2026-11-24 · **Estado:** borrador para revisión (Gabriel Pérez Calero)

Las migraciones de `main` definen las tablas de cuenta, roles, sesiones, recuperación, asignaciones, sujetos, periodos y auditoría. Las PK simples son `BIGINT`; las PK compuestas se indican en cada tabla.

## 1. Cuenta, rol, sesión, recuperación y asignación

`V202611240156__comun_cuenta_y_sesion.sql` crea `cuenta`, `cuenta_rol` y `sesion`. La contraseña y el token se guardan solo como resumen (digest), y el correo es único sin distinguir mayúsculas.

### `cuenta`

| Columna | Clave | Descripción |
| --- | --- | --- |
| `id_cuenta` | PK | Identificador de la cuenta. |
| `correo` | UNIQUE (sin distinguir mayúsculas) | Correo de ingreso. |
| `resumen_contrasena` | — | Resumen de la contraseña; nunca la contraseña. |
| `nombres` | — | Nombres de la persona titular de la cuenta. |
| `apellidos` | — | Apellidos. |
| `estado` | — | Estado de la cuenta: ACTIVA o INHABILITADA. |

### `cuenta_rol`

| Columna | Clave | Descripción |
| --- | --- | --- |
| `id_cuenta` | FK → `cuenta.id_cuenta` | Cuenta que tiene el rol. |
| `rol` | — | Rol asignado. |

La PK compuesta `(id_cuenta, rol)` impide que una cuenta repita el mismo rol.

### `sesion`

| Columna | Clave | Descripción |
| --- | --- | --- |
| `id_sesion` | PK | Identificador de la sesión. |
| `id_cuenta` | FK → `cuenta.id_cuenta` | Cuenta que abrió la sesión. |
| `resumen_token` | — | Resumen del token Bearer; nunca el token. |
| `rol_activo` | — | Rol con el que opera la sesión. La pareja `(id_cuenta, rol_activo)` es FK a `cuenta_rol(id_cuenta, rol)`. |
| `abierta_en` | — | Momento de apertura. |
| `expira_en` | — | Momento de expiración. |
| `cerrada_en` | — | Momento de cierre; vacío mientras la sesión siga abierta. |

La autenticación usa token Bearer con roles (no JWT), según las decisiones del sprint.

### `recuperacion_cuenta`

`V202611251100__comun_recuperacion_cuenta.sql` crea `id_recuperacion` como PK, `id_cuenta` como FK obligatoria a `cuenta.id_cuenta` y `resumen_token` único. `creada_en` y `expira_en` registran la vigencia, y `usada_en` queda nula hasta el uso.

### `cuenta_sujeto_asignado`

`V202611251130__comun_cuenta_sujeto_asignado.sql` crea la asignación explícita. La PK es `(id_cuenta, id_sujeto)`. Ambas columnas son FK obligatorias a `cuenta.id_cuenta` y `sujeto.id_sujeto`, respectivamente.

## 2. Sujeto y periodo

`V202611240830__comun_sujeto_periodo.sql` crea ambas tablas y las FK obligatorias de `evaluacion` que las referencian.

### `sujeto` (ENT-COM-03)

La persona evaluada. **No es una cuenta**: puede existir sin cuenta, y el nombre nunca es clave.

| Columna | Clave | Obligatorio | Restricción |
| --- | --- | --- | --- |
| `id_sujeto` | PK | Sí | Único e inmutable. |
| `codigo_institucional` | UNIQUE si se informa | No | Código externo o CIF; uso por confirmar. |
| `nombres` | — | Sí | No vacío. |
| `apellidos` | — | Sí | No vacío. |
| `fecha_nacimiento` | — | No | Fecha de referencia para calcular la edad. |
| `sexo_referencia` | — | No | Dato opcional del núcleo. |

### `periodo` (ENT-COM-04)

| Columna | Clave | Obligatorio | Restricción |
| --- | --- | --- | --- |
| `id_periodo` | PK | Sí | Único. |
| `codigo` | UNIQUE | Sí | Código administrativo, no vacío. |
| `nombre` | — | Sí | Denominación visible, no vacía. |
| `fecha_inicio` | — | No | Coherente con `fecha_fin`. |
| `fecha_fin` | — | No | No anterior a `fecha_inicio` si ambas existen. |
| `activo` | — | Sí | Disponibilidad administrativa. |

## 3. Evento de auditoría

Registra quién hizo qué, sobre qué y cuándo. El nombre físico `evento_auditoria` y las columnas en snake_case, como en la PR #89, los fijó la revisión de Gabriel Pérez Calero (comentario del 24/11 a las 11:22 en DOC-35-COM). La migración `V202611251204__comun_evento_auditoria.sql` fija sus columnas, PK y FK en `main`.

| Pregunta | Columna | Clave | Obligatorio | Descripción |
| --- | --- | --- | --- | --- |
| — | `id_evento_auditoria` | PK | Sí | Identificador del evento (regla 2 del núcleo, D-09). |
| **Quién** | `id_cuenta` | FK → `cuenta.id_cuenta` | Sí | Cuenta que actuó. No es el `sujeto` por defecto. |
| **Qué** | `accion` | — | Sí | Tipo de acción (catálogo por validar; lo fija el servidor). Se usa `accion` y no `tipo`, para no confundirlo con `entidad_tipo`. |
| **Sobre qué** | `entidad_tipo`, `entidad_id` | — | Sí | Entidad afectada e id; deben ser coherentes entre sí. |
| **Cuándo** | `ocurrido_en` | — | Sí | Momento oficial, fijado por el servidor. |
| Contexto | `id_evaluacion`, `id_evaluacion_subprueba` | FK opcionales | No | Evaluación y ejecución afectadas; obligatoria la evaluación en eventos de ejecución o resultado, no en los administrativos globales. Las FK apuntan a las PK de `evaluacion` y `evaluacion_subprueba`. |
| Detalle | `detalle` | — | No | Motivo y referencias. Nunca imágenes, claves ni respuestas completas. |

El atributo lógico `actorRef` de ENT-COM-11 («cuenta o identidad técnica») se reemplaza por `id_cuenta`, que sí puede ser clave foránea y es lo que sostiene la cardinalidad 1 / 0..N de la sección 4. Según D-09, si se registran acciones automáticas del sistema sin persona, se usa una cuenta técnica en `cuenta`; no se crea ninguna estructura distinta.

## 4. Cardinalidades

Formato «padres / hijos» (padres por hijo y hijos por padre).

| Relación | Cardinalidad | Justificación |
| --- | --- | --- |
| `cuenta` → `cuenta_rol` | 1 / 0..N | Una cuenta puede tener varios roles. |
| `cuenta` → `sesion` | 1 / 0..N | Una cuenta abre sesiones en distintos momentos. |
| `cuenta_rol` → `sesion` | 1 / 0..N | La sesión referencia un rol asignado a su cuenta. |
| `cuenta` → `recuperacion_cuenta` | 1 / 0..N | Cada recuperación corresponde a una cuenta. |
| `cuenta` y `sujeto` → `cuenta_sujeto_asignado` | 1 / 0..N (cada relación) | Cada asignación une una cuenta y un sujeto. |
| `cuenta` → `evento_auditoria` (`id_cuenta`) | 1 / 0..N | Cada evento tiene una cuenta como actor; una cuenta genera muchos eventos. |
| `sujeto` → evaluación | 1 / 0..N | Cada intento identifica a su titular; el alta del sujeto puede ser anterior. |
| `periodo` → evaluación | 1 / 0..N | Un periodo reúne muchas evaluaciones. |
| evaluación → `evento_auditoria` | 0..1 / 0..N | El evento administrativo puede no pertenecer a ninguna evaluación. |
| `evaluacion_subprueba` → `evento_auditoria` | 0..1 / 0..N | El evento puede referenciar la ejecución afectada. |

## 5. Criterios de aceptación

- [x] Usa los nombres de la PR #89 tal cual en `cuenta`, `cuenta_rol` y `sesion`.
- [x] `sujeto` y `cuenta` son entidades distintas, con propósito y claves propias.
- [x] Cardinalidades escritas (sección 4).
- [x] Auditoría con `evento_auditoria`, columnas en snake_case y `id_cuenta` como FK → `cuenta.id_cuenta` (corrección de la revisión del 24/11).
- [x] Nombres físicos de `sujeto` y `periodo` confirmados en la migración `V202611240830`.

## Fuentes

- Tarea ClickUp DOC-35-COM (columnas de #89 y regla «sujeto ≠ cuenta»).
- PR #89 «[GEN-06-BE] Persistir cuentas y sesiones…» y migración `V202611240156`: nombres de tablas, correo único y resúmenes.
- Documento general, fichas ENT-COM-03, ENT-COM-04 y ENT-COM-11, según su transcripción en S02-WEI-DOC-07 v01.
- Comentario de revisión de Gabriel Pérez Calero en DOC-35-COM (24/11/2026, 11:22): nombre físico, snake_case y `id_cuenta`.
