# Propuesta de persistencia de revisiones para GEN-39-BE

`comun_revisiones_resultado.sql` es una propuesta revisable. No pertenece a Flyway y no se ejecuta al iniciar la aplicación. No sustituye el procedimiento de custodia.

Antes de instalarla, Kisnner debe validar el esquema y reservar el nombre de migración. Se debe coordinar con BFA-BE-31 (revisión/publicación) para mantener una sola estructura de revisiones. `regla_calificacion` pertenece a la versión del formulario según DOC-34. Las referencias exclusivas de BFA a clave, baremo y rango quedan pendientes de las FK que sus módulos aprobados deban incorporar; esta propuesta no crea claves ni baremos paralelos.

La prueba `ResultadosPersistenciaIntegrationTest` carga el SQL dentro de su transacción aislada, con cuentas y factores ficticios, y revierte el DDL al finalizar cada prueba. No prueba un despliegue del esquema en el VPS.

La consulta pendiente funciona con las tablas ya integradas, sin esta propuesta. Una consulta publicada exige la revisión exacta, validada, del mismo contenedor y formulario, alcance cuenta–sujeto y audiencia del rol. El sujeto no recibe puntajes, conforme a GEN-39-BE vigente. Se auditan solo las consultas exitosas; un fallo de auditoría impide entregar resultados.

Los percentiles requieren además un rango exacto del mismo baremo y factor, y asignación aprobada para formulario/reglas. Si falta el módulo normativo o una referencia no coincide, E9 falla sin entregar valores. La integración de esos módulos requiere su revisión vigente; no se inventan percentiles.
