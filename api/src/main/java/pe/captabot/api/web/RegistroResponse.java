package pe.captabot.api.web;

/** Respuesta al asistente tras registrar un prospecto. */
public record RegistroResponse(Long id, String calificacion, int puntaje, String mensaje) {
}
