# Asistente virtual – chat web, Telegram y Azure AI Language

Responsable: Vladimir Angelino (@DeiosZ). Historias: HU01, HU02, HU04 y HU05 (SCRUM-12, 13, 15 y 16).

El asistente es gratuito de punta a punta:

- **El chat** aparece en la página web de la PYME, que sirve la propia API en `http://localhost:8080`. La
  conversación está programada en la API (`api/.../chat/ConversacionService.java`) y sigue el diagrama
  `docs/diagramas/flujo-asistente.png`. Cuando el cliente deja sus datos, la API registra y califica al
  prospecto, y este aparece en el panel.
- **Telegram**: el mismo asistente atiende en un bot de Telegram, con las opciones como botones y un botón
  para compartir el celular. Los prospectos quedan con el canal `TELEGRAM` en el panel (ver sección 6).
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

## 5. Cómo responde el chat (web y Telegram)

| El cliente escribe | El chat |
|---|---|
| Una pregunta frecuente | Responde y ofrece que un asesor lo contacte |
| «Quiero cotizar», «que me llame un asesor» | Pide consentimiento, nombre, celular, servicio, presupuesto y urgencia |
| Un celular sin 9 dígitos o un monto que no entiende | Explica el formato y lo vuelve a pedir |
| Algo que no entiende | Dice qué temas puede atender |
| «menú» | Vuelve al inicio |

El chat es público (no pide la clave X-API-Key) porque lo usa el navegador del cliente; solo permite
registrar prospectos siguiendo la conversación.

## 6. Canal de Telegram (gratis)

1. En Telegram, abran **@BotFather** y envíen `/newbot`. Elijan un nombre (por ejemplo «CaptaBot
   Demo») y un usuario que termine en `bot` (por ejemplo `captabot_error404_bot`).
2. BotFather responde con un **token**. Cópienlo en el archivo `.env`:

   ```bash
   TELEGRAM_BOT_TOKEN=123456789:AA...
   ```

   El token es como una contraseña: no lo compartan ni lo suban a GitHub.
3. Reinicien la API: con `docker compose up --build` o, desde el IDE, deteniéndola y volviéndola a
   iniciar (la API también lee el `.env` de la raíz). En `http://localhost:8080/api/health` debe decir
   `"telegram": "activo"` y en el log de la API aparece «Bot de Telegram @… escuchando mensajes».
4. Abran el bot en Telegram y toquen **Iniciar**. Prueben «Quiero cotizar» y, cuando pida el celular, el
   botón **📱 Compartir mi número**. El prospecto aparece en el panel con el canal «Telegram».

Opcional en @BotFather: `/setdescription` y `/setuserpic` para que el bot se vea profesional, y
`/setcommands` con `start - Empezar de nuevo`.

**Importante:** el bot recibe los mensajes preguntando a Telegram cada pocos segundos (long polling), así
que funciona en una laptop sin dirección pública. Pero solo **una** copia de la API puede usar el mismo
token a la vez: si dos integrantes levantan el sistema con el mismo token, uno de los dos no recibirá
mensajes. Para probar por separado, que cada uno cree su propio bot de prueba.

El bot solo responde en chats privados (no en grupos) y por ahora solo entiende mensajes de texto.

## 7. Avisos y seguimiento automáticos (HU13)

Con el bot de Telegram activo, CaptaBot automatiza tres cosas:

| Qué pasa | Cuándo | Quién lo recibe |
|---|---|---|
| Aviso de prospecto nuevo, con su calificación y un botón de WhatsApp | Apenas se registra (web, Telegram o API) | Los vendedores vinculados |
| Recordatorio de un prospecto caliente o tibio que sigue en «Nuevo» | Después de `CAPTABOT_RECORDATORIO_MINUTOS` (120) | Los vendedores vinculados |
| Mensaje de seguimiento con el menú del asistente | Después de `CAPTABOT_SEGUIMIENTO_MINUTOS` (1440 = 24 h), si la venta sigue abierta | El cliente que llegó por Telegram |

Cada recordatorio y cada seguimiento se envían **una sola vez**, y solo para prospectos de los últimos dos
días. A los clientes del chat web no se les puede escribir después (no dejan un canal), así que a ellos
los contacta el vendedor con el botón de WhatsApp.

### Activarlo

1. En el archivo `.env`, inventen un código para el equipo y, si quieren, ajusten los tiempos:

   ```bash
   TELEGRAM_CODIGO_VENTAS=ventas-error404
   CAPTABOT_RECORDATORIO_MINUTOS=120
   CAPTABOT_SEGUIMIENTO_MINUTOS=1440
   ```

2. Reinicien la API. En `/api/health`, la sección `automatizacion` debe decir `"avisosVendedores": "activo"`.
3. Cada vendedor abre el bot en su Telegram y escribe `/vendedor ventas-error404`. Desde ese momento recibe
   los avisos; con `/salir` deja de recibirlos. `vendedoresVinculados` en `/api/health` muestra cuántos hay.

### Mostrarlo en la presentación

Pongan tiempos cortos en el `.env` (por ejemplo `CAPTABOT_RECORDATORIO_MINUTOS=1` y
`CAPTABOT_SEGUIMIENTO_MINUTOS=2`) y reinicien la API. Luego:

1. Con un celular vinculado como vendedor, registren un prospecto desde la página web: el aviso llega al
   instante.
2. Si el prospecto es caliente o tibio y no le cambian la etapa, en uno o dos minutos llega el recordatorio.
3. Desde otro Telegram, coticen por el bot: a los dos o tres minutos el bot le escribe el seguimiento.

La revisión de pendientes corre cada minuto. Si Telegram no está configurado, nada de esto se activa y el
resto del sistema funciona igual.

## 8. Canal de WhatsApp (número de prueba de Meta, gratis)

El cliente conversa con CaptaBot por WhatsApp con el mismo flujo de la página web y Telegram: las opciones
salen como botones o como lista y, cuando pide el celular, basta tocar **📱 Usar este número**. El prospecto
llega al panel con el canal «WhatsApp» y el aviso al equipo de ventas llega por Telegram (sección 7).

### Costos

| Opción | Costo |
|---|---|
| Número de prueba de Meta (para el curso) | Gratis; solo puede conversar con hasta 5 celulares que registren |
| Número real de la empresa | Desde el 1 de octubre de 2026, las respuestas se cobran después de 1 000 gratis al mes por número; los mensajes que inicia la empresa (plantillas) siempre se cobran |

WhatsApp solo permite escribir libremente al cliente dentro de las 24 horas desde su último mensaje. Por eso
el seguimiento automático por WhatsApp solo se envía si `CAPTABOT_SEGUIMIENTO_MINUTOS` es menor que 1380
(23 horas); con el valor por defecto (1440) no se envía y queda para el vendedor.

### Activarlo

1. En **developers.facebook.com** creen una app de tipo empresa y agréguenle el producto **WhatsApp**. En
   **Configuración de la API** Meta les da un número de prueba, su **Phone number ID** y un **token de acceso
   temporal** (dura 24 horas).
2. En esa misma pantalla agreguen como destinatarios los celulares del equipo (hasta 5); a cada uno le llega
   un código para confirmarlo.
3. Completen el `.env`:

   ```bash
   WHATSAPP_TOKEN=EAAG...            # token de acceso
   WHATSAPP_PHONE_NUMBER_ID=1234...  # Phone number ID (no es el número de teléfono)
   WHATSAPP_VERIFY_TOKEN=captabot-wa # lo inventan ustedes
   WHATSAPP_APP_SECRET=abcd...       # Configuración de la app → Básica → Clave secreta
   ```

4. Abran un túnel para que Meta llegue a su laptop. Con **ngrok** (gratis, con un dominio fijo por cuenta):

   ```bash
   sudo snap install ngrok
   ngrok config add-authtoken SU_TOKEN_DE_NGROK
   ngrok http 8080
   ```

   ngrok muestra una dirección como `https://algo.ngrok-free.dev`, que no cambia entre reinicios.
5. En Meta, en **WhatsApp → Configuración → Webhook**, pongan la URL
   `https://algo.ngrok-free.dev/api/whatsapp/webhook` y el mismo `WHATSAPP_VERIFY_TOKEN`; toquen
   **Verificar y guardar** y suscríbanse al campo **messages**.
6. Reinicien la API. En `/api/health` debe decir `"whatsapp": "activo"`. Desde un celular registrado,
   escriban «Hola» al número de prueba.

El token temporal vence cada 24 horas: si deja de responder, generen uno nuevo y actualicen el `.env`. Para
no renovarlo, se puede crear un usuario del sistema con token permanente en el administrador comercial de Meta.

**Seguridad:** el webhook no pide la clave de la API porque lo llama Meta, pero solo acepta mensajes con la
firma de Meta (`WHATSAPP_APP_SECRET`). Sin `WHATSAPP_VERIFY_TOKEN`, el webhook queda apagado.
