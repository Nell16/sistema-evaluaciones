# PostgreSQL y Flyway — estructura inicial

PostgreSQL 17 está configurado en `compose.yaml`. Spring Boot incorpora el controlador JDBC y Flyway para futuras migraciones.

**Actualmente no hay tablas, datos ni migraciones del negocio.** La carpeta `migrations/` está reservada.

Cuando DOC-03 apruebe el modelo, el equipo agregará archivos `V1__descripcion.sql`, `V2__descripcion.sql`, etc. No editar migraciones ya aplicadas.

**Flyway ≠ Flyweight:** Flyway gestiona migraciones de base de datos; Flyweight es un patrón de diseño. Se usa Flyway según el contexto de los documentos del curso.
