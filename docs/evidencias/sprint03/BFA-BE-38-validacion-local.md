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

## Integración con GEN-39-BE (26-nov-2026)

Se incorpora el lector del PR #157 y se retira la consulta E9 anterior de FinalizacionEvaluacionService y su ruta duplicada. E9 ya no fija revision=1 ni entrega factores del contenedor como revisión publicada. Las pruebas anteriores de presentación se trasladan al módulo de consulta; la integración E8 consulta ese mismo caso de uso.

El lector comprueba revisión exacta, validación, formulario, audiencia y alcance. Los tests usan datos/sesiones reales ficticias y una propuesta DDL aislada. El SQL no es una migración instalada: requiere validación y nombre del custodio, y coordinación con BFA-BE-31.

Comprobación conjunta actualizada sobre main `12138f1003f06faf080a024284d5888a2bc1f042`: 443 pruebas backend sin fallos/errores. Incluye los casos de E8 y los 12 casos reales de persistencia/autenticación de E9; no acredita instalación del esquema en producción.

## Pendientes que impiden la aceptación completa

E8 por HTTP todavía invoca la sobrecarga sin puntuaciones: falta conectar los calificadores versionados a las respuestas persistidas. La sobrecarga con mapa de prueba no demuestra ese recorrido. No se deben inventar números ni asumir que familias ausentes fueron calculadas.

Quedan además la migración aprobada de revisiones y publicación E12, la prueba en la URL publicada, Testing y aceptación por el revisor indicado. No se ha escrito en el VPS ni se ha otorgado testing-ok a esta corrección. Se preservan el historial y los responsables del PR #143.
