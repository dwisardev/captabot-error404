# CaptaBot – Equipo ERROR 404

Asistente virtual para captar y dar seguimiento a clientes en PYMEs.
Proyecto Integrador – IES Cibertec. Tablero en Jira: proyecto SCRUM.

| Carpeta | Contenido |
|---|---|
| `api/` | API REST en Java + Spring Boot |
| `panel/` | Panel administrativo en C# ASP.NET Core |
| `base-datos/scripts/` | Scripts SQL de PostgreSQL |
| `base-datos/datos-prueba/` | Datos de ejemplo para pruebas y demo |
| `asistente/` | JSON exportado de watsonx Assistant |
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
