package pe.captabot.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.chat.PreguntasAzure;
import pe.captabot.api.telegram.TelegramApi;

import java.util.Map;

/** Permite comprobar que la API está en línea (no pide clave), qué motor usa el asistente y si Telegram está activo. */
@RestController
public class EstadoController {

    private final PreguntasAzure azure;
    private final TelegramApi telegram;

    public EstadoController(PreguntasAzure azure, TelegramApi telegram) {
        this.azure = azure;
        this.telegram = telegram;
    }

    @GetMapping("/api/health")
    public Map<String, String> estado() {
        return Map.of(
                "estado", "ok",
                "servicio", "captabot-api",
                "preguntasFrecuentes", azure.estaConfigurado() ? "azure-ai-language" : "local",
                "telegram", telegram.estaConfigurado() ? "activo" : "apagado");
    }
}
