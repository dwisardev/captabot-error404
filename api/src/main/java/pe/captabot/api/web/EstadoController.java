package pe.captabot.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Permite comprobar que la API está en línea (no pide clave). */
@RestController
public class EstadoController {

    @GetMapping("/api/health")
    public Map<String, String> estado() {
        return Map.of("estado", "ok", "servicio", "captabot-api");
    }
}
