package pe.captabot.api.web;

import pe.captabot.api.modelo.Prospecto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Prospecto tal como lo consulta el panel administrativo. */
public record ProspectoResponse(
        Long id,
        String nombre,
        String telefono,
        String servicioInteres,
        BigDecimal presupuesto,
        String urgencia,
        int puntaje,
        String calificacion,
        String etapa,
        String canal,
        Long vendedorId,
        OffsetDateTime creadoEn,
        // Automatización (HU13): cuándo se avisó al equipo, se recordó y se escribió al cliente
        OffsetDateTime avisoEnviadoEn,
        OffsetDateTime recordatorioEn,
        OffsetDateTime seguimientoClienteEn
) {

    public static ProspectoResponse desde(Prospecto p) {
        return new ProspectoResponse(
                p.getId(),
                p.getNombre(),
                p.getTelefono(),
                p.getServicioInteres(),
                p.getPresupuesto(),
                p.getUrgencia().name(),
                p.getPuntaje(),
                p.getCalificacion().name(),
                p.getEtapa().name(),
                p.getCanal(),
                p.getVendedorId(),
                p.getCreadoEn(),
                p.getAvisoEnviadoEn(),
                p.getRecordatorioEn(),
                p.getSeguimientoClienteEn());
    }
}
