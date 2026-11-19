package pe.captabot.api.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClient;
import pe.captabot.api.chat.ConversacionService;
import pe.captabot.api.chat.PreguntasAzure;
import pe.captabot.api.chat.PreguntasLocales;
import pe.captabot.api.chat.RespuestaChat;
import pe.captabot.api.servicio.ProspectoService;
import pe.captabot.api.web.ProspectoRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU02 por Telegram: el bot sigue la misma conversación que la página web. */
class TelegramBotTest {

    /** Guarda lo que el bot enviaría a Telegram, sin conectarse. */
    static class TelegramFalso extends TelegramApi {
        record Enviado(long chatId, String texto, Map<String, Object> teclado) {
        }

        final List<Enviado> enviados = new ArrayList<>();

        TelegramFalso() {
            super("123:abc", RestClient.builder());
        }

        @Override
        void enviar(long chatId, String texto, Map<String, Object> teclado) {
            enviados.add(new Enviado(chatId, texto, teclado));
        }

        Enviado ultimo() {
            return enviados.get(enviados.size() - 1);
        }
    }

    private TelegramFalso telegram;
    private ProspectoService prospectos;
    private AvisosService avisos;
    private TelegramBot bot;

    @BeforeEach
    void preparar() {
        telegram = new TelegramFalso();
        prospectos = mock(ProspectoService.class);
        ConversacionService chat = new ConversacionService(prospectos, new PreguntasAzure("", "", ""),
                new PreguntasLocales());
        avisos = mock(AvisosService.class);
        bot = new TelegramBot(telegram, chat, avisos);
    }

    private static Map<String, Object> texto(long chatId, String texto) {
        return Map.of("update_id", 1, "message", Map.of("chat", Map.of("id", chatId, "type", "private"), "text", texto));
    }

    private static Map<String, Object> contacto(long chatId, String telefono) {
        return Map.of("update_id", 2, "message", Map.of("chat", Map.of("id", chatId, "type", "private"),
                "contact", Map.of("phone_number", telefono)));
    }

    @Test
    void saludaConElMenuComoBotones() {
        bot.atender(texto(77, "/start"));

        TelegramFalso.Enviado saludo = telegram.ultimo();
        assertThat(saludo.chatId()).isEqualTo(77);
        assertThat(saludo.texto()).contains("Soy CaptaBot");
        assertThat((List<?>) saludo.teclado().get("keyboard")).hasSize(4);
    }

    @Test
    void registraAlProspectoConElCanalTelegram() {
        bot.atender(texto(77, "/start"));
        bot.atender(texto(77, "Quiero cotizar"));
        bot.atender(texto(77, "Sí, acepto"));
        bot.atender(texto(77, "Rosa Vega"));
        assertThat(telegram.ultimo().teclado().toString()).contains("request_contact=true");

        bot.atender(contacto(77, "+51 987 654 321"));
        bot.atender(texto(77, "Diseño web"));
        bot.atender(texto(77, "3000"));
        bot.atender(texto(77, "Alta: esta semana"));
        assertThat(telegram.ultimo().texto()).contains("Revisa tus datos");
        assertThat(telegram.ultimo().teclado().toString()).contains("Sí, registrar", "Corregir datos");
        bot.atender(texto(77, "Sí, registrar"));

        ArgumentCaptor<ProspectoRequest> datos = ArgumentCaptor.forClass(ProspectoRequest.class);
        verify(prospectos).registrar(datos.capture(), eq(ConversacionService.CANAL_TELEGRAM), eq("77"));
        assertThat(datos.getValue().telefono()).isEqualTo("987654321");
        assertThat(datos.getValue().nombre()).isEqualTo("Rosa Vega");
        assertThat(telegram.enviados.get(telegram.enviados.size() - 2).texto())
                .startsWith("Gracias, Rosa. Registramos tus datos");
    }

    @Test
    void cadaChatTieneSuPropiaConversacion() {
        bot.atender(texto(1, "Quiero cotizar"));
        bot.atender(texto(2, "¿Cuál es su horario?"));

        assertThat(telegram.enviados.get(0).texto()).contains("¿Estás de acuerdo?");
        assertThat(telegram.enviados.get(1).texto()).contains("lunes a viernes");
    }

    @Test
    void vinculaAlVendedorConElCodigo() {
        when(avisos.vincular(9L, "Ana", "clave-123")).thenReturn("Listo, Ana.");

        bot.atender(Map.of("update_id", 5, "message", Map.of("chat", Map.of("id", 9, "type", "private"),
                "from", Map.of("first_name", "Ana"), "text", "/vendedor clave-123")));

        verify(avisos).vincular(9L, "Ana", "clave-123");
        assertThat(telegram.ultimo().texto()).isEqualTo("Listo, Ana.");
        assertThat(telegram.ultimo().teclado()).containsEntry("remove_keyboard", true);
    }

    @Test
    void salirDejaDeRecibirAvisos() {
        when(avisos.desvincular(9L)).thenReturn("Listo, ya no recibirás avisos de ventas en este chat.");

        bot.atender(texto(9, "/salir"));

        verify(avisos).desvincular(9L);
        assertThat(telegram.ultimo().texto()).contains("ya no recibirás avisos");
    }

    @Test
    void ignoraLosGrupos() {
        bot.atender(Map.of("update_id", 3, "message",
                Map.of("chat", Map.of("id", -100, "type", "group"), "text", "hola")));

        assertThat(telegram.enviados).isEmpty();
    }

    @Test
    void avisaQueSoloEntiendeTexto() {
        bot.atender(Map.of("update_id", 4, "message",
                Map.of("chat", Map.of("id", 5, "type", "private"), "sticker", Map.of("emoji", "👍"))));

        assertThat(telegram.ultimo().texto()).isEqualTo(TelegramBot.SOLO_TEXTO);
    }

    @Test
    void sinOpcionesQuitaLosBotones() {
        Map<String, Object> teclado = TelegramBot.teclado(
                new RespuestaChat("s", List.of("¿Cuál es tu nombre?"), List.of(), false, false));

        assertThat(teclado).containsEntry("remove_keyboard", true);
    }
}
