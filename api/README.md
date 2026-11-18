# API de CaptaBot – Java 21 + Spring Boot 4.1

Responsable: Gustavo Huaman (@Gussoft)

Recibe los datos que el prospecto deja en el chat de watsonx Assistant, lo califica y lo guarda en PostgreSQL. También entrega la lista de prospectos al panel administrativo.

## Endpoints

| Método | Ruta | Uso |
|---|---|---|
| `GET` | `/api/health` | Comprueba que la API está en línea (no pide clave) |
| `POST` | `/api/prospectos` | Registra un prospecto desde el asistente (HU05) y lo califica (HU03) |
| `GET` | `/api/prospectos?calificacion=CALIENTE&etapa=NUEVO` | Lista prospectos; los filtros son opcionales |
| `GET` | `/api/prospectos/{id}` | Detalle de un prospecto |

Ejemplo de registro:

```bash
curl -X POST http://localhost:8080/api/prospectos \
  -H "Content-Type: application/json" \
  -d '{"nombre":"María Quispe","telefono":"987654321","servicioInteres":"Diseño de página web","presupuesto":6000,"urgencia":"alta"}'
```

Respuesta (`201 Created`):

```json
{"id":11,"calificacion":"CALIENTE","puntaje":100,"mensaje":"Gracias, María. Registramos tus datos y un asesor te contactará pronto."}
```

Si los datos no son válidos responde `400` con el detalle de cada campo, por ejemplo `{"error":"Datos inválidos","detalles":{"telefono":"El teléfono debe tener 9 dígitos"}}`.

## Reglas de calificación (HU03)

`puntaje = puntos por presupuesto + puntos por urgencia` (de 0 a 100)

| Presupuesto (S/) | Puntos | | Urgencia | Puntos |
|---|---|---|---|---|
| 5000 o más | 50 | | ALTA | 50 |
| 2000 a 4999.99 | 35 | | MEDIA | 30 |
| 500 a 1999.99 | 20 | | BAJA | 10 |
| menos de 500 | 5 | | | |

| Puntaje | Calificación |
|---|---|
| 70 o más | CALIENTE |
| 40 a 69 | TIBIO |
| menos de 40 | FRIO |

Las reglas están en `CalificacionService.java` y las prueba `CalificacionServiceTest.java`.

## Clave de la API

Si la variable `CAPTABOT_API_KEY` tiene valor, todas las rutas `/api/**` (menos `/api/health`) exigen la cabecera `X-API-Key` con esa clave. watsonx Assistant la envía desde la extensión personalizada (ver `asistente/README.md`). Para desarrollo local se puede dejar vacía.

## Ejecutar en tu computadora

Requisitos: JDK 21, Maven 3.9 y la base de datos creada (ver `base-datos/README.md`).

```bash
cd api
mvn test                      # pruebas unitarias
mvn spring-boot:run           # inicia la API en http://localhost:8080
```

Variables de entorno opcionales: `DB_URL` (por defecto `jdbc:postgresql://localhost:5432/captabot`), `DB_USER`, `DB_PASSWORD` (por defecto `captabot`), `PORT` (8080) y `CAPTABOT_API_KEY`.

En IntelliJ IDEA o VS Code basta con abrir la carpeta `api` como proyecto Maven y ejecutar `CaptabotApiApplication`.
