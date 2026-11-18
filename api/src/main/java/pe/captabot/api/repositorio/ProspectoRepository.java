package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.captabot.api.modelo.Calificacion;
import pe.captabot.api.modelo.Etapa;
import pe.captabot.api.modelo.Prospecto;

import java.util.List;

/** Acceso a la tabla prospectos. Las listas salen ordenadas con los de mayor puntaje primero. */
public interface ProspectoRepository extends JpaRepository<Prospecto, Long> {

    List<Prospecto> findAllByOrderByPuntajeDescCreadoEnDesc();

    List<Prospecto> findByCalificacionOrderByPuntajeDescCreadoEnDesc(Calificacion calificacion);

    List<Prospecto> findByEtapaOrderByPuntajeDescCreadoEnDesc(Etapa etapa);

    List<Prospecto> findByCalificacionAndEtapaOrderByPuntajeDescCreadoEnDesc(Calificacion calificacion,
                                                                              Etapa etapa);
}
