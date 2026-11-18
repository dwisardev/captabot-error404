package pe.captabot.api.servicio;

import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;

import java.time.LocalDate;

/** Filtros opcionales de la lista de prospectos (HU08). Un valor nulo no filtra. */
public record FiltroProspectos(Calificacion calificacion, Etapa etapa, LocalDate desde, LocalDate hasta) {

    public static FiltroProspectos sinFiltros() {
        return new FiltroProspectos(null, null, null, null);
    }
}
