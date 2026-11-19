package pe.captabot.api.whatsapp;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Formato de los mensajes de WhatsApp: texto, botones (hasta 3 cortas) o lista (hasta 10). */
class WhatsAppApiTest {

    @Test
    void sinOpcionesEnviaTexto() {
        Map<String, Object> cuerpo = WhatsAppApi.cuerpo("51955777888", "Hola", List.of());

        assertThat(cuerpo).containsEntry("to", "51955777888").containsEntry("type", "text");
        assertThat(cuerpo.get("text").toString()).contains("body=Hola");
    }

    @Test
    void hastaTresOpcionesCortasSonBotones() {
        Map<String, Object> cuerpo = WhatsAppApi.cuerpo("51955777888", "¿Los registro?",
                List.of("Sí, registrar", "Corregir datos"));

        assertThat(cuerpo).containsEntry("type", "interactive");
        String interactivo = cuerpo.get("interactive").toString();
        assertThat(interactivo).contains("type=button", "id=op:0", "title=Sí, registrar", "id=op:1");
    }

    @Test
    void opcionesLargasOMuchasSonUnaListaConTitulosRecortados() {
        Map<String, Object> cuerpo = WhatsAppApi.cuerpo("51955777888", "¿Qué urgencia?",
                List.of("Alta: esta semana", "Media: este mes", "Baja: solo estoy averiguando"));

        String interactivo = cuerpo.get("interactive").toString();
        assertThat(interactivo).contains("type=list", "button=Ver opciones", "title=Alta: esta semana",
                "title=Baja: solo estoy averigu…", "description=Baja: solo estoy averiguando");
    }

    @Test
    void masDeDiezOpcionesVanNumeradasEnElTexto() {
        List<String> once = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

        Map<String, Object> cuerpo = WhatsAppApi.cuerpo("51955777888", "Elige", once);

        assertThat(cuerpo).containsEntry("type", "text");
        assertThat(cuerpo.get("text").toString()).contains("1. 1", "11. 11");
    }

    @Test
    void publicaEnLaCloudApiConElToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        WhatsAppApi api = new WhatsAppApi("tok-123", "999", "v26.0", "verif", "", builder);
        servidor.expect(requestTo("https://graph.facebook.com/v26.0/999/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok-123"))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("51955777888"))
                .andRespond(withSuccess());

        api.enviar("51955777888", "Hola", List.of());

        servidor.verify();
        assertThat(api.ocultar("error con tok-123")).isEqualTo("error con ***");
    }

    @Test
    void sinTokenNoEnviaNada() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        WhatsAppApi api = new WhatsAppApi("", "", "", "verif", "", builder);

        api.enviar("51955777888", "Hola", List.of());

        servidor.verify();
        assertThat(api.estaConfigurado()).isFalse();
        assertThat(api.webhookActivo()).isTrue();
    }
}
