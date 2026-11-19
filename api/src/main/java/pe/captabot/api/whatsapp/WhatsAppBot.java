package pe.captabot.api.whatsapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.chat.RespuestaChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Canal de WhatsApp del asistente: cada número sigue la misma conversación que la página web y Telegram.
 * Las opciones se muestran como botones o como lista y, al pedir el celular, se ofrece «Usar este número».
 * Los mensajes llegan por el webhook y se atienden en orden, en un hilo aparte, para responder rápido a Meta.
 */
@Service
public class WhatsAppBot implements DisposableBean {

    static final String USAR_NUMERO = "📱 Usar este número";
    static final String SOLO_TEXTO = "Por ahora solo entiendo mensajes de texto.";
    private static final int MAXIMO_RECORDADOS = 1000;

    private static final Logger log = LoggerFactory.getLogger(WhatsAppBot.class);

    /** Mensaje recibido de WhatsApp. idOpcion es el id del botón o de la fila elegida, si eligió una. */
    record MensajeEntrante(String id, String de, String texto, String idOpcion) {
        boolean entendible() {
            return texto != null || idOpcion != null;
        }
    }

    private final WhatsAppApi api;
    private final ConversacionService conversaciones;
    private final Map<String, String> sesiones = new ConcurrentHashMap<>();
    private final Map<String, List<String>> ultimasOpciones = new ConcurrentHashMap<>();
    private final Set<String> recibidos = Collections.synchronizedSet(Collections.newSetFromMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> mayor) {
                    return size() > MAXIMO_RECORDADOS;
                }
            }));
    private final Executor atencion;
    private final ExecutorService hiloPropio;

    @Autowired
    public WhatsAppBot(WhatsAppApi api, ConversacionService conversaciones) {
        this(api, conversaciones, null);
    }

    /** Para las pruebas: atención en el mismo hilo. */
    WhatsAppBot(WhatsAppApi api, ConversacionService conversaciones, Executor atencion) {
        this.api = api;
        this.conversaciones = conversaciones;
        if (atencion == null) {
            hiloPropio = Executors.newSingleThreadExecutor(tarea -> {
                Thread hilo = new Thread(tarea, "whatsapp-bot");
                hilo.setDaemon(true);
                return hilo;
            });
            this.atencion = hiloPropio;
        } else {
            hiloPropio = null;
            this.atencion = atencion;
        }
    }

    @Override
    public void destroy() {
        if (hiloPropio != null) {
            hiloPropio.shutdown();
        }
    }

    /** Recibe la notificación del webhook y deja en cola cada mensaje nuevo (Meta puede reenviar los mismos). */
    public void recibir(Map<String, Object> notificacion) {
        for (MensajeEntrante mensaje : mensajes(notificacion)) {
            if (mensaje.id() != null && !recibidos.add(mensaje.id())) {
                continue;
            }
            atencion.execute(() -> {
                try {
                    atender(mensaje);
                } catch (RuntimeException ex) {
                    log.warn("No se pudo atender el mensaje de WhatsApp: {}", api.ocultar(ex.getMessage()));
                }
            });
        }
    }

    void atender(MensajeEntrante mensaje) {
        String de = mensaje.de();
        if (!mensaje.entendible()) {
            api.enviar(de, SOLO_TEXTO, List.of());
            return;
        }
        String texto = mensaje.idOpcion() != null ? opcionElegida(de, mensaje) : mensaje.texto().trim();
        if (USAR_NUMERO.equals(texto)) {
            texto = numeroLocal(de);
        }
        RespuestaChat respuesta = conversaciones.procesar(sesiones.get(de), texto,
                ConversacionService.CANAL_WHATSAPP, de);
        sesiones.put(de, respuesta.sesionId());

        List<String> opciones = respuesta.pideTelefono() ? List.of(USAR_NUMERO) : respuesta.opciones();
        ultimasOpciones.put(de, opciones);
        List<String> textos = respuesta.mensajes();
        for (int i = 0; i < textos.size(); i++) {
            boolean ultimo = i == textos.size() - 1;
            api.enviar(de, textos.get(i), ultimo ? opciones : List.of());
        }
    }

    /** Convierte «op:N» en el texto de la opción que se le mostró; si no se encuentra, usa el título recibido. */
    private String opcionElegida(String de, MensajeEntrante mensaje) {
        List<String> opciones = ultimasOpciones.getOrDefault(de, List.of());
        String id = mensaje.idOpcion();
        if (id.startsWith("op:")) {
            try {
                int posicion = Integer.parseInt(id.substring(3));
                if (posicion >= 0 && posicion < opciones.size()) {
                    return opciones.get(posicion);
                }
            } catch (NumberFormatException ignorado) {
                // se usa el título
            }
        }
        return mensaje.texto() == null ? "" : mensaje.texto().trim();
    }

    /** 51987654321 → 987654321 (celular peruano); otros números se dejan igual. */
    static String numeroLocal(String waId) {
        if (waId != null && waId.matches("51\\d{9}")) {
            return waId.substring(2);
        }
        return waId == null ? "" : waId;
    }

    /** Extrae los mensajes de la notificación de Meta (entry → changes → value → messages). */
    static List<MensajeEntrante> mensajes(Map<String, Object> notificacion) {
        List<MensajeEntrante> resultado = new ArrayList<>();
        for (Object entrada : lista(notificacion == null ? null : notificacion.get("entry"))) {
            if (!(entrada instanceof Map<?, ?> e)) {
                continue;
            }
            for (Object cambio : lista(e.get("changes"))) {
                if (!(cambio instanceof Map<?, ?> c) || !(c.get("value") instanceof Map<?, ?> valor)) {
                    continue;
                }
                for (Object m : lista(valor.get("messages"))) {
                    if (m instanceof Map<?, ?> mensaje && mensaje.get("from") instanceof String de) {
                        resultado.add(leer(mensaje, de));
                    }
                }
            }
        }
        return resultado;
    }

    private static MensajeEntrante leer(Map<?, ?> mensaje, String de) {
        String id = mensaje.get("id") instanceof String i ? i : null;
        Object tipo = mensaje.get("type");
        if ("text".equals(tipo) && mensaje.get("text") instanceof Map<?, ?> t && t.get("body") instanceof String cuerpo) {
            return new MensajeEntrante(id, de, cuerpo, null);
        }
        if ("interactive".equals(tipo) && mensaje.get("interactive") instanceof Map<?, ?> interactivo) {
            Object elegido = interactivo.get("button_reply") != null
                    ? interactivo.get("button_reply") : interactivo.get("list_reply");
            if (elegido instanceof Map<?, ?> opcion) {
                return new MensajeEntrante(id, de, texto(opcion.get("title")), texto(opcion.get("id")));
            }
        }
        if ("button".equals(tipo) && mensaje.get("button") instanceof Map<?, ?> boton) {
            return new MensajeEntrante(id, de, texto(boton.get("text")), null);
        }
        return new MensajeEntrante(id, de, null, null);
    }

    private static List<?> lista(Object valor) {
        return valor instanceof List<?> l ? l : List.of();
    }

    private static String texto(Object valor) {
        return valor instanceof String s ? s : null;
    }
}
