package pe.captabot.api.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pe.captabot.api.servicio.ProspectoService;
import pe.captabot.api.web.ProspectoRequest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Pruebas de la conversación del asistente (HU01, HU02, HU04 y HU05). */
class ConversacionServiceTest {

    private ProspectoService prospectos;
    private ConversacionService chat;

    @BeforeEach
    void preparar() {
        prospectos = mock(ProspectoService.class);
        chat = new ConversacionService(prospectos, new PreguntasAzure("", "", ""), new PreguntasLocales());
    }

    @Test
    void saludaYMuestraElMenuAlAbrirElChat() {
        RespuestaChat r = chat.procesar(null, "");

        assertThat(r.sesionId()).isNotBlank();
        assertThat(r.mensajes().get(0)).contains("Soy CaptaBot");
        assertThat(r.opciones()).contains("Quiero cotizar");
    }

    @Test
    void capturaLosDatosYRegistraAlProspecto() {
        String s = chat.procesar(null, "").sesionId();

        assertThat(chat.procesar(s, "Quiero cotizar").mensajes().get(0)).contains("¿Estás de acuerdo?");
        assertThat(chat.procesar(s, "Sí, acepto").mensajes().get(0)).contains("¿Cuál es tu nombre?");
        assertThat(chat.procesar(s, "me llamo maría quispe").mensajes().get(0)).startsWith("Gracias, María.");
        assertThat(chat.procesar(s, "987 654 321").opciones()).contains("Diseño web");
        chat.procesar(s, "Diseño web");
        assertThat(chat.procesar(s, "S/ 6,000").opciones()).contains("Alta: esta semana");
        RespuestaChat fin = chat.procesar(s, "Alta: esta semana");

        assertThat(fin.registrado()).isTrue();
        assertThat(fin.mensajes().get(0)).isEqualTo("Gracias, María. Registramos tus datos y un asesor te contactará pronto.");
        ArgumentCaptor<ProspectoRequest> datos = ArgumentCaptor.forClass(ProspectoRequest.class);
        verify(prospectos).registrar(datos.capture(), eq(ConversacionService.CANAL_WEB));
        assertThat(datos.getValue().nombre()).isEqualTo("maría quispe");
        assertThat(datos.getValue().telefono()).isEqualTo("987654321");
        assertThat(datos.getValue().servicioInteres()).isEqualTo("Diseño web");
        assertThat(datos.getValue().presupuesto()).isEqualByComparingTo(new BigDecimal("6000"));
        assertThat(datos.getValue().urgencia()).isEqualTo("ALTA");
    }

    @Test
    void vuelveAPedirElCelularSiNoTiene9Digitos() {
        String s = llegarAlCelular();

        RespuestaChat r = chat.procesar(s, "12345");

        assertThat(r.mensajes().get(0)).contains("9 dígitos");
        assertThat(chat.procesar(s, "+51 912 345 678").opciones()).contains("Marketing digital");
    }

    @Test
    void sinConsentimientoNoGuardaNada() {
        String s = chat.procesar(null, "").sesionId();
        chat.procesar(s, "Quiero que me llame un asesor");

        RespuestaChat r = chat.procesar(s, "no");

        assertThat(r.mensajes().get(0)).contains("no guardaremos tus datos");
        verify(prospectos, never()).registrar(any(), any());
    }

    @Test
    void respondeUnaPreguntaFrecuenteYOfreceUnAsesor() {
        String s = chat.procesar(null, "").sesionId();

        RespuestaChat r = chat.procesar(s, "¿Cuál es su horario?");

        assertThat(r.mensajes()).hasSize(2);
        assertThat(r.mensajes().get(0)).contains("lunes a viernes");
        assertThat(r.opciones()).containsExactly("Sí", "No");
        assertThat(chat.procesar(s, "No").opciones()).isEqualTo(ConversacionService.MENU);
    }

    @Test
    void despuesDeUnaRespuestaPuedeAceptarElContacto() {
        String s = chat.procesar(null, "").sesionId();
        chat.procesar(s, "¿Cuánto cuesta?");

        assertThat(chat.procesar(s, "sí").mensajes().get(0)).contains("¿Estás de acuerdo?");
    }

    @Test
    void avisaCuandoNoEntiende() {
        String s = chat.procesar(null, "").sesionId();

        assertThat(chat.procesar(s, "¿Venden pizzas?").mensajes().get(0)).isEqualTo(ConversacionService.NO_ENTIENDO);
    }

    @Test
    void usaLaRespuestaDeAzureCuandoEstaConfigurado() {
        PreguntasAzure azure = new PreguntasAzure("", "", "") {
            @Override
            public Optional<String> responder(String pregunta) {
                return Optional.of("Respuesta desde Azure AI Language");
            }
        };
        ConversacionService conAzure = new ConversacionService(prospectos, azure, new PreguntasLocales());
        String s = conAzure.procesar(null, "").sesionId();

        assertThat(conAzure.procesar(s, "¿Cuál es su horario?").mensajes().get(0))
                .isEqualTo("Respuesta desde Azure AI Language");
    }

    @Test
    void escribirMenuReiniciaLaConversacion() {
        String s = llegarAlCelular();

        RespuestaChat r = chat.procesar(s, "menú");

        assertThat(r.opciones()).isEqualTo(ConversacionService.MENU);
        verify(prospectos, never()).registrar(any(), any());
    }

    @Test
    void unaSesionDesconocidaEmpiezaDeNuevo() {
        RespuestaChat r = chat.procesar("sesion-que-no-existe", "hola");

        assertThat(r.sesionId()).isNotEqualTo("sesion-que-no-existe");
        assertThat(r.mensajes().get(0)).contains("Soy CaptaBot");
    }

    @Test
    void entiendeLaUrgenciaConPalabrasComunes() {
        assertThat(ConversacionService.leerUrgencia("la necesito esta semana")).hasToString("ALTA");
        assertThat(ConversacionService.leerUrgencia("Media: este mes")).hasToString("MEDIA");
        assertThat(ConversacionService.leerUrgencia("solo estoy averiguando")).hasToString("BAJA");
        assertThat(ConversacionService.leerUrgencia("cuando sea")).isNull();
    }

    @Test
    void avisaCuandoEsperaElCelular() {
        String s = chat.procesar(null, "").sesionId();
        chat.procesar(s, "Quiero cotizar");
        chat.procesar(s, "Sí");

        assertThat(chat.procesar(s, "Ana").pideTelefono()).isTrue();
        assertThat(chat.procesar(s, "987654321").pideTelefono()).isFalse();
    }

    private String llegarAlCelular() {
        String s = chat.procesar(null, "").sesionId();
        chat.procesar(s, "Quiero cotizar");
        chat.procesar(s, "Sí");
        chat.procesar(s, "Ana");
        return s;
    }
}
