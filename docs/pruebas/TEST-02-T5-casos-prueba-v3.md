# Casos de prueba TEST-02-T5 — recuperación y concurrencia del módulo BFA (v3)

## Identificación

- **Tarea de Testing:** [TEST-02-T5](https://app.clickup.com/t/86bcw9z5c)
- **Tarea padre:** [TEST-02 — probar de forma integrada el módulo BFA](https://app.clickup.com/t/86bcvajaj)
- **Se ejecuta en:** [TEST-02-EJEC](https://app.clickup.com/t/86bcvajau) (Rodrigo Romero), sobre el entorno `main`
- **Tester:** José Ariel Zeledón
- **Versión y fecha:** v3 · 2026-11-23
- **Estado del documento:** diseño de escenarios. No hay PR propio que probar: todos los casos quedan **BLOQUEADOS** hasta que el flujo BFA (E3 a E8) esté fusionado en `main`.

## Historial de cambios

| Versión | Fecha | Cambios |
| --- | --- | --- |
| v1 | 2026-11-09 | Diseño de 16 escenarios de desconexión, recuperación, idempotencia y concurrencia controlada. Consultas C-1 a C-4. |
| v2 | 2026-11-19 | Respuesta de DOC-05 a C-2: TC-TEST-02-T5-09 fija la respuesta del inicio repetido. C-4 respondida en parte: TC-TEST-02-T5-04 agrega la comprobación del cierre al finalizar con E8. Se actualizan el estado de DOC-04 y DOC-08, el responsable de TEST-02-EJEC y los bloqueos. Sin ejecución. |
| v3 | 2026-11-23 | Respuestas de DOC-05 a C-3 y C-4. TC-TEST-02-T5-08 fija `409 EVALUACION_EN_CURSO` en el E3 repetido. TC-TEST-02-T5-04 y 14 fijan que E4 muestra la subprueba vencida como CERRADA con `cerradaEn` igual a `finLimite`, sin esperar a E8. Solo queda abierta C-1. Sin ejecución. |

## Fuentes y estado

| Fuente | Estado (v1: 2026-11-09 · v2: 2026-11-19 · v3: 2026-11-23) | Uso en este documento |
| --- | --- | --- |
| Criterios de aceptación de TEST-02-T5 (ClickUp) | Publicados en la tarea | Base de todos los casos |
| Documento BFA §2: RNF-BFA-GEN-01, 04, 05 y 06; RF-BFA-GEN-06; RNF-FV-02 (requisitos específicos de Fluidez Verbal) | **Aprobado** ([DOC-02](https://app.clickup.com/t/86bctt9b8) `completed`) | Plazos, persistencia, aislamiento y umbral de guardado |
| Documento BFA §5: contratos E3 a E8 y ajustes §5.4.1 · Documento general §5.2 (E22, formato de error) | **Aprobado** ([DOC-05](https://app.clickup.com/t/86bctt9cy) `completed`) | Códigos HTTP, idempotencia de E7 y E8 |
| Documento general §4.6: CA-02 (recuperación y aislamiento) y CA-10 (alcance) | **Aprobado** ([DOC-03](https://app.clickup.com/t/86bctt9bt) `completed`) | Recuperación sin reiniciar tiempos |
| Documento general §5.1.7 (motor común: servidor como autoridad del tiempo, registro idempotente) | **Aprobado** ([DOC-06](https://app.clickup.com/t/86bctt9db) `completed`) | Reglas del motor común |
| Documento general §3.3 y CU-GEN-12, CU-GEN-13, CU-GEN-19 · Documento BFA §3: CU-BFA-P03 y CU-BFA-P04 · RF-GEN del anexo §8.1 | [DOC-04](https://app.clickup.com/t/86bctt9cm) `in review` al diseñar la v1; `completed` desde el 2026-11-10 | Valores marcados como **provisionales (DOC-04)** en la v1. Las marcas se mantienen hasta comparar el texto citado con la versión aprobada. |
| [DOC-07](https://app.clickup.com/t/86bctt9dn) — Calidad documental | `in progress` | Dependencia de TEST-02-T5 en ClickUp |
| [DOC-08](https://app.clickup.com/t/86bcvaj7m) — Material ficticio BFA | `Open` (responsables: Geovanny Ruiz y José Francisco López) | Dependencia de TEST-02-T5: contenido ficticio de las subpruebas |
| Respuestas de DOC-05 a las consultas de GEN-03 y GEN-04 ([comentario de Iván Chavarría en DOC-05](https://app.clickup.com/t/86bctt9cy?comment=1800450000021721), 2026-11-18) | Respuesta del equipo dueño del contrato aprobado. Indica que se reflejan en el Documento BFA §5.3. | C-2 y C-4 (TC-TEST-02-T5-04 y 09) |
| Respuesta de DOC-05 a C-3 y C-4 de TEST-02-T5 ([comentario de Iván Chavarría en DOC-05](https://app.clickup.com/t/86bctt9cy?comment=1800450000034525), 2026-11-21) | Respuesta del equipo dueño del contrato aprobado. Indica que quedó en el Documento BFA (Reglas de E4). | C-3 y C-4 (TC-TEST-02-T5-04, 08 y 14) |
| Plan operativo de Testing (`docs/pruebas/PLAN-OPERATIVO-TESTING.md`) | Vigente | Alcance: «no lanzar carga contra servidor compartido sin autorización» |

Los resultados esperados que provienen de documentos aprobados no llevan marca. Los que dependen de DOC-04 se marcan como **provisional (DOC-04)**.

### Relación con casos de otras tareas

Estos escenarios **no repiten** las pruebas de cada PR: las retoman de forma integrada, en `main` y en condiciones de falla. Casos relacionados:

| Caso | Qué cubre en su PR | Qué agrega TEST-02-T5 |
| --- | --- | --- |
| TC-GEN-03-004, 010, 014 y 027 | Recarga, reintento de E7 sin duplicar | Corte real de conexión desde el navegador, otra sesión y reinicio del servidor |
| TC-GEN-04-018 y 019 | Repetir y simultanear E8 | E8 repetido después de una reconexión |
| TC-BFA-TEST-03-005 | Reenvío idempotente de palabras de FV | Guardado de FV bajo carga controlada (umbral RNF-FV-02) |
| [BUG-02](https://app.clickup.com/90141806675/v/li/901422672550) — «Guardados simultáneos de la misma respuesta devuelven 500» | Defecto abierto en Bugs | TC-TEST-02-T5-11 lo vuelve a comprobar en `main` |

## Matriz de cobertura

**TEST-02-T5**

| Criterio | Casos | Estado |
| --- | --- | --- |
| No se alteran plazos ni se duplican intentos o respuestas. | TC-TEST-02-T5-01 a 14 | BLOQUEADO |
| Se verifican los umbrales RNF aprobados. | TC-TEST-02-T5-15 (y consulta C-1) | BLOQUEADO |

**Entregables de la tarea**

| Entregable | Casos |
| --- | --- |
| Escenarios de desconexión | TC-TEST-02-T5-01, 02, 03, 04, 05 |
| Escenarios de recuperación | TC-TEST-02-T5-01, 04, 05, 06, 07 |
| Escenarios de idempotencia | TC-TEST-02-T5-02, 08, 09, 10, 11 |
| Escenarios de carga controlada | TC-TEST-02-T5-12, 13, 14, 15 |

**Requisitos aprobados relacionados**

| Requisito | Casos |
| --- | --- |
| RNF-BFA-GEN-01 — los límites oficiales no se reinician ni pueden manipularse por refresco, cierre o pérdida temporal de conectividad. | TC-TEST-02-T5-01, 04, 05, 07, 09, 14 |
| RNF-BFA-GEN-04 — la base de datos es el estado oficial; la recuperación no debe duplicar intentos ni alterar el tiempo. | TC-TEST-02-T5-01, 02, 05, 06, 08 |
| RNF-BFA-GEN-06 — múltiples evaluados sin mezcla de respuestas, temporizadores, estados o resultados. | TC-TEST-02-T5-13, 15 |
| CA-02 (DOC-03) — recuperar conserva evaluación, respuestas y finLimite; tiempos en servidor. | TC-TEST-02-T5-01, 05, 06 |
| RNF-FV-02 — guardar palabras sin frenar la escritura; umbral documentado: guardado < 500 ms. | TC-TEST-02-T5-15 |

## Consultas pendientes

Se dejan en la tarea [TEST-02-T5](https://app.clickup.com/t/86bcw9z5c). Mientras sigan abiertas, los casos afectados no fijan ese resultado.

| ID | Consulta | Para | Casos afectados | Estado |
| --- | --- | --- | --- | --- |
| C-1 | RNF-BFA-GEN-01, 04 y 06 no fijan números. Los únicos umbrales documentados son RNF-FV-02 (guardado < 500 ms; «el requisito previo menciona 100 evaluados simultáneos») y el cambio entre sumas < 100 ms. ¿Cuántos evaluados simultáneos es el umbral aprobado? ¿En qué entorno se autoriza la carga controlada? | Documentación (DOC-02) y Scrum de Testing | TC-TEST-02-T5-15 | Abierta |
| C-2 | Si E5 se repite para la misma subprueba que ya está ACTIVA (doble clic), ¿responde `409 HAY_SUBPRUEBA_ACTIVA` o devuelve la misma subprueba activa? El contrato solo lista los 409. | Documentación (DOC-05) | TC-TEST-02-T5-09 | **Respondida** (DOC-05, 2026-11-18, en la consulta C-4 de GEN-03): «200 con el mismo inicio y finLimite (idempotente). 409 HAY_SUBPRUEBA_ACTIVA solo si hay otra subprueba distinta ACTIVA.» |
| C-3 | CU-BFA-P03 dice «reintento técnico no crea otra aplicación». Si E3 se repite con los mismos datos, ¿responde `409 EVALUACION_EN_CURSO` o devuelve la misma evaluación? | Documentación (DOC-05) | TC-TEST-02-T5-08 | **Respondida** (DOC-05, 2026-11-21): «E3 repetido con los mismos datos responde 409 EVALUACION_EN_CURSO, como ya dice E3; para retomar la evaluación se usa E4.» |
| C-4 | Si nadie consulta durante la desconexión, ¿quién registra el cierre por vencimiento: un proceso del servidor o la primera consulta (E4) al reconectar? CU-BFA-P04 dice «procesa el mismo cierre idempotente». Es la misma duda que C-1 de GEN-04. | Documentación (DOC-04) | TC-TEST-02-T5-04, 14 | **Respondida** (DOC-05). 2026-11-18, en la consulta C-1 de GEN-04: «E8 registra el cierre vencido (idempotente, con finLimite como fecha de cierre) y luego finaliza: 200.» 2026-11-21: «Si finLimite ya venció, E4 muestra la subprueba CERRADA con cerradaEn = finLimite aunque todavía no se haya llamado E8; es el mismo valor que registraría E8.» |

## Datos de prueba (ficticios)

Todo es inventado: no hay ítems reales de pruebas psicológicas ni datos personales. El contenido de las subpruebas sale de DOC-08 cuando esté publicado.

| Dato | Contenido | Usado en |
| --- | --- | --- |
| Versión publicada BFA ficticia | Al menos VOC1, VOC2 y FV, con preguntas y opciones inventadas | Todos |
| SUJ-T5-A y SUJ-T5-B | Dos Sujetos ficticios con cuenta propia de rol Sujeto | Todos (B solo en 13 y 15) |
| PER-T5 | Periodo ficticio activo | Todos |
| EV-T5-xx | Una evaluación nueva por caso, creada con E3 | Según el caso |
| Cuentas ficticias de carga | N Sujetos ficticios, N según la consulta C-1 | TC-TEST-02-T5-15 |

Para observar vencimientos en local se pueden usar duraciones cortas en la versión ficticia, indicándolo en la evidencia. En `main` se usan las duraciones de la versión publicada.

## Entorno

- **Entorno `main`** (http://85.239.244.233:8091): casos 01 a 05 y 07 a 14, cuando E3 a E8 estén fusionados. Antes de ejecutar, anotar la versión desplegada (Actions › Despliegue › `estado`). Si la base quedó con datos de otra prueba, pedir a Integración `reiniciar-bd`.
- **Entorno local:** casos 06 (reinicio del servidor) y 15 (carga controlada). **No se lanza carga contra el servidor compartido sin autorización** (plan operativo).
- **Herramientas:** Postman para la API (variables `{{base_url}}` y `{{token}}` de cuentas ficticias); navegador con DevTools (Network › Offline) para cortar la conexión; dos peticiones en paralelo con `curl` para los casos de simultaneidad, por ejemplo:

```bash
# Dos E7 idénticos al mismo tiempo (variables de entorno, sin secretos en el repo)
for i in 1 2; do
  curl -s -o /dev/null -w "%{http_code}\n" -X PUT \
    "$BASE_URL/api/v1/evaluaciones/$EV/subpruebas/VOC1/respuestas" \
    -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"respuestas\":[{\"preguntaId\":$PREGUNTA,\"opcion\":\"B\"}]}" &
done; wait
```

- **Conteo de evaluaciones:** E22 (`GET /api/v1/evaluaciones?sujetoId=…&estado=EN_CURSO`) permite comprobar que no se duplicó el intento.

## Casos — Desconexión y recuperación

### TC-TEST-02-T5-01 — Recargar dentro del plazo

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-01 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01, RNF-BFA-GEN-04, CA-02 | EV-T5-01 de SUJ-T5-A con VOC1 ACTIVA y 3 respuestas confirmadas | 1. E4: anotar `id`, `subpruebaActiva.finLimite` y `segundosRestantes`.<br>2. Recargar el navegador (F5) y luego cerrarlo y volver a abrirlo.<br>3. Repetir E4 y E6.<br>4. E22 con `sujetoId` de SUJ-T5-A y `estado=EN_CURSO`. | Paso 3: el mismo `id` y el mismo `finLimite`; `segundosRestantes` igual a `finLimite` menos la hora del servidor (no vuelve al total); E6 muestra las 3 respuestas. Paso 4: una sola evaluación EN_CURSO. | Manual (navegador y Postman); candidato e2e | **BLOQUEADO** |

### TC-TEST-02-T5-02 — Se corta la conexión mientras se guarda una respuesta

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-02 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>E7: "Es idempotente."<br>RNF-BFA-GEN-04 | EV-T5-02 con VOC1 ACTIVA | 1. En el navegador, marcar la opción B de la pregunta 1 y poner Network › Offline antes de que llegue la confirmación.<br>2. Volver a Online y dejar que el cliente reenvíe el mismo cambio (o reenviar el mismo E7 en Postman).<br>3. E6 y E7 con otra pregunta para leer `totalRespondidas`. | Paso 2: `200`. Paso 3: la pregunta 1 tiene una sola respuesta vigente (B) y `totalRespondidas` la cuenta una vez. Nunca `500`. | Manual (navegador y Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-03 — El cambio sin confirmar se ve como pendiente

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-03 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>CU-GEN-12 A2 y RF-GEN-14 (provisional DOC-04) | EV-T5-03 con VOC1 ACTIVA en el navegador | 1. Poner Network › Offline.<br>2. Marcar una respuesta.<br>3. Observar la pantalla.<br>4. Volver a Online. | Paso 3: el cambio se muestra como pendiente, no como guardado. Paso 4: al confirmarse, deja de estar pendiente. **Provisional (DOC-04).** | Manual (navegador) | **BLOQUEADO** |

### TC-TEST-02-T5-04 — Reconectar después del vencimiento

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-04 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01<br>E7: "409 TIEMPO_AGOTADO: llegó después de finLimite."<br>CU-BFA-P04 (provisional DOC-04)<br>Respuesta a C-4 (DOC-05) | EV-T5-04 con VOC1 ACTIVA y 2 respuestas confirmadas | 1. Anotar `finLimite` con E4.<br>2. Desconectar el cliente y esperar hasta pasar `finLimite`.<br>3. Reconectar y llamar a E4.<br>4. Enviar E7 con una respuesta nueva.<br>5. Llamar a E8 y después a E4. | Paso 3: VOC1 `CERRADA` con `cerradaEn` igual al `finLimite` del paso 1, aunque todavía no se llamó E8; las 2 respuestas se conservan. Paso 4: `409 TIEMPO_AGOTADO` (o `SUBPRUEBA_NO_ACTIVA` si ya está cerrada) y la respuesta nueva no se guarda. Paso 5: E8 responde `200`; VOC1 sigue `CERRADA` una sola vez, con el mismo `cerradaEn` del paso 3. | Manual (navegador y Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-05 — Retomar desde otro navegador u otra sesión

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-05 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01, CA-02<br>CU-GEN-19 (provisional DOC-04) | EV-T5-05 con VOC1 ACTIVA iniciada en el navegador 1 | 1. Iniciar sesión con SUJ-T5-A en un navegador 2 (E14).<br>2. Abrir la evaluación y llamar a E4.<br>3. E22 de SUJ-T5-A con `estado=EN_CURSO`. | Paso 2: el mismo `id`, la misma subprueba activa y el mismo `finLimite`; el tiempo sigue corriendo, no se reinicia. Paso 3: una sola evaluación EN_CURSO. | Manual (navegador y Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-06 — Reinicio del servidor con una subprueba activa

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-06 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-04: "La base de datos es el estado oficial" | **Entorno local.** EV-T5-06 con VOC1 ACTIVA y 3 respuestas | 1. Anotar E4.<br>2. Detener el backend y volver a levantarlo (PostgreSQL sigue arriba).<br>3. Llamar a E4 y E6. | Paso 3: mismo estado, mismo `finLimite` y las 3 respuestas; si el plazo pasó durante el reinicio, VOC1 queda `CERRADA` con sus respuestas. | Manual (local) | **BLOQUEADO** |

### TC-TEST-02-T5-07 — Una caída no adelanta la siguiente subprueba

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-07 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01<br>E5: "409 HAY_SUBPRUEBA_ACTIVA"<br>CU-BFA-P04 (provisional DOC-04) | EV-T5-07 con VOC1 ACTIVA y VOC2 PENDIENTE | 1. Desconectar y reconectar el cliente.<br>2. Llamar a E5 de VOC2 antes del `finLimite` de VOC1.<br>3. Después del cierre de VOC1, llamar a E5 de VOC2. | Paso 2: `409 HAY_SUBPRUEBA_ACTIVA`; VOC2 sigue PENDIENTE. Paso 3: `200` y VOC2 ACTIVA con su propio `finLimite`. | Manual (Postman) | **BLOQUEADO** |

## Casos — Idempotencia y reintentos

### TC-TEST-02-T5-08 — Repetir la creación de la evaluación

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-08 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>E3: "409 EVALUACION_EN_CURSO"<br>CU-BFA-P03 (provisional DOC-04)<br>Respuesta a C-3 (DOC-05) | SUJ-T5-A sin evaluación abierta en PER-T5 | 1. Enviar E3 con los mismos datos dos veces seguidas.<br>2. Enviar E3 otras dos veces en paralelo (con otro Sujeto ficticio sin evaluación).<br>3. E22 de cada Sujeto con `estado=EN_CURSO`. | Pasos 1 y 2: el primer envío de cada Sujeto crea la evaluación y el otro responde `409 EVALUACION_EN_CURSO`. Paso 3: una sola evaluación EN_CURSO por Sujeto. Nunca `500`. | Manual (Postman y curl) | **BLOQUEADO** |

### TC-TEST-02-T5-09 — Doble inicio de la misma subprueba

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-09 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01<br>Respuesta a C-2 (DOC-05) | EV-T5-09 con VOC1 PENDIENTE | 1. Enviar E5 de VOC1 dos veces en paralelo.<br>2. Esperar 10 s y enviar E5 de VOC1 otra vez.<br>3. Llamar a E4. | Pasos 1 y 2: todos los envíos responden `200` con el mismo inicio y el mismo `finLimite`; ninguno responde `409 HAY_SUBPRUEBA_ACTIVA`. Paso 3: un solo inicio; el `finLimite` es el del primer inicio y no se movió en el paso 2. Nunca `500`. | Manual (curl y Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-10 — Repetir la finalización después de reconectar

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-10 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>E8: "Repetir la finalización devuelve el cierre existente sin otro cálculo." | EV-T5-10 con todas las subpruebas incluidas CERRADAS | 1. Llamar a E8 y cortar la conexión antes de ver la respuesta.<br>2. Reconectar y llamar a E8 otra vez.<br>3. Llamar a E4. | Paso 2: `200` con el mismo `resultadoId` que generó el primer E8. Paso 3: una sola finalización (`estado: FINALIZADA`, `fechaFin` sin cambiar). | Manual (navegador y Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-11 — Dos guardados iguales al mismo tiempo

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-11 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>E7: "Es idempotente." | EV-T5-11 con VOC1 ACTIVA | 1. Enviar en paralelo dos E7 idénticos (pregunta 1, opción B) con el comando de «Entorno».<br>2. Llamar a E6. | Paso 1: ningún `500 ERROR_INTERNO`. Paso 2: la pregunta 1 tiene una sola respuesta vigente (B). Si responde `500`, se registra en BUG-02. | Manual (curl) | **BLOQUEADO** |

## Casos — Concurrencia controlada

### TC-TEST-02-T5-12 — Dos cambios distintos a la misma pregunta al mismo tiempo

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-12 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>§3.3 (provisional DOC-04): "Se conserva una respuesta vigente por ítem" | EV-T5-12 con VOC1 ACTIVA | 1. Enviar en paralelo E7 con la opción B y E7 con la opción C para la misma pregunta.<br>2. Llamar a E6. | Paso 1: ningún `500`. Paso 2: una sola respuesta vigente, que es B o C (una de las enviadas). **Provisional (DOC-04)** en cuanto al historial de cambios. | Manual (curl) | **BLOQUEADO** |

### TC-TEST-02-T5-13 — Dos evaluados a la vez no se mezclan

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-13 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-06: "sin mezcla de respuestas, temporizadores, estados o resultados"<br>RNF-BFA-GEN-05, CA-02 | EV-T5-13A de SUJ-T5-A y EV-T5-13B de SUJ-T5-B, misma versión | 1. Iniciar VOC1 en A y, 30 s después, en B.<br>2. A responde B en la pregunta 1; B responde D en la pregunta 1, al mismo tiempo.<br>3. E4 y E6 de cada evaluación con su propia cuenta.<br>4. E4 de EV-T5-13A con la cuenta de SUJ-T5-B. | Paso 3: cada evaluación tiene su propio `finLimite` (separados unos 30 s) y su propia respuesta (A: B; B: D). Paso 4: `403 SIN_PERMISO`. | Manual (navegador y curl) | **BLOQUEADO** |

### TC-TEST-02-T5-14 — Respuesta enviada justo al vencer

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-14 | TEST-02-T5: "No se alteran plazos ni se duplican intentos o respuestas."<br>RNF-BFA-GEN-01<br>E7: "409 TIEMPO_AGOTADO"<br>CU-GEN-13 A2 (provisional DOC-04)<br>Respuesta a C-4 (DOC-05) | EV-T5-14 con VOC1 ACTIVA y `finLimite` conocido | 1. Enviar E7 unos 2 s antes de `finLimite`.<br>2. Enviar E7 unos 2 s después de `finLimite`.<br>3. Llamar a E4 y E6. | Paso 1: `200`. Paso 2: `409 TIEMPO_AGOTADO`. Paso 3: VOC1 `CERRADA` una sola vez, con `cerradaEn` igual a `finLimite`; solo se conserva la respuesta del paso 1. | Manual (Postman) | **BLOQUEADO** |

### TC-TEST-02-T5-15 — Carga controlada con varios evaluados

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-15 | TEST-02-T5: "Se verifican los umbrales RNF aprobados."<br>RNF-FV-02: "Umbral documentado: guardado <500 ms"<br>RNF-BFA-GEN-06 | **Entorno local** o el que autorice Scrum. N cuentas ficticias (N según C-1), cada una con FV ACTIVA | 1. Que las N cuentas envíen E7 de FV (una palabra cada pocos segundos) al mismo tiempo durante un minuto.<br>2. Medir el tiempo de respuesta de cada E7.<br>3. Revisar E6 de varias evaluaciones al azar. | Paso 2: cada guardado de FV tarda menos de 500 ms; ningún `5xx`. Paso 3: cada evaluación tiene solo sus palabras, en su orden y sin duplicados. El valor de N depende de C-1. | Manual (script de carga local) | **BLOQUEADO** |

## Caso fuera de alcance

### TC-TEST-02-T5-16 — Aplicación colectiva sincronizada

| ID | CA cubierto | Datos y precondiciones | Pasos | Resultado esperado | Manual o e2e | Estado |
| --- | --- | --- | --- | --- | --- | --- |
| TC-TEST-02-T5-16 | CU-GEN-19 A4 (provisional DOC-04) | — | — | No se ejecuta: CU-BFA-P03 dice «Coordinación colectiva excluida hasta D02» y CA-10 (DOC-03) indica que la sincronización colectiva no se presenta como capacidad aprobada. | — | **NO APLICA** |

## Bloqueos para ejecutar

| Bloqueo | Responsable | Próximo paso |
| --- | --- | --- |
| E3 a E8 del flujo BFA no están fusionados en `main`. Al 2026-11-23, según el orden que fijó Desarrollo en el #41: primero el #51, después las piezas del #41 (modelo `Evaluacion` y E4 a E7) y luego E3 (GEN-02-BE, se rehace desde `main`) y E8 (sobre el modelo del #41) | Desarrollo e Integración (GEN-02 a GEN-04, BFA-BE) | Ejecutar en TEST-02-EJEC cuando estén fusionados |
| DOC-07 `in progress` y DOC-08 `Open` | Documentación | Contenido ficticio de las subpruebas |
| Umbral de concurrencia y entorno de carga sin definir | Documentación y Scrum de Testing | Consulta C-1 |

## Registro de ejecución

| Fecha | Entorno / versión | Tester | Resultado | Evidencia / bug |
| --- | --- | --- | --- | --- |
| — | — | — | Sin ejecutar: 15 BLOQUEADO, 1 NO APLICA | — |
| 2026-11-19 | — | José Ariel Zeledón | Sin ejecución (v2). Se actualizan TC-TEST-02-T5-04 y 09 con las respuestas a C-2 y C-4. Siguen 15 BLOQUEADO y 1 NO APLICA. | [Respuesta de DOC-05](https://app.clickup.com/t/86bctt9cy?comment=1800450000021721) |
| 2026-11-23 | — | José Ariel Zeledón | Sin ejecución (v3). Se actualizan TC-TEST-02-T5-04, 08 y 14 con las respuestas a C-3 y C-4. Siguen 15 BLOQUEADO y 1 NO APLICA. | [Respuesta de DOC-05](https://app.clickup.com/t/86bctt9cy?comment=1800450000034525) |

### Revisión final

- [x] Cada caso corresponde a un criterio aprobado y a la tarea correcta.
- [ ] Se ejecutó contra la versión vigente de `main`.
- [ ] Se registraron resultados reales y evidencia verificable.
- [x] No contiene secretos ni datos personales reales.
- [ ] Se informó a TEST-02-EJEC y a Scrum.
