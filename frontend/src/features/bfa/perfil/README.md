# Perfil de resultados BFA (BFA-FE-08)

Ruta: `/bfa/evaluaciones/:evaluacionId/perfil`. Consulta E9 y presenta el perfil tal como lo entrega el servidor, sin calcular puntuaciones ni percentiles.

- **Reutiliza GEN-05-FE:** `useResultado` y el cliente de E9 (mock o backend real), la traducción de errores (`aErrorResultados`, `esNoFinalizada`) y `EstadoCarga` y `MensajeError`. No repite su mecanismo de consulta.
- **Reutiliza UI-05:** `PerfilBfa` organiza los 18 factores en Verbal, Espacial, Razonamiento, Numérico y compuestos.
- **Agrega:** la página, el contenedor `PerfilBfaConsulta`, las fases en `EstadoPerfilBfa` y `resultadoMostrable`, que descarta los factores cuando no hay revisión publicada, aunque el backend los envíe.

Una evaluación no finalizada (409) se muestra como aviso; un resultado pendiente de validación o en cálculo llega como estado y se explica en el perfil.

## Probarlo sin backend

Con `npm run dev`, los IDs del mock de E9 son: `16` parcial, `17` pendiente de validación, `18` en cálculo, `15` evaluación no finalizada, `403` sin permiso, `500` y `503` fallos y cualquier otro, no existe. `VITE_USAR_MOCKS=false` usa el backend real.

## Pendiente

- E9 no indica el tipo de test, por eso el perfil BFA tiene su propia ruta.
- `services/http.ts` no envía token: con el backend real habrá `401` hasta que GEN-06 lo resuelva.
- El enlace desde el final del flujo BFA y un `renderResultado` en `ConsultaResultados` (GEN-05) quedan para otro PR, porque tocan archivos de otras tareas.
