package pe.captabot.api.whatsapp;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * Webhook de WhatsApp (Cloud API de Meta). No pide la clave de la API: Meta se identifica con el token de
 * verificación (GET) y con la firma X-Hub-Signature-256 de cada notificación (POST), si hay secreto de la app.
 */
@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final WhatsAppApi api;
    private final WhatsAppBot bot;

    public WhatsAppWebhookController(WhatsAppApi api, WhatsAppBot bot) {
        this.api = api;
        this.bot = bot;
    }

    /** Meta llama una vez al configurar el webhook: hay que devolver el reto si el token coincide. */
    @GetMapping
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
                                            @RequestParam(name = "hub.verify_token", required = false) String token,
                                            @RequestParam(name = "hub.challenge", required = false) String reto) {
        boolean valido = api.webhookActivo() && "subscribe".equals(modo) && reto != null && token != null
                && MessageDigest.isEqual(api.verificacion().getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
        if (!valido) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(reto);
    }

    /** Mensajes de los clientes. Se responde 200 enseguida; el bot los atiende en orden en otro hilo. */
    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Void> recibir(@RequestBody byte[] cuerpo,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String firma) {
        if (!api.webhookActivo()) {
            return ResponseEntity.notFound().build();
        }
        byte[] secreto = api.secretoApp();
        if (secreto.length > 0 && !firmaValida(secreto, cuerpo, firma)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Map<String, Object> notificacion;
        try {
            notificacion = JSON.readValue(cuerpo, Map.class);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().build();
        }
        bot.recibir(notificacion);
        return ResponseEntity.ok().build();
    }

    /** Firma de Meta: «sha256=» + HMAC-SHA256 del cuerpo con el secreto de la app, en hexadecimal. */
    static boolean firmaValida(byte[] secreto, byte[] cuerpo, String firma) {
        if (firma == null || !firma.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            String esperada = "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo));
            return MessageDigest.isEqual(esperada.getBytes(StandardCharsets.US_ASCII),
                    firma.toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.US_ASCII));
        } catch (GeneralSecurityException ex) {
            return false;
        }
    }
}
