package pe.captabot.api.chat;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat de la página web (HU02). Es público: no pide la clave X-API-Key porque lo usa el navegador del cliente.
 * Solo permite registrar prospectos siguiendo el flujo de la conversación.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ConversacionService servicio;

    public ChatController(ConversacionService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/mensajes")
    public RespuestaChat mensaje(@Valid @RequestBody MensajeChatRequest solicitud) {
        return servicio.procesar(solicitud.sesionId(), solicitud.texto());
    }
}
