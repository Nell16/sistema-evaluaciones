# Sistema Integrado de Evaluaciones — esqueleto inicial

> **Alcance actual: solo configuración y estructura.** No incluye endpoints de negocio, cuestionarios, fórmulas, entidades, tablas de evaluación, autenticación ni reportes. La única clase Java es el punto de arranque de Spring Boot; el único componente React es una pantalla de bienvenida.

## Tecnologías solicitadas

- **Backend:** Spring Boot 3.5.16 con Java **17** y Maven.
- **Frontend:** React web (no React Native), TypeScript y Vite como herramientas de arranque.
- **Base de datos:** PostgreSQL 17; **Flyway** preparado para migraciones futuras (todavía no hay tablas de negocio).
- **Colaboración:** un solo repositorio privado de GitHub, ramas, pull requests y comprobaciones de compilación.

**Nota terminológica:** Flyweight es un patrón de diseño; Flyway es la herramienta de migraciones de PostgreSQL mencionada en los documentos. Aquí no se implementa el patrón Flyweight.

## Estructura

| Ruta | Propósito | Estado |
| --- | --- | --- |
| `backend/` | Proyecto Maven, arranque de Spring Boot y paquetes separados por capas | Esqueleto |
| `frontend/` | Proyecto React web con un componente de bienvenida y carpetas por módulos | Esqueleto |
| `database/migrations/` | Lugar reservado para SQL versionado con Flyway | Vacío |
| `docs/` | Requisitos, reglas, contratos, modelo, diagramas y traspaso entre equipos | Plantillas y referencias |
| `tests/` | Espacio para futuros casos end-to-end | Vacío |
| `.github/` | GitHub Actions, plantillas de issues y pull requests | Configurado |
| `compose.yaml` | PostgreSQL local reproducible | Configurado |
| `.env.example` | Ejemplo de variables locales, sin datos reales | Configurado |

## Iniciar el esqueleto desde IntelliJ IDEA

1. Clonar el repositorio y abrir la **raíz completa** en IntelliJ IDEA.
2. Tener instalados JDK 17, Node.js 22, npm y Docker (o PostgreSQL 17).
3. Ejecutar `docker compose up -d db` desde la raíz.
4. Ejecutar `mvn -f backend/pom.xml spring-boot:run` para iniciar Spring Boot en `localhost:8080`.
5. Ejecutar `cd frontend`, después `npm ci` y `npm run dev` para abrir React en `localhost:5173`.

El backend **no expone endpoints de evaluación**; solo puede exponer el estado técnico de salud de Spring Boot Actuator. El frontend únicamente muestra que la estructura fue preparada.

## Comprobaciones técnicas

```bash
mvn -f backend/pom.xml verify
cd frontend
npm ci
npm run typecheck
npm run build
```

GitHub Actions valida la compilación del esqueleto. Cuando se desarrollen funcionalidades, se agregarán pruebas y criterios de aceptación.

## Antes de comenzar funcionalidades

1. Revisar requisitos y aprobaciones en `docs/requisitos/` y `docs/casos-de-uso/`.
2. Acordar el modelo de datos en `docs/modelo-datos/` antes de crear migraciones SQL.
3. Acordar los contratos en `docs/api/` antes de programar controladores o consumo de API.
4. Seguir `docs/equipos/FLUJO.md` y `CONTRIBUTING.md` para ramas, issues, revisión y traspasos.
5. No subir datos personales ni pruebas psicológicas reales. Ver `SECURITY.md`.

Los cinco tipos de evaluación citados en el plan son **módulos futuros**, no funcionalidades implementadas. Los documentos y diagramas se preservan como material de referencia.
