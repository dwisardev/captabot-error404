package pe.captabot.api.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Llamadas a la Bot API de Telegram (https://core.telegram.org/bots/api).
 * El token del bot va en la variable TELEGRAM_BOT_TOKEN; sin ella, el canal de Telegram queda apagado.
 */
@Component
public class TelegramApi {

    static final String BASE = "https://api.telegram.org";

    private final String token;
    private final RestClient http;

    @Autowired
    public TelegramApi(@Value("${captabot.telegram.token:}") String token) {
        this(token, RestClient.builder().requestFactory(conTiempoLimite()));
    }

    TelegramApi(String token, RestClient.Builder builder) {
        this.token = token == null ? "" : token.trim();
        this.http = builder.build();
    }

    public boolean estaConfigurado() {
        return !token.isEmpty();
    }

    /** Espera hasta esperaSegundos por mensajes nuevos (long polling) y los devuelve. */
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> recibir(long desde, int esperaSegundos) {
        Map<?, ?> respuesta = http.get()
                .uri(URI.create(BASE + "/bot" + token + "/getUpdates?timeout=" + esperaSegundos + "&offset=" + desde
                        + "&allowed_updates=%5B%22message%22%5D"))
                .retrieve()
                .body(Map.class);
        Object resultado = respuesta == null ? null : respuesta.get("result");
        return resultado instanceof List<?> lista ? (List<Map<String, Object>>) lista : List.of();
    }

    void enviar(long chatId, String texto, Map<String, Object> teclado) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("chat_id", chatId);
        cuerpo.put("text", texto);
        if (teclado != null) {
            cuerpo.put("reply_markup", teclado);
        }
        http.post()
                .uri(URI.create(BASE + "/bot" + token + "/sendMessage"))
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    /** Un bot con webhook no recibe mensajes por long polling: se quita por si quedó uno configurado. */
    void borrarWebhook() {
        http.post().uri(URI.create(BASE + "/bot" + token + "/deleteWebhook")).retrieve().toBodilessEntity();
    }

    String nombreDelBot() {
        Map<?, ?> respuesta = http.get().uri(URI.create(BASE + "/bot" + token + "/getMe")).retrieve().body(Map.class);
        if (respuesta != null && respuesta.get("result") instanceof Map<?, ?> bot) {
            return String.valueOf(bot.get("username"));
        }
        return "desconocido";
    }

    /** Quita el token de un texto antes de escribirlo en el log. */
    String ocultar(String texto) {
        if (texto == null) {
            return "";
        }
        return token.isEmpty() ? texto : texto.replace(token, "***");
    }

    private static SimpleClientHttpRequestFactory conTiempoLimite() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5000);
        fabrica.setReadTimeout(40000);   // mayor que la espera del long polling
        return fabrica;
    }
}
