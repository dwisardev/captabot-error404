package pe.captabot.api.chat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** HU01: el buscador local reconoce cada tema por sus palabras clave. */
class PreguntasLocalesTest {

    private final PreguntasLocales motor = new PreguntasLocales();

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource(delimiter = '|', value = {
            "¿A qué hora atienden?|lunes a viernes",
            "¿Trabajan los sábados?|lunes a viernes",
            "¿Dónde están ubicados?|Av. Principal",
            "¿Qué servicios ofrecen?|diseño de páginas web",
            "¿Cuánto cuesta una página web?|parte de S/ 800",
            "¿Puedo pagar con Yape?|Yape, Plin",
            "¿Cuánto demoran en entregar?|días hábiles",
            "¿Tienen garantía?|30 días de soporte"
    })
    void reconoceCadaPreguntaFrecuente(String pregunta, String fragmento) {
        assertThat(motor.responder(pregunta)).hasValueSatisfying(r -> assertThat(r).contains(fragmento));
    }

    @Test
    void noRespondeLoQueNoConoce() {
        assertThat(motor.responder("¿Venden pizzas?")).isEmpty();
    }
}
