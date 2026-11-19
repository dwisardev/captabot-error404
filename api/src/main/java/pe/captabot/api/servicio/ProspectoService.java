package pe.captabot.api.servicio;

import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.web.ProspectoRequest;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registro y consulta de prospectos (HU03, HU05 y HU08). */
@Service
public class ProspectoService {

    public static final int TAMANO_PAGINA = 20;
    static final int TAMANO_MAXIMO = 100;

    /** Mayor puntaje primero: los calientes salen arriba (HU08). */
    static final Sort ORDEN = Sort.by(Sort.Order.desc("puntaje"), Sort.Order.desc("creadoEn"), Sort.Order.desc("id"));

    private final ProspectoRepository repositorio;
    private final CalificacionService calificacionService;
    private final ApplicationEventPublisher eventos;

    public ProspectoService(ProspectoRepository repositorio, CalificacionService calificacionService,
                            ApplicationEventPublisher eventos) {
        this.repositorio = repositorio;
        this.calificacionService = calificacionService;
        this.eventos = eventos;
    }

    /** Registra el prospecto que llega del asistente y lo califica. */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud) {
        return registrar(solicitud, "CHAT_WEB");
    }

    /** Igual que registrar(solicitud), indicando el canal: CHAT_WEB o TELEGRAM. */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud, String canal) {
        return registrar(solicitud, canal, null);
    }

    /**
     * Registra el prospecto indicando el canal y el contacto del cliente en ese canal (chat de Telegram o número de
     * WhatsApp), que se usa para el seguimiento de HU13. Publica ProspectoRegistrado para avisar al equipo.
     */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud, String canal, String contacto) {
        Urgencia urgencia = Urgencia.desdeTexto(solicitud.urgencia());
        ResultadoCalificacion resultado = calificacionService.calificar(solicitud.presupuesto(), urgencia);

        Prospecto prospecto = new Prospecto();
        prospecto.setNombre(solicitud.nombre().trim());
        prospecto.setTelefono(solicitud.telefono().trim());
        prospecto.setServicioInteres(solicitud.servicioInteres().trim());
        prospecto.setPresupuesto(solicitud.presupuesto());
        prospecto.setUrgencia(urgencia);
        prospecto.setPuntaje(resultado.puntaje());
        prospecto.setCalificacion(resultado.calificacion());
        prospecto.setEtapa(Etapa.NUEVO);
        prospecto.setCanal(canal == null || canal.isBlank() ? "CHAT_WEB" : canal);
        if (contacto != null && !contacto.isBlank()) {
            if ("TELEGRAM".equals(prospecto.getCanal())) {
                prospecto.setTelegramChatId(numero(contacto));
            } else if ("WHATSAPP".equals(prospecto.getCanal())) {
                prospecto.setWhatsappId(contacto.trim());
            }
        }
        Prospecto guardado = repositorio.save(prospecto);
        eventos.publishEvent(new ProspectoRegistrado(guardado));
        return guardado;
    }

    /** Lista paginada de prospectos con filtros opcionales por calificación, etapa y fechas. */
    @Transactional(readOnly = true)
    public Page<Prospecto> listar(FiltroProspectos filtro, int pagina, int tamano) {
        if (pagina < 0) {
            throw new IllegalArgumentException("La página no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO);
        }
        Fechas.validarRango(filtro.desde(), filtro.hasta());
        return repositorio.findAll(conFiltro(filtro), PageRequest.of(pagina, tamano, ORDEN));
    }

    static Specification<Prospecto> conFiltro(FiltroProspectos filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.calificacion() != null) {
                condiciones.add(cb.equal(root.get("calificacion"), filtro.calificacion()));
            }
            if (filtro.etapa() != null) {
                condiciones.add(cb.equal(root.get("etapa"), filtro.etapa()));
            }
            if (filtro.desde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("creadoEn"),
                        Fechas.inicioDelDia(filtro.desde())));
            }
            if (filtro.hasta() != null) {
                condiciones.add(cb.lessThan(root.<OffsetDateTime>get("creadoEn"),
                        Fechas.inicioDelDia(filtro.hasta().plusDays(1))));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    @Transactional(readOnly = true)
    public Prospecto obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ProspectoNoEncontradoException(id));
    }

    private static Long numero(String texto) {
        try {
            return Long.parseLong(texto.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Indicadores de la lista del panel: por canal, por calificación, calientes sin contactar y nuevos de hoy. */
    @Transactional(readOnly = true)
    public ResumenProspectos resumen() {
        Map<String, Long> porCanal = new LinkedHashMap<>();
        for (String canal : List.of("CHAT_WEB", "TELEGRAM", "WHATSAPP")) {
            porCanal.put(canal, 0L);
        }
        for (Object[] fila : repositorio.contarPorCanal()) {
            porCanal.merge(String.valueOf(fila[0]), ((Number) fila[1]).longValue(), Long::sum);
        }
        Map<String, Long> porCalificacion = new LinkedHashMap<>();
        for (Calificacion c : Calificacion.values()) {
            porCalificacion.put(c.name(), 0L);
        }
        for (Object[] fila : repositorio.contarPorCalificacion()) {
            porCalificacion.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }
        long total = porCalificacion.values().stream().mapToLong(Long::longValue).sum();
        OffsetDateTime inicioDeHoy = LocalDate.now(Fechas.ZONA).atStartOfDay(Fechas.ZONA).toOffsetDateTime();
        return new ResumenProspectos(total, porCanal, porCalificacion,
                repositorio.countByCalificacionAndEtapa(Calificacion.CALIENTE, Etapa.NUEVO),
                repositorio.countByCreadoEnGreaterThanEqual(inicioDeHoy));
    }

    /**
     * Campana del panel: sin despuesDe solo informa el último id; con despuesDe devuelve los registrados después
     * (hasta 20, del más nuevo al más antiguo).
     */
    @Transactional(readOnly = true)
    public Recientes recientes(Long despuesDe) {
        List<Prospecto> nuevos = despuesDe == null ? List.of() : repositorio.findTop20ByIdGreaterThanOrderByIdDesc(despuesDe);
        return new Recientes(repositorio.ultimoId(), nuevos);
    }

    public record ResumenProspectos(long total, Map<String, Long> porCanal, Map<String, Long> porCalificacion,
                                    long calientesSinContactar, long nuevosHoy) {
    }

    public record Recientes(long ultimoId, List<Prospecto> prospectos) {
    }
}
