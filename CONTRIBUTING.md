# Contribución y gobierno del repositorio

## Etapa actual: solo esqueleto

La rama `main` contiene la configuración inicial de Spring Boot, React, PostgreSQL y la estructura documental. Todavía **no** hay funcionalidades de evaluación ni pruebas funcionales.

## Reglas para los equipos

1. Trabajar en una rama por issue desde `main`: `feature/...`, `fix/...`, `docs/...` o `chore/...`.
2. No agregar cuestionarios, endpoints, entidades, SQL ni reglas de puntuación sin requisitos, contratos y modelo aprobados.
3. Abrir pull requests con propósito, issue, validación realizada y documentación modificada.
4. Coordinar cualquier cambio de API entre frontend, backend, documentación e integración.
5. Usar Flyway para nuevas migraciones solo después de aprobar DOC-03. No modificar una migración ya aplicada.
6. No subir datos reales, contraseñas, tokens ni archivos `.env`.
7. La revisión y fusión corresponden a los integradores designados; testers y documentadores participan según la rotación.

## Comprobación técnica inicial

GitHub Actions ejecuta `backend-build` y `frontend-build`: se confirma que los proyectos base compilan. **No equivale a validar funcionalidades que aún no existen.**

Cuando comiencen las historias, agregar pruebas unitarias, de integración y criterios de aceptación. Proteger `main` mediante las reglas del repositorio y exigir PR y las comprobaciones disponibles.

Una vez identificados los integrantes reales, copiar `.github/CODEOWNERS.example` a `.github/CODEOWNERS` y corregir las cuentas antes de activarlo.
