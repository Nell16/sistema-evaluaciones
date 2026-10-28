# Arquitectura prevista — sin implementación funcional

Este repositorio prepara un **monorepositorio** con Spring Boot, React y PostgreSQL. Las capas son una organización **propuesta** para cuando existan requisitos aprobados.

## Paquetes previstos en backend

| Carpeta | Responsabilidad futura |
| --- | --- |
| `controller/` | Entradas HTTP REST; actualmente sin controladores |
| `dto/` y `mapper/` | Contratos y transformación de datos |
| `application/usecase/` y `application/service/` | Casos de uso y su coordinación |
| `application/evaluator/` | Estrategias de evaluación, después de aprobar reglas |
| `domain/model/`, `domain/repository/`, `domain/exception/` | Modelo, puertos y reglas de dominio |
| `infrastructure/persistence/` | Acceso a PostgreSQL mediante JPA |
| `infrastructure/security/` | Integración futura de autenticación y permisos |
| `config/` y `exception/` | Configuración y manejo de errores |

## Flujo previsto

React → API HTTP → Controller → Application → Domain → Repository → PostgreSQL.

Esta es una **guía de arquitectura**, no un flujo ejecutable hoy. Los módulos BFA, Weill, Thurstone, Allport y orientación vocacional permanecen sin lógica hasta que se aprueben reglas, contratos y modelos.

Los diagramas originales están en `docs/diagramas/`. Ver la decisión [ADR-0001](decisiones/0001-monorepositorio.md) y el [flujo de los equipos](../equipos/FLUJO.md).
