package pe.captabot.api.chat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** El presupuesto se entiende aunque el cliente lo escriba de distintas formas. */
class MontosTest {

    @ParameterizedTest(name = "«{0}» → {1}")
    @CsvSource(delimiter = '|', value = {
            "2500|2500.00",
            "S/ 2,500|2500.00",
            "s/. 2500.50|2500.50",
            "2.500 soles|2500.00",
            "1.000.000|1000000.00",
            "2 mil|2000.00",
            "1.5k|1500.00",
            "3,5|3.50",
            "0|0.00",
            "unos 800 soles más o menos|800.00"
    })
    void leeElMonto(String texto, String esperado) {
        assertThat(Montos.leer(texto)).hasValueSatisfying(
                v -> assertThat(v).isEqualByComparingTo(new BigDecimal(esperado)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no sé", "mucho", ""})
    void avisaCuandoNoHayNumero(String texto) {
        assertThat(Montos.leer(texto)).isEmpty();
    }
}
