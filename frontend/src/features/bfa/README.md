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

### Supuestos y consultas abiertas

DOC-05 y DOC-06 están aprobados, pero este módulo se basó en el §5.3 del Documento BFA y en los casos de Testing. Está marcado como `SUPUESTO` en `api/tipos.ts` lo que ninguno fija:

1. **E6 en FV** devuelve `palabras` y `letraInicial`; E4 no trae la letra y hace falta para retomar FV.
2. `recursoUrl` puede venir en la pregunta y en cada opción.
3. E7 usa `opcion` (la letra); el §5.4.1 propone `opcionId`.
4. Los recursos gráficos «se precargan antes de E5», pero E6 exige subprueba activa: hoy no se precarga nada.
5. **Volver atrás** (GEN-03 C-1): se permite en todas las subpruebas de selección salvo Sumas y Multiplicaciones (`volverAtras` en `familias.ts`). Falta saber qué subpruebas lo restringen y dónde se configura.
6. **Repetir E5** sobre la subprueba activa devuelve el mismo inicio y fin (CU-BFA-P05); el código HTTP está pendiente (GEN-03 C-4).
7. **Sin cierre anticipado:** el contrato aprobado no lo incluye aunque DOC-04 rev. 2 lo menciona. Tras la última pregunta se espera el cierre oficial.
8. Estados `PENDIENTE / ACTIVA / CERRADA` según el contrato aprobado; DOC-04 rev. 2 usa `HABILITADA / EN_CURSO / CERRADA` y se pidió unificarlos.
9. El acceso real (sesión, sujeto, periodo y edición) depende de GEN-06; `/bfa` los toma de la URL o usa valores de ejemplo con la API simulada. Un `401` o `403` se informa y no se reintenta.

### Dependencias externas

- **BUG-01 (backend):** si el cierre por tiempo no se registra en E4, la pantalla espera unos 20 s y muestra un error reintentable.
- **GEN-03-FE (PR #22):** cuando se fusione, reemplazar las pantallas de instrucciones, práctica y progreso por sus componentes y revisar que no se dupliquen sus tipos de contrato.
- **S2:** DOC-04 pidió permitir selección múltiple en Desplazamiento; hoy todo es selección única.

### Pendiente

Texto oficial de instrucciones y práctica, pausa de Comprensión Verbal más allá del aviso, y sustituir la API simulada por los mocks de INT-02.
