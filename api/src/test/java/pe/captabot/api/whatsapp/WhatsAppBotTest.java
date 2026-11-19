package pe.captabot.api.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.chat.PreguntasAzure;
import pe.captabot.api.chat.PreguntasLocales;
import pe.captabot.api.servicio.ProspectoService;
import pe.captabot.api.web.ProspectoRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** HU02 por WhatsApp: el bot sigue la misma conversación que la página web y Telegram. */
class WhatsAppBotTest {

    private static final String CLIENTE = "51955777888";

    /** Guarda lo que el bot enviaría a WhatsApp, sin conectarse. */
    static class WhatsAppFalso extends WhatsAppApi {
        record Enviado(String para, String texto, List<String> opciones) {
        }

        final List<Enviado> enviados = new ArrayList<>();

        WhatsAppFalso() {
            super("tok", "999", "v26.0", "verif", "", RestClient.builder());
        }

        @Override
        public void enviar(String para, String texto, List<String> opciones) {
            enviados.add(new Enviado(para, texto, opciones));
        }

        Enviado ultimo() {
            return enviados.get(enviados.size() - 1);
        }
    }

    private WhatsAppFalso whatsapp;
    private ProspectoService prospectos;
    private WhatsAppBot bot;

    @BeforeEach
    void preparar() {
        whatsapp = new WhatsAppFalso();
        prospectos = mock(ProspectoService.class);
        ConversacionService chat = new ConversacionService(prospectos, new PreguntasAzure("", "", ""),
                new PreguntasLocales());
        bot = new WhatsAppBot(whatsapp, chat, Runnable::run);
    }

    private static Map<String, Object> notificacion(Map<String, Object> mensaje) {
        return Map.of("object", "whatsapp_business_account", "entry", List.of(Map.of("changes", List.of(
                Map.of("field", "messages", "value", Map.of("messaging_product", "whatsapp",
                        "messages", List.of(mensaje)))))));
    }

    private static Map<String, Object> texto(String id, String texto) {
        return Map.of("from", CLIENTE, "id", id, "type", "text", "text", Map.of("body", texto));
    }

    private static Map<String, Object> boton(String id, String opcion, String titulo) {
        return Map.of("from", CLIENTE, "id", id, "type", "interactive",
                "interactive", Map.of("type", "button_reply", "button_reply", Map.of("id", opcion, "title", titulo)));
    }

    private static Map<String, Object> lista(String id, String opcion, String titulo) {
        return Map.of("from", CLIENTE, "id", id, "type", "interactive",
                "interactive", Map.of("type", "list_reply", "list_reply", Map.of("id", opcion, "title", titulo)));
    }

    @Test
    void saludaConElMenu() {
        bot.recibir(notificacion(texto("m1", "Hola")));

        assertThat(whatsapp.ultimo().para()).isEqualTo(CLIENTE);
        assertThat(whatsapp.ultimo().texto()).contains("CaptaBot");
        assertThat(whatsapp.ultimo().opciones()).isEqualTo(ConversacionService.MENU);
    }

    @Test
    void registraAlProspectoConElCanalYElNumeroDeWhatsapp() {
        bot.recibir(notificacion(texto("m1", "Quiero cotizar")));
        bot.recibir(notificacion(texto("m2", "Sí, acepto")));
        bot.recibir(notificacion(texto("m3", "Rosa Vega")));
        assertThat(whatsapp.ultimo().opciones()).containsExactly(WhatsAppBot.USAR_NUMERO);

        bot.recibir(notificacion(boton("m4", "op:0", "📱 Usar este número")));
        bot.recibir(notificacion(texto("m5", "Diseño web")));
        bot.recibir(notificacion(texto("m6", "3000")));
        // La urgencia llega como fila de una lista: el título viene recortado, el id indica la opción
        bot.recibir(notificacion(lista("m7", "op:0", "Alta: esta semana")));
        assertThat(whatsapp.ultimo().texto()).contains("Revisa tus datos", "955777888");
        bot.recibir(notificacion(boton("m8", "op:0", "Sí, registrar")));

        ArgumentCaptor<ProspectoRequest> datos = ArgumentCaptor.forClass(ProspectoRequest.class);
        verify(prospectos).registrar(datos.capture(), eq(ConversacionService.CANAL_WHATSAPP), eq(CLIENTE));
        assertThat(datos.getValue().telefono()).isEqualTo("955777888");
        assertThat(datos.getValue().urgencia()).isEqualTo("ALTA");
        assertThat(whatsapp.enviados.get(whatsapp.enviados.size() - 2).texto())
                .startsWith("Gracias, Rosa. Registramos tus datos");
    }

    @Test
    void ignoraLosMensajesRepetidosQueReenviaMeta() {
        bot.recibir(notificacion(texto("m1", "Hola")));
        bot.recibir(notificacion(texto("m1", "Hola")));

        assertThat(whatsapp.enviados).hasSize(1);
    }

    @Test
    void avisaQueSoloEntiendeTexto() {
        bot.recibir(notificacion(Map.of("from", CLIENTE, "id", "m9", "type", "image", "image", Map.of("id", "x"))));

        assertThat(whatsapp.ultimo().texto()).isEqualTo(WhatsAppBot.SOLO_TEXTO);
    }

    @Test
    void ignoraLasNotificacionesSinMensajes() {
        bot.recibir(Map.of("entry", List.of(Map.of("changes", List.of(Map.of("value",
                Map.of("statuses", List.of(Map.of("id", "s1", "status", "read")))))))));

        assertThat(whatsapp.enviados).isEmpty();
    }

    @Test
    void usaElNumeroLocalSiEsPeruano() {
        assertThat(WhatsAppBot.numeroLocal("51955777888")).isEqualTo("955777888");
        assertThat(WhatsAppBot.numeroLocal("5491112345678")).isEqualTo("5491112345678");
    }
}
