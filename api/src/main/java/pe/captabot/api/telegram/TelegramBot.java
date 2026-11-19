package pe.captabot.api.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.chat.RespuestaChat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Canal de Telegram del asistente (HU02): cada chat privado con el bot sigue la misma conversación que la
 * página web. Las opciones se muestran como botones y, al pedir el celular, se ofrece «Compartir mi número».
 * Usa long polling, así que funciona sin una dirección pública (también en una laptop).
 */
@Component
public class TelegramBot implements DisposableBean {

    static final int ESPERA_SEGUNDOS = 25;
    static final String BOTON_TELEFONO = "📱 Compartir mi número";
    static final String SOLO_TEXTO = "Por ahora solo entiendo mensajes de texto.";

    private static final Logger log = LoggerFactory.getLogger(TelegramBot.class);

    private final TelegramApi api;
    private final ConversacionService conversaciones;
    private final AvisosService avisos;
    private final Map<Long, String> sesiones = new ConcurrentHashMap<>();
    private volatile boolean activo;
    private Thread hilo;
    private long siguiente;

    public TelegramBot(TelegramApi api, ConversacionService conversaciones, AvisosService avisos) {
        this.api = api;
        this.conversaciones = conversaciones;
        this.avisos = avisos;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void iniciar() {
        if (!api.estaConfigurado()) {
            log.info("Canal de Telegram apagado: falta TELEGRAM_BOT_TOKEN");
            return;
        }
        activo = true;
        hilo = new Thread(this::escuchar, "telegram-bot");
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void destroy() {
        activo = false;
        if (hilo != null) {
            hilo.interrupt();
        }
    }

    private void escuchar() {
        try {
            api.borrarWebhook();
            log.info("Bot de Telegram @{} escuchando mensajes", api.nombreDelBot());
        } catch (RuntimeException ex) {
            log.warn("No se pudo conectar con Telegram: {}", api.ocultar(ex.getMessage()));
        }
        while (activo) {
            try {
                for (Map<String, Object> actualizacion : api.recibir(siguiente, ESPERA_SEGUNDOS)) {
                    siguiente = Math.max(siguiente, numero(actualizacion.get("update_id")) + 1);
                    atender(actualizacion);
                }
            } catch (RuntimeException ex) {
                if (activo) {
                    log.warn("Error al hablar con Telegram: {}", api.ocultar(ex.getMessage()));
                    esperar(5000);
                }
            }
        }
    }

    /** Responde un mensaje recibido de Telegram. Solo atiende chats privados. */
    void atender(Map<String, Object> actualizacion) {
        if (!(actualizacion.get("message") instanceof Map<?, ?> mensaje)
                || !(mensaje.get("chat") instanceof Map<?, ?> chat)
                || !"private".equals(chat.get("type"))) {
            return;
        }
        long chatId = numero(chat.get("id"));
        String texto = textoDe(mensaje);
        if (texto == null) {
            api.enviar(chatId, SOLO_TEXTO, null);
            return;
        }
        String comando = texto.split("\\s+", 2)[0].toLowerCase(java.util.Locale.ROOT).replaceFirst("@.*$", "");
        if (comando.equals("/vendedor")) {
            String codigo = texto.contains(" ") ? texto.substring(texto.indexOf(' ') + 1) : "";
            api.enviar(chatId, avisos.vincular(chatId, nombreDe(mensaje), codigo), Map.of("remove_keyboard", true));
            return;
        }
        if (comando.equals("/salir")) {
            api.enviar(chatId, avisos.desvincular(chatId), null);
            return;
        }
        String sesion = sesiones.get(chatId);
        if (comando.equals("/start")) {
            sesion = null;
            texto = "";
        }
        RespuestaChat respuesta = conversaciones.procesar(sesion, texto, ConversacionService.CANAL_TELEGRAM,
                String.valueOf(chatId));
        sesiones.put(chatId, respuesta.sesionId());
        List<String> mensajes = respuesta.mensajes();
        for (int i = 0; i < mensajes.size(); i++) {
            boolean ultimo = i == mensajes.size() - 1;
            api.enviar(chatId, mensajes.get(i), ultimo ? teclado(respuesta) : null);
        }
    }

    /** Nombre de pila de quien escribe, para saludar al vendedor que se vincula. */
    static String nombreDe(Map<?, ?> mensaje) {
        if (mensaje.get("from") instanceof Map<?, ?> de && de.get("first_name") instanceof String nombre) {
            return nombre;
        }
        return null;
    }

    /** El texto del mensaje, o el número si el cliente usó «Compartir mi número». */
    static String textoDe(Map<?, ?> mensaje) {
        if (mensaje.get("text") instanceof String texto) {
            return texto.trim();
        }
        if (mensaje.get("contact") instanceof Map<?, ?> contacto && contacto.get("phone_number") instanceof String tel) {
            return tel;
        }
        return null;
    }

    /** Botones de respuesta rápida de Telegram a partir de las opciones del chat. */
    static Map<String, Object> teclado(RespuestaChat respuesta) {
        if (respuesta.pideTelefono()) {
            Map<String, Object> boton = Map.of("text", BOTON_TELEFONO, "request_contact", true);
            return Map.<String, Object>of("keyboard", List.of(List.of(boton)),
                    "resize_keyboard", true, "one_time_keyboard", true);
        }
        if (respuesta.opciones().isEmpty()) {
            return Map.<String, Object>of("remove_keyboard", true);
        }
        List<List<Map<String, Object>>> filas = respuesta.opciones().stream()
                .map(opcion -> List.<Map<String, Object>>of(Map.<String, Object>of("text", opcion)))
                .toList();
        return Map.<String, Object>of("keyboard", filas, "resize_keyboard", true, "one_time_keyboard", true);
    }

    private static long numero(Object valor) {
        return valor instanceof Number n ? n.longValue() : 0L;
    }

    private static void esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
