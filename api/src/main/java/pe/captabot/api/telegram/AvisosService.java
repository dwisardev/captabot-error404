package pe.captabot.api.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.modelo.AvisoTelegram;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.AvisoTelegramRepository;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.servicio.ProspectoRegistrado;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Automatización de CaptaBot (HU13), por Telegram:
 * <ul>
 *   <li>Avisa al equipo de ventas apenas se registra un prospecto, con un botón para escribirle por WhatsApp.</li>
 *   <li>Recuerda al equipo los prospectos calientes o tibios que siguen en «Nuevo» después de un tiempo.</li>
 *   <li>Escribe al cliente que llegó por el bot de Telegram para darle seguimiento.</li>
 * </ul>
 * Los vendedores se vinculan escribiendo al bot «/vendedor CÓDIGO» (TELEGRAM_CODIGO_VENTAS).
 */
@Service
public class AvisosService implements DisposableBean {

    static final List<Calificacion> A_RECORDAR = List.of(Calificacion.CALIENTE, Calificacion.TIBIO);
    static final List<Etapa> CERRADAS = List.of(Etapa.CERRADO_GANADO, Etapa.CERRADO_PERDIDO);
    /** Solo se recuerdan y siguen prospectos recientes, para no avisar de registros antiguos. */
    static final Duration VENTANA = Duration.ofDays(2);

    private static final Logger log = LoggerFactory.getLogger(AvisosService.class);

    private final TelegramApi telegram;
    private final AvisoTelegramRepository vendedores;
    private final ProspectoRepository prospectos;
    private final String codigo;
    private final long minutosRecordatorio;
    private final long minutosSeguimiento;
    private final Clock reloj;
    private final Executor envios;
    private final ExecutorService hiloPropio;

    @Autowired
    public AvisosService(TelegramApi telegram, AvisoTelegramRepository vendedores, ProspectoRepository prospectos,
                         @Value("${captabot.avisos.codigo:}") String codigo,
                         @Value("${captabot.avisos.recordatorio-minutos:120}") String minutosRecordatorio,
                         @Value("${captabot.avisos.seguimiento-minutos:1440}") String minutosSeguimiento) {
        this(telegram, vendedores, prospectos, codigo, minutos(minutosRecordatorio, 120),
                minutos(minutosSeguimiento, 1440), Clock.systemDefaultZone(), null);
    }

    /** Lee los minutos del .env; si el valor está vacío o no es un número, usa el valor por defecto. */
    static long minutos(String valor, long porDefecto) {
        try {
            return valor == null || valor.isBlank() ? porDefecto : Long.parseLong(valor.trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    /** Para las pruebas: reloj fijo y envíos en el mismo hilo. */
    AvisosService(TelegramApi telegram, AvisoTelegramRepository vendedores, ProspectoRepository prospectos,
                  String codigo, long minutosRecordatorio, long minutosSeguimiento, Clock reloj, Executor envios) {
        this.telegram = telegram;
        this.vendedores = vendedores;
        this.prospectos = prospectos;
        this.codigo = codigo == null ? "" : codigo.trim();
        this.minutosRecordatorio = Math.max(1, minutosRecordatorio);
        this.minutosSeguimiento = Math.max(1, minutosSeguimiento);
        this.reloj = reloj;
        if (envios == null) {
            hiloPropio = Executors.newSingleThreadExecutor(tarea -> {
                Thread hilo = new Thread(tarea, "avisos-telegram");
                hilo.setDaemon(true);
                return hilo;
            });
            this.envios = hiloPropio;
        } else {
            hiloPropio = null;
            this.envios = envios;
        }
    }

    @Override
    public void destroy() {
        if (hiloPropio != null) {
            hiloPropio.shutdown();
        }
    }

    // ------------------------------------------------------------------ estado (para /api/health)

    public boolean avisosActivos() {
        return telegram.estaConfigurado() && !codigo.isEmpty();
    }

    public long vendedoresVinculados() {
        return vendedores.count();
    }

    public long minutosRecordatorio() {
        return minutosRecordatorio;
    }

    public long minutosSeguimiento() {
        return minutosSeguimiento;
    }

    // ------------------------------------------------------------------ vínculo de los vendedores

    /** Comando «/vendedor CÓDIGO» del bot: el chat empieza a recibir los avisos de ventas. */
    @Transactional
    public String vincular(long chatId, String nombre, String codigoIngresado) {
        if (codigo.isEmpty()) {
            return "Los avisos para vendedores están apagados: falta TELEGRAM_CODIGO_VENTAS en el archivo .env.";
        }
        String ingresado = codigoIngresado == null ? "" : codigoIngresado.trim();
        if (!MessageDigest.isEqual(codigo.getBytes(StandardCharsets.UTF_8), ingresado.getBytes(StandardCharsets.UTF_8))) {
            return "El código no es correcto. Escribe /vendedor seguido del código que te dio el administrador.";
        }
        String quien = nombre == null || nombre.isBlank() ? "vendedor" : nombre.trim();
        vendedores.save(new AvisoTelegram(chatId, quien.length() > 120 ? quien.substring(0, 120) : quien));
        return "Listo, " + quien + ". Desde ahora te aviso aquí de cada prospecto nuevo y de los calientes o tibios "
                + "que sigan en «Nuevo» después de " + duracion(minutosRecordatorio) + ".\n"
                + "Para dejar de recibir avisos, escribe /salir.";
    }

    /** Comando «/salir» del bot: el chat deja de recibir avisos. */
    @Transactional
    public String desvincular(long chatId) {
        if (!vendedores.existsById(chatId)) {
            return "Este chat no recibe avisos de ventas.";
        }
        vendedores.deleteById(chatId);
        return "Listo, ya no recibirás avisos de ventas en este chat.";
    }

    // ------------------------------------------------------------------ 1. aviso inmediato

    /** Después de guardar el prospecto, avisa al equipo sin demorar la respuesta del chat. */
    @TransactionalEventListener(fallbackExecution = true)
    public void alRegistrar(ProspectoRegistrado evento) {
        Prospecto prospecto = evento.prospecto();
        envios.execute(() -> avisarNuevo(prospecto));
    }

    void avisarNuevo(Prospecto prospecto) {
        if (!telegram.estaConfigurado()) {
            return;
        }
        enviarAVendedores(vendedores.findAll(), textoNuevo(prospecto), botonWhatsApp(prospecto));
    }

    // ------------------------------------------------------------------ 2 y 3. revisión periódica

    @Scheduled(fixedDelayString = "${captabot.avisos.revision-ms:60000}", initialDelay = 30_000)
    public void revisar() {
        if (!telegram.estaConfigurado()) {
            return;
        }
        try {
            recordarPendientes();
            seguirClientes();
        } catch (RuntimeException ex) {
            log.warn("No se pudieron revisar los avisos pendientes: {}", telegram.ocultar(ex.getMessage()));
        }
    }

    /** Recuerda una sola vez cada prospecto caliente o tibio que sigue en «Nuevo». */
    void recordarPendientes() {
        List<AvisoTelegram> lista = vendedores.findAll();
        if (lista.isEmpty()) {
            return;
        }
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        OffsetDateTime hasta = ahora.minusMinutes(minutosRecordatorio);
        for (Prospecto p : prospectos.pendientesDeRecordatorio(Etapa.NUEVO, A_RECORDAR, hasta.minus(VENTANA), hasta)) {
            enviarAVendedores(lista, textoRecordatorio(p, ahora), botonWhatsApp(p));
            p.setRecordatorioEn(ahora);
            prospectos.save(p);
        }
    }

    /** Escribe una sola vez al cliente que llegó por Telegram, si su venta sigue abierta. */
    void seguirClientes() {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        OffsetDateTime hasta = ahora.minusMinutes(minutosSeguimiento);
        for (Prospecto p : prospectos.pendientesDeSeguimiento(CERRADAS, hasta.minus(VENTANA), hasta)) {
            try {
                telegram.enviar(p.getTelegramChatId(), textoSeguimiento(p), tecladoMenu());
            } catch (RuntimeException ex) {
                log.warn("No se pudo enviar el seguimiento al prospecto {}: {}", p.getId(),
                        telegram.ocultar(ex.getMessage()));
            }
            // Se marca aunque falle (por ejemplo, si el cliente bloqueó el bot), para no insistir.
            p.setSeguimientoClienteEn(ahora);
            prospectos.save(p);
        }
    }

    private void enviarAVendedores(List<AvisoTelegram> lista, String texto, Map<String, Object> teclado) {
        for (AvisoTelegram vendedor : lista) {
            try {
                telegram.enviar(vendedor.getChatId(), texto, teclado);
            } catch (RuntimeException ex) {
                log.warn("No se pudo avisar a {} por Telegram: {}", vendedor.getNombre(),
                        telegram.ocultar(ex.getMessage()));
            }
        }
    }

    // ------------------------------------------------------------------ textos

    static String textoNuevo(Prospecto p) {
        return emoji(p.getCalificacion()) + " Nuevo prospecto " + calificacion(p.getCalificacion())
                + " (" + p.getPuntaje() + " puntos)\n"
                + "👤 " + nombre(p.getNombre()) + "\n"
                + "📱 " + telefono(p.getTelefono()) + "\n"
                + "🛠 " + p.getServicioInteres() + "\n"
                + "💰 " + soles(p.getPresupuesto()) + "\n"
                + "⏱ Urgencia: " + urgencia(p.getUrgencia()) + "\n"
                + "📍 Llegó por: " + canal(p.getCanal());
    }

    static String textoRecordatorio(Prospecto p, OffsetDateTime ahora) {
        long minutos = p.getCreadoEn() == null ? 0 : Duration.between(p.getCreadoEn(), ahora).toMinutes();
        return "⏰ Recordatorio: " + nombre(p.getNombre()) + " (" + calificacion(p.getCalificacion()) + ", "
                + p.getPuntaje() + " puntos) sigue en «Nuevo» desde hace " + duracion(minutos) + ".\n"
                + "🛠 " + p.getServicioInteres() + " · 📱 " + telefono(p.getTelefono()) + "\n"
                + "Contáctalo y cambia su etapa en el panel.";
    }

    static String textoSeguimiento(Prospecto p) {
        String primero = nombre(p.getNombre()).split(" ")[0];
        return "Hola, " + primero + " 👋\n"
                + "Seguimos atentos a tu solicitud de " + p.getServicioInteres() + ". Un asesor se comunicará "
                + "contigo al " + telefono(p.getTelefono()) + ".\n"
                + "Si tienes otra duda, elige una opción o escríbeme aquí.";
    }

    static Map<String, Object> botonWhatsApp(Prospecto p) {
        Map<String, Object> boton = Map.of("text", "💬 Escribir por WhatsApp", "url", "https://wa.me/51" + p.getTelefono());
        return Map.of("inline_keyboard", List.of(List.of(boton)));
    }

    static Map<String, Object> tecladoMenu() {
        List<List<Map<String, Object>>> filas = ConversacionService.MENU.stream()
                .map(opcion -> List.<Map<String, Object>>of(Map.<String, Object>of("text", opcion)))
                .toList();
        return Map.of("keyboard", filas, "resize_keyboard", true, "one_time_keyboard", true);
    }

    static String duracion(long minutos) {
        if (minutos < 60) {
            return minutos + " min";
        }
        if (minutos < 1440) {
            long horas = minutos / 60;
            long resto = minutos % 60;
            return horas + " h" + (resto == 0 ? "" : " " + resto + " min");
        }
        long dias = minutos / 1440;
        return dias + (dias == 1 ? " día" : " días");
    }

    /** «wilder tudela» → «Wilder Tudela» (solo para mostrar). */
    static String nombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "Sin nombre";
        }
        return Arrays.stream(nombre.trim().split("\\s+"))
                .map(p -> p.substring(0, 1).toUpperCase(Locale.ROOT) + p.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(" "));
    }

    static String telefono(String telefono) {
        if (telefono != null && telefono.matches("\\d{9}")) {
            return telefono.substring(0, 3) + " " + telefono.substring(3, 6) + " " + telefono.substring(6);
        }
        return telefono;
    }

    private static String soles(BigDecimal monto) {
        return monto == null ? "S/ 0.00" : String.format(Locale.US, "S/ %,.2f", monto);
    }

    private static String emoji(Calificacion calificacion) {
        return switch (calificacion) {
            case CALIENTE -> "🔥";
            case TIBIO -> "🌤";
            case FRIO -> "❄️";
        };
    }

    private static String calificacion(Calificacion calificacion) {
        return switch (calificacion) {
            case CALIENTE -> "caliente";
            case TIBIO -> "tibio";
            case FRIO -> "frío";
        };
    }

    private static String urgencia(Urgencia urgencia) {
        return switch (urgencia) {
            case ALTA -> "alta (esta semana)";
            case MEDIA -> "media (este mes)";
            case BAJA -> "baja (solo averiguando)";
        };
    }

    private static String canal(String canal) {
        return ConversacionService.CANAL_TELEGRAM.equals(canal) ? "Telegram" : "Chat web";
    }
}
