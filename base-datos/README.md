# Base de datos – PostgreSQL 16

Responsable: Said Oyola (@said5596)

## Tablas

| Tabla | Contenido |
|---|---|
| `usuarios` | Usuarios del panel (administrador o vendedor), contraseña con BCrypt |
| `prospectos` | Prospectos captados por el asistente, con su puntaje, calificación y etapa |
| `historial_etapas` | Cada cambio de etapa de un prospecto (HU09) |
| `notas_seguimiento` | Notas del equipo de ventas sobre cada prospecto (HU10) |

Vistas para reportes: `v_prospectos_por_semana` (HU11), `v_embudo_etapas` y `v_tasa_conversion` (HU12).

## Crear la base en tu computadora

```bash
# 1. Crear usuario y base (una sola vez)
sudo -u postgres psql -c "CREATE ROLE captabot LOGIN PASSWORD 'captabot';"
sudo -u postgres psql -c "CREATE DATABASE captabot OWNER captabot;"

# 2. Ejecutar los scripts en orden
export PGPASSWORD=captabot
psql -h localhost -U captabot -d captabot -f scripts/01_crear_esquema.sql
psql -h localhost -U captabot -d captabot -f scripts/02_vistas_reportes.sql
psql -h localhost -U captabot -d captabot -f scripts/03_automatizacion.sql
psql -h localhost -U captabot -d captabot -f datos-prueba/01_datos_prueba.sql
```

En Windows, abre "SQL Shell (psql)" o pgAdmin y ejecuta los mismos tres archivos en ese orden.

Los datos de prueba crean dos usuarios (`admin@captabot.pe` y `ventas@captabot.pe`, contraseña `Demo2026!`) y 10 prospectos. Son solo para desarrollo.
