package pe.captabot.api.servicio;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.web.ProspectoRequest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro y consulta de prospectos (HU03, HU05 y HU08). */
@Service
public class ProspectoService {

    public static final int TAMANO_PAGINA = 20;
    static final int TAMANO_MAXIMO = 100;

    /** Mayor puntaje primero: los calientes salen arriba (HU08). */
    static final Sort ORDEN = Sort.by(Sort.Order.desc("puntaje"), Sort.Order.desc("creadoEn"), Sort.Order.desc("id"));

    private final ProspectoRepository repositorio;
    private final CalificacionService calificacionService;

    public ProspectoService(ProspectoRepository repositorio, CalificacionService calificacionService) {
        this.repositorio = repositorio;
        this.calificacionService = calificacionService;
    }

    /** Registra el prospecto que llega del asistente y lo califica. */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud) {
        return registrar(solicitud, "CHAT_WEB");
    }

    /** Igual que registrar(solicitud), indicando el canal: CHAT_WEB o TELEGRAM. */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud, String canal) {
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
        return repositorio.save(prospecto);
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
}
