# Propuesta de persistencia de revisiones para GEN-39-BE

`comun_revisiones_resultado.sql` es una propuesta revisable. No pertenece a Flyway y no se ejecuta al iniciar la aplicación. No sustituye el procedimiento de custodia.

Antes de instalarla, Kisnner debe validar el esquema y reservar el nombre de migración. Se debe coordinar con BFA-BE-31 (revisión/publicación) para mantener una sola estructura de revisiones. `regla_calificacion` pertenece a la versión del formulario según DOC-34. Las referencias exclusivas de BFA a clave, baremo y rango quedan pendientes de las FK que sus módulos aprobados deban incorporar; esta propuesta no crea claves ni baremos paralelos.

La prueba `ResultadosPersistenciaIntegrationTest` carga el SQL dentro de su transacción aislada, con cuentas y factores ficticios, y revierte el DDL al finalizar cada prueba. No prueba un despliegue del esquema en el VPS.

La consulta pendiente funciona con las tablas ya integradas, sin esta propuesta. Una consulta publicada exige la revisión exacta, validada, del mismo contenedor y formulario, alcance cuenta–sujeto y audiencia del rol. El sujeto no recibe puntajes, conforme a GEN-39-BE vigente. Las consultas exitosas quedan auditadas con su evaluación y, si está publicada, con la PK de la revisión consultada. Un fallo de auditoría impide entregar resultados.

En BFA, el percentil requiere un rango exacto del mismo baremo y factor, y una asignación aprobada para el formulario y la versión de reglas. Si falta ese módulo normativo o una referencia no coincide, E9 falla sin entregar valores. Para los demás instrumentos, E9 conserva el puntaje directo verificado y deja nulos el percentil y el rango hasta que se integre su fuente normativa.
