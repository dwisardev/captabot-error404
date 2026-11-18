package pe.captabot.api.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.captabot.api.web.ManejadorErrores;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HU02: el chat de la página web usa este endpoint. */
class ChatControllerTest {

    private ConversacionService servicio;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        servicio = mock(ConversacionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(servicio))
                .setControllerAdvice(new ManejadorErrores())
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void devuelveLosMensajesYLasOpciones() throws Exception {
        when(servicio.procesar(null, "hola"))
                .thenReturn(new RespuestaChat("abc", List.of("¡Hola!"), List.of("Quiero cotizar"), false, false));

        mvc.perform(post("/api/chat/mensajes").contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"hola\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionId").value("abc"))
                .andExpect(jsonPath("$.mensajes[0]").value("¡Hola!"))
                .andExpect(jsonPath("$.opciones[0]").value("Quiero cotizar"))
                .andExpect(jsonPath("$.registrado").value(false));
    }

    @Test
    void rechazaMensajesMuyLargos() throws Exception {
        String largo = "a".repeat(501);

        mvc.perform(post("/api/chat/mensajes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"" + largo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles.texto").value("El mensaje admite hasta 500 caracteres"));
    }
}
