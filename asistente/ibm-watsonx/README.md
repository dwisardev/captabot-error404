# Propuesta anterior: IBM watsonx Assistant

El equipo reemplazó watsonx por el chat propio con Azure AI Language (ver `../README.md`), porque crear
la cuenta de IBM Cloud pide una tarjeta. Estos archivos quedan por si el curso exige watsonx:

- `extension-openapi.json`: extensión personalizada para que watsonx llame a `POST /api/prospectos`.
- `index.html`: página de ejemplo con el chat web de watsonx insertado.

Con watsonx, el flujo de la conversación se configura en el asistente (acciones) y la API se usa igual.
