# GEN-39-BE: lectura integrada de E9

Comprobación del 25-nov-2026, Nicaragua. Base main: `8c0014ba91869a5f7659c58ea1826f7c24d63e3c`.

Se recupera el caso de uso, modelos, DTO y pruebas de Daysi (PR #73) y su preparación local de GEN-39, y se conectan con la autenticación Bearer, alcance cuenta–sujeto, PostgreSQL y auditoría reales del main actual. Se conserva su aporte; no se reclaman como nuevos los componentes recuperados.

## Actualización 26-nov-2026

Base actualizada sin conflictos a main `12138f1003f06faf080a024284d5888a2bc1f042`, que incorpora PR #148, #150 y #151. La suite completa sobre esta base pasa 435 pruebas, cero fallos/errores; incluye los 12 casos de persistencia/autenticación real de E9. Los recuentos siguientes documentan las comprobaciones previas y no se suman entre sí.

## Evidencia observada

- Suite independiente sobre main: 430 pruebas backend, cero fallos/errores, Maven verify y Spotless correctos.
- Después se añadieron dos casos de protección: la prueba PostgreSQL completa de E9 pasó sus 12 casos. Estos usan cuentas/sesiones reales y la propuesta DDL en transacciones aisladas.
- Suite combinada con la corrección E8 de PR #153: 438 pruebas sin fallos antes de añadir esos dos casos. No confundir los recuentos con una sola ejecución.
- HTTP real en API local combinada: 16 respuestas esperadas. Pendiente sin factores; revisión 2 exacta aunque existe la 3; puntaje cero conservado; 401 sin sesión; 403 por audiencia, sujeto o falta de alcance; 500 sin validación; dos consultas exitosas auditadas.
- Los tests MVC con puertos de prueba verifican presentación/errores. Los tests PostgreSQL con sesiones reales verifican aparte el filtro Bearer y el acceso; los dobles no se presentan como evidencia de autenticación real.
- Compilación local con JDK 21 y source/target 17; Actions debe corroborar JDK 17 sobre el SHA publicado.

## Pendientes reales

El SQL permanece en `docs/modelo-datos/propuestas/`: no hay nombre Flyway reservado ni migración instalada. Requiere revisión del custodio y coordinación con BFA-BE-31. No activar ni declarar operativo el resultado publicado hasta resolverlo.

La publicación E12, los calificadores E8 y los módulos de claves/baremos mantienen sus tareas y criterios propios. No se ha probado E9 publicado en el VPS ni se ha concedido testing-ok por esta evidencia. GEN-39 requiere además la revisión de Oliver indicada en su ficha.

## Rescate sobre main 479a575

- PostgreSQL 17 y JDK 17 con Maven verify y Spotless: 439 pruebas, 14 de persistencia de E9, sin fallos ni omisiones. Frontend: lint, formato, tipos, 148 pruebas y build correctos.
- Las regresiones reproducen el 500 para Weill y la falta de la PK de revisión en auditoría. Ahora E9 devuelve el bruto verificado de Weill sin consultar las tablas BFA. Dos lecturas de revisiones publicadas distintas dejan dos eventos con la misma evaluación y distintas PK de revisión.
- En una copia aislada, Flyway aplicó la propuesta con una versión temporal y el SQL de baremos de #135. Se ejecutaron 15 pruebas sin cargar el DDL desde el test. Incluyeron pendiente, publicada y BFA con una asignación aprobada ficticia, intervalo exacto y percentil 50. La versión temporal solo se usó en esta comprobación. Sigue pendiente la aprobación y reserva de la migración que se integrará a main.
