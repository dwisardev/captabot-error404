package pe.captabot.api.chat;

/** Paso de la conversación en el que está el cliente (ver docs/diagramas/flujo-asistente.png). */
enum EstadoConversacion {
    MENU, PREGUNTA_ASESOR, CONSENTIMIENTO, NOMBRE, CELULAR, SERVICIO, PRESUPUESTO, URGENCIA, CONFIRMACION
}
