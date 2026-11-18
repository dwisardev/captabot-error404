package pe.captabot.api.servicio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Urgencia;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Pruebas de las reglas de calificación (HU03). */
class CalificacionServiceTest {

    private final CalificacionService servicio = new CalificacionService();

    @ParameterizedTest(name = "S/ {0} con urgencia {1} -> {2} puntos, {3}")
    @CsvSource({
            "6000,    ALTA,  100, CALIENTE",
            "5000,    BAJA,   60, TIBIO",
            "5500,    MEDIA,  80, CALIENTE",
            "3000,    ALTA,   85, CALIENTE",
            "1000,    ALTA,   70, CALIENTE",
            "2500,    MEDIA,  65, TIBIO",
            "4999.99, BAJA,   45, TIBIO",
            "2000,    BAJA,   45, TIBIO",
            "500,     MEDIA,  50, TIBIO",
            "499.99,  ALTA,   55, TIBIO",
            "0,       MEDIA,  35, FRIO",
            "800,     BAJA,   30, FRIO",
            "300,     BAJA,   15, FRIO"
    })
    void calificaSegunPresupuestoYUrgencia(String presupuesto, Urgencia urgencia, int puntaje,
                                           Calificacion calificacion) {
        ResultadoCalificacion resultado = servicio.calificar(new BigDecimal(presupuesto), urgencia);

        assertThat(resultado.puntaje()).isEqualTo(puntaje);
        assertThat(resultado.calificacion()).isEqualTo(calificacion);
    }

    @Test
    void rechazaPresupuestoNegativo() {
        assertThatThrownBy(() -> servicio.calificar(new BigDecimal("-1"), Urgencia.ALTA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convierteLaUrgenciaQueLlegaDelChat() {
        assertThat(Urgencia.desdeTexto("alta")).isEqualTo(Urgencia.ALTA);
        assertThat(Urgencia.desdeTexto("  Media ")).isEqualTo(Urgencia.MEDIA);
        assertThat(Urgencia.desdeTexto("BAJA")).isEqualTo(Urgencia.BAJA);
        assertThatThrownBy(() -> Urgencia.desdeTexto("urgente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ALTA, MEDIA o BAJA");
    }
}
