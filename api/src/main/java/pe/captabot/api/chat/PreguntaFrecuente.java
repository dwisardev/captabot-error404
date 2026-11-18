package pe.captabot.api.chat;

import java.util.Set;

/**
 * Una pregunta frecuente (HU01): las raíces de palabras que la identifican, la respuesta y un peso.
 * Ejemplo: la raíz "horari" reconoce "horario" y "horarios". Con peso 2, una pregunta como
 * "¿cuánto cuesta una página web?" se responde con el precio y no con la lista de servicios.
 */
record PreguntaFrecuente(String tema, Set<String> raices, String respuesta, int peso) {

    PreguntaFrecuente(String tema, Set<String> raices, String respuesta) {
        this(tema, raices, respuesta, 1);
    }
}
