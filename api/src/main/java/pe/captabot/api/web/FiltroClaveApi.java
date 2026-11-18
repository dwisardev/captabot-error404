package pe.captabot.api.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Exige la cabecera X-API-Key en /api/** cuando captabot.api-key tiene valor. Quedan libres /api/health y
 * /api/chat/**, que usa el navegador del cliente. El panel y las integraciones envían la clave.
 */
@Component
public class FiltroClaveApi extends OncePerRequestFilter {

    static final String CABECERA = "X-API-Key";

    private final byte[] claveEsperada;

    public FiltroClaveApi(@Value("${captabot.api-key:}") String clave) {
        this.claveEsperada = clave == null ? new byte[0] : clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI();
        return claveEsperada.length == 0
                || !ruta.startsWith("/api/")
                || ruta.equals("/api/health")
                || ruta.startsWith("/api/chat/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String recibida = request.getHeader(CABECERA);
        boolean valida = recibida != null
                && MessageDigest.isEqual(claveEsperada, recibida.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"Falta la clave de la API o no es válida\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
