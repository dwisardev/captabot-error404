package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.captabot.api.modelo.Prospecto;

/** Acceso a la tabla prospectos. Los filtros de la lista se arman en ProspectoService. */
public interface ProspectoRepository extends JpaRepository<Prospecto, Long>, JpaSpecificationExecutor<Prospecto> {
}
