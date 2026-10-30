# Configuración de GitHub y trabajo colaborativo

> Guía operativa para los cuatro equipos. Actualizada el 30 de octubre de 2026. No contiene credenciales ni autoriza operaciones sobre servidores.

## Modelo de permisos

- **Integración:** colaboradores con acceso de escritura al repositorio central. Revisan y fusionan PRs.
- **Desarrollo, Documentación y Testing:** hacen un **fork** del repositorio público, crean una rama por tarea en su fork y abren un PR hacia `Nell16/sistema-evaluaciones:main`.
- Ninguna persona trabaja directamente en `main`. La ruleset `Protección de main` está activa.
- Una persona no puede aprobar su propio PR; se necesita otro revisor autorizado.

## Flujo por tarea

1. Revisar la tarea de ClickUp y obtener su código (ej. `GEN-01-BE`).
2. Actualizar el fork con los cambios de `main` y crear una rama `tipo/nombre-CODIGO`. Ejemplo: `feature/ana-GEN-01-BE`.
3. Hacer los cambios en esa rama, manteniendo pruebas y documentación relevantes.
4. Abrir un PR a `main`, con título `[GEN-01-BE] Descripción`. La primera línea de la descripción debe ser `Tarea ClickUp: GEN-01-BE · CU-ID[in review]` (reemplazando el ID por el real).
5. Revisar que pasen `reglas`, `backend-build`, `frontend-build` y `ci-ok`; responder observaciones y solicitar aprobación de Integración.
6. Solo los integradores autorizados fusionan un PR aprobado. La rama del PR se puede eliminar después de integrar.

## Configuración pendiente del propietario (cuenta Nell16)

1. **Colaboradores:** en Settings → Collaborators, invitar únicamente a los usuarios reales de Integración. Pedir que acepten.
2. **Revisión específica de Integración:** crear `.github/CODEOWNERS` con los nombres de usuario reales de los integradores (ejemplo: `* @usuario1 @usuario2`). En Settings → Rules → Rulesets → Protección de main, activar **Require review from Code Owners**. Esto sustituye al marcador de ejemplo; no usar cuentas ficticias. Si se rota el equipo, actualizar CODEOWNERS por PR y revisar el impacto sobre aprobaciones.
3. **Checks requeridos:** después de fusionar el PR de CI y confirmar que `ci-ok` aparece en verde, cambiar los checks requeridos en la ruleset a `ci-ok`; mantener PR obligatorio, una aprobación, conversaciones resueltas, prohibición de eliminación/force-push y ninguna excepción de bypass.
4. **Método de fusión:** en Settings → General → Pull Requests, permitir solo **Squash merging** (mensaje por defecto: título del PR), habilitar actualización sugerida de ramas y eliminación automática de ramas después de fusionar.
5. **ClickUp:** Alexander conecta la integración oficial de GitHub a su espacio de ClickUp; Nell16 autoriza el acceso de esa aplicación al repositorio concreto. No asumir que la conexión está completada sin comprobarla.

## Despliegue al VPS: bloqueado hasta revisión de seguridad

- **No publicar, copiar ni reutilizar llaves SSH privadas incluidas en documentos o mensajes.** Antes de despliegues hay que **revocar esa llave y crear otra** de alcance mínimo. Verificar la huella pública del servidor por un canal independiente.
- Configurar los secretos desde Settings → Secrets and variables → Actions, preferiblemente en **environments** separados para `main` y `release` con aprobaciones; jamás en código, documentación ni PRs.
- El dueño del servidor debe confirmar infraestructura, usuarios de despliegue, dominios/puertos, scripts remotos y método de reversión antes de habilitar GitHub Actions de despliegue.
- **No habilitar un botón de reinicio que borre la base de datos** sin una decisión explícita de responsables, copias de seguridad verificadas y controles adicionales.
- No añadir un workflow de despliegue automático a `main` mientras sigan pendientes esos pasos.

## Estado del repositorio

- Repositorio público único, Spring Boot + Java 17, React web y PostgreSQL.
- `main` protegida por ruleset activa (PR, 1 aprobación, checks, sin eliminación/force-push).
- PR `[INT-01]` de configuración avanzada de CI propuesto para revisión, sin cambios de negocio.
- `CODEOWNERS`, ajustes administrativos finales, conexión ClickUp y despliegue siguen pendientes de autorización/configuración del propietario.

Referencias: [README](../../README.md) · [CONTRIBUTING](../../CONTRIBUTING.md) · [Flujo de equipos](FLUJO.md).
