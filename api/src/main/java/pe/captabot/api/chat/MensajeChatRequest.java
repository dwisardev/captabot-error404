package pe.captabot.api.chat;

import jakarta.validation.constraints.Size;

/** Mensaje del cliente. Sin sesionId se inicia una conversación nueva y el texto puede ir vacío. */
public record MensajeChatRequest(
        @Size(max = 64, message = "La sesión no es válida") String sesionId,
        @Size(max = 500, message = "El mensaje admite hasta 500 caracteres") String texto
) {
}
