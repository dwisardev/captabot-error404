package pe.captabot.api.web;

import pe.captabot.api.modelo.HistorialEtapa;

import java.time.OffsetDateTime;
import java.util.Map;

public record HistorialResponse(String etapaAnterior, String etapaNueva, Long usuarioId, String usuario,
                                OffsetDateTime cambiadoEn) {

    public static HistorialResponse desde(HistorialEtapa h, Map<Long, String> nombres) {
        return new HistorialResponse(
                h.getEtapaAnterior() == null ? null : h.getEtapaAnterior().name(),
                h.getEtapaNueva().name(),
                h.getUsuarioId(),
                h.getUsuarioId() == null ? null : nombres.getOrDefault(h.getUsuarioId(), "Usuario " + h.getUsuarioId()),
                h.getCambiadoEn());
    }
}
