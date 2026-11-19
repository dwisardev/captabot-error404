package pe.captabot.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.chat.PreguntasAzure;
import pe.captabot.api.telegram.AvisosService;
import pe.captabot.api.telegram.TelegramApi;

import java.util.LinkedHashMap;
import java.util.Map;

/** Permite comprobar que la API está en línea (no pide clave), qué motor usa el asistente y si Telegram está activo. */
@RestController
public class EstadoController {

    private final PreguntasAzure azure;
    private final TelegramApi telegram;
    private final AvisosService avisos;

    public EstadoController(PreguntasAzure azure, TelegramApi telegram, AvisosService avisos) {
        this.azure = azure;
        this.telegram = telegram;
        this.avisos = avisos;
    }

    @GetMapping("/api/health")
    public Map<String, Object> estado() {
        Map<String, Object> automatizacion = new LinkedHashMap<>();
        automatizacion.put("avisosVendedores", avisos.avisosActivos() ? "activo" : "apagado");
        automatizacion.put("vendedoresVinculados", contarVendedores());
        automatizacion.put("recordatorioMinutos", avisos.minutosRecordatorio());
        automatizacion.put("seguimientoClienteMinutos", avisos.minutosSeguimiento());

        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("estado", "ok");
        estado.put("servicio", "captabot-api");
        estado.put("preguntasFrecuentes", azure.estaConfigurado() ? "azure-ai-language" : "local");
        estado.put("telegram", telegram.estaConfigurado() ? "activo" : "apagado");
        estado.put("automatizacion", automatizacion);
        return estado;
    }

    /** El estado responde aunque la base de datos no esté disponible. */
    private Object contarVendedores() {
        try {
            return avisos.vendedoresVinculados();
        } catch (RuntimeException ex) {
            return "sin datos";
        }
    }
}
