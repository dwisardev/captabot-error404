package pe.captabot.api.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Preguntas frecuentes con Azure AI Language, función "custom question answering" (plan gratuito F0).
 * Es opcional: si no se configuran las tres variables AZURE_LANGUAGE_*, el chat usa PreguntasLocales.
 */
@Component
public class PreguntasAzure implements MotorPreguntas {

    static final String VERSION_API = "2021-10-01";
    static final double CONFIANZA_MINIMA = 0.5;

    private static final Logger log = LoggerFactory.getLogger(PreguntasAzure.class);

    private final String endpoint;
    private final String clave;
    private final String proyecto;
    private final RestClient http;

    @Autowired
    public PreguntasAzure(@Value("${captabot.azure.language.endpoint:}") String endpoint,
                          @Value("${captabot.azure.language.clave:}") String clave,
                          @Value("${captabot.azure.language.proyecto:}") String proyecto) {
        this(endpoint, clave, proyecto, RestClient.builder().requestFactory(conTiempoLimite()));
    }

    PreguntasAzure(String endpoint, String clave, String proyecto, RestClient.Builder builder) {
        this.endpoint = endpoint == null ? "" : endpoint.trim().replaceAll("/+$", "");
        this.clave = clave == null ? "" : clave.trim();
        this.proyecto = proyecto == null ? "" : proyecto.trim();
        this.http = builder.build();
    }

    public boolean estaConfigurado() {
        return !endpoint.isEmpty() && !clave.isEmpty() && !proyecto.isEmpty();
    }

    @Override
    public Optional<String> responder(String pregunta) {
        if (!estaConfigurado() || pregunta == null || pregunta.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<?, ?> respuesta = http.post()
                    .uri(direccion())
                    .header("Ocp-Apim-Subscription-Key", clave)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("question", pregunta, "top", 1, "confidenceScoreThreshold", CONFIANZA_MINIMA))
                    .retrieve()
                    .body(Map.class);
            return mejorRespuesta(respuesta);
        } catch (RestClientException ex) {
            log.warn("Azure AI Language no respondió; se usan las preguntas locales: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    URI direccion() {
        String nombre = URLEncoder.encode(proyecto, StandardCharsets.UTF_8);
        return URI.create(endpoint + "/language/:query-knowledgebases?projectName=" + nombre
                + "&api-version=" + VERSION_API + "&deploymentName=production");
    }

    static Optional<String> mejorRespuesta(Map<?, ?> respuesta) {
        if (respuesta == null || !(respuesta.get("answers") instanceof List<?> respuestas) || respuestas.isEmpty()
                || !(respuestas.get(0) instanceof Map<?, ?> primera)) {
            return Optional.empty();
        }
        Object texto = primera.get("answer");
        double confianza = primera.get("confidenceScore") instanceof Number n ? n.doubleValue() : 0;
        if (!(texto instanceof String t) || t.isBlank() || confianza < CONFIANZA_MINIMA) {
            return Optional.empty();
        }
        return Optional.of(t);
    }

    private static SimpleClientHttpRequestFactory conTiempoLimite() {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(3000);
        fabrica.setReadTimeout(5000);
        return fabrica;
    }
}
