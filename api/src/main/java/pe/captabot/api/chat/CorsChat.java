package pe.captabot.api.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * Permite insertar el chat en otra página web (por ejemplo, la web real de la PYME).
 * Variable CAPTABOT_CHAT_ORIGENES, separada por comas. Vacía: solo la página que sirve la propia API.
 */
@Configuration
public class CorsChat implements WebMvcConfigurer {

    private final String[] origenes;

    public CorsChat(@Value("${captabot.chat.origenes:}") String origenes) {
        this.origenes = Arrays.stream(origenes.split(",")).map(String::trim).filter(o -> !o.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        if (origenes.length > 0) {
            registro.addMapping("/api/chat/**").allowedOrigins(origenes).allowedMethods("POST");
        }
    }
}
