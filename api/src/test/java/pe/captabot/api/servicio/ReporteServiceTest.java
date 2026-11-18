package pe.captabot.api.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pruebas de los reportes (HU11 y HU12). Las consultas SQL se prueban en la integración continua. */
class ReporteServiceTest {

    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final ReporteService servicio = new ReporteService(jdbc);

    @ParameterizedTest(name = "{0} ganados de {1} = {2} %")
    @CsvSource({
            "0, 0, 0.00",
            "1, 10, 10.00",
            "1, 3, 33.33",
            "2, 3, 66.67",
            "5, 5, 100.00"
    })
    void calculaLaTasaDeConversion(long ganados, long total, String esperada) {
        assertThat(ReporteService.calcularTasa(ganados, total)).isEqualByComparingTo(new BigDecimal(esperada));
    }

    @Test
    void limitaLaCantidadDeSemanas() {
        assertThatThrownBy(() -> servicio.porSemana(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio.porSemana(53)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        assertThatThrownBy(() -> servicio.conversion(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(jdbc);
    }
}
