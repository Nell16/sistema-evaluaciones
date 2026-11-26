# BFA-BE-38: corrección del cierre E8

Comprobación del 25-Nov-2026, 23:02 Nicaragua, sobre PR #143, base `bac6a2e110864f3132cea633622a003fb6b790e8`, más la corrección de persistencia de este PR.

## Problema y corrección

`FinalizacionEvaluacionJdbcAdapter` crea el contenedor antes de consolidar. El adaptador JPA intentaba insertar otro contenedor sin PK para la misma evaluación. Ahora actualiza la entidad existente, conserva identidad/fecha/referencia y vacía los factores anteriores antes de insertar sus sustitutos. La prueba de integración usa un tipo de respuesta válido.

## Resultados observados

- Antes: 393 pruebas backend; dos fallos HTTP 500 en E8 y dos errores de fixture por tipo de respuesta nulo.
- Después: 393 pruebas backend, cero fallos/errores; prueba PostgreSQL adicional dentro del caso existente confirma un contenedor, conservación del ID/fecha y reemplazo de factores sin duplicados.
- Frontend: lint, formato, typecheck, 137 pruebas y build correctos. Dependencias locales reutilizadas con `package-lock.json` idéntico; CI realizará `npm ci`.
- Formato Java aplicado y verificado por Spotless.
- PostgreSQL 17 aislado; compilación local con JDK 21 y source/target 17. CI debe corroborar JDK 17.
- HTTP real con cuentas ficticias y Bearer emitido por E14, API local: Newman 12 solicitudes, 13 aserciones, cero fallos; E27 201, E28 200/403/409.
- E8 HTTP 200: factores persistidos y resultado PENDIENTE_VALIDACION. Repetición y dos peticiones concurrentes devuelven el mismo resultado; un registro y un evento de auditoría.
- Fallo deliberado de auditoría en la base aislada: HTTP 500 y rollback de evaluación, resultado y cierre vencido.
- E9 sin publicación: HTTP 200, sin revisión ni factores expuestos.

## Pendientes que impiden la aceptación completa

Esta corrección resuelve los cuatro fallos reproducidos; no acredita toda BFA-BE-38. La consulta E9 publicada del PR #143 todavía fija `revision=1`, usa factores del contenedor y no comprueba la audiencia de una revisión exacta. Requiere integrar GEN-39-BE y el modelo/publicación de revisiones con la custodia de migraciones vigente. E8 por HTTP todavía invoca la sobrecarga sin puntuaciones: la integración de calificadores versionados debe aportar las puntuaciones reales; no se deben inventar números ni confundir el caso con mapa de prueba con ese recorrido.

No se ejecutaron escrituras en el VPS ni se validaron resultados publicados allí. No hay `testing-ok`, aprobación independiente ni despliegue demostrado de esta corrección. Se conserva el aporte y la asignación del autor del PR #143. No cerrar la tarea hasta resolver estos pendientes y obtener la revisión indicada.
