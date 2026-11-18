package pe.captabot.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.chat.PreguntasAzure;

import java.util.Map;

/** Permite comprobar que la API está en línea (no pide clave) y qué motor usa el asistente. */
@RestController
public class EstadoController {

    private final PreguntasAzure azure;

    public EstadoController(PreguntasAzure azure) {
        this.azure = azure;
    }

    @GetMapping("/api/health")
    public Map<String, String> estado() {
        return Map.of(
                "estado", "ok",
                "servicio", "captabot-api",
                "preguntasFrecuentes", azure.estaConfigurado() ? "azure-ai-language" : "local");
    }
}
