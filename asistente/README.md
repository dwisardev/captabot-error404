# Asistente virtual – watsonx Assistant

Guía para configurar el asistente de CaptaBot en IBM watsonx Assistant y conectarlo
con la API. Responsable: Vladimir Angelino (@DeiosZ). Tareas en Jira: SCRUM-12,
SCRUM-13 y SCRUM-15.

| Archivo | Para qué sirve |
|---|---|
| `preguntas-frecuentes.md` | Acciones de preguntas frecuentes con frases de ejemplo y respuestas |
| `extension-openapi.json` | Especificación OpenAPI para crear la extensión personalizada que llama a la API |
| `web/index.html` | Página de demo con el chat web insertado |
| `captabot-asistente.json` | (Pendiente) Exportación del asistente; se sube cuando esté configurado |

## 1. Crear la instancia

1. Crear una cuenta en IBM Cloud (cloud.ibm.com) y buscar **watsonx Assistant** en el catálogo.
2. Elegir el plan **Lite** (gratuito) y la región más cercana, por ejemplo Dallas (`us-south`).
3. Abrir la herramienta (**Launch watsonx Assistant**) y crear un asistente llamado **CaptaBot**, en español.

> En el plan Lite, IBM elimina la instancia si pasa 30 días sin uso. Conviene probar el
> asistente al menos una vez por semana y exportarlo a este repositorio (paso 6).

## 2. Acciones de preguntas frecuentes

Crear una acción por cada fila de `preguntas-frecuentes.md`, copiando las frases de
ejemplo y la respuesta. Al final de cada respuesta, agregar un paso que pregunte si
desea que un asesor lo contacte y, si responde que sí, ir a **Registrar interés**.

## 3. Acción "Registrar interés" (captación de datos)

Frases de ejemplo: *quiero una cotización, quiero contratar, me interesa, quiero que me
llamen, necesito un presupuesto*.

| Paso | El asistente dice | Tipo de respuesta | Variable |
|---|---|---|---|
| 1 | Para que un asesor te contacte, guardaremos tu nombre y teléfono. ¿Estás de acuerdo? | Sí / No | Si es **No**, despedirse y terminar |
| 2 | ¿Cuál es tu nombre? | Texto libre | `nombre` |
| 3 | ¿A qué número de celular te podemos llamar? (9 dígitos) | Texto libre | `telefono` |
| 4 | ¿Qué servicio te interesa? | Opciones: Diseño web, Marketing digital, Soporte técnico, Otro | `servicio` |
| 5 | ¿Cuál es tu presupuesto aproximado en soles? | Moneda (PEN) o número | `presupuesto` |
| 6 | ¿Qué tan pronto lo necesitas? Alta: esta semana · Media: este mes · Baja: solo estoy averiguando | Opciones: Alta, Media, Baja | `urgencia` |
| 7 | Llamar a la extensión **CaptaBot API → Registrar prospecto** con las cinco variables | Extensión | — |
| 8 | Mostrar el campo `mensaje` de la respuesta de la extensión | — | — |

La API valida los datos: si el teléfono no tiene 9 dígitos responde con error 400. En
ese caso, el paso 8 debe tener una condición: si la extensión no respondió bien, decir
*"No pudimos registrar tus datos. Revisa que tu número tenga 9 dígitos e inténtalo
otra vez"* y volver al paso 3.

La API acepta la urgencia en mayúsculas o minúsculas, con o sin tilde (`Alta`, `ALTA`, `alta`).

## 4. Conectar la extensión con la API

1. La API debe estar en una URL pública con **HTTPS**. Para la demo se puede exponer la
   API local con un túnel, por ejemplo `cloudflared tunnel --url http://localhost:8080`
   o `ngrok http 8080`, y copiar la URL que muestra.
2. En `extension-openapi.json`, reemplazar `https://REEMPLAZAR-CON-LA-URL-PUBLICA-DE-LA-API`
   por esa URL (sin `/` al final).
3. En watsonx Assistant: **Integrations → Build custom extension**, subir el archivo,
   nombrarla **CaptaBot API** y agregarla al asistente.
4. En la autenticación de la extensión elegir **API key** y escribir el mismo valor que la
   variable `CAPTABOT_API_KEY` con la que se inició la API.
5. Probar en **Preview**: completar la acción y revisar que el prospecto aparezca en
   `GET /api/prospectos`.

La API se ejecuta como explica `api/README.md`. Ejemplo para la demo:

```bash
export CAPTABOT_API_KEY=una-clave-larga-y-secreta
cd api && mvn spring-boot:run
```

## 5. Publicar el chat web

1. **Integrations → Web chat**, ajustar el color, el nombre (**CaptaBot**) y el mensaje
   de bienvenida.
2. En la pestaña **Embed** copiar `integrationID`, `region` y `serviceInstanceID`.
3. Pegarlos en `web/index.html` y abrir la página en el navegador.

## 6. Exportar el asistente al repositorio

Cada vez que cambien el asistente: **Actions → Global settings → Upload/Download →
Download**, guardar el archivo como `asistente/captabot-asistente.json` y subirlo con un
commit que incluya la clave de la tarea, por ejemplo
`SCRUM-12 Actualiza las acciones de preguntas frecuentes`.
