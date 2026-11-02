# Frontend

## Preview del sistema visual UI-01

La preview local de `UI-01` permite revisar la interfaz común sin API, scoring ni datos reales. Al abrirla, usa el
panel de controles “Variaciones del test” para cambiar los estados mediante botones; la URL solo conserva una forma
opcional de iniciar una combinación concreta.

Inicia el frontend desde este directorio:

```bash
npm run dev -- --host 0.0.0.0
```

Abre la vista base:

```text
http://localhost:5173/?preview=ui-01
```

### Variaciones disponibles

| URL | Qué muestra |
| --- | --- |
| `?preview=ui-01` | Pregunta de selección con cinco opciones |
| `?preview=ui-01&response=text` | Campo de respuesta libre |
| `?preview=ui-01&phase=instructions` | Pantalla de instrucciones |
| `?preview=ui-01&phase=practice` | Pantalla de práctica |
| `?preview=ui-01&phase=closed` | Subprueba cerrada |
| `?preview=ui-01&notice=warning` | Aviso textual de tiempo |
| `?preview=ui-01&notice=error` | Error textual |
| `?preview=ui-01&save=saving` | Guardado en progreso |
| `?preview=ui-01&save=error` | Error de guardado |
| `?preview=ui-01&resource=visual` | Recurso visual de ejemplo |

Los parámetros se pueden combinar. Por ejemplo:

```text
http://localhost:5173/?preview=ui-01&save=error&resource=visual
```

La preview usa contenido demostrativo y sirve para validar la biblioteca de componentes, los estados y la semántica accesible antes de conectarla al motor de evaluación.
