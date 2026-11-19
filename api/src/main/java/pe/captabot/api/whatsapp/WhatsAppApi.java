package pe.captabot.api.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API oficial de WhatsApp (Cloud API de Meta). Con el número de prueba de Meta es gratis y puede
 * escribir hasta a 5 celulares registrados. Ver asistente/README.md, sección 8.
 */
@Component
public class WhatsAppApi {

    static final String BASE = "https://graph.facebook.com";
    static final String VERSION_POR_DEFECTO = "v26.0";
    static final int MAX_BOTONES = 3;
    static final int MAX_TITULO_BOTON = 20;
    static final int MAX_FILAS = 10;
    static final int MAX_TITULO_FILA = 24;
    static final int MAX_DESCRIPCION_FILA = 72;
    static final int MAX_CUERPO_INTERACTIVO = 1024;
    static final int MAX_TEXTO = 4096;

    private static final Logger log = LoggerFactory.getLogger(WhatsAppApi.class);

    private final String token;
    private final String numeroId;
    private final String version;
    private final String verificacion;
    private final byte[] secretoApp;
    private final RestClient http;

    @Autowired
    public WhatsAppApi(@Value("${captabot.whatsapp.token:}") String token,
                       @Value("${captabot.whatsapp.numero-id:}") String numeroId,
                       @Value("${captabot.whatsapp.version:" + VERSION_POR_DEFECTO + "}") String version,
                       @Value("${captabot.whatsapp.verificacion:}") String verificacion,
                       @Value("${captabot.whatsapp.secreto-app:}") String secretoApp) {
        this(token, numeroId, version, verificacion, secretoApp, RestClient.builder().requestFactory(conTiempoLimite()));
    }

    WhatsAppApi(String token, String numeroId, String version, String verificacion, String secretoApp,
                RestClient.Builder builder) {
        this.token = limpio(token);
        this.numeroId = limpio(numeroId);
        this.version = limpio(version).isEmpty() ? VERSION_POR_DEFECTO : limpio(version);
        this.verificacion = limpio(verificacion);
        this.secretoApp = limpio(secretoApp).getBytes(StandardCharsets.UTF_8);
        this.http = builder.build();
    }

    /** Puede enviar mensajes: tiene el token y el identificador del número. */
    public boolean estaConfigurado() {
        return !token.isEmpty() && !numeroId.isEmpty();
    }

    /** Puede recibir mensajes: tiene el token de verificación del webhook. */
    public boolean webhookActivo() {
        return !verificacion.isEmpty();
    }

    String verificacion() {
        return verificacion;
    }

    byte[] secretoApp() {
        return secretoApp;
    }

    /**
     * Envía un mensaje. Si hay opciones, las muestra como botones (hasta 3 cortas) o como lista (hasta 10);
     * el id de cada opción es «op:N», su posición.
     */
    public void enviar(String para, String texto, List<String> opciones) {
        if (!estaConfigurado()) {
            log.debug("WhatsApp sin token: no se envía el mensaje a {}", para);
            return;
        }
        Map<String, Object> cuerpo = cuerpo(para, texto, opciones);
        http.post()
                .uri(URI.create(BASE + "/" + version + "/" + numeroId + "/messages"))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(cuerpo)
                .retrieve()
                .toBodilessEntity();
    }

    /** Arma el JSON del mensaje según la cantidad y el largo de las opciones. */
    static Map<String, Object> cuerpo(String para, String texto, List<String> opciones) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("messaging_product", "whatsapp");
        cuerpo.put("recipient_type", "individual");
        cuerpo.put("to", para);
        List<String> lista = opciones == null ? List.of() : opciones;

        if (lista.isEmpty() || lista.size() > MAX_FILAS) {
            StringBuilder todo = new StringBuilder(texto);
            for (int i = 0; i < lista.size(); i++) {
                todo.append(i == 0 ? "\n\n" : "\n").append(i + 1).append(". ").append(lista.get(i));
            }
            cuerpo.put("type", "text");
            cuerpo.put("text", Map.of("body", recortar(todo.toString(), MAX_TEXTO), "preview_url", false));
            return cuerpo;
        }

        Map<String, Object> interactivo = new LinkedHashMap<>();
        boolean botones = lista.size() <= MAX_BOTONES && lista.stream().allMatch(o -> o.length() <= MAX_TITULO_BOTON);
        interactivo.put("type", botones ? "button" : "list");
        interactivo.put("body", Map.of("text", recortar(texto, MAX_CUERPO_INTERACTIVO)));
        if (botones) {
            List<Map<String, Object>> lista2 = new ArrayList<>();
            for (int i = 0; i < lista.size(); i++) {
                lista2.add(Map.of("type", "reply", "reply", Map.of("id", "op:" + i, "title", lista.get(i))));
            }
            interactivo.put("action", Map.of("buttons", lista2));
        } else {
            List<Map<String, Object>> filas = new ArrayList<>();
            for (int i = 0; i < lista.size(); i++) {
                String opcion = lista.get(i);
                Map<String, Object> fila = new LinkedHashMap<>();
                fila.put("id", "op:" + i);
                fila.put("title", recortar(opcion, MAX_TITULO_FILA));
                if (opcion.length() > MAX_TITULO_FILA) {
                    fila.put("description", recortar(opcion, MAX_DESCRIPCION_FILA));
                }
                filas.add(fila);
            }
            interactivo.put("action", Map.of("button", "Ver opciones",
                    "sections", List.of(Map.of("title", "Opciones", "rows", filas))));
        }
        cuerpo.put("type", "interactive");
        cuerpo.put("interactive", interactivo);
        return cuerpo;
    }

    /** Quita el token de un texto antes de escribirlo en el log. */
    String ocultar(String texto) {
        if (texto == null) {
            return "";
        }
        return token.isEmpty() ? texto : texto.replace(token, "***");
    }

    static String recortar(String texto, int maximo) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1) + "…";
    }

    private static String limpio(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static SimpleClientHttpRequestFactory conTiempoLimite() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(5000);
        fabrica.setReadTimeout(15000);
        return fabrica;
    }
}
