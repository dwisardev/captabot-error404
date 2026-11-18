# Prueba de integración con Postman (SCRUM-47)

La colección `CaptaBot.postman_collection.json` tiene 41 solicitudes con sus pruebas
automáticas, ordenadas en cinco carpetas que se ejecutan en orden:

| Carpeta | Historias | Qué comprueba |
|---|---|---|
| 1. Asistente | HU03, HU05 | Registro desde el chat, calificación caliente/tibio/frío, validaciones y clave de la API |
| 2. Inicio de sesión y usuarios | HU06, HU07 | Login, contraseña incorrecta, correo repetido, edición y desactivación |
| 3. Lista, etapas y notas | HU08, HU09, HU10 | Filtros y orden, cambio de etapa con historial, notas y permiso del autor |
| 4. Reportes | HU11, HU12 | Prospectos por semana, embudo y tasa de conversión |
| 5. Chat del asistente | HU01, HU02, HU04, HU05 | Conversación completa sin clave: pregunta frecuente, consentimiento, datos, validaciones y registro |

## Ejecutar en Postman

1. Levanten la base de datos y la API (ver el README principal).
2. En Postman: **Import** y elijan los dos archivos de esta carpeta.
3. Elijan el entorno **CaptaBot local**. Si la API se inició con `CAPTABOT_API_KEY`,
   escriban la misma clave en la variable `apiKey`.
4. Clic derecho en la colección → **Run collection** → **Run**.

## Ejecutar desde la terminal

```bash
npx newman run pruebas/postman/CaptaBot.postman_collection.json \
  -e pruebas/postman/CaptaBot-local.postman_environment.json
```

GitHub Actions ejecuta esta misma colección en cada push, contra la API y una base de
datos PostgreSQL recién creada.
