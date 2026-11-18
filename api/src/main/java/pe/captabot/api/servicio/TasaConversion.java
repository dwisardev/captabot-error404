package pe.captabot.api.servicio;

import java.math.BigDecimal;

/** Porcentaje de prospectos que llegaron a CERRADO_GANADO (HU12). */
public record TasaConversion(long totalProspectos, long ganados, BigDecimal tasaPorcentaje) {
}
