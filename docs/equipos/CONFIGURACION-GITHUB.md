# Configuración de GitHub e integración de los cuatro equipos

> Guía operativa sin credenciales. La fase de configuración ya cuenta con CI y despliegue de `main` verificados; la publicación de `release` requiere aprobación y pruebas. El sistema aún es un esqueleto, no una aplicación funcional.

## 1. Colaboradores y contribución

La propuesta de Davide permite añadir como colaboradores a integrantes de **Integración, Desarrollo, Documentación y Testing**. Cada integrante usa una **rama propia por tarea** dentro del repositorio central y abre un PR hacia `main`. Aunque tengan permiso de escritura, nadie debe hacer push directo a `main`; la ruleset lo impide.

También se admiten **forks** para quienes no hayan sido invitados, pues el repositorio es público: fork → rama personal → PR al repositorio oficial.

**Responsables de aprobación:** Integración. Ya existe `.github/CODEOWNERS` y la ruleset de `main` exige su revisión. Solo cuentan las aprobaciones de usuarios con permisos suficientes; los colaboradores que aún no aceptan la invitación quedan pendientes. No debe aprobar el PR su propio autor; la revisión corresponde a otro integrador. La lista se actualiza por PR al rotar equipos.

## 2. Flujo de tarea y CI

1. Obtener el código de tarea y el ID de ClickUp.
2. Crear rama desde `main`: `tipo/nombre-CODIGO`, por ejemplo `feature/ana-GEN-01-BE` (`feature`, `fix`, `docs`, `test` o `chore`).
3. Hacer commits en la rama; nunca en `main`.
4. Abrir PR con título `[GEN-01-BE] Descripción` y primera línea `Tarea ClickUp: GEN-01-BE · CU-ID` (ID real, sin sufijo `[estado]` de forma predeterminada).
5. Esperar los checks `reglas`, `backend-build`, `frontend-build` y `ci-ok`, resolver los comentarios y solicitar revisión de Integración.
6. Integración aprueba y fusiona mediante **Squash and merge** cuando todo esté conforme.

El CI valida nombres, archivos, migraciones, compilación y pruebas de arquitectura. No demuestra que existan módulos de evaluación funcionales.

## 3. Ajustes del repositorio (cuenta Nell16)

- **Settings → General → Pull Requests:** permitir solo **Allow squash merging**, con título del PR como mensaje del commit y eliminación automática de ramas al fusionar; las ramas deben actualizarse según la ruleset.
- **Settings → Collaborators:** invitar a los usuarios reales de los cuatro equipos conforme a la propuesta; alternativamente, conservar colaboradores solo para Integración y permitir forks. En ambos casos la revisión por CODEOWNERS debe estar activa antes de permitir que cualquier colaborador apruebe PRs.
- **Settings → Rules → Rulesets → Protección de main:** ruleset activa; exige `ci-ok`, rama actualizada, 1 aprobación de CODEOWNERS, conversaciones resueltas y solo squash. Sin bypass, eliminación ni force-push. No cambiar estas reglas para pasar una prueba.
- La conexión actual de ChatGPT a GitHub puede editar código, pero **no** los ajustes administrativos ni los secretos; esos cambios se efectúan en Settings.

## 4. Conectar ClickUp y comprobar la trazabilidad

La cuenta GitHub `Nell16` se conectó por OAuth desde el App Center del Workspace de ClickUp. Se debe mantener **solo** el repositorio `Nell16/sistema-evaluaciones` vinculado al espacio `Sistema_tests` para este proyecto. No duplicar conexiones ni modificar otras asociaciones.

1. En ClickUp, abrir **App Center → GitHub → Entorno de trabajo → Ajustes**; verificar el repositorio y su espacio asociado.
2. Abrir una tarea dentro de `Sistema_tests`; copiar su **ID real**, por ejemplo `86bcvajad` para la subtarea `INT-01-CU`.
3. Crear un PR válido con título `[INT-01-CU] ...` y descripción cuya primera línea sea `Tarea ClickUp: INT-01-CU · CU-86bcvajad`.
4. Comprobar que el PR recibe el comentario/enlace de ClickUp y que la tarea muestra la actividad del PR. **No fusionar el PR de prueba** hasta que Integración revise y autorice el despliegue automático.
5. El sufijo `[in review]` o similar **puede cambiar el estado de una tarea**. Añadirlo únicamente si Scrum/Integración acordaron esa transición y se verificó que ese estado existe. Primero probar el enlace **sin sufijo**.
6. El nombre de rama generado por defecto desde ClickUp puede incumplir el CI; crear ramas manualmente con `tipo/nombre-CODIGO` hasta que se acuerde un formato compatible, sin modificar preferencias globales del Workspace.
7. La vinculación GitHub–ClickUp no autoriza a modificar tareas, responsables, prioridades ni estados de otros equipos. Las automatizaciones de ClickUp deben aprobarse y configurarse separadamente.

Referencia: [tarea de prueba INT-01-CU](https://app.clickup.com/t/86bcvajad).

## 5. Despliegue a VPS (main probado; release preparado)

El workflow `.github/workflows/deploy.yml` usa los secretos `VPS_HOST`, `VPS_KNOWN_HOSTS` y `VPS_SSH_KEY` almacenados en GitHub Actions; nunca se copian al repositorio.

- **`main`:** `DEPLOY_ENABLED=true`; la ejecución manual de `desplegar` terminó correctamente y `estado` confirmó contenedores de PostgreSQL, backend y frontend activos con el commit `aa00b9a`. Los nuevos merges a `main` pueden disparar otra compilación y despliegue **automático**.
- **`release`:** la conexión SSH con `estado` funcionó, pero todavía **no hay versión publicada**. La protección del entorno requiere revisores; solo se publica una etiqueta `v0.<semana>.0` después del visto bueno de Testing, Integración y Davide.
- **Operación segura:** consultar `Actions → Despliegue → Run workflow` con `accion=estado` o `accion=logs` sin modificar servidor.
- **Seguridad:** `ALLOW_DB_RESET` debe permanecer desactivada. Davide, dueño del VPS, confirma cualquier rotación de la clave SSH y el procedimiento de despliegue.

### Operación peligrosa: reiniciar-bd

El workflow incluye una opción manual `reiniciar-bd` para main, **deshabilitada** mientras `ALLOW_DB_RESET` no sea `true`. Incluso activada, solo `Nell16` puede solicitarla escribiendo la confirmación `BORRAR BD MAIN`. Esta operación puede borrar datos; habilitarla únicamente con copia de seguridad verificada y acuerdo explícito con el propietario del VPS. Nunca se permite en release.

## 6. Rotación y entrega

- **Lunes:** Integración entrante actualiza `CODEOWNERS` por PR; el equipo saliente revisa ese cambio.
- **Viernes:** Testing confirma versión estable antes de crear un release con etiqueta `v0.<semana>.0`. No crear etiquetas a modo de prueba en el entorno real.
- No almacenar secretos ni datos personales reales en GitHub. Revisar `SECURITY.md`.

Referencias: [README](../../README.md) · [CONTRIBUTING](../../CONTRIBUTING.md) · [Flujo de equipos](FLUJO.md).
