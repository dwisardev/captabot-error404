package pe.captabot.api.chat;

import java.util.Optional;

/** Busca la respuesta a una pregunta frecuente (HU01). Vacío si no la encuentra. */
public interface MotorPreguntas {

    Optional<String> responder(String pregunta);
}
