package pe.captabot.api.servicio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** Los filtros por fecha usan la hora de Lima. */
public final class Fechas {

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    private Fechas() {
    }

    public static OffsetDateTime inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZONA).toOffsetDateTime();
    }

    /** @throws IllegalArgumentException si desde es posterior a hasta */
    public static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
    }
}
