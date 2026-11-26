# Motor de evaluación (tablas)

Las tablas del motor se crean en `V202611240800__comun_motor_evaluacion.sql` (GEN-37-BE). Las PK son `BIGINT`. Las FK de sujeto y periodo se agregan en `V202611240830__comun_sujeto_periodo.sql`.

| Tabla | Clave | Notas |
| --- | --- | --- |
| `evaluacion` | `id_evaluacion` | `id_test` y `id_version_formulario` con FK. `id_sujeto` → `sujeto.id_sujeto`; `id_periodo` → `periodo.id_periodo`. Estado `EN_CURSO` / `FINALIZADA`. |
| `evaluacion_subprueba` | `id_evaluacion_subprueba` | FK `id_evaluacion` → `evaluacion` e `id_configuracion_subprueba` → `configuracion_subprueba`. Única por `(id_evaluacion, codigo)`. Estado `PENDIENTE` → `ACTIVA` → `CERRADA`; una sola `ACTIVA` por evaluación. Incluye `permitir_retroceso` (booleano obligatorio, por defecto verdadero), `letra_inicial` y `maximo_entradas` (Fluidez Verbal). |
| `respuesta_evaluacion` | `id_respuesta_evaluacion` | FK compuesta `(id_evaluacion, subprueba_codigo)` → `evaluacion_subprueba(id_evaluacion, codigo)`. FK opcionales a `pregunta` y `opcion_respuesta`. Con `id_pregunta` nulo solo se admiten palabras de la subprueba `FV` (hasta 75 entradas). |
| `solicitud_calculo_subprueba` | `id_evaluacion_subprueba` | La PK también es FK a `evaluacion_subprueba`. `id_evaluacion` referencia `evaluacion`. Entrega durable al evaluador; `procesada_en` nulo = pendiente. |

E4 (`GET /api/v1/evaluaciones/{evaluacionId}`) lee `evaluacion` y `evaluacion_subprueba`; el tiempo restante se calcula en el servidor.
