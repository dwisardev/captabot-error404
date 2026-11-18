package pe.captabot.api.chat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** HU01 con Azure AI Language: la llamada sigue la API de custom question answering. */
class PreguntasAzureTest {

    private static final String URL = "https://captabot.cognitiveservices.azure.com/language/:query-knowledgebases"
            + "?projectName=captabot-faq&api-version=2021-10-01&deploymentName=production";

    private MockRestServiceServer servidor;

    private PreguntasAzure azure() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        return new PreguntasAzure("https://captabot.cognitiveservices.azure.com/", "clave-azure", "captabot-faq", builder);
    }

    @Test
    void devuelveLaRespuestaDeAzureCuandoTieneConfianza() {
        PreguntasAzure azure = azure();
        servidor.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Ocp-Apim-Subscription-Key", "clave-azure"))
                .andExpect(jsonPath("$.question").value("¿A qué hora abren?"))
                .andRespond(withSuccess("""
                        {"answers":[{"answer":"Atendemos de 9 a 6","confidenceScore":0.92,"id":1}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(azure.responder("¿A qué hora abren?")).contains("Atendemos de 9 a 6");
        servidor.verify();
    }

    @Test
    void ignoraRespuestasConPocaConfianza() {
        PreguntasAzure azure = azure();
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"answers":[{"answer":"No answer found","confidenceScore":0.0,"id":-1}]}
                """, MediaType.APPLICATION_JSON));

        assertThat(azure.responder("¿Venden pizzas?")).isEmpty();
    }

    @Test
    void siAzureFallaNoRompeElChat() {
        PreguntasAzure azure = azure();
        servidor.expect(requestTo(URL)).andRespond(withServerError());

        assertThat(azure.responder("¿Horario?")).isEmpty();
    }

    @Test
    void sinConfiguracionNoLlamaAAzure() {
        PreguntasAzure azure = new PreguntasAzure("", "", "");

        assertThat(azure.estaConfigurado()).isFalse();
        assertThat(azure.responder("¿Horario?")).isEmpty();
    }
}
