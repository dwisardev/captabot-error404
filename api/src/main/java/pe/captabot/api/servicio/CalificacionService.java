package pe.captabot.api.servicio;

import org.springframework.stereotype.Service;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Urgencia;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Califica a cada prospecto (HU03). Reglas documentadas en api/README.md:
 *
 * <pre>
 * puntaje = puntos por presupuesto + puntos por urgencia   (0 a 100)
 *
 * Presupuesto (S/):  5000 o más = 50 | 2000 a 4999.99 = 35 | 500 a 1999.99 = 20 | menos de 500 = 5
 * Urgencia:          ALTA = 50       | MEDIA = 30          | BAJA = 10
 *
 * Calificación:      70 o más = CALIENTE | 40 a 69 = TIBIO | menos de 40 = FRIO
 * </pre>
 */
@Service
public class CalificacionService {

    public static final int UMBRAL_CALIENTE = 70;
    public static final int UMBRAL_TIBIO = 40;

    private static final BigDecimal PRESUPUESTO_ALTO = new BigDecimal("5000");
    private static final BigDecimal PRESUPUESTO_MEDIO = new BigDecimal("2000");
    private static final BigDecimal PRESUPUESTO_BAJO = new BigDecimal("500");

    public ResultadoCalificacion calificar(BigDecimal presupuesto, Urgencia urgencia) {
        Objects.requireNonNull(presupuesto, "El presupuesto es obligatorio");
        Objects.requireNonNull(urgencia, "La urgencia es obligatoria");
        if (presupuesto.signum() < 0) {
            throw new IllegalArgumentException("El presupuesto no puede ser negativo");
        }

        int puntaje = puntosPorPresupuesto(presupuesto) + puntosPorUrgencia(urgencia);
        Calificacion calificacion;
        if (puntaje >= UMBRAL_CALIENTE) {
            calificacion = Calificacion.CALIENTE;
        } else if (puntaje >= UMBRAL_TIBIO) {
            calificacion = Calificacion.TIBIO;
        } else {
            calificacion = Calificacion.FRIO;
        }
        return new ResultadoCalificacion(puntaje, calificacion);
    }

    int puntosPorPresupuesto(BigDecimal presupuesto) {
        if (presupuesto.compareTo(PRESUPUESTO_ALTO) >= 0) {
            return 50;
        }
        if (presupuesto.compareTo(PRESUPUESTO_MEDIO) >= 0) {
            return 35;
        }
        if (presupuesto.compareTo(PRESUPUESTO_BAJO) >= 0) {
            return 20;
        }
        return 5;
    }

    int puntosPorUrgencia(Urgencia urgencia) {
        return switch (urgencia) {
            case ALTA -> 50;
            case MEDIA -> 30;
            case BAJA -> 10;
        };
    }
}
