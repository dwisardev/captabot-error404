package pe.captabot.api.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.servicio.ProspectoService;
import pe.captabot.api.web.ProspectoRequest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asistente virtual de CaptaBot (HU01, HU02, HU04 y HU05). Sigue el flujo de docs/diagramas/flujo-asistente.png:
 * responde preguntas frecuentes (con Azure AI Language si está configurado), pide consentimiento, capta los datos
 * del cliente y registra al prospecto, que la API califica como caliente, tibio o frío.
 */
@Service
public class ConversacionService {

    public static final String CANAL_WEB = "CHAT_WEB";
    public static final String CANAL_TELEGRAM = "TELEGRAM";

    static final Duration EXPIRACION = Duration.ofMinutes(30);
    static final int MAXIMO_SESIONES = 5000;

    public static final List<String> MENU = List.of("¿Qué servicios ofrecen?", "¿Cuánto cuesta?", "¿Cuál es su horario?",
            "Quiero cotizar");
    static final List<String> SI_NO = List.of("Sí", "No");
    static final List<String> SERVICIOS = List.of("Diseño web", "Marketing digital", "Soporte técnico", "Otro");
    static final List<String> URGENCIAS = List.of("Alta: esta semana", "Media: este mes", "Baja: solo estoy averiguando");
    static final List<String> CONFIRMAR = List.of("Sí, registrar", "Corregir datos");

    static final String SALUDO = "¡Hola! Soy CaptaBot, el asistente virtual. Puedo resolver tus dudas sobre horarios, "
            + "ubicación, servicios, precios, formas de pago y tiempos de entrega, o ayudarte a pedir una cotización.";
    static final String NO_ENTIENDO = "No estoy seguro de haber entendido. Puedo ayudarte con horarios, ubicación, "
            + "servicios, precios, formas de pago y tiempos de entrega. También puedo pedir que un asesor te contacte.";

    private static final Set<String> CONTACTO = Set.of("cotiz", "contrat", "asesor", "llam", "contact", "vendedor",
            "persona", "humano", "comprar", "presupuest");
    private static final Set<String> SALUDOS = Set.of("hola", "buenas", "buenos", "buen", "hey", "alo");
    private static final Set<String> REINICIO = Set.of("menu", "inicio", "cancelar", "reiniciar", "salir");

    private static final Logger log = LoggerFactory.getLogger(ConversacionService.class);

    private final Map<String, Conversacion> sesiones = new ConcurrentHashMap<>();
    private final ProspectoService prospectos;
    private final PreguntasAzure azure;
    private final PreguntasLocales locales;

    public ConversacionService(ProspectoService prospectos, PreguntasAzure azure, PreguntasLocales locales) {
        this.prospectos = prospectos;
        this.azure = azure;
        this.locales = locales;
    }

    /** Mensaje del chat de la página web. */
    public RespuestaChat procesar(String sesionId, String texto) {
        return procesar(sesionId, texto, CANAL_WEB);
    }

    /** Mensaje de cualquier canal (página web o Telegram). */
    public RespuestaChat procesar(String sesionId, String texto, String canal) {
        return procesar(sesionId, texto, canal, null);
    }

    /** Mensaje del bot de Telegram: guarda el chat del cliente para el seguimiento automático (HU13). */
    public RespuestaChat procesar(String sesionId, String texto, String canal, Long telegramChatId) {
        limpiarVencidas();
        String entrada = texto == null ? "" : texto.trim().replaceAll("\\s+", " ");
        Conversacion c = sesionId == null ? null : sesiones.get(sesionId);
        if (c == null) {
            c = nueva(canal);
            c.telegramChatId = telegramChatId;
            if (entrada.isEmpty() || esSaludo(entrada)) {
                return responder(c, List.of(SALUDO), MENU);
            }
        }
        c.ultimaActividad = Instant.now();
        if (entrada.isEmpty()) {
            return responder(c, List.of("Escribe tu mensaje, por favor."), opciones(c));
        }
        if (REINICIO.contains(Textos.normalizar(entrada))) {
            c.estado = EstadoConversacion.MENU;
            c.reiniciarDatos();
            return responder(c, List.of("Listo, empecemos de nuevo. ¿En qué te ayudo?"), MENU);
        }
        return switch (c.estado) {
            case MENU -> enMenu(c, entrada);
            case PREGUNTA_ASESOR -> enPreguntaAsesor(c, entrada);
            case CONSENTIMIENTO -> enConsentimiento(c, entrada);
            case NOMBRE -> enNombre(c, entrada);
            case CELULAR -> enCelular(c, entrada);
            case SERVICIO -> enServicio(c, entrada);
            case PRESUPUESTO -> enPresupuesto(c, entrada);
            case URGENCIA -> enUrgencia(c, entrada);
            case CONFIRMACION -> enConfirmacion(c, entrada);
        };
    }

    /** Usa Azure AI Language si está configurado; si no responde, el buscador local. */
    Optional<String> buscarRespuesta(String pregunta) {
        Optional<String> deAzure = azure.responder(pregunta);
        return deAzure.isPresent() ? deAzure : locales.responder(pregunta);
    }

    int sesionesActivas() {
        return sesiones.size();
    }

    // ------------------------------------------------------------------ pasos del flujo

    private RespuestaChat enMenu(Conversacion c, String entrada) {
        if (Textos.contiene(entrada, CONTACTO)) {
            return pedirConsentimiento(c);
        }
        if (esSaludo(entrada) && Textos.palabras(entrada).size() <= 3) {
            return responder(c, List.of(SALUDO), MENU);
        }
        Optional<String> respuesta = buscarRespuesta(entrada);
        if (respuesta.isPresent()) {
            c.estado = EstadoConversacion.PREGUNTA_ASESOR;
            return responder(c, List.of(respuesta.get(), "¿Quieres que un asesor te contacte?"), SI_NO);
        }
        return responder(c, List.of(NO_ENTIENDO), MENU);
    }

    private RespuestaChat enPreguntaAsesor(Conversacion c, String entrada) {
        Boolean quiere = Textos.siONo(entrada);
        if (Boolean.TRUE.equals(quiere)) {
            return pedirConsentimiento(c);
        }
        if (Boolean.FALSE.equals(quiere)) {
            c.estado = EstadoConversacion.MENU;
            return responder(c, List.of("Perfecto. Si tienes otra pregunta, escríbela aquí."), MENU);
        }
        c.estado = EstadoConversacion.MENU;
        return enMenu(c, entrada);
    }

    private RespuestaChat pedirConsentimiento(Conversacion c) {
        c.estado = EstadoConversacion.CONSENTIMIENTO;
        return responder(c, List.of("Para que un asesor te contacte guardaremos tu nombre y tu número de celular "
                + "solo con ese fin. ¿Estás de acuerdo?"), List.of("Sí, acepto", "No"));
    }

    private RespuestaChat enConsentimiento(Conversacion c, String entrada) {
        Boolean acepta = Textos.siONo(entrada);
        if (Boolean.TRUE.equals(acepta)) {
            c.estado = EstadoConversacion.NOMBRE;
            return responder(c, List.of("Gracias. ¿Cuál es tu nombre?"), List.of());
        }
        if (Boolean.FALSE.equals(acepta)) {
            c.estado = EstadoConversacion.MENU;
            c.reiniciarDatos();
            return responder(c, List.of("Entendido, no guardaremos tus datos. ¿Puedo ayudarte con algo más?"), MENU);
        }
        return responder(c, List.of("Responde «Sí» o «No», por favor."), List.of("Sí, acepto", "No"));
    }

    private RespuestaChat enNombre(Conversacion c, String entrada) {
        String nombre = entrada.replaceFirst("(?i)^(me llamo|mi nombre es|soy)\\s+", "").trim();
        if (nombre.length() > 120 || !nombre.matches(".*\\p{L}.*")) {
            return responder(c, List.of("Escribe solo tu nombre, por favor."), List.of());
        }
        c.nombre = nombre;
        c.estado = EstadoConversacion.CELULAR;
        return responder(c, List.of("Gracias, " + Textos.primerNombre(nombre)
                + ". ¿A qué número de celular te podemos llamar? (9 dígitos)"), List.of());
    }

    private RespuestaChat enCelular(Conversacion c, String entrada) {
        String digitos = entrada.replaceAll("\\D", "");
        if (digitos.length() == 11 && digitos.startsWith("51")) {
            digitos = digitos.substring(2);
        }
        if (!digitos.matches("\\d{9}")) {
            return responder(c, List.of("El número debe tener 9 dígitos, por ejemplo 987654321. ¿Cuál es tu celular?"),
                    List.of());
        }
        c.telefono = digitos;
        c.estado = EstadoConversacion.SERVICIO;
        return responder(c, List.of("¿Qué servicio te interesa?"), SERVICIOS);
    }

    private RespuestaChat enServicio(Conversacion c, String entrada) {
        if (entrada.length() > 120) {
            return responder(c, List.of("Descríbelo en pocas palabras, por favor."), SERVICIOS);
        }
        c.servicio = entrada;
        c.estado = EstadoConversacion.PRESUPUESTO;
        return responder(c, List.of("¿Cuál es tu presupuesto aproximado en soles? Si aún no lo sabes, escribe 0."),
                List.of());
    }

    private RespuestaChat enPresupuesto(Conversacion c, String entrada) {
        Optional<BigDecimal> monto = Montos.leer(entrada);
        if (monto.isEmpty()) {
            return responder(c, List.of("No entendí el monto. Escríbelo en números, por ejemplo 2500."), List.of());
        }
        c.presupuesto = monto.get();
        c.estado = EstadoConversacion.URGENCIA;
        return responder(c, List.of("¿Qué tan pronto lo necesitas?"), URGENCIAS);
    }

    private RespuestaChat enUrgencia(Conversacion c, String entrada) {
        Urgencia urgencia = leerUrgencia(entrada);
        if (urgencia == null) {
            return responder(c, List.of("Elige una opción: Alta, Media o Baja."), URGENCIAS);
        }
        c.urgencia = urgencia;
        c.estado = EstadoConversacion.CONFIRMACION;
        return responder(c, List.of(resumen(c)), CONFIRMAR);
    }

    /** HU04: antes de registrar, el cliente revisa sus datos y confirma o los corrige. */
    private RespuestaChat enConfirmacion(Conversacion c, String entrada) {
        if (Textos.normalizar(entrada).contains("correg")) {
            c.reiniciarDatos();
            c.estado = EstadoConversacion.NOMBRE;
            return responder(c, List.of("De acuerdo, corrijamos tus datos. ¿Cuál es tu nombre?"), List.of());
        }
        Boolean confirma = Textos.siONo(entrada);
        if (confirma == null) {
            return responder(c, List.of("Responde «Sí, registrar» o «Corregir datos», por favor."), CONFIRMAR);
        }
        if (!confirma) {
            c.estado = EstadoConversacion.MENU;
            c.reiniciarDatos();
            return responder(c, List.of("Entendido, no registré tus datos. ¿Puedo ayudarte con algo más?"), MENU);
        }
        try {
            prospectos.registrar(new ProspectoRequest(c.nombre, c.telefono, c.servicio, c.presupuesto,
                    c.urgencia.name()), c.canal, c.telegramChatId);
        } catch (RuntimeException ex) {
            log.error("No se pudo registrar el prospecto del chat", ex);
            return responder(c, List.of("No pudimos registrar tus datos en este momento. Inténtalo de nuevo en unos "
                    + "minutos."), CONFIRMAR);
        }
        String nombre = Textos.primerNombre(c.nombre);
        c.estado = EstadoConversacion.MENU;
        c.reiniciarDatos();
        return new RespuestaChat(c.id, List.of("Gracias, " + nombre
                + ". Registramos tus datos y un asesor te contactará pronto.", "¿Puedo ayudarte con algo más?"),
                MENU, true, false);
    }

    static String resumen(Conversacion c) {
        String urgencia = switch (c.urgencia) {
            case ALTA -> "Alta (esta semana)";
            case MEDIA -> "Media (este mes)";
            case BAJA -> "Baja (solo averiguando)";
        };
        return "Revisa tus datos:\n"
                + "• Nombre: " + c.nombre + "\n"
                + "• Celular: " + c.telefono + "\n"
                + "• Servicio: " + c.servicio + "\n"
                + "• Presupuesto: " + String.format(Locale.US, "S/ %,.2f", c.presupuesto) + "\n"
                + "• Urgencia: " + urgencia + "\n"
                + "¿Los registro?";
    }

    static Urgencia leerUrgencia(String entrada) {
        String t = Textos.normalizar(entrada);
        if (t.startsWith("alta") || t.contains("semana") || t.contains("urgent") || t.contains("hoy")) {
            return Urgencia.ALTA;
        }
        if (t.startsWith("media") || t.contains("este mes") || t.contains("mes")) {
            return Urgencia.MEDIA;
        }
        if (t.startsWith("baja") || t.contains("averigu") || t.contains("sin apuro") || t.contains("no hay apuro")) {
            return Urgencia.BAJA;
        }
        return null;
    }

    // ------------------------------------------------------------------ apoyo

    private static boolean esSaludo(String entrada) {
        List<String> palabras = Textos.palabras(entrada);
        return !palabras.isEmpty() && SALUDOS.contains(palabras.get(0));
    }

    private static List<String> opciones(Conversacion c) {
        return switch (c.estado) {
            case MENU -> MENU;
            case PREGUNTA_ASESOR -> SI_NO;
            case CONSENTIMIENTO -> List.of("Sí, acepto", "No");
            case SERVICIO -> SERVICIOS;
            case URGENCIA -> URGENCIAS;
            case CONFIRMACION -> CONFIRMAR;
            default -> List.of();
        };
    }

    private static RespuestaChat responder(Conversacion c, List<String> mensajes, List<String> opciones) {
        return new RespuestaChat(c.id, mensajes, opciones, false, c.estado == EstadoConversacion.CELULAR);
    }

    private Conversacion nueva(String canal) {
        if (sesiones.size() >= MAXIMO_SESIONES) {
            limpiarVencidas();
            if (sesiones.size() >= MAXIMO_SESIONES) {
                // Libera la más antigua para no crecer sin límite
                sesiones.values().stream()
                        .min((a, b) -> a.ultimaActividad.compareTo(b.ultimaActividad))
                        .ifPresent(vieja -> sesiones.remove(vieja.id));
            }
        }
        Conversacion c = new Conversacion(UUID.randomUUID().toString(), canal == null ? CANAL_WEB : canal);
        sesiones.put(c.id, c);
        return c;
    }

    private void limpiarVencidas() {
        Instant limite = Instant.now().minus(EXPIRACION);
        List<String> vencidas = new ArrayList<>();
        sesiones.forEach((id, c) -> {
            if (c.ultimaActividad.isBefore(limite)) {
                vencidas.add(id);
            }
        });
        vencidas.forEach(sesiones::remove);
    }
}
