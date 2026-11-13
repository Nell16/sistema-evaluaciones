# Módulo BFA

Agregar componentes y páginas de BFA aquí, **sin duplicar** el diseño, el cliente HTTP, los DTOs o los componentes reutilizables.

## BFA-FE-01 · Flujo compartido de la batería

Navega y ejecuta las subpruebas con los componentes comunes (`TestScreen`, `SaveStatus`, `TestNotice`). El flujo obedece al backend: el orden, los estados y el tiempo salen de E2/E4 (DOC-05); el cliente no salta subpruebas, no reabre las cerradas y no decide el cierre.

| Ruta                              | Pantalla                                                              |
| --------------------------------- | --------------------------------------------------------------------- |
| `/bfa`                            | Orden y tiempos de las subpruebas; crea la evaluación (E3)            |
| `/bfa/evaluaciones/:evaluacionId` | Instrucciones, práctica, ejecución, transición, cierre y recuperación |

Abrir la misma URL de la evaluación retoma la sesión sin reiniciar el reloj (E4).

### Fases

`cargando → preparación (instrucciones → práctica) → iniciando (E5) → ejecución (E6/E7) → transición → … → lista para finalizar → finalizada (E8)`, más `error`.

- Instrucciones y práctica ocurren **antes** de E5: no consumen tiempo oficial.
- El tiempo lo manda el servidor; cada respuesta reancla el reloj local. Al llegar a cero el cliente solo **consulta** (E4) y espera la confirmación del cierre.
- No hay cierre anticipado: tras la última pregunta la persona espera el cierre oficial y puede revisar respuestas.
- Si falla el guardado se avisa y se reenvía lo pendiente en el siguiente cambio (E7 es idempotente).

### Carpetas

| Carpeta / archivo | Responsabilidad                                                            |
| ----------------- | -------------------------------------------------------------------------- |
| `api/`            | Tipos de DOC-05, cliente HTTP (`bfaApi.ts`) y API simulada                 |
| `flujo/`          | `ControladorBfa`: estado y reglas del flujo, sin React (se prueba en Node) |
| `vistas/`         | Pantallas por fase                                                         |
| `familias.ts`     | Cómo se presenta cada subprueba; aquí se enchufan las familias (UI-04)     |
| `components/`     | Piezas visuales de UI-04: `Figura`, `Operacion`, `FluidezVerbal`           |

### Probarlo sin backend

Mientras no existan endpoints, en desarrollo se usa una API simulada que aplica las reglas del servidor. Con `npm run dev` abre:

```text
http://localhost:5173/bfa              # tiempo oficial
http://localhost:5173/bfa?tiempo=20    # cada subprueba dura 20 s
```

El estado simulado sobrevive a recargar la página (así se prueba la recuperación). `VITE_BFA_API=real` usa el backend real; `VITE_BFA_API=simulada` fuerza la simulada.

### Integrar una familia sin duplicar la navegación

El flujo solo conoce `E6` (enunciado y alternativas). Para una familia, ajusta su entrada en `familias.ts` (variante del enunciado, distribución de alternativas, instrucciones y práctica) o reemplaza la vista de pregunta en `vistas/VistaFlujo.tsx`. La navegación general no cambia.

### Casos de Testing que cubre

Los casos de `docs/pruebas/` sobre esta interfaz: TC-GEN-03-003 a 008 (instrucciones y práctica antes del tiempo, reloj del servidor al recargar, pendiente frente a confirmado, volver atrás, fallo de guardado, fin del tiempo) y TC-GEN-04-001 a 010 (finalizar, pendientes, E8, resultado pendiente de validación, errores, doble clic, sin cálculos en el navegador). Las pruebas automáticas del controlador (`flujo/controlador.test.ts`) llevan el ID del caso cuando aplica.

### Contrato (DOC-05)

Verificado contra `docs/api/openapi.yaml` del PR #37 (aún sin fusionar; su encabezado lo presenta como contrato aprobado). Confirmado: rutas y métodos de E1–E8 y E24; `opcion` es la letra de E6; `recursoUrl` viene en la pregunta y en cada opción; FV usa `palabras` con `ordenEntrada` en E6 y E7; estados `PENDIENTE / ACTIVA / CERRADA`; `versionFormularioId` en E2 y E4 (E3 la exige, se toma de E2); formato de error común. Las instrucciones y los ejemplos de práctica se leen de E24; mientras el backend no la implemente (`RUTA_NO_ENCONTRADA`) se usan textos provisionales, y cualquier otro error se informa.

### Pendiente de confirmar

1. **Letra de FV al recargar:** ni E4 ni E6 la devuelven. Se recuerda en `sessionStorage` desde E5 (respaldo temporal; en otro navegador se informa el error). Conviene que E6 la incluya.
2. **Rechazos de E7 en FV** (texto vacío, orden repetido, entrada 76): el contrato solo dice `400 SOLICITUD_INVALIDA` en general (consulta C-7).
3. **Borrar una palabra ya guardada:** E7 exige texto no vacío y «registra o reemplaza»; no queda claro si lo que no se envía se elimina.
4. **Sesión:** toda operación exige Bearer (E14). `services/http.ts` es compartido y hoy no envía token, así que con el backend real habría `401` hasta que GEN-06 lo resuelva; se informa y no se reintenta.
5. **Volver atrás** (C-1): el botón «Anterior» aparece desde la segunda pregunta en todas las subpruebas de selección, incluidas Sumas y Multiplicaciones. El Documento BFA §5.2 dice que la navegación interna «puede» permitir volver a reactivos anteriores y no restringe ninguna subprueba; en Sumas y Multiplicaciones solo pide rapidez y cambio de ítem en menos de 100 ms. Si C-1 decide restringirlo, se cambia `volverAtras` en `familias.ts`.
6. **Repetir E5** sobre la subprueba activa devuelve el mismo inicio y fin (CU-BFA-P05); el código HTTP está pendiente (C-4).
7. **Sin cierre anticipado:** el contrato aprobado no lo incluye aunque DOC-04 rev. 2 lo menciona.
8. Los recursos gráficos «se precargan antes de E5», pero E6 exige subprueba activa: hoy no se precarga nada.
9. **E3 `EVALUACION_EN_CURSO`:** la respuesta no trae el id de la evaluación abierta; para retomarla habría que consultar la lista de evaluaciones.
10. El acceso real (sujeto y periodo) depende de GEN-06; `/bfa` los toma de la URL o usa valores de ejemplo con la API simulada.

### Dependencias externas

- **Dos pantallas de subprueba:** existen la del PR #22 (GEN-03-FE) y la del PR #34 (GEN-03-BE). Integración decide cuál se fusiona; este flujo debe apoyarse en esa, sin duplicar lo que ya traiga.
- **BUG-01 (backend):** si el cierre por tiempo no se registra en E4, la pantalla espera unos 20 s y muestra un error reintentable.
- **S2:** DOC-04 pidió permitir selección múltiple en Desplazamiento; hoy todo es selección única.

### Pendiente

Implementación de E24 en el backend (C-6) y textos oficiales de instrucciones y práctica, pausa de Comprensión Verbal más allá del aviso, y sustituir la API simulada por los mocks de INT-02.
