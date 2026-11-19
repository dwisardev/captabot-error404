package pe.captabot.api.whatsapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Seguridad del webhook de WhatsApp: token de verificación y firma de Meta. */
class WhatsAppWebhookControllerTest {

    private static final String CUERPO = "{\"entry\":[]}";

    private WhatsAppBot bot;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        bot = mock(WhatsAppBot.class);
        WhatsAppApi api = new WhatsAppApi("", "", "", "verif-123", "secreto", RestClient.builder());
        mvc = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(api, bot)).build();
    }

    private static String firma(String secreto, String cuerpo) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(cuerpo.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void devuelveElRetoSiElTokenCoincide() throws Exception {
        mvc.perform(get("/api/whatsapp/webhook").param("hub.mode", "subscribe")
                        .param("hub.verify_token", "verif-123").param("hub.challenge", "98765"))
                .andExpect(status().isOk())
                .andExpect(content().string("98765"));
    }

    @Test
    void rechazaUnTokenDeVerificacionIncorrecto() throws Exception {
        mvc.perform(get("/api/whatsapp/webhook").param("hub.mode", "subscribe")
                        .param("hub.verify_token", "otro").param("hub.challenge", "98765"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aceptaUnaNotificacionConFirmaValida() throws Exception {
        mvc.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Hub-Signature-256", firma("secreto", CUERPO)))
                .andExpect(status().isOk());

        verify(bot).recibir(any());
    }

    @Test
    void rechazaUnaFirmaIncorrecta() throws Exception {
        mvc.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(CUERPO)
                        .header("X-Hub-Signature-256", firma("otro", CUERPO)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isUnauthorized());

        verify(bot, never()).recibir(any());
    }

    @Test
    void sinTokenDeVerificacionElWebhookEstaApagado() throws Exception {
        WhatsAppApi apagado = new WhatsAppApi("", "", "", "", "", RestClient.builder());
        MockMvc sinWebhook = MockMvcBuilders.standaloneSetup(new WhatsAppWebhookController(apagado, bot)).build();

        sinWebhook.perform(post("/api/whatsapp/webhook").contentType(MediaType.APPLICATION_JSON).content(CUERPO))
                .andExpect(status().isNotFound());
        assertThat(WhatsAppWebhookController.firmaValida("s".getBytes(), CUERPO.getBytes(), null)).isFalse();
    }
}
