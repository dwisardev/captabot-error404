package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.captabot.api.modelo.NotaSeguimiento;

import java.util.List;
import java.util.Optional;

public interface NotaSeguimientoRepository extends JpaRepository<NotaSeguimiento, Long> {

    /** De la más reciente a la más antigua (HU10). */
    List<NotaSeguimiento> findByProspectoIdOrderByCreadoEnDescIdDesc(Long prospectoId);

    Optional<NotaSeguimiento> findByIdAndProspectoId(Long id, Long prospectoId);
}
