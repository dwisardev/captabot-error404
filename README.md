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
| `asistente/` | Asistente: guía de Azure AI Language, preguntas frecuentes y script de carga |
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
| Vladimir Angelino | @DeiosZ | Desarrollo | Asistente (chat y Azure AI Language) y documentación |

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
| Asistente virtual | Listo: chat en la página web y bot de Telegram, con preguntas frecuentes y captación de datos; Azure AI Language opcional (gratis) |
| Automatización (HU13) | Listo: avisos de prospectos nuevos al Telegram de los vendedores, recordatorios de los que siguen sin contactar y seguimiento al cliente de Telegram o WhatsApp |
| Canal de WhatsApp | Listo: mismo flujo del asistente por WhatsApp con el número de prueba gratis de Meta (guía en `asistente/README.md`, sección 8) |

## Cómo ejecutarlo en local

### Opción 1: Docker (recomendada)

Solo necesitan Docker. Desde la carpeta del repositorio:

```bash
docker compose up --build
```

La primera vez tarda unos minutos porque descarga las imágenes y compila. Luego abran
`http://localhost:5080` e inicien sesión con `admin@captabot.pe` / `Demo2026!`. La página web de la
PYME con el chat del asistente queda en `http://localhost:8080` y PostgreSQL en el puerto `5433`. Para apagar: `Ctrl+C` y
`docker compose down` (agreguen `-v` para borrar la base y volver a cargar los datos de prueba).
Si ya tenían la base creada antes de la automatización (HU13), ejecuten una vez `docker compose down -v`
para que se cree con el script nuevo.

### Opción 2: instalando cada parte

Requisitos: PostgreSQL 16, Java 21 con Maven y el SDK de .NET 10.

```bash
# 1. Base de datos (ver base-datos/README.md)
sudo -u postgres psql -c "CREATE ROLE captabot LOGIN PASSWORD 'captabot';"
sudo -u postgres psql -c "CREATE DATABASE captabot OWNER captabot;"
export PGPASSWORD=captabot
psql -h localhost -U captabot -d captabot -f base-datos/scripts/01_crear_esquema.sql
psql -h localhost -U captabot -d captabot -f base-datos/scripts/02_vistas_reportes.sql
psql -h localhost -U captabot -d captabot -f base-datos/scripts/03_automatizacion.sql
psql -h localhost -U captabot -d captabot -f base-datos/datos-prueba/01_datos_prueba.sql

# 2. API, en una terminal (ver api/README.md)
cd api && mvn spring-boot:run

# 3. Panel, en otra terminal (ver panel/README.md)
cd panel/CaptaBot.Panel && dotnet run
```

Abran `http://localhost:5080` e inicien sesión con `admin@captabot.pe` / `Demo2026!`.
La página web de la PYME con el chat queda en `http://localhost:8080`. Para conectar Azure AI Language y el bot de Telegram, sigan `asistente/README.md`.

La API lee sola el archivo `.env` de la raíz del proyecto (copien `.env.ejemplo`), también al ejecutarla
desde el IDE o con `mvn spring-boot:run`. Ahí van el token de Telegram y la configuración de los avisos
automáticos (sección 7 de `asistente/README.md`).

## Integración continua

En cada push a `main` y en cada pull request, GitHub Actions:

1. Ejecuta los scripts SQL en un PostgreSQL limpio.
2. Compila la API y corre las pruebas JUnit.
3. Compila el panel y corre las pruebas xUnit.
4. Levanta la base de datos, la API y el panel, y prueba el sistema de punta a punta: registro
   desde el chat del asistente, inicio de sesión, cambio de etapa, notas, reportes y descarga de Excel.

Si algo falla, el resultado aparece en la pestaña **Actions** y en el pull request.

## Página pública

La carpeta `sitio/` es la página pública de CaptaBot: portada, política de privacidad, instrucciones para
eliminar datos y condiciones de uso. Meta las pide para crear la app de WhatsApp.

- Dirección: https://dwisardev.github.io/captabot-error404/
- Privacidad: https://dwisardev.github.io/captabot-error404/privacidad.html
- Eliminar datos: https://dwisardev.github.io/captabot-error404/eliminar-datos.html
- Condiciones: https://dwisardev.github.io/captabot-error404/condiciones.html

El workflow `.github/workflows/pages.yml` publica **solo** la carpeta `sitio/`, y antes revisa que no
contenga tokens ni claves. El repositorio sigue siendo privado. La página es estática: no llama a la API
ni usa cookies, y las fuentes están dentro de la carpeta. Para que funcione, en GitHub
**Settings → Pages → Source** debe estar en **GitHub Actions**.
