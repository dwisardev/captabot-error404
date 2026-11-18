package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.captabot.api.modelo.HistorialEtapa;

import java.util.List;

public interface HistorialEtapaRepository extends JpaRepository<HistorialEtapa, Long> {

    List<HistorialEtapa> findByProspectoIdOrderByCambiadoEnDescIdDesc(Long prospectoId);
}
