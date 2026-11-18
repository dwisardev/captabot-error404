# CaptaBot – Equipo ERROR 404

[![CI](https://github.com/dwisardev/captabot-error404/actions/workflows/ci.yml/badge.svg)](https://github.com/dwisardev/captabot-error404/actions/workflows/ci.yml)

Asistente virtual para captar y dar seguimiento a clientes en PYMEs.
Proyecto Integrador – IES Cibertec. Tablero en Jira: proyecto SCRUM.

| Carpeta | Contenido |
|---|---|
| `api/` | API REST en Java + Spring Boot |
| `panel/` | Panel administrativo en C# ASP.NET Core |
| `base-datos/scripts/` | Scripts SQL de PostgreSQL |
| `base-datos/datos-prueba/` | Datos de ejemplo para pruebas y demo |
| `asistente/` | Guía, preguntas frecuentes y extensión de watsonx Assistant |
| `pruebas/postman/` | Colección de Postman para la prueba de integración |
| `docs/diagramas/` | Diagramas de draw.io |
| `docs/entregables/` | Documentos del curso |
| `docs/informes-sprint/` | Informes de avance de cada sprint |
| `.github/workflows/` | Integración continua con GitHub Actions |

## Equipo y roles

| Integrante | GitHub | Rol en Scrum | Área principal |
|---|---|---|---|
| Wilder Tudela | @dwisardev | Líder y Product Owner | Panel .NET, Product Backlog |
| Alfredo Fara | @YakFar | Scrum Master | Diagramas, integración continua, informes |
| Gustavo Huaman | @Gussoft | Desarrollo | API Java y reportes |
| Said Oyola | @said5596 | Desarrollo | Base de datos y pruebas |
| Vladimir Angelino | @DeiosZ | Desarrollo | Asistente watsonx y documentación |

El archivo `.github/CODEOWNERS` indica el responsable de cada carpeta, quien revisa los pull requests que la modifican.

## Cómo trabajar con Jira

El repositorio está conectado a Jira. Pongan la clave de la tarea en la rama, el commit
o el pull request y aparecerá en la tarea, por ejemplo:

- Rama: `SCRUM-36-crear-bd`
- Commit: `SCRUM-36 crea tablas de interesados`

## Estado del producto

| Componente | Estado |
|---|---|
| Base de datos PostgreSQL | Listo: tablas, vistas de reportes y 10 prospectos de prueba |
| API Java (Spring Boot) | Listo: registro, calificación automática, consulta y filtros |
| Pruebas | 23 pruebas JUnit y una prueba de integración en cada push |
| Integración continua | GitHub Actions: scripts SQL, pruebas de la API e integración |
| Asistente watsonx | Material listo; falta crear la instancia y configurarlo |
| Panel administrativo .NET | Pendiente (Sprint 2) |

## Cómo ejecutarlo en local

Requisitos: PostgreSQL 16, Java 21 y Maven.

```bash
# 1. Base de datos (ver base-datos/README.md)
createdb captabot
psql -d captabot -f base-datos/scripts/01_crear_esquema.sql
psql -d captabot -f base-datos/scripts/02_vistas_reportes.sql
psql -d captabot -f base-datos/datos-prueba/01_datos_prueba.sql

# 2. API (ver api/README.md)
cd api
mvn spring-boot:run
```

La API queda en `http://localhost:8080`; `GET /api/health` confirma que está activa.
Para conectar el asistente, sigan `asistente/README.md`.

## Integración continua

En cada push a `main` y en cada pull request, GitHub Actions:

1. Ejecuta los scripts SQL en un PostgreSQL limpio.
2. Compila la API y corre las pruebas JUnit.
3. Levanta la API con la base de datos y prueba el registro completo de un prospecto.

Si algo falla, el resultado aparece en la pestaña **Actions** y en el pull request.
