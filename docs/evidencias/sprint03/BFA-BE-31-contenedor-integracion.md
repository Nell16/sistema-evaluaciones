# bfa-be-31: corrección del contenedor y precondiciones

base: pr #166, `38359ab`, integrado localmente con main `6b77b2a`.

el adaptador actualiza la entidad persistida y conserva su id, fecha y referencia de revisión. fuerza el borrado de factores antes de insertar sus sustitutos para respetar la unicidad por resultado y código. la regresión con postgres reprodujo `resultado_id_evaluacion_key` antes del cambio y pasó después; comprueba un contenedor, sustitución de factores y conservación del cero.

el fixture de finalización ahora declara selección y crea la cuenta ficticia usada por auditoría, sin depender del id 1 ni del orden de la suite. se normalizó el formato de los dos archivos de revisión que fallaban spotless.

validación final: jdk 17, postgres 16 aislado; `mvn -f backend/pom.xml verify`, 459 pruebas, cero fallos, errores y omisiones. localmente se limitaron hikari a dos conexiones por contexto y la caché de contextos a cuatro para compartir el servidor entre suites. no se cambiaron los límites de producción.

esta corrección no acepta la publicación e12: el ddl de #166 todavía define `revision_resultado` con columnas incompatibles con el modelo común de #163. debe adaptar la escritura de revisiones, escalas, audiencia y validación al esquema común antes de fusionarse. `create table if not exists` no resuelve esa incompatibilidad. no hay prueba del vps ni testing-ok independiente por esta evidencia.

se conservan el historial y los aportes de rodrigo y nelson; estos cambios son apoyo de integración.
