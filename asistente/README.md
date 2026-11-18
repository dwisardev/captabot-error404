# Asistente virtual – chat de CaptaBot + Azure AI Language

Responsable: Vladimir Angelino (@DeiosZ). Historias: HU01, HU02, HU04 y HU05 (SCRUM-12, 13, 15 y 16).

El asistente es gratuito de punta a punta:

- **El chat** aparece en la página web de la PYME, que sirve la propia API en `http://localhost:8080`. La
  conversación está programada en la API (`api/.../chat/ConversacionService.java`) y sigue el diagrama
  `docs/diagramas/flujo-asistente.png`. Cuando el cliente deja sus datos, la API registra y califica al
  prospecto, y este aparece en el panel.
- **Las preguntas frecuentes** las responde **Azure AI Language** (función *custom question answering*,
  plan gratuito F0). Si Azure no está configurado o no responde, el chat usa su buscador local con las
  mismas respuestas, así la demo nunca se cae.

| Archivo | Para qué sirve |
|---|---|
| `preguntas-frecuentes.md` | Las 7 respuestas y sus frases de ejemplo |
| `azure/preguntas-frecuentes.tsv` | Las mismas preguntas en el formato que se carga en Azure |
| `azure/cargar-preguntas.py` | Crea, carga y publica el proyecto de preguntas en Azure |
| `ibm-watsonx/` | La propuesta anterior con IBM watsonx Assistant, por si el curso la exige |

## 1. Probar el chat (sin Azure)

```bash
docker compose up --build
```

Abran `http://localhost:8080` y hagan clic en el botón azul de la esquina inferior derecha. Prueben
«¿Cuánto cuesta?», «Quiero cotizar» y completen los datos; el prospecto aparece en el panel
(`http://localhost:5080`). En `http://localhost:8080/api/health` dice `"preguntasFrecuentes": "local"`.

## 2. Conectar Azure AI Language (gratis)

### 2.1 Activar Azure for Students

En [azure.microsoft.com/free/students](https://azure.microsoft.com/free/students) inicien sesión y
verifiquen que son estudiantes con su correo de Cibertec. No pide tarjeta, da USD 100 de crédito por
12 meses y se renueva cada año mientras sean estudiantes. Lo que usamos aquí entra en planes gratuitos
y no consume ese crédito.

### 2.2 Crear el recurso de Language

En el [portal de Azure](https://portal.azure.com): **Crear un recurso** → busquen **Language** (Azure AI
Language / Language service) → **Crear**. En las funciones adicionales elijan **Custom question answering**
y continúen. Completen:

| Campo | Valor |
|---|---|
| Suscripción | Azure for Students |
| Grupo de recursos | Nuevo: `captabot` |
| Región | East US, East US 2 o la que permita su suscripción |
| Nombre | Uno único, por ejemplo `captabot-lenguaje-404` |
| Plan de tarifa | **Free F0** |
| Ubicación de Azure Search | La misma región |
| Plan de Azure Search | **Free F** |

Acepten el aviso de IA responsable y elijan **Revisar y crear** → **Crear**. Si la región da error, prueben
otra: algunas suscripciones de estudiante solo permiten ciertas regiones. Cada suscripción admite un solo
recurso Free F0 y un solo Azure Search gratuito.

### 2.3 Copiar la clave y el endpoint

En el recurso creado: **Claves y punto de conexión**. En la carpeta del repositorio, copien `.env.ejemplo`
como `.env` y completen:

```bash
AZURE_LANGUAGE_ENDPOINT=https://captabot-lenguaje-404.cognitiveservices.azure.com/
AZURE_LANGUAGE_KEY=la-clave-1-del-portal
AZURE_LANGUAGE_PROYECTO=captabot-faq
```

El archivo `.env` no se sube a GitHub (está en `.gitignore`). No compartan la clave por chat ni en el informe.

### 2.4 Cargar las preguntas

```bash
python3 asistente/azure/cargar-preguntas.py
```

El script crea el proyecto `captabot-faq` en español, carga las 7 respuestas con sus 35 formas de
preguntar, lo publica y hace una pregunta de prueba. Con `--simular` muestra lo que enviaría sin llamar a
Azure; con `--reiniciar` borra el proyecto y lo vuelve a crear (úsenlo si cambian las respuestas).

### 2.5 Reiniciar y comprobar

```bash
docker compose up --build
```

`http://localhost:8080/api/health` debe decir `"preguntasFrecuentes": "azure-ai-language"`. Ahora Azure
entiende también preguntas escritas de otra forma, por ejemplo «¿abren los domingos?».

### Límites del plan gratuito

El plan F0 de Language incluye 5 000 registros de texto al mes compartidos entre sus funciones. Si se
alcanza el límite, Azure deja de responder hasta el mes siguiente y **no cobra**; mientras tanto el chat
usa el buscador local.

## 3. Cambiar las respuestas

Las respuestas están en tres lugares que deben coincidir: `preguntas-frecuentes.md`,
`azure/preguntas-frecuentes.tsv` y `api/src/main/java/pe/captabot/api/chat/PreguntasLocales.java`.
Después de cambiarlas, ejecuten `cargar-preguntas.py --reiniciar`.

## 4. Insertar el chat en otra página web

```html
<link rel="stylesheet" href="https://SU-API/chat/captabot-chat.css">
<script src="https://SU-API/chat/captabot-chat.js" data-api="https://SU-API"></script>
```

Y agreguen la dirección de esa página en la variable `CAPTABOT_CHAT_ORIGENES` de la API.

## 5. Cómo responde el chat

| El cliente escribe | El chat |
|---|---|
| Una pregunta frecuente | Responde y ofrece que un asesor lo contacte |
| «Quiero cotizar», «que me llame un asesor» | Pide consentimiento, nombre, celular, servicio, presupuesto y urgencia |
| Un celular sin 9 dígitos o un monto que no entiende | Explica el formato y lo vuelve a pedir |
| Algo que no entiende | Dice qué temas puede atender |
| «menú» | Vuelve al inicio |

El chat es público (no pide la clave X-API-Key) porque lo usa el navegador del cliente; solo permite
registrar prospectos siguiendo la conversación.
