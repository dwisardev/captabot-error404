package pe.captabot.api.servicio;

import java.time.LocalDate;

/** Prospectos que llegaron en una semana (que empieza el lunes), por calificación (HU11). */
public record ReporteSemanal(LocalDate semana, long total, long calientes, long tibios, long frios) {
}
