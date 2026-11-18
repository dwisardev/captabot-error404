package pe.captabot.api.telegram;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Las llamadas siguen la Bot API de Telegram. */
class TelegramApiTest {

    @Test
    void recibeLosMensajesNuevosConLongPolling() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        TelegramApi api = new TelegramApi("123:abc", builder);
        servidor.expect(requestTo("https://api.telegram.org/bot123:abc/getUpdates?timeout=25&offset=7"
                        + "&allowed_updates=%5B%22message%22%5D"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"ok":true,"result":[{"update_id":7,"message":{"chat":{"id":5,"type":"private"},"text":"hola"}}]}
                        """, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> mensajes = api.recibir(7, 25);

        assertThat(mensajes).hasSize(1);
        assertThat(mensajes.get(0).get("update_id")).isEqualTo(7);
        servidor.verify();
    }

    @Test
    void enviaElMensajeConSusBotones() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        TelegramApi api = new TelegramApi("123:abc", builder);
        servidor.expect(requestTo("https://api.telegram.org/bot123:abc/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value(5))
                .andExpect(jsonPath("$.text").value("¡Hola!"))
                .andExpect(jsonPath("$.reply_markup.keyboard[0][0].text").value("Quiero cotizar"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        api.enviar(5, "¡Hola!", Map.of("keyboard", List.of(List.of(Map.of("text", "Quiero cotizar")))));

        servidor.verify();
    }

    @Test
    void noEscribeElTokenEnLosLogs() {
        TelegramApi api = new TelegramApi("123:abc", RestClient.builder());

        assertThat(api.ocultar("I/O error on GET https://api.telegram.org/bot123:abc/getUpdates"))
                .doesNotContain("123:abc")
                .contains("bot***");
        assertThat(api.estaConfigurado()).isTrue();
        assertThat(new TelegramApi("", RestClient.builder()).estaConfigurado()).isFalse();
    }
}
