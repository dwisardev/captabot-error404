package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.captabot.api.modelo.Usuario;

import java.util.List;
import java.util.Optional;

/** Acceso a la tabla usuarios. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    List<Usuario> findAllByOrderByNombreAsc();
}
