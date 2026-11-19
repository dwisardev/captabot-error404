package pe.captabot.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.captabot.api.modelo.AvisoTelegram;

/** Acceso a la tabla avisos_telegram (vendedores que reciben avisos por Telegram). */
public interface AvisoTelegramRepository extends JpaRepository<AvisoTelegram, Long> {
}
