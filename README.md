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
| Base de datos PostgreSQL | Listo: tablas, vistas de reportes y datos de prueba |
| API Java (Spring Boot) | Listo: registro y calificación, usuarios, inicio de sesión, seguimiento y reportes |
| Panel administrativo .NET 10 | Listo: inicio de sesión, prospectos, etapas, notas, usuarios y reportes con Excel |
| Pruebas | JUnit en la API, xUnit en el panel y prueba de punta a punta en cada push |
| Integración continua | GitHub Actions: BD, API, panel y prueba de punta a punta |
| Asistente watsonx | Material listo; falta crear la instancia y configurarlo |

## Cómo ejecutarlo en local

### Opción 1: Docker (recomendada)

Solo necesitan Docker. Desde la carpeta del repositorio:

```bash
docker compose up --build
```

La primera vez tarda unos minutos porque descarga las imágenes y compila. Luego abran
`http://localhost:5080` e inicien sesión con `admin@captabot.pe` / `Demo2026!`. La API queda
en `http://localhost:8080` y PostgreSQL en el puerto `5433`. Para apagar: `Ctrl+C` y
`docker compose down` (agreguen `-v` para borrar la base y volver a cargar los datos de prueba).

### Opción 2: instalando cada parte

Requisitos: PostgreSQL 16, Java 21 con Maven y el SDK de .NET 10.

```bash
# 1. Base de datos (ver base-datos/README.md)
sudo -u postgres psql -c "CREATE ROLE captabot LOGIN PASSWORD 'captabot';"
sudo -u postgres psql -c "CREATE DATABASE captabot OWNER captabot;"
export PGPASSWORD=captabot
psql -h localhost -U captabot -d captabot -f base-datos/scripts/01_crear_esquema.sql
psql -h localhost -U captabot -d captabot -f base-datos/scripts/02_vistas_reportes.sql
psql -h localhost -U captabot -d captabot -f base-datos/datos-prueba/01_datos_prueba.sql

# 2. API, en una terminal (ver api/README.md)
cd api && mvn spring-boot:run

# 3. Panel, en otra terminal (ver panel/README.md)
cd panel/CaptaBot.Panel && dotnet run
```

Abran `http://localhost:5080` e inicien sesión con `admin@captabot.pe` / `Demo2026!`.
Para conectar el asistente, sigan `asistente/README.md`.

## Integración continua

En cada push a `main` y en cada pull request, GitHub Actions:

1. Ejecuta los scripts SQL en un PostgreSQL limpio.
2. Compila la API y corre las pruebas JUnit.
3. Compila el panel y corre las pruebas xUnit.
4. Levanta la base de datos, la API y el panel, y prueba el sistema de punta a punta: registro
   desde el asistente, inicio de sesión, cambio de etapa, notas, reportes y descarga de Excel.

Si algo falla, el resultado aparece en la pestaña **Actions** y en el pull request.
