package pe.captabot.api.servicio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;
import pe.captabot.api.modelo.Urgencia;
import pe.captabot.api.repositorio.ProspectoRepository;
import pe.captabot.api.web.ProspectoRequest;

import java.util.List;

/** Registro y consulta de prospectos (HU03 y HU05). */
@Service
public class ProspectoService {

    private final ProspectoRepository repositorio;
    private final CalificacionService calificacionService;

    public ProspectoService(ProspectoRepository repositorio, CalificacionService calificacionService) {
        this.repositorio = repositorio;
        this.calificacionService = calificacionService;
    }

    /** Registra el prospecto que llega del asistente y lo califica. */
    @Transactional
    public Prospecto registrar(ProspectoRequest solicitud) {
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
        return repositorio.save(prospecto);
    }

    /** Lista los prospectos, con filtros opcionales por calificación y por etapa. */
    @Transactional(readOnly = true)
    public List<Prospecto> listar(Calificacion calificacion, Etapa etapa) {
        if (calificacion != null && etapa != null) {
            return repositorio.findByCalificacionAndEtapaOrderByPuntajeDescCreadoEnDesc(calificacion, etapa);
        }
        if (calificacion != null) {
            return repositorio.findByCalificacionOrderByPuntajeDescCreadoEnDesc(calificacion);
        }
        if (etapa != null) {
            return repositorio.findByEtapaOrderByPuntajeDescCreadoEnDesc(etapa);
        }
        return repositorio.findAllByOrderByPuntajeDescCreadoEnDesc();
    }

    @Transactional(readOnly = true)
    public Prospecto obtener(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new ProspectoNoEncontradoException(id));
    }
}
