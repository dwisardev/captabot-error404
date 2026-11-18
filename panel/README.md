# Panel administrativo – ASP.NET Core (.NET 10)

Panel web del equipo de ventas. Consume la API de CaptaBot (Java) y no se conecta
directamente a la base de datos. Responsable: Wilder Tudela (@dwisardev).

| Página | Historia | Qué hace |
|---|---|---|
| Iniciar sesión | HU06 | Ingreso con correo y contraseña; ninguna página se abre sin sesión |
| Usuarios | HU07 | Solo el administrador: crear, editar, activar y desactivar usuarios |
| Prospectos | HU08 | Lista con filtros por calificación, etapa y fechas; 20 por página; calientes primero |
| Detalle del prospecto | HU09, HU10 | Cambiar la etapa (con historial) y agregar notas; solo el autor edita su nota |
| Reportes | HU11, HU12 | Prospectos por semana, embudo y tasa de conversión; descarga en Excel e impresión a PDF |

## Cómo ejecutarlo

Requisitos: SDK de .NET 10 y la API corriendo (ver `api/README.md`).

```bash
cd panel/CaptaBot.Panel
dotnet run
```

El panel queda en `http://localhost:5080`. Usuarios de prueba (creados por
`base-datos/datos-prueba/01_datos_prueba.sql`):

| Correo | Contraseña | Rol |
|---|---|---|
| `admin@captabot.pe` | `Demo2026!` | Administrador |
| `ventas@captabot.pe` | `Demo2026!` | Vendedor |

## Configuración

En `appsettings.json`, sección `CaptabotApi`, o con variables de entorno:

| Variable | Por defecto | Uso |
|---|---|---|
| `CaptabotApi__UrlBase` | `http://localhost:8080` | Dirección de la API |
| `CaptabotApi__ClaveApi` | vacío | El mismo valor que `CAPTABOT_API_KEY` en la API, si la API pide clave |

## Pruebas

```bash
dotnet test panel/CaptaBot.Panel.Tests
```

Las pruebas xUnit revisan el cliente de la API, los controladores, la exportación a Excel
y que las páginas no se abran sin sesión. En GitHub Actions también se prueba el panel de
punta a punta: inicia sesión de verdad contra la API y la base de datos.
