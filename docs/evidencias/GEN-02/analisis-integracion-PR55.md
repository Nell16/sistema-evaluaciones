# GEN-02-BE: análisis de integración del PR #55

Fecha: 2026-11-18
Revisión local: `origin/main` `15b7c432`, PR #55 `798d159`, PR #34 `718cdb0`.

## Resultado

- El PR #23 (GEN-07) ya está incorporado en `origin/main`. Al comparar el PR #55 con esa rama, `git merge-tree` reporta un conflicto textual en `docs/api/openapi.yaml`; no reporta conflictos en los archivos Java o de base de datos de GEN-07.
- Al comparar el PR #55 con el PR #34, el único conflicto textual reportado también es `docs/api/openapi.yaml`.
- Que Git reporte un solo conflicto de texto no significa que ambas implementaciones puedan activarse juntas sin problema.
- El diff de PR #55 contra `origin/main` reemplaza el OpenAPI aprobado: `docs/api/openapi.yaml` tiene 199 líneas añadidas y 1.408 eliminadas. El PR introduce el contrato de dos rutas, cambia `bearerAuth` por autenticación local y cambia título/versión; por tanto, no es solo un conflicto de formato.
- La página del PR #55 en GitHub muestra que hay conflictos pendientes y un check requerido (`ci-ok`) que todavía no reporta estado.

## Riesgo de API al combinar GEN-02 y GEN-03

El PR #55 añade `com.universidad.tests.controller.gen.EvaluacionController`, con `@Profile("gen02-draft")` y base `/api/v1/evaluaciones`. Expone `POST /api/v1/evaluaciones` y `GET /api/v1/evaluaciones/{id}`.

El PR #34 añade `com.universidad.tests.controller.EvaluacionController` bajo la misma base y expone `GET /api/v1/evaluaciones/{evaluacionId}`. Si se activa `gen02-draft` junto con el controlador de GEN-03, ambos controladores declaran el mismo método y patrón GET; Spring podría fallar al registrar el mapeo ambiguo. Debe probarse el contexto de Spring con el perfil antes de integrar ambos.

Los dos controladores también tienen el mismo nombre simple `EvaluacionController`; con la convención de nombres por defecto de Spring, ambos beans pueden llamarse `evaluacionController`, causando una colisión incluso antes de resolver el mapeo HTTP. Para integrarlos se debe conservar un único dueño de E4 o asignar explícitamente un nombre distinto y resolver la ruta.

El `POST` de GEN-02 también representa E3, cuya forma aprobada está en el OpenAPI de DOC-05. La definición de PR #55 se declara propuesta/draft, por lo que no debe reemplazar silenciosamente el contrato aprobado. El OpenAPI necesita una resolución coordinada que preserve el contrato ratificado.

## Solapamiento de persistencia y responsabilidades

- `backend/src/main/resources/gen02/schema-draft.sql` crea tablas provisionales `gen02_test`, `gen02_version` y `gen02_configuracion`. El catálogo versionado de `origin/main` (integrado por GEN-07/#23) ya tiene `test`, `version_formulario` y `configuracion_subprueba`, además de subpruebas, preguntas y opciones. Antes de integrar, GEN-02 debe reutilizar el catálogo común y usar migraciones oficiales para cualquier tabla propia aprobada.
- El borrador agrega `gen02_permiso`, roles y autenticación Basic local en `LocalDraftAuthentication`. Eso no debe convertirse en seguridad de producción: la autorización/autenticación institucional se coordina con GEN-06 y el puerto común de autorización.
- La descripción del PR usa `CU-ID_REAL`; el ID de tarea GEN-02 en ClickUp es `86bcvah52`.

## Recomendación para integración

1. Mantener GEN-02 como draft aislado hasta conciliar su modelo y el contrato E3/E4 con DOC-03/DOC-05 y GEN-03.
2. Restaurar `docs/api/openapi.yaml` a la base aprobada y dejar propuestas pendientes en `docs/api/GEN-02-BE-propuesta-contrato.md`; cualquier cambio al contrato se acuerda con la persona responsable de DOC-05.
3. Coordinar un único controlador/bean para E4 y retirar el esquema paralelo `gen02_*` en favor del catálogo versionado y migraciones acordadas.
4. Dejar autorización provisional detrás del puerto común; no activar Basic ni los roles provisionales en la integración.
5. Actualizar la trazabilidad al ID real `86bcvah52`, resolver conflictos, ejecutar pruebas de contexto y el check requerido `ci-ok`, y mantener el PR en draft hasta que todo eso y la revisión de Integración estén listos.

## Alcance de esta revisión

La revisión se hizo con los refs locales indicados arriba y la vista autenticada de GitHub del PR #55. `git merge-tree` no modifica el árbol de trabajo; no se ejecutó una integración real ni las pruebas de los PR. Este informe es una revisión de Integración, no una aprobación para fusionar. El único cambio preparado para esta contribución es este informe.

