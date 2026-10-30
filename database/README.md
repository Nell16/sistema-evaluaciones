# PostgreSQL y Flyway — estructura inicial

PostgreSQL 17 está configurado en `compose.yaml`. Spring Boot incorpora el controlador JDBC y Flyway para futuras migraciones.

**Actualmente no hay tablas, datos ni migraciones del negocio.** La carpeta `migrations/` está reservada.

Cuando DOC-03 apruebe el modelo, el equipo agregará migraciones con el formato
`V<AAAAMMDDHHmm>__<modulo>_<descripcion>.sql` (ej. `V202611031530__bfa_crear_tablas.sql`).
La fecha y hora evita que dos ramas usen la misma versión. Spring Boot las empaqueta desde esta
carpeta (ver `backend/pom.xml`). No editar ni borrar migraciones ya fusionadas: el CI lo rechaza.

**Flyway ≠ Flyweight:** Flyway gestiona migraciones de base de datos; Flyweight es un patrón de diseño. Se usa Flyway según el contexto de los documentos del curso.
