package pe.captabot.api.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.captabot.api.telegram.AvisosService;
import pe.captabot.api.telegram.TelegramApi;
import pe.captabot.api.whatsapp.WhatsAppApi;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Página «Automatización» del panel (HU13): estado de los canales, tiempos, vendedores y envíos del día. */
@RestController
@RequestMapping("/api/automatizacion")
public class AutomatizacionController {

    private final AvisosService avisos;
    private final TelegramApi telegram;
    private final WhatsAppApi whatsapp;

    public AutomatizacionController(AvisosService avisos, TelegramApi telegram, WhatsAppApi whatsapp) {
        this.avisos = avisos;
        this.telegram = telegram;
        this.whatsapp = whatsapp;
    }

    @GetMapping
    public Map<String, Object> estado() {
        List<Map<String, Object>> vendedores = avisos.vendedores().stream()
                .map(v -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("chatId", v.getChatId());
                    fila.put("nombre", v.getNombre());
                    fila.put("vinculadoEn", v.getCreadoEn());
                    return fila;
                })
                .toList();
        Map<String, Object> estado = new LinkedHashMap<>();
        estado.put("telegram", telegram.estaConfigurado() ? "activo" : "apagado");
        estado.put("whatsapp", whatsapp.estaConfigurado() ? "activo" : "apagado");
        estado.put("whatsappWebhook", whatsapp.webhookActivo() ? "activo" : "apagado");
        estado.put("avisosVendedores", avisos.avisosActivos() ? "activo" : "apagado");
        estado.put("codigoConfigurado", avisos.codigoConfigurado());
        estado.put("recordatorioMinutos", avisos.minutosRecordatorio());
        estado.put("seguimientoClienteMinutos", avisos.minutosSeguimiento());
        estado.put("seguimientoWhatsappDisponible", avisos.seguimientoWhatsappDisponible());
        estado.put("vendedores", vendedores);
        estado.put("enviadosHoy", avisos.enviadosHoy());
        return estado;
    }

    /** Quita a un vendedor de los avisos (por ejemplo, si dejó el equipo). */
    @DeleteMapping("/vendedores/{chatId}")
    public ResponseEntity<Void> quitarVendedor(@PathVariable long chatId) {
        return avisos.quitarVendedor(chatId) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
