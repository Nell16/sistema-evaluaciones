# Aplicación y respuestas

**Tarea:** DOC-33 (subtarea DOC-33-BFAW) · **Estado:** borrador para revisión del verificador.

Este documento propone las tablas de «Aplicación y respuestas» que necesitan BFA y Weill. Todas cuelgan del núcleo de DOC-31 (`DOC-03-MODELO.md`, sección «Núcleo y glosario») y usan sus nombres y su glosario. No contiene ítems, claves ni valores de baremos reales.

## BFA y Weill

**Insumos:** núcleo y glosario (DOC-31); Documento BFA, ENT-BFA-01 y ENT-BFA-11; Nota Weill (DOC-36); decisiones D-03, D-04, D-05 y D-08 de DOC-42.

### Qué se reutiliza del núcleo

Primero se reutilizan las tablas del núcleo. Solo se crea una tabla propia cuando el dato no cabe en ellas.

| Necesidad | Dónde se guarda | Test |
| --- | --- | --- |
| Evaluación: sujeto, período, versión de formulario, estado y fechas | `evaluacion` | BFA y Weill |
| Subpruebas incluidas en una aplicación completa o parcial | Una fila de `evaluacion_subprueba` por cada subprueba incluida | BFA |
| Estado y tiempos de cada aplicación de subprueba | `evaluacion_subprueba`: `estado`, `fecha_inicio`, `fecha_fin_limite`, `fecha_cierre` | BFA y Weill |
| Respuesta de selección (alternativas de BFA; opciones 1 a 8 de Weill) | `respuesta_evaluacion.id_opcion` | BFA y Weill |
| Respuesta de texto libre (Fluidez Verbal) | `respuesta_evaluacion`: `valor_texto` y `orden_entrada`, con `id_pregunta` e `id_opcion` nulas | BFA |
| Letra inicial de Fluidez Verbal | `evaluacion_subprueba.letra_inicial` | BFA |
| Cambios de respuesta | `evento_auditoria`: el endpoint E7 escribe la respuesta y un evento | BFA y Weill |
| Cierre por tiempo | `evaluacion_subprueba`: `fecha_cierre` frente a `fecha_fin_limite` | BFA |
| Weill sin límite de tiempo | `configuracion_subprueba.tiempo_limite_segundos` nulo y `fecha_fin_limite` nula | Weill |
| Horas de inicio y finalización de la hoja de aplicación de Weill | `evaluacion.fecha_inicio` y `evaluacion.fecha_fin` | Weill |

### Tabla propuesta: `aplicacion_bfa`

Extensión de `evaluacion` con el contexto propio de una aplicación BFA (ENT-BFA-01), pendiente de migración en `main`.

| Campo | Detalle |
| --- | --- |
| Nombre | `aplicacion_bfa` |
| PK | `id_evaluacion` (también es FK) |
| FK | `id_evaluacion` → `evaluacion` |
| Atributos principales | `alcance` (COMPLETA, PARCIAL; obligatorio), `modalidad` (INDIVIDUAL, COLECTIVA; obligatorio), `finalidad_examen` (texto; opcional), `edad_al_aplicar` (entero; opcional) |
| Cardinalidad | `evaluacion` 1 — 0..1 `aplicacion_bfa`. Una evaluación BFA iniciada exige exactamente una. |

**Por qué es tabla propia:** `alcance`, `modalidad`, `finalidad_examen` y `edad_al_aplicar` son datos propios de BFA y no caben en `evaluacion` sin alterar el núcleo. La versión de formulario no se repite aquí: se obtiene desde `evaluacion.id_version_formulario`. Tampoco se repiten sujeto, período, fechas ni estado.

**Reglas de la tabla:**

- Con `alcance` COMPLETA, las filas de `evaluacion_subprueba` deben corresponder a toda la configuración de la versión.
- Con `alcance` PARCIAL, deben ser un subconjunto no vacío y sin duplicados.
- `edad_al_aplicar` es un dato histórico derivable de `sujeto.fecha_nacimiento` y de la fecha de aplicación. No es una nota normativa.

### Relaciones y cardinalidades

| Relación | Cardinalidad | Nota |
| --- | --- | --- |
| `evaluacion` — `aplicacion_bfa` | 1:0..1 | La FK es la propia PK de `aplicacion_bfa`. |
| `evaluacion` — `evaluacion_subprueba` | 1:N | Núcleo. Una fila por subprueba incluida. |
| `configuracion_subprueba` — `evaluacion_subprueba` | 1:N | Núcleo. La configuración puede aplicarse en muchas evaluaciones. |
| `evaluacion_subprueba` — `respuesta_evaluacion` | 1:0..N | FK compuesta `(id_evaluacion, subprueba_codigo)` → `evaluacion_subprueba(id_evaluacion, codigo)`. |
| `pregunta` — `respuesta_evaluacion` | 0..1:0..N | FK nula en Fluidez Verbal. |
| `opcion_respuesta` — `respuesta_evaluacion` | 0..1:0..N | FK nula en texto libre. |
| `evaluacion` — `evento_auditoria` | 0..1:0..N | La FK es opcional. La cuenta que actuó es obligatoria. |

### Fluidez Verbal

ENT-BFA-11 (AplicacionFluidezBFA) no crea tabla. Según la regla 5 del núcleo, Fluidez Verbal vive en columnas de `evaluacion_subprueba` y `respuesta_evaluacion`:

- La letra asignada se guarda en `evaluacion_subprueba.letra_inicial`. Se fija antes de iniciar y no cambia durante la ejecución.
- Cada palabra es una fila de `respuesta_evaluacion` con `valor_texto` (texto original) y `orden_entrada`. Se admiten hasta 75 entradas por ejecución, y no son 75 preguntas.
- La calificación de las palabras corresponde a DOC-34, no a este tema.

### Weill

Weill Forma A no necesita tablas propias en este tema, porque cabe por completo en el núcleo:

- Es una sola subprueba de 60 preguntas (D-03), por lo que hay una fila de `evaluacion_subprueba` por evaluación.
- La lámina no se guarda: se deriva de la posición de la pregunta, doce por lámina.
- Cada pregunta tiene una opción única, de 1 a 8, guardada en `respuesta_evaluacion.id_opcion`.
- Se puede finalizar con preguntas omitidas, y valen 0 (D-04).
- No tiene límite de tiempo por defecto (D-05). Si se aplicara uno, se configura en `configuracion_subprueba.tiempo_limite_segundos`.
- Los subtotales por lámina, el grupo y el baremo se resuelven en DOC-34.

### Incógnitas para el Registro de incógnitas

| ID | Incógnita e impacto | Quién |
| --- | --- | --- |
| A-01 | La PK de `aplicacion_bfa` es `id_evaluacion` y no `id_aplicacion_bfa`, porque ENT-BFA-01 prohíbe un segundo identificador. Es una excepción a la regla 2 del núcleo, como la de `solicitud_calculo_subprueba`. Confirmarla. | Integración |
| A-02 | `alcance` puede derivarse de las filas de `evaluacion_subprueba`. ENT-BFA-01 lo guarda como atributo. Decidir si se guarda o se deriva. | Integración y DOC-34 |
| A-03 | La modalidad COLECTIVA exige la entidad común de sesión colectiva (D02 de ENT-BFA-01), que no existe. | Iván |
| A-04 | Confirmar si `finalidad_examen` y `edad_al_aplicar` son obligatorias (D01 de ENT-BFA-01). | Iván |
| A-05 | `maximo_entradas` aparece en `configuracion_subprueba` y en `evaluacion_subprueba`. ENT-BFA-11 (D12) pide consultarlo desde la configuración. Decidir si la columna de `evaluacion_subprueba` sobra. | GEN-37-BE |
| A-06 | La respuesta se enlaza a su ejecución mediante `(id_evaluacion, subprueba_codigo)`, también en Fluidez Verbal. | Resuelta en `V202611240800` |
| A-07 | Cambios de respuesta: E7 guarda una fila por pregunta. Decidir si basta `evento_auditoria` o se necesita un historial consultable. | Integración |
| A-08 | Weill no tiene límite de tiempo. Definir quién cierra la aplicación de subprueba y con qué `fecha_cierre`, y si el cierre por tiempo se distingue por `fecha_cierre` igual a `fecha_fin_limite`. | Integración e Iván |
| A-09 | El núcleo lista `recurso_ref` en `opcion_respuesta`, pero la incógnita I-12 de DOC-42 dice que no tiene recurso propio. Weill necesita una imagen por opción. | DOC-32 y Backend |
| A-10 | La Nota Weill dice que se registra «una opción o una omisión». El núcleo solo prevé `id_opcion` nula en texto libre. Decidir si una omisión es una fila con `id_opcion` nula o la ausencia de fila. | DOC-34 y Backend |

## Thurstone, Allport y Orientación vocacional

**Insumos:** núcleo y glosario (DOC-31); Nota Thurstone (DOC-37); Nota Allport (DOC-38) y su registro de incógnitas; Nota Orientación vocacional (DOC-39) y su registro de incógnitas. Las notas están pendientes de aceptación de Iván, por lo que lo siguiente es una propuesta para revisión, no una definición aprobada del modelo.

La clave de corrección, los baremos y el cálculo por escala o área no forman parte de este tema: corresponden a los temas de claves, baremos y resultados. No se incluyen ítems, claves ni valores de baremos reales.

### Qué se reutiliza del núcleo

Primero se reutilizan las tablas existentes en DOC-31. Esta sección describe su uso para los tres instrumentos; no vuelve a definir ni duplica sus estructuras, PK o FK canónicas.

| Necesidad                                                               | Thurstone                                                                 | Allport                                                                                                      | Orientación vocacional                                                                                                                                                                    |
| ----------------------------------------------------------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Evaluación y versión de formulario                                      | `evaluacion` conserva sujeto, período, versión, estado y fechas.          | `evaluacion` conserva sujeto, período, versión, estado y fechas.                                             | `evaluacion` conserva sujeto, período, versión, estado y fechas. F1 y F2 podrían representarse como versiones distintas (OV-04).                                                          |
| Partes / subpruebas (`evaluacion_subprueba`, `configuracion_subprueba`) | Una sola subprueba para los 100 pares de ocupaciones.                     | Dos subpruebas: Parte I (30 ítems) y Parte II (15 situaciones efectivas, pendiente de confirmar por ALL-01). | Se proponen ocho subpruebas, una por bloque temático (B-06).                                                                                                                              |
| Preguntas (`pregunta`)                                                  | 100 preguntas, una por par.                                               | 30 preguntas en Parte I y 15 situaciones en Parte II, si se confirma ALL-01.                                 | Preguntas de cada bloque, según el formato que se adopte (OV-04).                                                                                                                         |
| Opciones (`opcion_respuesta`)                                           | Cuatro opciones por par: opción 1, opción 2, ambas y tachadas.            | Dos alternativas por ítem en Parte I; cuatro alternativas por situación en Parte II.                         | Categorías, Sí/No y pares de rasgos, según cada pregunta.                                                                                                                                 |
| Respuestas (`respuesta_evaluacion`)                                     | Una respuesta por par, referida a `id_opcion`.                            | Una fila por alternativa, asociada a un valor en `valor_respuesta`.                                          | Texto libre en `valor_texto`; selección única en `id_opcion`; selección múltiple, una fila por opción marcada; jerarquía del primero al tercero, una fila por opción con `orden_entrada`. |
| Tiempo límite                                                           | Sin límite; `configuracion_subprueba.tiempo_limite_segundos` nulo (I-01). | Sin límite según F1; no se configura duración.                                                               | Sin límite; `tiempo_limite_segundos` nulo (OV-01).                                                                                                                                        |
| Versión                                                                 | `version_formulario`.                                                     | `version_formulario`; las reglas se vinculan a la edición del formulario.                                    | `version_formulario`; F1 y F2 pueden ser versiones distintas (OV-04).                                                                                                                     |

Una aplicación sin límite se representa con `evaluacion_subprueba.fecha_fin_limite` nula, según el patrón común descrito para Weill (D-05). Las tablas del núcleo conservan sus PK, FK y atributos canónicos según DOC-31: `evaluacion` (`id_evaluacion`; FK a `sujeto`, `periodo`, `test` y `version_formulario`), `evaluacion_subprueba` (`id_evaluacion_subprueba`; FK a `evaluacion` y `configuracion_subprueba`), `configuracion_subprueba` (`id_configuracion_subprueba`; FK a `version_formulario` y `subprueba`), `pregunta` (`id_pregunta`; FK a `configuracion_subprueba`), `opcion_respuesta` (`id_opcion`; FK a `pregunta`) y `respuesta_evaluacion` (`id_respuesta_evaluacion`; FK a `evaluacion`, `pregunta` y opcionalmente `opcion_respuesta`). Sus relaciones se detallan en «Relaciones y cardinalidades».

### Tabla nueva: `valor_respuesta`

Allport no se limita a seleccionar una alternativa: asigna puntos a las alternativas de la Parte I y valores de orden a las alternativas de la Parte II. El núcleo guarda la alternativa elegida en `respuesta_evaluacion`, pero no tiene un atributo para ese valor numérico. Se propone `valor_respuesta` como extensión de `respuesta_evaluacion`; esta sección la define, pero no crea una migración ni modifica el núcleo.

| Campo                 | Detalle                                                                                                                                            |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| Nombre                | `valor_respuesta` (nombre y alcance pendientes de confirmación, B-02).                                                                             |
| PK                    | `id_valor_respuesta`.                                                                                                                              |
| FK                    | `id_respuesta_evaluacion` → `respuesta_evaluacion`; la FK es única.                                                                                |
| Atributos principales | `valor` (entero, obligatorio): puntos asignados a la alternativa.                                                                                  |
| Cardinalidad          | `respuesta_evaluacion` 1 — 0..1 `valor_respuesta`. Cada respuesta puede tener cero o un valor asociado; cada valor pertenece a una sola respuesta. |

**Uso propuesto en Allport:**

- **Parte I:** cada ítem reparte tres puntos entre dos alternativas. Los pares permitidos son (3,0), (0,3), (2,1) y (1,2). Se guardan dos filas de `respuesta_evaluacion`, una por alternativa, y cada una se asocia con su `valor_respuesta`.
- **Parte II:** cada situación ordena cuatro alternativas con los valores 4, 3, 2 y 1, sin repetir; la suma por situación es 10. Se guardan cuatro filas, una por alternativa, cada una con su valor.
- El cero se guarda explícitamente. Una situación sin filas representa una omisión y no equivale a una respuesta con valor cero (ALL-02 y B-05).
- La validación de los pares permitidos y de las sumas se realiza al guardar y al cerrar la subprueba; no se pretende expresarla mediante una restricción simple de la tabla.

### Thurstone

- **Estructura:** una sola subprueba de comparación por pares, sin subpruebas con nombre propio. Comprende 100 pares en una cuadrícula de 10 por 10; cada área aparece en 20 pares.
- **Respuesta:** hay cuatro alternativas por par: marcar la opción 1, marcar la opción 2, marcar ambas o tachar ambas. Se proponen cuatro filas de `opcion_respuesta` por `pregunta`, con códigos `1`, `2`, `AMBAS` y `TACHADAS`. La propuesta las representa como respuestas de selección (`SELECCION`); confirmar que «ambas» y «tachadas» sean opciones válidas y cómo se puntúan (B-03 e I-03).
- **Áreas:** las diez áreas ocupacionales son factores del resultado. Su cálculo corresponde al tema de resultados; aquí no se crean tablas de factores ni se define la calificación.
- **Datos del encabezado:** nombre, fecha, edad y sexo se obtienen de `sujeto` y `evaluacion`. Institución, ciudad y grado no tienen columna en el núcleo; su almacenamiento queda pendiente (B-04).
- **Casilla T:** su significado no está explicado en la fuente revisada (I-04); no se modela.
- **Tiempo:** no se indica tiempo límite; se deja `configuracion_subprueba.tiempo_limite_segundos` nulo (I-01).

### Allport

- **Estructura:** dos partes. La Parte I contiene 30 ítems. La Parte II tiene instrucciones propias y, según la distribución del cuadro de calificación, 15 situaciones efectivas. Esto se deduce del cuadro, no de una numeración limpia; se conserva como incógnita ALL-01. Las partes no equivalen a las seis escalas.
- **Subpruebas:** se proponen dos filas de `configuracion_subprueba`, una para cada parte, porque tienen instrucciones y tipos de respuesta distintos. No se crea una subprueba por escala.
- **Escalas:** son seis: teórico/teorético, económico, estético, social, político y religioso. Cada escala recibe aportes de ambas partes. La correspondencia entre columnas y escalas cambia por página y pertenece a la clave de corrección; no se define aquí.
- **Respuesta:** se conserva una fila de `respuesta_evaluacion` por alternativa, con el valor numérico asociado mediante `valor_respuesta`. En Parte I se asignan tres puntos entre dos alternativas; en Parte II se ordenan cuatro alternativas con 4, 3, 2 y 1.
- **Tiempo:** el formulario F1 declara que no hay límite; no se configura un tiempo límite.
- **Omisiones:** el instrumento permite omitir cuando no se puede decidir, pero la corrección pide todas las respuestas. Se conserva la ausencia de filas como distinta de cero. No se prorratea ni se imputa un valor sin una regla aprobada (ALL-02 y B-05).
- **Formulario F2:** la nota DOC-38 identifica F2 como un cuestionario distinto (Wilson et al., 2002), no como la edición de Allport. Se excluye de la configuración de Allport hasta confirmar la edición autorizada (ALL-05).
- **Baremos y resultado:** la nota DOC-38 indica que las fuentes revisadas no aportan tablas normativas; no se deben inventar percentiles, categorías ni conversiones. La persistencia/publicación del resultado digital queda como propuesta pendiente (ALL-03 y ALL-04).

### Orientación vocacional

- **Estructura:** es una entrevista cualitativa, no un test con puntajes. F1 tiene ocho bloques temáticos y F2 ocho secciones numeradas, con numeración inconsistente en las últimas.
- **Subpruebas:** se propone una subprueba por bloque temático, ya que el núcleo no tiene una tabla de secciones y esta alternativa conserva el agrupamiento sin crear otra tabla (B-06).
- **Tipos de respuesta:** texto libre, opción única, opción múltiple, jerarquía del primero al tercero y pares bipolares o Sí/No. Los pares bipolares y Sí/No se representan como selección con dos opciones. La compatibilidad de estas modalidades con `configuracion_subprueba.tipo_respuesta`, que actualmente admite `SELECCION` y `TEXTO_LIBRE`, queda pendiente en B-01.
- **Opción múltiple:** cada opción marcada se representa como una fila de `respuesta_evaluacion` para la misma pregunta. Esto requiere confirmar que el núcleo y el endpoint E7 permitan varias filas por pregunta (B-09).
- **Jerarquía:** cada elemento ordenado se representa como una fila con su `id_opcion` y su posición en `orden_entrada`.
- **Identificación y contexto:** datos personales, colegio y grado se obtienen de `sujeto` y de las respuestas. Familia, salud, situación económica, historial académico, intereses y proyecto profesional se guardan como respuestas del cuestionario; no se proponen columnas propias para esos datos. Los permisos de acceso a datos de salud y situación económica quedan pendientes (B-08).
- **Resultado:** es un expediente cualitativo, sin puntajes, fórmulas ni baremos; no se calcula por escalas (OV-02 y OV-03). Queda pendiente decidir si se genera una fila de `resultado` o si la evaluación queda sin ella (B-07).
- **Tiempo:** no hay temporizador ni cierre automático por tiempo (OV-01).

### Relaciones y cardinalidades

Las relaciones siguientes reutilizan las FK del núcleo DOC-31, salvo la extensión `valor_respuesta` definida en esta sección.

| Relación                                         | Cardinalidad | Nota                                                                                                                                       |
| ------------------------------------------------ | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `version_formulario` — `configuracion_subprueba` | 1:N          | Una versión puede contener varias configuraciones de subprueba.                                                                            |
| `configuracion_subprueba` — `pregunta`           | 1:N          | Cada configuración contiene sus preguntas.                                                                                                 |
| `pregunta` — `opcion_respuesta`                  | 1:N          | Thurstone propone cuatro opciones por par; Allport y Orientación vocacional usan las alternativas que requiera cada pregunta.              |
| `evaluacion` — `evaluacion_subprueba`            | 1:N          | Allport propone dos subpruebas por evaluación; Orientación vocacional propone ocho. Thurstone usa una.                                     |
| `evaluacion` — `respuesta_evaluacion`            | 1:N          | Una evaluación contiene las respuestas registradas en sus preguntas.                                                                       |
| `pregunta` — `respuesta_evaluacion`              | 1:N          | En selección múltiple y Allport se necesitan varias filas por pregunta; confirmar compatibilidad con E7 y restricciones del núcleo (B-09). |
| `opcion_respuesta` — `respuesta_evaluacion`      | 1:N          | Cada respuesta de selección referencia la opción elegida; en texto libre `id_opcion` puede ser nulo.                                       |
| `respuesta_evaluacion` — `valor_respuesta`       | 1:0..1       | Extensión propuesta para almacenar los puntos/orden asignados por alternativa en Allport. La FK `id_respuesta_evaluacion` es única.        |

### Incógnitas heredadas de las notas

| ID            | Incógnita                                                                      | Efecto en este tema                                                                                              |
| ------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------------------- |
| I-01          | Thurstone no indica tiempo límite.                                             | Se deja sin límite.                                                                                              |
| I-04          | Significado de la casilla T de Thurstone.                                      | No se modela.                                                                                                    |
| ALL-01        | Numeración y variante por sexo de la Parte II de Allport; 15 o 16 situaciones. | No cargar la configuración de la Parte II hasta fijar la numeración.                                             |
| ALL-02        | Omisiones en Allport.                                                          | Se conserva el cero explícito y la omisión como ausencia de filas; no se imputa ni prorratea sin regla aprobada. |
| ALL-03        | Las fuentes F1 revisadas no contienen baremos normativos de Allport.           | Obtener y validar el manual/baremo de la edición y población aplicables; no inventar conversiones.               |
| ALL-04        | Persistencia y publicación del resultado digital de Allport.                   | Confirmar el formato del resultado y su integración con el modelo común.                                         |
| ALL-05        | El formulario F2 corresponde a un cuestionario distinto (Wilson et al., 2002). | Se excluye F2 de la configuración de Allport hasta confirmar la edición autorizada.                              |
| OV-01 a OV-03 | Orientación vocacional no tiene tiempo límite, puntaje ni baremos.             | No se propone temporizador, cálculo de puntajes ni tablas de baremos.                                            |
| OV-04         | F1 y F2 tienen bloques/secciones diferentes.                                   | Pueden requerir versiones de formulario distintas; confirmar estructura y numeración.                            |

### Incógnitas nuevas de este tema

Las siguientes dudas se trasladan al Registro de incógnitas DOC-33. Permanecen abiertas; las propuestas de esta sección no equivalen a decisiones aprobadas.

| ID   | Incógnita e impacto                                                                                                                                                                                                                                                                                                                                                             | Quién                   |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------- |
| B-01 | `configuracion_subprueba.tipo_respuesta` solo admite `SELECCION` y `TEXTO_LIBRE`. Allport (reparto y orden) y Orientación vocacional (opción múltiple y jerarquía) necesitan representaciones adicionales. Esta propuesta describe cómo persistir las respuestas, pero no amplía el catálogo del núcleo. Decidir si se agregan tipos o se conserva la representación propuesta. | Integración e Iván      |
| B-02 | Nombre y alcance de `valor_respuesta`: confirmar si se mantiene genérico o lleva un sufijo del test, como `version_clave_bfa`.                                                                                                                                                                                                                                                  | Custodio de migraciones |
| B-03 | Thurstone: confirmar que «ambas» y «tachadas» sean dos opciones adicionales por par. La puntuación de esas respuestas queda pendiente (I-03).                                                                                                                                                                                                                                   | Iván                    |
| B-04 | Thurstone: institución, ciudad y grado del encabezado no tienen columna en `sujeto` ni en `evaluacion`. Decidir dónde guardarlos.                                                                                                                                                                                                                                               | Iván e Integración      |
| B-05 | Allport: confirmar que el cero se guarde explícitamente y que la ausencia de filas signifique omisión.                                                                                                                                                                                                                                                                          | Iván                    |
| B-06 | Orientación vocacional: confirmar que cada bloque sea una subprueba. Si no, falta una tabla o columna de sección.                                                                                                                                                                                                                                                               | Iván e Integración      |
| B-07 | Orientación vocacional no calcula puntajes. Decidir si la evaluación genera una fila de `resultado` o queda sin ella (OV-02).                                                                                                                                                                                                                                                   | Iván                    |
| B-08 | Orientación vocacional recoge datos de salud y situación económica. Confirmar qué roles pueden verlos.                                                                                                                                                                                                                                                                          | Iván                    |
| B-09 | La selección múltiple y el reparto de Allport necesitan varias filas de `respuesta_evaluacion` por pregunta. Confirmar que el núcleo y E7 no impongan una sola fila por pregunta.                                                                                                                                                                                               | GEN-37-BE               |
