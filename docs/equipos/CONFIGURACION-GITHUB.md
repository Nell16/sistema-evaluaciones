# Configuración de GitHub e integración de los cuatro equipos

> Guía operativa sin credenciales. Ajustada a la propuesta de Davide. La implementación está distribuida entre archivos del PR de configuración y ajustes administrativos de GitHub.

## 1. Colaboradores y contribución

La propuesta de Davide permite añadir como colaboradores a integrantes de **Integración, Desarrollo, Documentación y Testing**. Cada integrante usa una **rama propia por tarea** dentro del repositorio central y abre un PR hacia `main`. Aunque tengan permiso de escritura, nadie debe hacer push directo a `main`; la ruleset lo impide.

También se admiten **forks** para quienes no hayan sido invitados, pues el repositorio es público: fork → rama personal → PR al repositorio oficial.

**Responsables de aprobación:** Integración. El propietario debe incorporar los usuarios GitHub reales del equipo a `.github/CODEOWNERS` y activar **Require review from Code Owners**. El archivo se entrega inicialmente con `@Nell16` como marcador válido; debe sustituirse o ampliarse **antes** de obligar la revisión de CODEOWNERS para no bloquear PRs del propietario. Una aprobación de otra persona con escritura no equivale a una aprobación de Integración hasta activar esa regla.

## 2. Flujo de tarea y CI

1. Obtener el código de tarea y el ID de ClickUp.
2. Crear rama desde `main`: `tipo/nombre-CODIGO`, por ejemplo `feature/ana-GEN-01-BE` (`feature`, `fix`, `docs`, `test` o `chore`).
3. Hacer commits en la rama; nunca en `main`.
4. Abrir PR con título `[GEN-01-BE] Descripción` y primera línea `Tarea ClickUp: GEN-01-BE · CU-ID[in review]` (reemplazar con ID real).
5. Esperar los checks `reglas`, `backend-build`, `frontend-build` y `ci-ok`, resolver los comentarios y solicitar revisión de Integración.
6. Integración aprueba y fusiona mediante **Squash and merge** cuando todo esté conforme.

El CI valida nombres, archivos, migraciones, compilación y pruebas de arquitectura. No demuestra que existan módulos de evaluación funcionales.

## 3. Ajustes del repositorio (cuenta Nell16)

- **Settings → General → Pull Requests:** permitir solo **Allow squash merging**, con título del PR como mensaje del commit; activar **Always suggest updating pull request branches** y **Automatically delete head branches**.
- **Settings → Collaborators:** invitar a los usuarios reales de los cuatro equipos conforme a la propuesta; alternativamente, conservar colaboradores solo para Integración y permitir forks. En ambos casos la revisión por CODEOWNERS debe estar activa antes de permitir que cualquier colaborador apruebe PRs.
- **Settings → Rules → Rulesets → Protección de main:** después de integrar el nuevo CI y comprobar `ci-ok` en verde, exigir `ci-ok`, rama actualizada, 1 aprobación de CODEOWNERS, conversaciones resueltas y solo squash. Mantener sin bypass, sin eliminación ni force-push.
- La conexión actual de ChatGPT a GitHub puede editar código, pero **no** los ajustes administrativos ni los secretos; esos cambios se efectúan en Settings.

## 4. Conectar ClickUp

Alexander conecta GitHub en ClickUp y enlaza `Nell16/sistema-evaluaciones` al espacio **Sistema_tests**; Nell16 autoriza la aplicación oficial de ClickUp exclusivamente sobre este repositorio. Comprobar con un PR de prueba y el ID real de una tarea antes de considerar integrada la automatización.

## 5. Despliegue a VPS (preparado, desactivado por defecto)

El PR incorpora `deploy/` (Dockerfiles, Compose y Nginx) y `.github/workflows/deploy.yml`. Este workflow **no se ejecuta hasta activar la variable** `DEPLOY_ENABLED=true`. No contiene claves ni contraseñas.

Antes de activarlo, hacer lo siguiente desde la cuenta Nell16:

1. Confirmar con Davide que el VPS tiene configurados los usuarios restringidos `evaluaciones-main` y `evaluaciones-release` y los comandos `estado`, `logs`, `desplegar <ref>` y `reiniciar-bd`. Verificar las claves públicas del host por un canal independiente.
2. En **Settings → Secrets and variables → Actions**, crear los secretos `VPS_HOST`, `VPS_KNOWN_HOSTS`, `VPS_SSH_KEY`. Pegar cada valor directamente en la interfaz privada de GitHub; **nunca** en README, PR, código, logs o mensajes públicos. Davide debe confirmar que la llave proporcionada sigue autorizada; es recomendable rotarla si el documento se compartió fuera del equipo responsable.
3. En **Settings → Environments**, configurar `main` y `release`, y usar reglas de aprobación/revisores para despliegues si están disponibles. El entorno release debe ser distinto de main.
4. Definir la variable de repositorio `DEPLOY_ENABLED` con valor `true` **solo después** de instalar los secretos, validar el servidor y fusionar el PR de configuración.
5. Probar desde **Actions → Despliegue → Run workflow** una acción no destructiva (`estado` o `logs`). Confirmar el estado real del backend y frontend. Posteriormente, los pushes a `main` despliegan main y las etiquetas `v0.<semana>.0` despliegan release.

### Operación peligrosa: reiniciar-bd

El workflow incluye una opción manual `reiniciar-bd` para main, **deshabilitada** mientras `ALLOW_DB_RESET` no sea `true`. Incluso activada, solo `Nell16` puede solicitarla escribiendo la confirmación `BORRAR BD MAIN`. Esta operación puede borrar datos; habilitarla únicamente con copia de seguridad verificada y acuerdo explícito con el propietario del VPS. Nunca se permite en release.

## 6. Rotación y entrega

- **Lunes:** Integración entrante actualiza `CODEOWNERS` por PR; el equipo saliente revisa ese cambio.
- **Viernes:** Testing confirma versión estable antes de crear un release con etiqueta `v0.<semana>.0`. No crear etiquetas a modo de prueba en el entorno real.
- No almacenar secretos ni datos personales reales en GitHub. Revisar `SECURITY.md`.

Referencias: [README](../../README.md) · [CONTRIBUTING](../../CONTRIBUTING.md) · [Flujo de equipos](FLUJO.md).
