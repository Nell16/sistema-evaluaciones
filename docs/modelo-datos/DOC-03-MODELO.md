# DOC-03 Modelo de datos

**Estado: pendiente de definición y aprobación.** No existen tablas del dominio en este esqueleto.

Posibles conceptos por investigar en el análisis funcional: evaluados, períodos, preguntas, opciones, intentos, respuestas, resultados e informes.

El equipo documentador debe acordar entidades, atributos, relaciones, restricciones, trazabilidad y protección de datos **antes** de generar SQL o clases JPA.

## Decisiones registradas en el repositorio

| Fecha | Tarea | Decisión |
| --- | --- | --- |
| 2026-11-17 | GEN-01-BE | `subprueba.factor` (`VARCHAR(80)`, nulo mientras no se cargue): código del factor al que aporta la subprueba según el Documento BFA §5.3. Varias subpruebas pueden compartir factor (S1 y S1b → S1; N1-A y N1-B → N1). Migración `V202611170600__comun_agregar_factor_subprueba.sql`. |
| 2026-11-17 | GEN-01-BE | E2 no guarda `opciones` ni `numeroPreguntas`: los calcula desde `pregunta` y `opcion_respuesta` de la configuración publicada. |
