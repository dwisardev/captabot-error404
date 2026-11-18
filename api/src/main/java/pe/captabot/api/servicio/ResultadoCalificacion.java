package pe.captabot.api.servicio;

import pe.captabot.api.modelo.Calificacion;

/** Puntaje (0 a 100) y calificación resultante de un prospecto. */
public record ResultadoCalificacion(int puntaje, Calificacion calificacion) {
}
