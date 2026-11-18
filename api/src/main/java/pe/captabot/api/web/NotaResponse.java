package pe.captabot.api.web;

import pe.captabot.api.modelo.NotaSeguimiento;

import java.time.OffsetDateTime;
import java.util.Map;

public record NotaResponse(Long id, String contenido, Long autorId, String autor, OffsetDateTime creadoEn,
                           OffsetDateTime editadoEn) {

    public static NotaResponse desde(NotaSeguimiento n, Map<Long, String> nombres) {
        return new NotaResponse(n.getId(), n.getContenido(), n.getUsuarioId(),
                nombres.getOrDefault(n.getUsuarioId(), "Usuario " + n.getUsuarioId()),
                n.getCreadoEn(), n.getEditadoEn());
    }
}
